package me.hsgamer.extrastorage.configs.converters;

import io.github.projectunified.craftconfig.annotation.converter.Converter;

import java.util.LinkedHashMap;
import java.util.Map;

public class MapMapConverter implements Converter {
    @Override
    public Object convert(Object raw) {
        if (raw instanceof Map) {
            Map<?, ?> src = (Map<?, ?>) raw;
            Map<String, Map<String, Object>> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : src.entrySet()) {
                if (entry.getValue() instanceof Map) {
                    Map<?, ?> itemSrc = (Map<?, ?>) entry.getValue();
                    Map<String, Object> itemMap = new LinkedHashMap<>();
                    for (Map.Entry<?, ?> itemEntry : itemSrc.entrySet()) {
                        itemMap.put(itemEntry.getKey().toString(), itemEntry.getValue());
                    }
                    result.put(entry.getKey().toString(), itemMap);
                }
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
