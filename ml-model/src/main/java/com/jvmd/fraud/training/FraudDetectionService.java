package com.jvmd.fraud.training;

import ai.djl.Model;
import ai.djl.inference.Predictor;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.DataType;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.Block;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.EasyTrain;
import ai.djl.training.Trainer;
import ai.djl.training.dataset.ArrayDataset;
import ai.djl.training.dataset.Batch;
import ai.djl.training.evaluator.Accuracy;
import ai.djl.training.listener.TrainingListener;
import ai.djl.training.loss.Loss;
import ai.djl.training.optimizer.Optimizer;
import ai.djl.training.tracker.Tracker;
import ai.djl.translate.Batchifier;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import com.jvmd.fraud.model.FraudDetectionModel;
import com.jvmd.fraud.data.TransactionData;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class FraudDetectionService {
    private final Map<String, Model> modelVersions = new ConcurrentHashMap<>();
    private final String modelDir = "ml-model/models";
    private String currentVersion = "1.0";
    private Predictor<float[], Float> predictor;
    private final NDManager manager = NDManager.newBaseManager();
    private final int inputSize = 8;
    
    public FraudDetectionService() {
        Path modelsPath = Paths.get(modelDir);
        try {
            if (!modelsPath.toFile().exists()) {
                modelsPath.toFile().mkdirs();
                System.out.println("Created models directory: " + modelsPath.toAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not create models directory: " + e.getMessage());
        }
    }

    public void initialize() throws Exception {
        loadModel(currentVersion);
    }

    private synchronized void loadModel(String version) throws Exception {
        if (modelVersions.containsKey(version)) {
            if (predictor != null) {
                predictor.close();
            }
            Model model = modelVersions.get(version);
            predictor = model.newPredictor(new TransactionTranslator());
            currentVersion = version;
            return;
        }
        Model model = Model.newInstance("fraud-detection");
        Path paramsPath = Paths.get(modelDir, "fraud-detection-" + version + ".pt");
        Path modelPath = Paths.get(modelDir, "fraud-detection-" + version);
        Block block = new FraudDetectionModel(inputSize, new int[]{64, 32}).newBlock(model, paramsPath, null);
        model.setBlock(block);
        if (modelPath.toFile().exists()) {
            model.load(modelPath.getParent(), "fraud-detection-" + version);
        } else {
            model.getBlock().initialize(manager, DataType.FLOAT32, new Shape(1, inputSize));
            model.save(modelPath.getParent(), "fraud-detection-" + version);
        }
        modelVersions.put(version, model);
        if (predictor != null) {
            predictor.close();
        }
        predictor = model.newPredictor(new TransactionTranslator());
        currentVersion = version;
    }

    public CompletableFuture<Float> predictAsync(TransactionData transaction) {
        float[] features = preprocess(transaction);
        return CompletableFuture.supplyAsync(() -> {
            try {
                return predictor.predict(features);
            } catch (Exception e) {
                throw new RuntimeException("Prediction failed", e);
            }
        });
    }

    private float[] preprocess(TransactionData transaction) {
        return new float[]{
                (float) transaction.getAmount(),
                (float) transaction.getHourOfDay(),
                (float) transaction.getDayOfWeek(),
                (float) transaction.getAccountFromHash(),
                (float) transaction.getAccountToHash(),
                (float) transaction.getTransactionType(),
                (float) transaction.getIpRiskScore(),
                (float) transaction.getLocationRisk()
        };
    }

    public TrainingResult trainModel(List<TransactionData> trainingData, float[] labels,
                                     int epochs, int batchSize, float learningRate) throws Exception {
        if (trainingData.size() != labels.length) {
            throw new IllegalArgumentException("Training data and labels size mismatch");
        }
        System.out.println("=== Starting Training ===");
        System.out.println("Training samples: " + trainingData.size());
        System.out.println("Epochs: " + epochs);
        System.out.println("Batch size: " + batchSize);
        System.out.println("Learning rate: " + learningRate);
        float[][] features = new float[trainingData.size()][inputSize];
        for (int i = 0; i < trainingData.size(); i++) {
            features[i] = preprocess(trainingData.get(i));
        }
        Model model = Model.newInstance("fraud-detection-training");
        Path modelPath = Paths.get(modelDir, "fraud-detection-" + currentVersion + ".pt");
        Block block = new FraudDetectionModel(inputSize, new int[]{64, 32}).newBlock(model, modelPath, null);
        model.setBlock(block);
        DefaultTrainingConfig config = new DefaultTrainingConfig(Loss.sigmoidBinaryCrossEntropyLoss())
                .optOptimizer(Optimizer.adam()
                        .optLearningRateTracker(Tracker.fixed(learningRate))
                        .build())
                .addEvaluator(new Accuracy())
                .addTrainingListeners(TrainingListener.Defaults.logging());
        TrainingResult result = new TrainingResult();
        try (Trainer trainer = model.newTrainer(config)) {
            trainer.initialize(new Shape(batchSize, inputSize));
            NDArray featuresArray = manager.create(features);
            NDArray labelsArray = manager.create(labels).reshape(-1, 1);
            ArrayDataset dataset = new ArrayDataset.Builder()
                    .setData(featuresArray)
                    .optLabels(labelsArray)
                    .setSampling(batchSize, false)
                    .build();
            for (int epoch = 0; epoch < epochs; epoch++) {
                for (Batch batch : trainer.iterateDataset(dataset)) {
                    EasyTrain.trainBatch(trainer, batch);
                    trainer.step();
                    batch.close();
                }
                float loss = calculateLoss(features, labels, model);
                float accuracy = calculateAccuracy(features, labels, model);
                result.addEpochLoss(loss);
                result.addEpochAccuracy(accuracy);
                System.out.printf("Epoch %d/%d - Loss: %.4f, Accuracy: %.2f%%\n",
                        epoch + 1, epochs, loss, accuracy * 100);
            }
            featuresArray.close();
            labelsArray.close();
        }
        Path savePath = Paths.get(modelDir);
        model.save(savePath, "fraud-detection-" + currentVersion);
        System.out.println("Model saved to: " + savePath);
        modelVersions.put(currentVersion, model);
        if (predictor != null) {
            predictor.close();
        }
        predictor = model.newPredictor(new TransactionTranslator());
        System.out.println("=== Training Complete ===");
        return result;
    }

    private float calculateAccuracy(float[][] features, float[] labels, Model model) throws Exception {
        try (Predictor<float[], Float> tempPredictor = model.newPredictor(new TransactionTranslator())) {
            int correct = 0;
            for (int i = 0; i < features.length; i++) {
                float prediction = tempPredictor.predict(features[i]);
                int predictedClass = prediction > 0.5f ? 1 : 0;
                int actualClass = (int) labels[i];
                if (predictedClass == actualClass) {
                    correct++;
                }
            }
            return (float) correct / features.length;
        }
    }

    private float calculateLoss(float[][] features, float[] labels, Model model) throws Exception {
        try (Predictor<float[], Float> tempPredictor = model.newPredictor(new TransactionTranslator())) {
            float totalLoss = 0f;
            float epsilon = 1e-7f;
            for (int i = 0; i < features.length; i++) {
                float prediction = tempPredictor.predict(features[i]);
                float label = labels[i];
                prediction = Math.max(epsilon, Math.min(1 - epsilon, prediction));
                float loss = -(label * (float) Math.log(prediction) + (1 - label) * (float) Math.log(1 - prediction));
                totalLoss += loss;
            }
            return totalLoss / features.length;
        }
    }

    public static class DatasetPair {
        public final List<TransactionData> data;
        public final float[] labels;

        public DatasetPair(List<TransactionData> data, float[] labels) {
            this.data = data;
            this.labels = labels;
        }
    }

    public static DatasetPair generateSyntheticDataset(int numSamples, Random random) {
        List<TransactionData> data = new ArrayList<>();
        float[] labels = new float[numSamples];
        for (int i = 0; i < numSamples; i++) {
            TransactionData tx = new TransactionData();
            boolean isFraud = random.nextFloat() < 0.2;
            labels[i] = isFraud ? 1.0f : 0.0f;
            if (isFraud) {
                tx.setAmount(random.nextFloat() * 0.7 + 0.3);
                tx.setHourOfDay((random.nextFloat() * 0.3) + 0.05);
                tx.setDayOfWeek(random.nextFloat());
                tx.setAccountFromHash(random.nextFloat());
                tx.setAccountToHash(random.nextFloat());
                tx.setTransactionType(random.nextFloat() * 0.5);
                tx.setIpRiskScore(random.nextFloat() * 0.4 + 0.6);
                tx.setLocationRisk(random.nextFloat() * 0.4 + 0.6);
            } else {
                tx.setAmount(random.nextFloat() * 0.4 + 0.05);
                tx.setHourOfDay((random.nextFloat() * 0.5) + 0.35);
                tx.setDayOfWeek(random.nextFloat());
                tx.setAccountFromHash(random.nextFloat() * 0.5);
                tx.setAccountToHash(random.nextFloat() * 0.5);
                tx.setTransactionType(random.nextFloat() * 0.3 + 0.5);
                tx.setIpRiskScore(random.nextFloat() * 0.3);
                tx.setLocationRisk(random.nextFloat() * 0.3);
            }
            data.add(tx);
        }
        return new DatasetPair(data, labels);
    }

    public void close() {
        if (predictor != null) {
            predictor.close();
        }
        modelVersions.values().forEach(Model::close);
        manager.close();
    }

    public static class TrainingResult {
        private final List<Float> epochLosses = new ArrayList<>();
        private final List<Float> epochAccuracies = new ArrayList<>();

        public void addEpochLoss(float loss) {
            epochLosses.add(loss);
        }

        public void addEpochAccuracy(float accuracy) {
            epochAccuracies.add(accuracy);
        }

        public List<Float> getEpochLosses() {
            return epochLosses;
        }

        public List<Float> getEpochAccuracies() {
            return epochAccuracies;
        }

        public float getFinalLoss() {
            return epochLosses.isEmpty() ? 0 : epochLosses.get(epochLosses.size() - 1);
        }

        public float getFinalAccuracy() {
            return epochAccuracies.isEmpty() ? 0 : epochAccuracies.get(epochAccuracies.size() - 1);
        }
    }

    private static class TransactionTranslator implements Translator<float[], Float> {
        @Override
        public NDList processInput(TranslatorContext ctx, float[] input) {
            NDManager manager = ctx.getNDManager();
            return new NDList(manager.create(input).expandDims(0));
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

    public static void main(String[] args) throws Exception {
        FraudDetectionService service = new FraudDetectionService();
        Random random = new Random(42);
        int numTrainingSamples = 1000;
        System.out.println("Generating " + numTrainingSamples + " synthetic training samples...");
        DatasetPair dataset = generateSyntheticDataset(numTrainingSamples, random);
        List<TransactionData> trainingData = dataset.data;
        float[] labels = dataset.labels;
        int epochs = 50;
        int batchSize = 32;
        float learningRate = 0.001f;
        TrainingResult result = service.trainModel(trainingData, labels, epochs, batchSize, learningRate);
        System.out.println("\n=== Training Results ===");
        System.out.printf("Final Loss: %.4f\n", result.getFinalLoss());
        System.out.printf("Final Accuracy: %.2f%%\n\n", result.getFinalAccuracy() * 100);
        System.out.println("=== Testing Trained Model ===");
        TransactionData normalTx = new TransactionData();
        normalTx.setAmount(0.2);
        normalTx.setHourOfDay(0.5);
        normalTx.setDayOfWeek(0.5);
        normalTx.setAccountFromHash(0.25);
        normalTx.setAccountToHash(0.3);
        normalTx.setTransactionType(0.75);
        normalTx.setIpRiskScore(0.1);
        normalTx.setLocationRisk(0.15);
        float normalProb = service.predictAsync(normalTx).get();
        System.out.printf("Normal transaction - Fraud probability: %.4f (Predicted: %s)\n",
                normalProb, normalProb > 0.5 ? "FRAUD" : "NORMAL");
        TransactionData fraudTx = new TransactionData();
        fraudTx.setAmount(0.95);
        fraudTx.setHourOfDay(0.1);
        fraudTx.setDayOfWeek(0.85);
        fraudTx.setAccountFromHash(0.8);
        fraudTx.setAccountToHash(0.9);
        fraudTx.setTransactionType(0.25);
        fraudTx.setIpRiskScore(0.92);
        fraudTx.setLocationRisk(0.88);
        float fraudProb = service.predictAsync(fraudTx).get();
        System.out.printf("Suspicious transaction - Fraud probability: %.4f (Predicted: %s)\n",
                fraudProb, fraudProb > 0.5 ? "FRAUD" : "NORMAL");
        System.out.println("\n=== Training Progress ===");
        List<Float> losses = result.getEpochLosses();
        List<Float> accuracies = result.getEpochAccuracies();
        for (int i = 0; i < Math.min(10, losses.size()); i++) {
            System.out.printf("Epoch %d: Loss=%.4f, Accuracy=%.2f%%\n",
                    i + 1, losses.get(i), accuracies.get(i) * 100);
        }
        if (losses.size() > 10) {
            System.out.println("...");
            int lastIdx = losses.size() - 1;
            System.out.printf("Epoch %d: Loss=%.4f, Accuracy=%.2f%%\n",
                    lastIdx + 1, losses.get(lastIdx), accuracies.get(lastIdx) * 100);
        }
        service.close();
        System.out.println("\nTraining and testing complete!");
    }
}
