package com.cloud.arch.mybatis.core;

import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

@SuppressWarnings("unchecked")
public enum ValueType {

    STRING(String.class.getName()) {
        @Override
        public void setParameter(PreparedStatement ps, int index, Object parameter) throws SQLException {
            ps.setString(index, (String) parameter);
        }

        @Override
        public String getResult(ResultSet rs, String columnName) throws SQLException {
            return rs.getString(columnName);
        }

        @Override
        public String getResult(ResultSet rs, int columnIndex) throws SQLException {
            return rs.getString(columnIndex);
        }

        @Override
        public String getResult(CallableStatement cs, int columnIndex) throws SQLException {
            return cs.getString(columnIndex);
        }

        @Override
        public JdbcType jdbcType() {
            return JdbcType.VARCHAR;
        }
    },
    INT(Integer.class.getName()) {
        @Override
        public void setParameter(PreparedStatement ps, int index, Object parameter) throws SQLException {
            ps.setInt(index, (int) parameter);
        }

        @Override
        public Integer getResult(ResultSet rs, String columnName) throws SQLException {
            return rs.getInt(columnName);
        }

        @Override
        public Integer getResult(ResultSet rs, int columnIndex) throws SQLException {
            return rs.getInt(columnIndex);
        }

        @Override
        public Integer getResult(CallableStatement cs, int columnIndex) throws SQLException {
            return cs.getInt(columnIndex);
        }

        @Override
        public JdbcType jdbcType() {
            return JdbcType.INTEGER;
        }
    },
    LONG(Long.class.getName()) {
        @Override
        public void setParameter(PreparedStatement ps, int index, Object parameter) throws SQLException {
            ps.setLong(index, (long) parameter);
        }

        @Override
        public Long getResult(ResultSet rs, String columnName) throws SQLException {
            return rs.getLong(columnName);
        }

        @Override
        public Long getResult(ResultSet rs, int columnIndex) throws SQLException {
            return rs.getLong(columnIndex);
        }

        @Override
        public Long getResult(CallableStatement cs, int columnIndex) throws SQLException {
            return cs.getLong(columnIndex);
        }

        @Override
        public JdbcType jdbcType() {
            return JdbcType.BIGINT;
        }
    },
    DOUBLE(Double.class.getName()) {
        @Override
        public void setParameter(PreparedStatement ps, int index, Object parameter) throws SQLException {
            ps.setDouble(index, (double) parameter);
        }

        @Override
        public Double getResult(ResultSet rs, String columnName) throws SQLException {
            return rs.getDouble(columnName);
        }

        @Override
        public Double getResult(ResultSet rs, int columnIndex) throws SQLException {
            return rs.getDouble(columnIndex);
        }

        @Override
        public Double getResult(CallableStatement cs, int columnIndex) throws SQLException {
            return cs.getDouble(columnIndex);
        }

        @Override
        public JdbcType jdbcType() {
            return JdbcType.DOUBLE;
        }
    },
    FLOAT(Float.class.getName()) {
        @Override
        public void setParameter(PreparedStatement ps, int index, Object parameter) throws SQLException {
            ps.setFloat(index, (float) parameter);
        }

        @Override
        public Float getResult(ResultSet rs, String columnName) throws SQLException {
            return rs.getFloat(columnName);
        }

        @Override
        public Float getResult(ResultSet rs, int columnIndex) throws SQLException {
            return rs.getFloat(columnIndex);
        }

        @Override
        public Float getResult(CallableStatement cs, int columnIndex) throws SQLException {
            return cs.getFloat(columnIndex);
        }

        @Override
        public JdbcType jdbcType() {
            return JdbcType.FLOAT;
        }
    },
    SHORT(Short.class.getName()) {
        @Override
        public void setParameter(PreparedStatement ps, int index, Object parameter) throws SQLException {
            ps.setShort(index, (short) parameter);
        }

        @Override
        public Short getResult(ResultSet rs, String columnName) throws SQLException {
            return rs.getShort(columnName);
        }

        @Override
        public Short getResult(ResultSet rs, int columnIndex) throws SQLException {
            return rs.getShort(columnIndex);
        }

        @Override
        public Short getResult(CallableStatement cs, int columnIndex) throws SQLException {
            return cs.getShort(columnIndex);
        }

        @Override
        public JdbcType jdbcType() {
            return JdbcType.SMALLINT;
        }
    };

    private final String name;

    ValueType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract void setParameter(PreparedStatement ps, int index, Object parameter) throws SQLException;

    public abstract <T extends Comparable<T>> T getResult(ResultSet rs, String columnName) throws SQLException;

    public abstract <T extends Comparable<T>> T getResult(ResultSet rs, int columnIndex) throws SQLException;

    public abstract <T extends Comparable<T>> T getResult(CallableStatement cs, int columnIndex) throws SQLException;

    public abstract JdbcType jdbcType();

    public static ValueType findOf(String name) {
        return Arrays.stream(values()).filter(v -> v.name.equals(name)).findFirst().orElse(null);
    }
}
