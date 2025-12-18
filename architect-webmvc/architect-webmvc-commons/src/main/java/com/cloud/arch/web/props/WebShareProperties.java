package com.cloud.arch.web.props;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@Data
public class WebShareProperties {

    /**
     * 全局排除路径,逗号分隔，授权和权限公用
     */
    private String excludes;

    /**
     *
     */
    public List<String> excludes() {
        List<String> result = Lists.newArrayList();
        if (StringUtils.isNotBlank(this.excludes)) {
            List<String> append = Splitter.on(",").trimResults().splitToList(this.excludes);
            result.addAll(append);
        }
        return result;
    }

}
