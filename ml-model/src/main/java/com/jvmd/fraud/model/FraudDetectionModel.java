package com.jvmd.fraud.model;
import ai.djl.Model;
import ai.djl.nn.Block;
import ai.djl.nn.BlockFactory;
import ai.djl.nn.SequentialBlock;
import ai.djl.nn.core.Linear;
import ai.djl.nn.Activation;
import ai.djl.nn.norm.Dropout;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
public class FraudDetectionModel implements BlockFactory {
    private final int inputSize;
    private final int[] hiddenLayers;
    public FraudDetectionModel(int inputSize, int[] hiddenLayers) {
        this.inputSize = inputSize;
        this.hiddenLayers = hiddenLayers;
    }
    @Override
    public Block newBlock(Model model, Path modelPath, Map<String, ?> arguments) throws IOException {
        SequentialBlock block = new SequentialBlock();
        for (int layerSize : hiddenLayers) {
            block.add(Linear.builder().setUnits(layerSize).build());
            block.add(Activation::relu);
            block.add(Dropout.builder().optRate(0.2f).build());
        }
        block.add(Linear.builder().setUnits(1).build());
        block.add(Activation::sigmoid);
        return block;
    }
}
