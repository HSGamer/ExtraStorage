package me.hsgamer.extrastorage.configs.converters;

import io.github.projectunified.craftconfig.annotation.converter.Converter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StringListConverter implements Converter {
    @Override
    public Object convert(Object raw) {
        if (raw instanceof List) {
            List<?> list = (List<?>) raw;
            List<String> result = new ArrayList<>(list.size());
            for (Object element : list) {
                result.add(element.toString());
            }
            return result;
        }
        return Collections.emptyList();
    }

    @Override
    public Object convertToRaw(Object value) {
        return value;
    }
}
