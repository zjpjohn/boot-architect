package com.cloud.arch;

import com.google.common.collect.Sets;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashSet;

@Data
public class IpRegionResult {

    private String ip;
    private String country;
    private String region;
    private String province;
    private String city;
    private String isp;

    public String getAddress() {
        LinkedHashSet<String> set = Sets.newLinkedHashSet();
        set.add(country);
        set.add(province);
        set.add(city);
        set.add(region);
        set.removeIf(StringUtils::isBlank);
        return String.join(",", set);
    }

}
