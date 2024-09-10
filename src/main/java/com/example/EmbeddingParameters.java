package com.example;

public class EmbeddingParameters {
    private int outputDimensionality;

    public EmbeddingParameters(int outputDimensionality) {
        this.outputDimensionality = outputDimensionality;
    }

    public int getOutputDimensionality() {
        return outputDimensionality;
    }

    public void setOutputDimensionality(int outputDimensionality) {
        this.outputDimensionality = outputDimensionality;
    }
}