package com.cloud.arch.mybatis.core.handler;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Slf4j
public class JsonTypeHandler<T> extends BaseTypeHandler<T> {

    private final Class<T> clazz;

    public JsonTypeHandler(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public void setNonNullParameter(PreparedStatement preparedStatement, int index, T t, JdbcType jdbcType) throws SQLException {
        preparedStatement.setString(index, JSON.toJSONString(t));
    }

    @Override
    public T getNullableResult(ResultSet resultSet, String s) throws SQLException {
        return JSON.parseObject(resultSet.getString(s), clazz);
    }

    @Override
    public T getNullableResult(ResultSet resultSet, int index) throws SQLException {
        return JSON.parseObject(resultSet.getString(index), clazz);
    }

    @Override
    public T getNullableResult(CallableStatement statement, int index) throws SQLException {
        return JSON.parseObject(statement.getString(index), clazz);
    }

}
