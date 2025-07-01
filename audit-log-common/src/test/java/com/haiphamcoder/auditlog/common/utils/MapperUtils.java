package com.haiphamcoder.auditlog.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MapperUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static <T> T map(Object source, Class<T> targetClass) {
        return objectMapper.convertValue(source, targetClass);
    }

    public static String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert object to JSON", e);
        }
    }
}
