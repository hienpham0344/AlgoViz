package com.aisc.algoviz.problem.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum biểu diễn 3 cấp độ khó của bài toán theo chuẩn hệ thống:
 * - EASY (Xanh lá - #10B981)
 * - MEDIUM (Vàng cam - #F59E0B)
 * - HARD (Đỏ / Tím đậm)
 */
public enum Difficulty {
    EASY("Easy"),
    MEDIUM("Medium"),
    HARD("Hard");

    private final String value;

    Difficulty(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static Difficulty fromString(String text) {
        if (text == null) {
            return null;
        }
        for (Difficulty d : Difficulty.values()) {
            if (d.value.equalsIgnoreCase(text) || d.name().equalsIgnoreCase(text)) {
                return d;
            }
        }
        throw new IllegalArgumentException("Unknown difficulty level: " + text);
    }
}
