package com.boot.architect.application.command.dto;

import com.boot.architect.infrast.persist.enums.Gender;
import com.cloud.arch.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserListQuery extends PageQuery {

    private String name;
    private Gender gender;

}
