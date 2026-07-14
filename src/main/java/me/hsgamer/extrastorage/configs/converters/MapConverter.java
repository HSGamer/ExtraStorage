package me.hsgamer.extrastorage.configs.converters;

import io.github.projectunified.craftconfig.annotation.converter.Converter;

import java.util.LinkedHashMap;
import java.util.Map;

public class MapConverter implements Converter {
    @Override
    public Object convert(Object raw) {
        if (raw instanceof Map) {
            Map<?, ?> src = (Map<?, ?>) raw;
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : src.entrySet()) {
                result.put(entry.getKey().toString(), entry.getValue());
            }
            return result;
        }
        return raw;
    }

    @Override
    public Object convertToRaw(Object value) {
        return value;
    }
}
