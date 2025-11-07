package com.cloud.arch.web.converter.factory;

import com.cloud.arch.enums.EnumValue;
import com.cloud.arch.enums.Value;
import com.cloud.arch.web.converter.ConvertParseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@SuppressWarnings({"rawtypes", "unchecked"})
public class EnumConverterFactory implements ConverterFactory<String, Value> {

    private final ConcurrentHashMap<Class<?>, EnumValueConverter> converterMap = new ConcurrentHashMap<>(64);

    @Override
    public <T extends Value> Converter<String, T> getConverter(Class<T> targetType) {
        return converterMap.computeIfAbsent(targetType, key -> new EnumValueConverter(targetType));
    }

    private static class EnumValueConverter<K extends Comparable<K>, V extends Value<K>>
            implements Converter<String, V> {

        private final EnumValue<K, V> enumValue;

        public EnumValueConverter(Class<V> targetType) {
            this.enumValue = new EnumValue<>(targetType);
        }

        @Override
        public V convert(String value) {
            return Optional.ofNullable(this.enumValue.of(value)).orElseThrow(() -> buildException(value));

        }

        private ConvertParseException buildException(String value) {
            String message = "enum value '" + value + "' error,";
            String ranges  = this.enumValue.values().toString();
            return new ConvertParseException(message + "params ranges " + ranges);
        }
    }

}
