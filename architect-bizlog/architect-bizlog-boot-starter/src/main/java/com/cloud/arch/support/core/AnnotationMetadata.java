package com.cloud.arch.support.core;

import com.cloud.arch.annotations.OperateLog;
import com.google.common.collect.Lists;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@Getter
public class AnnotationMetadata {

    private final String       group;
    private final String       tenant;
    private final String       bizNo;
    private final String       success;
    private final String       fail;
    private final String       operator;
    private final String       detail;
    private final String       condition;
    //SpEl表达式集合
    private final List<String> spelTemplates = Lists.newArrayList();

    public AnnotationMetadata(OperateLog annotation) {
        if (StringUtils.isBlank(annotation.success()) && StringUtils.isBlank(annotation.failure())) {
            throw new IllegalArgumentException("Invalid operation annotation config on OperateLog. One of 'success' and 'failure' template must be set.");
        }
        this.group     = annotation.group();
        this.tenant    = annotation.tenant();
        this.bizNo     = annotation.bizNo();
        this.success   = annotation.success();
        this.fail      = annotation.failure();
        this.operator  = annotation.operator();
        this.detail    = annotation.detail();
        this.condition = annotation.condition();
        this.templates();
    }

    private void templates() {
        spelTemplates.add(bizNo);
        if (StringUtils.isNotBlank(tenant)) {
            spelTemplates.add(tenant);
        }
        if (StringUtils.isNotBlank(detail)) {
            spelTemplates.add(detail);
        }
        if (StringUtils.isNotBlank(operator)) {
            spelTemplates.add(operator);
        }
        if (StringUtils.isNotBlank(condition)) {
            spelTemplates.add(condition);
        }
    }

    public List<String> getSpelTemplates(boolean succeed) {
        List<String> templates = Lists.newArrayList(spelTemplates);
        templates.add(succeed ? success : fail);
        return templates;
    }

}
