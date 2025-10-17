package com.jvmd.transationapp.service.rules;

import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.TranslateException;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
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
import java.nio.file.Paths;
import java.util.Map;

@Component
@Slf4j
public class MLRuleEvaluator {

    private final ObjectMapper objectMapper;
    @Value("${app.ml.model-path:models/fraud-detection-1.0-0000.params}")
    private String defaultModelPath;
    @Value("${app.ml.threshold:0.7}")
    private double defaultThreshold;
    private ZooModel<float[], Float> model;
    private Predictor<float[], Float> predictor;

    public MLRuleEvaluator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void initModel() {
        try {
            loadModel(defaultModelPath);
        } catch (Exception e) {
            log.warn("Failed to load ML model at startup: {}. ML rules will be disabled until model is loaded.",
                    e.getMessage());
        }
    }

    public synchronized void loadModel(String modelPath) throws ModelNotFoundException, MalformedModelException, IOException {
        if (predictor != null) {
            predictor.close();
        }
        if (model != null) {
            model.close();
        }
        Criteria<float[], Float> criteria = Criteria.builder()
                .setTypes(float[].class, Float.class)
                .optModelPath(Paths.get(modelPath))
                .optTranslator(new FraudTranslator())
                .optProgress(new ProgressBar())
                .build();
        model = criteria.loadModel();
        predictor = model.newPredictor();
        log.info("ML model loaded successfully from: {}", modelPath);
    }

    public boolean evaluate(Rule rule, Transactions transaction) {
        try {
            if (predictor == null) {
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
        float normalizedAmount = transaction.getAmount().floatValue() / 1_000_000f;
        normalizedAmount = Math.min(1.0f, normalizedAmount);
        float normalizedHour = transaction.getTimestamp().getHour() / 24.0f;
        float normalizedDay = (transaction.getTimestamp().getDayOfWeek().getValue() - 1) / 6.0f;
        float ipRiskScore = calculateIpRiskScore(transaction.getIpAddress());
        float locationRisk = calculateLocationRisk(transaction.getLocation());
        return new float[]{
                normalizedAmount,
                normalizedHour,
                normalizedDay,
                hashAccount(transaction.getFrom()),
                hashAccount(transaction.getTo()),
                hashType(transaction.getType()),
                ipRiskScore,
                locationRisk
        };
    }

    private float hashAccount(String account) {
        if (account == null || account.isEmpty()) {
            return 0.5f;
        }
        return Math.abs(account.hashCode() % 1000) / 1000.0f;
    }

    private float hashType(String type) {
        return switch (type) {
            case "TRANSFER" -> 0.25f;
            case "PAYMENT" -> 0.5f;
            case "WITHDRAWAL" -> 0.75f;
            case "DEPOSIT" -> 1.0f;
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
        float hash = Math.abs(ipAddress.hashCode() % 100) / 100.0f;
        return hash * 0.5f;
    }

    private float calculateLocationRisk(String location) {
        if (location == null || location.isEmpty()) {
            return 0.3f;
        }
        String lowerLocation = location.toLowerCase();
        if (lowerLocation.contains("moscow") ||
                lowerLocation.contains("petersburg") ||
                lowerLocation.contains("atm")) {
            return 0.1f;
        }
        if (lowerLocation.contains("unknown") ||
                lowerLocation.equals("n/a")) {
            return 0.6f;
        }
        return 0.3f;
    }

    @PreDestroy
    public void cleanup() {
        if (predictor != null) {
            predictor.close();
        }
        if (model != null) {
            model.close();
        }
    }

    private static class FraudTranslator implements Translator<float[], Float> {
        @Override
        public NDList processInput(TranslatorContext ctx, float[] input) {
            NDManager manager = ctx.getNDManager();
            NDArray array = manager.create(input);
            return new NDList(array);
        }

        @Override
        public Float processOutput(TranslatorContext ctx, NDList list) {
            NDArray output = list.singletonOrThrow();
            return output.toFloatArray()[0];
        }
    }
}
