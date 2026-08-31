package com.aisc.algoviz.problem.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;



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
        throw new IllegalArgumentException(ProblemErrorCode.INVALID_DIFFICULTY);
    }
}
