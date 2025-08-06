package com.playdata.certificateservice.client.dto;

public enum Type {
    EMPLOYMENT("재직증명서"),
    CAREER("경력증명서");

    private final String displayName;

    Type(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
