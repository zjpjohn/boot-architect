package com.cloud.arch.web.utils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.InetAddress;
import java.util.Optional;

@Slf4j
public class RequestUtils {

    private static final String IP_DELIMITER  = ",";
    private static final String UNKNOWN       = "unknown";
    private static final String LOCALHOST_IP  = "0:0:0:0:0:0:0:1";
    private static final String LOCALHOST_IP1 = "127.0.0.1";

    /**
     * 获取指定请求头信息
     */
    public static String header(String header) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return Optional.ofNullable(attributes)
                       .map(ServletRequestAttributes::getRequest)
                       .map(request -> request.getHeader(header))
                       .orElse("");
    }

    /**
     * 获取当前请求的ip地址
     */
    public static String ipAddress() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return Optional.ofNullable(attributes)
                       .map(ServletRequestAttributes::getRequest)
                       .map(RequestUtils::ipAddress)
                       .orElse("");
    }


    /**
     * 获取请求的详细地址
     *
     * @param request {@link ServerHttpRequest}
     * @return 请求ip地址
     */
    public static String ipAddress(ServerHttpRequest request) {
        HttpHeaders headers = request.getHeaders();
        String      ip      = headers.getFirst("x-forwarded-for");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = headers.getFirst("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = headers.getFirst("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = headers.getFirst("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddress().getAddress().getHostAddress();
        }
        if (ip != null && ip.length() > 15 && ip.contains(",")) {
            ip = ip.substring(0, ip.indexOf(","));
        }
        return ip;
    }

    /**
     * 获取请求的ip地址
     *
     * @param request 指定请求
     */
    public static String ipAddress(HttpServletRequest request) {
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
            if (!StringUtils.isEmpty(ip) && ip.indexOf(IP_DELIMITER) > 0) {
                ip = ip.substring(0, ip.indexOf(IP_DELIMITER));
            }
            return ip.equals(LOCALHOST_IP) ? LOCALHOST_IP1 : ip;
        } catch (Exception e) {
            log.error("get servlet request ip exception:", e);
            throw new RuntimeException("get servlet request ip exception.", e);
        }
    }

}
