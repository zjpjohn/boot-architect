package com.cloud.arch.mybatis.core.handler;

import com.cloud.arch.enums.Value;
import com.cloud.arch.mybatis.core.ValueType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.springframework.util.Assert;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class EnumTypeHandler<K extends Comparable<K>, T extends Value<K>> extends BaseTypeHandler<T> {

    @Getter
    private final ValueType valueType;
    private final Map<K, T> enumMaps;

    public EnumTypeHandler(Class<T> clazz) {
        T[] values = clazz.getEnumConstants();
        Assert.state(values != null && values.length > 0, "enum values collection must not be null.");
        String typeName = values[0].value().getClass().getName();
        this.valueType = ValueType.findOf(typeName);
        Assert.notNull(this.valueType,
                       "enum value type only support [ 'String' , 'Integer' , 'Long' , 'Double' , 'Float' , 'Short' ] , but this value type is '"
                               + typeName
                               + "'");
        this.enumMaps = Arrays.stream(values).collect(Collectors.toMap(Value::value, Function.identity()));
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int index, T parameter, JdbcType jdbcType)
            throws SQLException {
        valueType.setParameter(ps, index, parameter.value());
    }

    @Override
    public T getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return ofResult().apply(valueType.getResult(rs, columnName));
    }

    @Override
    public T getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return ofResult().apply(valueType.getResult(rs, columnIndex));
    }

    @Override
    public T getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return ofResult().apply(valueType.getResult(cs, columnIndex));
    }

    private Function<K, T> ofResult() {
        return key -> Optional.ofNullable(key).map(enumMaps::get).orElse(null);
    }

}
