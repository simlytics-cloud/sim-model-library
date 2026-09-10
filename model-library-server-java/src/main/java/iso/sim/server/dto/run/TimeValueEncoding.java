package iso.sim.server.dto.run;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum TimeValueEncoding {
    INT64("int64"),
    DECIMAL_STRING("decimal-string"),
    FLOAT64("float64");

    private final String value;

    TimeValueEncoding(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static TimeValueEncoding fromValue(String value) {
        return Arrays.stream(values())
            .filter(encoding -> encoding.value.equals(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unsupported time value encoding: " + value));
    }
}
