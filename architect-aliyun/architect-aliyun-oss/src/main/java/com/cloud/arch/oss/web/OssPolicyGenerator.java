package com.cloud.arch.oss.web;

import com.alibaba.fastjson2.JSON;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.MatchMode;
import com.aliyun.oss.model.PolicyConditions;
import com.cloud.arch.oss.props.OssCloudProperties;
import com.cloud.arch.utils.IdWorker;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
public record OssPolicyGenerator(OSSClient client, OssCloudProperties properties) {

    /**
     * 获取文件上传policy
     */
    public OssPolicy generatePolicy(OssPolicyReq req) {
        OssScene                               scene      = req.getScene();
        OssCloudProperties.WebDirectProperties webDirect  = properties.getWebDirect();
        Date                                   expiration = webDirect.expireDate();
        String                                 objectKey  = buildObjectKey(scene, req.getFileName());

        // 限制上传目录前缀，防止客户端上传到其他位置
        String           keyPrefix  = keyPrefix(scene);
        PolicyConditions conditions = new PolicyConditions();
        conditions.addConditionItem(PolicyConditions.COND_CONTENT_LENGTH_RANGE, 1, scene.maxSize());
        conditions.addConditionItem(MatchMode.StartWith, PolicyConditions.COND_KEY, keyPrefix);

        String postPolicy = client.generatePostPolicy(expiration, conditions);
        String signature  = client.calculatePostSignature(postPolicy);
        String policy     = Base64.getEncoder().encodeToString(postPolicy.getBytes(StandardCharsets.UTF_8));

        boolean             callbackEnabled = StringUtils.isNotBlank(webDirect.getCallback());
        String              callback        = null;
        Map<String, String> callbackVars    = null;
        if (callbackEnabled) {
            Map<String, Object> callbackJson = new LinkedHashMap<>();
            callbackJson.put("callbackUrl", webDirect.getCallback());
            StringBuilder bodySb = new StringBuilder();
            bodySb.append("object=${object}&size=${size}&mimeType=${mimeType}");
            bodySb.append("&scene=${x:scene}");

            callbackVars = new LinkedHashMap<>();
            callbackVars.put("x:scene", scene.value());
            if (req.getUploadBy() != null) {
                bodySb.append("&uploadBy=${x:upload_by}");
                callbackVars.put("x:upload_by", String.valueOf(req.getUploadBy()));
            }
            if (StringUtils.isNotBlank(req.getReplaceUrl())) {
                String oldObjectKey = extractObjectKey(req.getReplaceUrl());
                if (oldObjectKey != null) {
                    bodySb.append("&oldObject=${x:old_object}");
                    callbackVars.put("x:old_object", oldObjectKey);
                }
            }
            callbackJson.put("callbackBody", bodySb.toString());
            callbackJson.put("callbackBodyType", "application/x-www-form-urlencoded");
            callback = Base64.getEncoder()
                             .encodeToString(JSON.toJSONString(callbackJson).getBytes(StandardCharsets.UTF_8));
        }
        return OssPolicy.builder()
                        .appId(properties.getAppId())
                        .policy(policy)
                        .signature(signature)
                        .domain(negotiateHost())
                        .objKey(objectKey)
                        .expire(webDirect.getExpire())
                        .callback(callback)
                        .callbackVars(callbackVars)
                        .build();
    }

    private String negotiateHost() {
        if (StringUtils.isNotBlank(properties.getDomainUri())) {
            return properties.getDomainUri();
        }
        return String.format("https://%s.%s", properties.getBucket(), properties.getEndpoint());
    }

    /**
     * objKey前缀
     * @param scene 上传文件前缀
     */
    private String keyPrefix(OssScene scene) {
        String module = scene.module();
        if (StringUtils.isNotBlank(module)) {
            return module + "/" + scene.value() + "/";
        }
        return scene.value() + "/";
    }

    /**
     * 解析objKey
     * @param accessUrl 文件路径
     */
    public String extractObjectKey(String accessUrl) {
        if (StringUtils.isBlank(accessUrl)) {
            return null;
        }
        try {
            String withoutProtocol = accessUrl.substring(accessUrl.indexOf("://") + 3);
            int    slashIdx        = withoutProtocol.indexOf('/');
            if (slashIdx < 0 || slashIdx == withoutProtocol.length() - 1) {
                return null;
            }
            return withoutProtocol.substring(slashIdx + 1);
        } catch (Exception e) {
            log.warn("无法从 URL 提取 object_key: {}", accessUrl, e);
            return null;
        }
    }

    /**
     * 构建objKey
     * @param scene  上传文件场景
     * @param fileName 文件名称
     */
    private String buildObjectKey(OssScene scene, String fileName) {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String ext  = "";
        int    dot  = fileName.lastIndexOf('.');
        if (dot > 0) {
            ext = fileName.substring(dot);
        }
        String uuid   = String.valueOf(IdWorker.nextId());
        String module = scene.module();
        if (StringUtils.isNotBlank(module)) {
            return String.format("%s/%s/%s/%s%s", module, scene.value(), date, uuid, ext);
        }
        return String.format("%s/%s/%s%s", scene.value(), date, uuid, ext);
    }

}
