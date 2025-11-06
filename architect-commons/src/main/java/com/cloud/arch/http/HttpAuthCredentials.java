package com.cloud.arch.http;

import lombok.Getter;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Getter
public class HttpAuthCredentials {

    private final String userName;
    private final String password;

    private static final ThreadLocal<HttpAuthCredentials> credentialsHolder = new ThreadLocal<>();

    private HttpAuthCredentials(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }

    public static void load(String userName, String password) {
        credentialsHolder.set(new HttpAuthCredentials(userName, password));
    }

    public static HttpAuthCredentials getCredentials() {
        return credentialsHolder.get();
    }

    public static void clear() {
        credentialsHolder.remove();
    }

    public static String basicCredential() {
        HttpAuthCredentials credentials = credentialsHolder.get();
        if (credentials == null
            || !StringUtils.hasText(credentials.userName)
            || !StringUtils.hasText(credentials.password)) {
            return null;
        }
        String authStr = credentials.userName + ":" + credentials.password;
        return "Basic " + Base64.getEncoder().encodeToString(authStr.getBytes(StandardCharsets.UTF_8));
    }

}
