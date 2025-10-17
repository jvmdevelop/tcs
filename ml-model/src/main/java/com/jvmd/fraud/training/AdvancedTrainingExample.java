package com.jvmd.fraud.training;
import java.util.List;
import java.util.Random;
public class AdvancedTrainingExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Advanced Fraud Detection Training ===\n");
        Random random = new Random(42);
        int trainSize = 2000;
        int testSize = 500;
        System.out.println("Generating training data: " + trainSize + " samples");
        FraudDetectionService.DatasetPair trainDataset = 
            FraudDetectionService.generateSyntheticDataset(trainSize, random);
        System.out.println("Generating test data: " + testSize + " samples\n");
        FraudDetectionService.DatasetPair testDataset = 
            FraudDetectionService.generateSyntheticDataset(testSize, random);
        TrainingScenario[] scenarios = {
            new TrainingScenario("Quick Training", 20, 64, 0.01f),
            new TrainingScenario("Balanced Training", 50, 32, 0.001f),
            new TrainingScenario("Deep Training", 100, 16, 0.0005f)
        };
        for (TrainingScenario scenario : scenarios) {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("Scenario: " + scenario.name);
            System.out.println("=".repeat(60));
            FraudDetectionService service = new FraudDetectionService();
            long startTime = System.currentTimeMillis();
            FraudDetectionService.TrainingResult result = service.trainModel(
                trainDataset.data,
                trainDataset.labels,
                scenario.epochs,
                scenario.batchSize,
                scenario.learningRate
            );
            long duration = System.currentTimeMillis() - startTime;
            float testAccuracy = evaluateModel(service, testDataset);
            System.out.println("\n--- Summary ---");
            System.out.printf("Training Time: %.2f seconds\n", duration / 1000.0);
            System.out.printf("Final Training Loss: %.4f\n", result.getFinalLoss());
            System.out.printf("Final Training Accuracy: %.2f%%\n", result.getFinalAccuracy() * 100);
            System.out.printf("Test Accuracy: %.2f%%\n", testAccuracy * 100);
            printLearningCurve(result, 5);
            service.close();
        }
        System.out.println("\n" + "=".repeat(60));
        System.out.println("All training scenarios completed!");
        System.out.println("=".repeat(60));
    }
    private static float evaluateModel(FraudDetectionService service, 
                                       FraudDetectionService.DatasetPair testData) throws Exception {
        int correct = 0;
        for (int i = 0; i < testData.data.size(); i++) {
            float prediction = service.predictAsync(testData.data.get(i)).get();
            int predictedClass = prediction > 0.5f ? 1 : 0;
            int actualClass = (int) testData.labels[i];
            if (predictedClass == actualClass) {
                correct++;
            }
        }
        return (float) correct / testData.data.size();
    }
    private static void printLearningCurve(FraudDetectionService.TrainingResult result, int numPoints) {
        System.out.println("\n--- Learning Curve ---");
        List<Float> losses = result.getEpochLosses();
        List<Float> accuracies = result.getEpochAccuracies();
        int step = Math.max(1, losses.size() / numPoints);
        System.out.printf("%-10s %-15s %-15s\n", "Epoch", "Loss", "Accuracy");
        System.out.println("-".repeat(40));
        for (int i = 0; i < losses.size(); i += step) {
            System.out.printf("%-10d %-15.4f %-15.2f%%\n", 
                i + 1, losses.get(i), accuracies.get(i) * 100);
        }
        if ((losses.size() - 1) % step != 0) {
            int last = losses.size() - 1;
            System.out.printf("%-10d %-15.4f %-15.2f%%\n", 
                last + 1, losses.get(last), accuracies.get(last) * 100);
        }
    }
    static class TrainingScenario {
        String name;
        int epochs;
        int batchSize;
        float learningRate;
        TrainingScenario(String name, int epochs, int batchSize, float learningRate) {
            this.name = name;
            this.epochs = epochs;
            this.batchSize = batchSize;
            this.learningRate = learningRate;
        }
    }
}
