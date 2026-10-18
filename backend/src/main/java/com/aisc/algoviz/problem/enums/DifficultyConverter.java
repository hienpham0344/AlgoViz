package com.aisc.algoviz.problem.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Shares the difficulty values used by the existing API and the JSON importer. */
@Converter
public class DifficultyConverter implements AttributeConverter<Difficulty, String> {
    @Override
    public String convertToDatabaseColumn(Difficulty difficulty) {
        return difficulty == null ? null : difficulty.getValue();
    }

    @Override
    public Difficulty convertToEntityAttribute(String value) {
        return Difficulty.fromString(value);
    }
}
