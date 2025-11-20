package com.cloud.arch.operate.core;

import com.cloud.arch.enums.Value;
import com.cloud.arch.web.dict.Dictionary;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
@Dictionary(name = "operate_type", type = "string", remark = "系统操作类型")
public enum OperateType implements Value<String> {
    ADD("add", "新增"),
    EDIT("edit", "修改"),
    DELETE("delete", "删除"),
    RECOVER("recover", "恢复"),
    REVOKE("revoke", "撤销"),
    CLEAR("clear", "清理"),
    IMPORT("import", "导入"),
    EXPORT("export", "导出"),
    UPLOAD("upload", "上传"),
    DOWNLOAD("download", "下载"),
    ISSUE("issue", "发布"),
    ACTIVE("active", "激活"),
    CANCEL("cancel", "取消"),
    TAKE_UP("take_up", "上架"),
    TAKE_OFF("take_off", "下架"),
    LOOK("look", "查看"),
    OTHER("other", "其他");

    private final String type;
    private final String label;

    @Override
    public String value() {
        return this.type;
    }

    @Override
    public String label() {
        return this.label;
    }

    public static OperateType of(String type) {
        return Arrays.stream(values()).filter(e -> e.type.equals(type)).findFirst().orElse(OTHER);
    }

}
