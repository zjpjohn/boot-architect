package com.cloud.arch.web.converter.factory;

import com.cloud.arch.enums.EnumValue;
import com.cloud.arch.enums.Value;
import com.cloud.arch.web.converter.ConvertParseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 枚举参数转换工厂，将请求中的字符串转换为 {@link Value} 枚举实例。
 */
@Slf4j
@SuppressWarnings({"unchecked", "rawtypes"})
public class EnumConverterFactory implements ConverterFactory<String, Value> {

    @SuppressWarnings("rawtypes")
    private final ConcurrentHashMap<Class<?>, EnumValueConverter> converterMap = new ConcurrentHashMap<>(64);

    @Override
    public <T extends Value> Converter<String, T> getConverter(Class<T> targetType) {
        return converterMap.computeIfAbsent(targetType, key -> new EnumValueConverter(targetType));
    }

    private static class EnumValueConverter<K extends Comparable<K>, V extends Value<K>> implements Converter<String, V> {

        private final EnumValue<K, V> enumValue;

        public EnumValueConverter(Class<V> targetType) {
            this.enumValue = new EnumValue<>(targetType);
        }

        @Override
        public V convert(String value) {
            V result = this.enumValue.of(value);
            if (result == null) {
                throw buildException(value);
            }
            return result;
        }

        private ConvertParseException buildException(String value) {
            String name    = this.enumValue.getType().getSimpleName();
            String message = "Input value '" + value + "' error , ";
            String ranges  = this.enumValue.values().toString();
            return new ConvertParseException(message + name + " value ranges " + ranges);
        }
    }

}
