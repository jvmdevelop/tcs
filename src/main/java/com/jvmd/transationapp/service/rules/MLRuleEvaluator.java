package com.jvmd.transationapp.service.rules;

import ai.djl.Model;
import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.nn.Block;
import ai.djl.translate.Batchifier;
import ai.djl.translate.TranslateException;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import com.jvmd.fraud.model.FraudDetectionModel;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jvmd.transationapp.model.Rule;
import com.jvmd.transationapp.model.Transactions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Component
@Slf4j
public class MLRuleEvaluator {

    private final ObjectMapper objectMapper;
    
    @Value("${app.ml.model-path:ml-model/models}")
    private String modelBasePath;
    
    @Value("${app.ml.model-name:fraud-detection}")
    private String modelName;
    
    @Value("${app.ml.model-version:1.0}")
    private String modelVersion;
    
    @Value("${app.ml.threshold:0.7}")
    private double defaultThreshold;
    
    private Model model;
    private Predictor<float[], Float> predictor;
    private NDManager manager;
    private final int inputSize = 15; 
    private volatile boolean modelLoaded = false;
    private volatile boolean mlAvailable = true;

    public MLRuleEvaluator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void initModel() {
        try {
            testMLAvailability();
            loadModel();
            modelLoaded = true;
            log.info("ML model initialized successfully");
        } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
            mlAvailable = false;
            log.warn("ML engine (PyTorch) not available: {}. ML rules will be disabled.", e.getMessage());
            log.info("To enable ML rules, ensure PyTorch native libraries are installed.");
        } catch (Exception e) {
            log.warn("Failed to load ML model at startup: {}. ML rules will be disabled until model is loaded.", 
                    e.getMessage());
            log.debug("Model loading error details", e);
            modelLoaded = false;
        }
    }

    private void testMLAvailability() {
        try {
            if (manager == null) {
                manager = NDManager.newBaseManager();
            }
        } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
            mlAvailable = false;
            throw e;
        }
    }

    public synchronized void loadModel() throws IOException, MalformedModelException {
        if (!mlAvailable) {
            throw new IOException("ML engine not available. PyTorch native libraries may be missing.");
        }

        if (manager == null) {
            try {
                manager = NDManager.newBaseManager();
            } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
                mlAvailable = false;
                throw new IOException("ML engine not available. PyTorch native libraries may be missing.", e);
            }
        }
        
        closeResources();
        
        Path modelDir = Paths.get(modelBasePath);
        String fullModelName = modelName + "-" + modelVersion;
        

        if (!Files.exists(modelDir)) {
            throw new IOException("Model directory does not exist: " + modelDir.toAbsolutePath());
        }
        
        Path paramsFile = modelDir.resolve(fullModelName + "-0000.params");
        if (!Files.exists(paramsFile)) {
            throw new IOException("Model params file not found: " + paramsFile.toAbsolutePath() + 
                    ". Please train the model first using ml-model project.");
        }
        
        try {

            model = Model.newInstance(modelName);
            

            Block block = createFraudDetectionBlock();
            model.setBlock(block);
            

            model.load(modelDir, fullModelName);
            
            predictor = model.newPredictor(new FraudTranslator());
            
            log.info("ML model loaded successfully from: {} (version: {})", modelDir.toAbsolutePath(), modelVersion);
            modelLoaded = true;
            
        } catch (Exception e) {
            closeResources();
            throw new IOException("Failed to load model: " + e.getMessage(), e);
        }
    }

    private Block createFraudDetectionBlock() throws IOException {
        Path modelDir = Paths.get(modelBasePath);
        String fullModelName = modelName + "-" + modelVersion;
        Path modelPath = modelDir.resolve(fullModelName + "-0000.params");

        FraudDetectionModel fraudModel = new FraudDetectionModel(inputSize, new int[]{64, 32});
        return fraudModel.newBlock(model, modelPath, null);
    }

    public boolean isModelLoaded() {
        return mlAvailable && modelLoaded && predictor != null;
    }
    
    private void closeResources() {
        if (predictor != null) {
            predictor.close();
            predictor = null;
        }
        if (model != null) {
            model.close();
            model = null;
        }
    }

    public boolean evaluate(Rule rule, Transactions transaction) {
        try {
            if (!mlAvailable) {
                log.debug("ML engine not available, skipping ML rule evaluation");
                return false;
            }
            if (!isModelLoaded()) {
                log.warn("ML model not loaded, skipping ML rule evaluation");
                return false;
            }
            Map<String, Object> config = objectMapper.readValue(
                    rule.getConfiguration(),
                    new TypeReference<Map<String, Object>>() {
                    }
            );
            double threshold = config.containsKey("threshold")
                    ? ((Number) config.get("threshold")).doubleValue()
                    : defaultThreshold;
            float[] features = extractFeatures(transaction);
            Float prediction = predictor.predict(features);
            transaction.setMlScore(prediction.doubleValue());
            boolean triggered = prediction >= threshold;
            log.debug("ML prediction: score={}, threshold={}, triggered={}",
                    prediction, threshold, triggered);
            return triggered;
        } catch (TranslateException e) {
            log.error("Error during ML inference: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Error evaluating ML rule: {}", e.getMessage());
            return false;
        }
    }

    private float[] extractFeatures(Transactions transaction) {
        float normalizedAmount = Math.min(1.0f, transaction.getAmount().floatValue() / 10_000f);
        
        float normalizedHour = transaction.getTimestamp().getHour() / 24.0f;
        float normalizedDay = (transaction.getTimestamp().getDayOfWeek().getValue() - 1) / 6.0f;
        
        float accountFromHash = hashAccount(transaction.getFrom());
        float accountToHash = hashAccount(transaction.getTo());
        
        float transactionTypeEncoded = encodeTransactionType(transaction.getType());
        
        float merchantCategoryEncoded = encodeMerchantCategory(
                transaction.getMerchantCategory());
        
        float deviceUsedEncoded = encodeDeviceUsed(transaction.getDeviceUsed());
        
        float locationHash = hashString(transaction.getLocation());
        
        float timeSinceLastTx = transaction.getTimeSinceLastTransaction() != null
                ? transaction.getTimeSinceLastTransaction().floatValue() : 0.0f;
        
        float spendingDeviation = transaction.getSpendingDeviationScore() != null
                ? transaction.getSpendingDeviationScore().floatValue() : 0.0f;
        
        float velocityScore = transaction.getVelocityScore() != null
                ? Math.min(1.0f, transaction.getVelocityScore().floatValue() / 50.0f) : 0.0f;
        
        float geoAnomalyScore = transaction.getGeoAnomalyScore() != null
                ? transaction.getGeoAnomalyScore().floatValue() : 0.0f;
        
        float paymentChannelEncoded = encodePaymentChannel(transaction.getPaymentChannel());
        
        float ipRiskScore = calculateIpRiskScore(transaction.getIpAddress());
        
        return new float[]{
                normalizedAmount,
                normalizedHour,
                normalizedDay,
                accountFromHash,
                accountToHash,
                transactionTypeEncoded,
                merchantCategoryEncoded,
                deviceUsedEncoded,
                locationHash,
                timeSinceLastTx,
                spendingDeviation,
                velocityScore,
                geoAnomalyScore,
                paymentChannelEncoded,
                ipRiskScore
        };
    }

    private float hashAccount(String account) {
        if (account == null || account.isEmpty()) {
            return 0.5f;
        }
        return Math.abs(account.hashCode() % 1000) / 1000.0f;
    }

    private float hashString(String str) {
        if (str == null || str.isEmpty()) {
            return 0.5f;
        }
        return Math.abs(str.hashCode() % 1000) / 1000.0f;
    }

    private float encodeTransactionType(String type) {
        if (type == null) return 0.5f;
        return switch (type.toLowerCase()) {
            case "withdrawal" -> 0.25f;
            case "deposit" -> 0.5f;
            case "transfer" -> 0.75f;
            case "payment" -> 1.0f;
            default -> 0.5f;
        };
    }

    private float encodeMerchantCategory(String category) {
        if (category == null) return 0.5f;
        return switch (category.toLowerCase()) {
            case "utilities" -> 0.1f;
            case "online" -> 0.2f;
            case "grocery" -> 0.3f;
            case "restaurant" -> 0.4f;
            case "entertainment" -> 0.5f;
            case "travel" -> 0.6f;
            case "gas" -> 0.7f;
            case "retail" -> 0.8f;
            case "other" -> 0.9f;
            default -> 0.5f;
        };
    }

    private float encodeDeviceUsed(String device) {
        if (device == null) return 0.5f;
        return switch (device.toLowerCase()) {
            case "mobile" -> 0.25f;
            case "atm" -> 0.5f;
            case "pos" -> 0.75f;
            case "online" -> 1.0f;
            default -> 0.5f;
        };
    }

    private float encodePaymentChannel(String channel) {
        if (channel == null) return 0.5f;
        return switch (channel.toLowerCase()) {
            case "card" -> 0.25f;
            case "ach" -> 0.5f;
            case "wire_transfer" -> 0.75f;
            case "cash" -> 1.0f;
            default -> 0.5f;
        };
    }

    private float calculateIpRiskScore(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return 0.5f;
        }
        if (ipAddress.startsWith("192.168.") ||
                ipAddress.startsWith("10.") ||
                ipAddress.startsWith("172.")) {
            return 0.1f;
        }
        return Math.abs(ipAddress.hashCode() % 100) / 200.0f;
    }

    @PreDestroy
    public void cleanup() {
        log.info("Cleaning up ML model resources");
        closeResources();
        if (manager != null) {
            manager.close();
        }
    }

    private static class FraudTranslator implements Translator<float[], Float> {
        @Override
        public NDList processInput(TranslatorContext ctx, float[] input) {
            NDManager manager = ctx.getNDManager();
            NDArray array = manager.create(input).expandDims(0);
            return new NDList(array);
        }

        @Override
        public Float processOutput(TranslatorContext ctx, NDList list) {
            NDArray output = list.singletonOrThrow();
            return output.getFloat();
        }
        
        @Override
        public Batchifier getBatchifier() {
            return Batchifier.STACK;
        }
    }
}
