package com.cloud.arch.rocket.domain;

public enum MessageModel {
    BROADCASTING("BROADCASTING"),
    CLUSTERING("CLUSTERING");

    private final String model;

    MessageModel(String model) {
        this.model = model;
    }

    public String getModel() {
        return model;
    }
}
