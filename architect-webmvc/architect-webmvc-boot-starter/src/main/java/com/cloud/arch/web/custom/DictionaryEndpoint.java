package com.cloud.arch.web.custom;


import com.cloud.arch.web.dict.DictionaryFactory;
import com.cloud.arch.web.domain.ApiReturn;
import com.cloud.arch.web.props.WebmvcProperties;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;

@Slf4j
@AllArgsConstructor
public class DictionaryEndpoint implements InitializingBean {

    private final DictionaryFactory            dictionaryFactory;
    private final WebmvcProperties             properties;
    private final RequestMappingHandlerMapping handlerMapping;

    /**
     * 字典请求处理：
     * 1./endpoint 获取全部字典信息
     * 2./endpoint?name=xxx 获取指定名称字典数据集合
     */
    public ApiReturn<Object> dictionary(@RequestParam(value = "name", required = false) String name) {
        if (StringUtils.isBlank(name)) {
            return ApiReturn.success("all dictionary list.", dictionaryFactory.list());
        }
        return ApiReturn.success(name + " dictionary detail.", dictionaryFactory.of(name));
    }

    /**
     * 注册字典暴露端点到spring mvc
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        String             endpoint    = properties.getDictionary().getEndpoint();
        RequestMappingInfo mappingInfo = RequestMappingInfo.paths(endpoint).methods(RequestMethod.GET).build();
        Method             method      = DictionaryEndpoint.class.getMethod("dictionary", String.class);
        handlerMapping.registerMapping(mappingInfo, this, method);
    }

}
