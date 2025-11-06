package com.cloud.arch.oss.web;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.common.utils.BinaryUtil;
import com.aliyun.oss.model.MatchMode;
import com.aliyun.oss.model.PolicyConditions;
import com.cloud.arch.oss.props.OssCloudProperties;
import org.codehaus.jettison.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Date;

public class OssPolicyGenerator {

    private final OSSClient          client;
    private final OssCloudProperties properties;

    public OssPolicyGenerator(OSSClient client, OssCloudProperties properties) {
        this.client     = client;
        this.properties = properties;
    }

    /**
     * @param directory 上传文件夹
     */
    public OssPolicy execute(String directory) throws Exception {
        PolicyConditions conditions = new PolicyConditions();
        String           dir        = directory.endsWith("/") ? directory : directory + "/";

        conditions.addConditionItem(PolicyConditions.COND_CONTENT_LENGTH_RANGE, 0, 1048576000);
        conditions.addConditionItem(MatchMode.StartWith, PolicyConditions.COND_KEY, dir);

        //上传policy信息
        long   expireAt      = System.currentTimeMillis() + properties.getWebDirect().getExpire() * 1000;
        String policy        = client.generatePostPolicy(new Date(expireAt), conditions);
        String encodedPolicy = BinaryUtil.toBase64String(policy.getBytes(StandardCharsets.UTF_8));

        //policy签名
        String signature = client.calculatePostSignature(policy);

        OssPolicy ossPolicy = new OssPolicy();
        ossPolicy.setAppId(properties.getAppId());
        ossPolicy.setPolicy(encodedPolicy);
        ossPolicy.setSignature(signature);
        ossPolicy.setDir(directory);
        ossPolicy.setDomain(properties.getDomainUri());
        ossPolicy.setExpire(expireAt);
        ossPolicy.setHost(properties.getHostUri());

        //设置回调信息
        ossPolicy.setCallback(getCallback());
        return ossPolicy;
    }

    /**
     * 构建回调信息
     */
    private String getCallback() throws Exception {
        JSONObject callback = new JSONObject();
        callback.put("callbackUrl", properties.getWebDirect().getCallback());
        callback.put("callbackBodyType", "application/json");
        callback.put("callbackBody", """
                {
                "filename":"${object}",
                "size":"${size}",
                "mimeType":"${mimeType}",
                "height":"${imageInfo.height}",
                "width":"${imageInfo.width}"
                }
                """);
        return BinaryUtil.toBase64String(callback.toString().getBytes(StandardCharsets.UTF_8));
    }

    public OSSClient getClient() {
        return client;
    }

    public OssCloudProperties getProperties() {
        return properties;
    }
}
