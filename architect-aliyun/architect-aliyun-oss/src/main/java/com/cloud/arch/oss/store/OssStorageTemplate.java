package com.cloud.arch.oss.store;

import com.aliyun.oss.OSSClient;
import com.cloud.arch.oss.props.OssCloudProperties;
import com.google.common.collect.Lists;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

public class OssStorageTemplate {
    //图片类型
    public static final List<String> IMAGES = Lists.newArrayList(".jpg", ".jpeg", ".png", ".gif");
    //视频类型
    public static final List<String> VIDEOS
                                            = Lists.newArrayList(".mp4", ".avi", ".mov", ".wmv", ".asf", ".navi", ".3gp", ".mkv", ".f4v", ".rmvb", ".webm");

    private final OSSClient          client;
    private final OssCloudProperties properties;

    public OssStorageTemplate(OSSClient client, OssCloudProperties properties) {
        this.client     = client;
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
