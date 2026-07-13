package com.cloud.arch.oss.store;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.DeleteObjectsRequest;
import com.aliyun.oss.model.DeleteObjectsResult;
import com.cloud.arch.oss.props.OssCloudProperties;
import com.cloud.arch.utils.CollectionUtils;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

@Getter
public class OssStorageTemplate {
    //图片类型
    public static final List<String> IMAGES = Lists.newArrayList(".jpg", ".jpeg", ".png", ".gif");
    //视频类型
    public static final List<String> VIDEOS = Lists.newArrayList(".mp4", ".avi", ".mov", ".wmv", ".asf", ".navi", ".3gp", ".mkv", ".f4v", ".rmvb", ".webm");

    private final OSSClient          client;
    private final OssCloudProperties properties;

    public OssStorageTemplate(OSSClient client, OssCloudProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    /**
     * 上传二进制数据到启用的云服务器中
     *
     * @param data 文件二进制数据
     * @param key  文件名称
     */
    public OssResult store(byte[] data, String key) throws Exception {
        return store(new ByteArrayInputStream(data), key);
    }

    /**
     * 上传文件到启用的云服务器中
     *
     * @param file 上传文件
     * @param key  文件名称
     */
    public OssResult store(File file, String key) throws Exception {
        String fileName = file.getName();
        String suffix   = fileName.substring(fileName.lastIndexOf("."));
        return store(new FileInputStream(file), key + suffix);
    }

    /**
     * 上传文件流到启用的云服务器中
     *
     * @param stream 文件流
     * @param key    文件名称
     */
    public OssResult store(InputStream stream, String key) throws Exception {
        String nPath = realKey(key);
        client.putObject(this.properties.getBucket(), nPath, stream);
        return OssResult.builder().key(key).url(properties.getDomainUri() + "/" + nPath).build();
    }

    /**
     * 删除指定云文件
     *
     * @param key 文件名称
     */
    public String remove(String key) throws Exception {
        client.deleteObject(this.properties.getBucket(), realKey(key));
        return key;
    }

    /**
     * 批量删除文件
     *
     * @param keys 文件名集合
     */
    public List<String> removeBatch(List<String> keys) throws Exception {
        if (CollectionUtils.isEmpty(keys)) {
            return Collections.emptyList();
        }
        List<String>         realKeys = keys.stream().map(this::realKey).toList();
        DeleteObjectsRequest request  = new DeleteObjectsRequest(properties.getBucket()).withKeys(realKeys);
        DeleteObjectsResult  result   = client.deleteObjects(request);
        return result.getDeletedObjects();
    }

    /**
     * 获取 objKey 访问路径
     */
    public String urlPath(String objKey) {
        Preconditions.checkArgument(StringUtils.isNotBlank(objKey), "oss object key must not bet null.");
        String key = objKey.startsWith("/") ? objKey : "/" + objKey;
        return properties.hostPrefix() + key;
    }

    private String realKey(String key) {
        return key.startsWith("/") ? key.substring(1) : key;
    }

    public static boolean isImage(String fileExtension) {
        return IMAGES.contains(fileExtension.toLowerCase());
    }

    public static boolean isVideo(String fileExtension) {
        return VIDEOS.contains(fileExtension.toLowerCase());
    }

}
