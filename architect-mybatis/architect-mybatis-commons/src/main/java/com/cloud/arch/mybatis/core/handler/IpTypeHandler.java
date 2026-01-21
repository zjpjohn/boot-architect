package com.cloud.arch.mybatis.core.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.*;

public class IpTypeHandler extends BaseTypeHandler<String> {

    /**
     * ipv4转int
     */
    private Integer ipToInt(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return 0;
        }
        String[] ipAddressInArray = ipAddress.split("\\.");
        if (ipAddressInArray.length != 4) {
            throw new IllegalArgumentException("Invalid IP address: " + ipAddress);
        }
        int result = 0;
        for (int i = 0; i < 4; i++) {
            int power = 3 - i;
            int ip    = Integer.parseInt(ipAddressInArray[i].trim());
            if (ip < 0 || ip > 255) {
                throw new IllegalArgumentException("Invalid IP segment: " + ip);
            }
            result += (int) (ip * Math.pow(256, power));
        }
        return result;
    }

    /**
     * int转ipv4
     */
    private String intToIp(int ip) {
        if (ip <= 0) {
            return "";
        }
        return ((ip >> 24) & 0xFF) + "." + ((ip >> 16) & 0xFF) + "." + ((ip >> 8) & 0xFF) + "." + (ip & 0xFF);
    }

    @Override
    public void setNonNullParameter(PreparedStatement statement,
                                    int index,
                                    String parameter,
                                    JdbcType jdbcType) throws SQLException {
        statement.setInt(index, ipToInt(parameter));
    }

    @Override
    public String getNullableResult(ResultSet resultSet, String name) throws SQLException {
        int ipInt = resultSet.getInt(name);
        if (resultSet.wasNull()) {
            return null;
        }
        return intToIp(ipInt);
    }

    @Override
    public String getNullableResult(ResultSet resultSet, int index) throws SQLException {
        int ipInt = resultSet.getInt(index);
        if (resultSet.wasNull()) {
            return null;
        }
        return intToIp(ipInt);
    }

    @Override
    public String getNullableResult(CallableStatement statement, int index) throws SQLException {
        int ipInt = statement.getInt(index);
        if (statement.wasNull()) {
            return null;
        }
        return intToIp(ipInt);
    }

}
