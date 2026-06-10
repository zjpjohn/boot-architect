package com.boot.architect.application.command.dto;

import com.boot.architect.infrast.persist.enums.Gender;
import com.boot.architect.infrast.persist.po.UserPo;
import com.cloud.arch.mybatis.extend.PageWhere;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserListQuery extends PageWhere {

    private String name;
    private Gender gender;

    @Override
    public void accept(QueryWrapper query) {
        query.eq(UserPo::getName, this.name).eq(UserPo::getGender, this.gender).orderBy(UserPo::getGmtCreate, false);
    }

}
