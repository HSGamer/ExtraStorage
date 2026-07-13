package me.hsgamer.extrastorage.configs.converters;

import io.github.projectunified.craftconfig.annotation.converter.Converter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class StringMapConverter implements Converter {
    @Override
    public Object convert(Object raw) {
        if (raw instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) raw;
            Map<String, String> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                result.put(entry.getKey().toString(), entry.getValue().toString());
            }
            return result;
        }
        return Collections.emptyMap();
    }

    @Override
    public Object convertToRaw(Object value) {
        return value;
    }
}
