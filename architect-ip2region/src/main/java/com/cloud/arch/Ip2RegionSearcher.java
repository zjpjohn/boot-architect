package com.cloud.arch;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;

import java.io.InputStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.regex.Pattern;

@Slf4j
public class Ip2RegionSearcher implements DisposableBean, SmartInitializingSingleton {

    private static final Pattern DELIMITER_PATTERN = Pattern.compile("\\|");
    private static final String  DEFAULT_PATH      = "data/ip2region.xdb";

    private final String   dbPath;
    private       Searcher searcher;

    public Ip2RegionSearcher() {
        this.dbPath = DEFAULT_PATH;
    }

    public Ip2RegionSearcher(String dbPath) {
        if (StringUtils.isBlank(dbPath)) {
            throw new IllegalArgumentException("ip region data base must not be null.");
        }
        this.dbPath = dbPath;
    }

    public IpRegionResult search(String ip) {
        if (this.searcher == null || StringUtils.isBlank(ip)) {
            log.warn("ip searcher bean或ip地址为空.");
            return null;
        }
        try {
            String   region = this.searcher.search(ip);
            String[] splits = DELIMITER_PATTERN.split(region);
            if (splits.length < 5) {
                splits = Arrays.copyOf(splits, 5);
            }
            IpRegionResult result = new IpRegionResult();
            result.setIp(ip);
            result.setCountry(filterNull(splits[0]));
            result.setRegion(filterNull(splits[1]));
            result.setProvince(filterNull(splits[2]));
            result.setCity(filterNull(splits[3]));
            result.setIsp(filterNull(splits[4]));
            return result;
        } catch (Exception error) {
            log.error("查询Ip对应区域地址异常:", error);
        }
        return null;
    }

    private String filterNull(String part) {
        if (part == null || BigInteger.ZERO.toString().equals(part)) {
            return null;
        }
        return part;
    }

    @Override
    public void destroy() throws Exception {
        if (this.searcher != null) {
            this.searcher.close();
        }
    }

    @Override
    public void afterSingletonsInstantiated() {
        this.loadIpDatabase();
    }

    private void loadIpDatabase() {
        ClassLoader classLoader = Ip2RegionSearcher.class.getClassLoader();
        try (InputStream stream = classLoader.getResourceAsStream(this.dbPath)) {
            if (stream != null) {
                this.searcher = Searcher.newWithBuffer(IOUtils.toByteArray(stream));
            }
        } catch (Exception error) {
            log.error("加载Ip2Region数据库xdb文件异常:", error);
            throw new RuntimeException(error);
        }
    }

}
