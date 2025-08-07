package com.playdata.certificateservice.entity; // 패키지 경로는 그대로 유지

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