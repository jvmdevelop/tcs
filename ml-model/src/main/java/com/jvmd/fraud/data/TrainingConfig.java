package com.jvmd.fraud.data;

/**
 * Configuration class for model training
 */
public class TrainingConfig {
    private int epochs = 50;
    private int batchSize = 32;
    private float learningRate = 0.001f;
    private float validationSplit = 0.2f;
    private boolean earlyStoppingEnabled = false;
    private int earlyStoppingPatience = 10;
    private float earlyStoppingMinDelta = 0.0001f;
    
    public TrainingConfig() {
    }
    
    public TrainingConfig(int epochs, int batchSize, float learningRate) {
        this.epochs = epochs;
        this.batchSize = batchSize;
        this.learningRate = learningRate;
    }

    public static class Builder {
        private final TrainingConfig config = new TrainingConfig();
        
        public Builder epochs(int epochs) {
            config.epochs = epochs;
            return this;
        }
        
        public Builder batchSize(int batchSize) {
            config.batchSize = batchSize;
            return this;
        }
        
        public Builder learningRate(float learningRate) {
            config.learningRate = learningRate;
            return this;
        }
        
        public Builder validationSplit(float validationSplit) {
            config.validationSplit = validationSplit;
            return this;
        }
        
        public Builder enableEarlyStopping(int patience, float minDelta) {
            config.earlyStoppingEnabled = true;
            config.earlyStoppingPatience = patience;
            config.earlyStoppingMinDelta = minDelta;
            return this;
        }
        
        public TrainingConfig build() {
            return config;
        }
    }

    public int getEpochs() {
        return epochs;
    }
    
    public int getBatchSize() {
        return batchSize;
    }
    
    public float getLearningRate() {
        return learningRate;
    }
    
    public float getValidationSplit() {
        return validationSplit;
    }
    
    public boolean isEarlyStoppingEnabled() {
        return earlyStoppingEnabled;
    }
    
    public int getEarlyStoppingPatience() {
        return earlyStoppingPatience;
    }
    
    public float getEarlyStoppingMinDelta() {
        return earlyStoppingMinDelta;
    }
    
    @Override
    public String toString() {
        return String.format("TrainingConfig{epochs=%d, batchSize=%d, learningRate=%.4f, " +
                "validationSplit=%.2f, earlyStoppingEnabled=%b}", 
                epochs, batchSize, learningRate, validationSplit, earlyStoppingEnabled);
    }
}
