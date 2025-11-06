package com.cloud.arch.web.utils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.InetAddress;
import java.util.Optional;

@Slf4j
public class RequestIpUtils {

    private static final String IP_DELIMITER  = ",";
    private static final String UNKNOWN       = "unknown";
    private static final String LOCALHOST_IP  = "0:0:0:0:0:0:0:1";
    private static final String LOCALHOST_IP1 = "127.0.0.1";

    private RequestIpUtils() {
    }

    /**
     * 获取当前请求的ip地址
     */
    public static String getIpAddress() {
        ServletRequestAttributes attributes = (ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
        return Optional.ofNullable(attributes)
                       .map(ServletRequestAttributes::getRequest)
                       .map(RequestIpUtils::getIpAddress)
                       .orElse("");
    }

    /**
     * 获取请求的ip地址
     *
     * @param request 指定请求
     */
    public static String getIpAddress(HttpServletRequest request) {
        try {
            String ip = request.getHeader("X-Original-Forwarded-For");
            if (StringUtils.isEmpty(ip) || UNKNOWN.equalsIgnoreCase(ip)) {
                ip = request.getHeader("X-Forwarded-For");
            }
            if (StringUtils.isEmpty(ip) || UNKNOWN.equalsIgnoreCase(ip)) {
                ip = request.getHeader("x-forwarded-for");
            }
            if (StringUtils.isEmpty(ip) || UNKNOWN.equalsIgnoreCase(ip)) {
                ip = request.getHeader("Proxy-Client-IP");
            }
            if (StringUtils.isEmpty(ip) || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip)) {
                ip = request.getHeader("WL-Proxy-Client-IP");
            }
            if (StringUtils.isEmpty(ip) || UNKNOWN.equalsIgnoreCase(ip)) {
                ip = request.getHeader("HTTP_CLIENT_IP");
            }
            if (StringUtils.isEmpty(ip) || UNKNOWN.equalsIgnoreCase(ip)) {
                ip = request.getHeader("HTTP_X_FORWARDED_FOR");
            }
            if (StringUtils.isEmpty(ip) || UNKNOWN.equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
                if (LOCALHOST_IP1.equalsIgnoreCase(ip) || LOCALHOST_IP.equalsIgnoreCase(ip)) {
                    ip = InetAddress.getLocalHost().getHostAddress();
                }
            }
            if (!StringUtils.isEmpty(ip)) {
                int index = ip.indexOf(IP_DELIMITER);
                if (index > 0) {
                    ip = ip.substring(0, index);
                }
            }
            return ip.equals(LOCALHOST_IP) ? LOCALHOST_IP1 : ip;
        } catch (Exception e) {
            log.error("get servlet request ip exception:", e);
            throw new RuntimeException("get servlet request ip exception.", e);
        }
    }

}
