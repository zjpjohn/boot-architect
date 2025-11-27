package com.boot.architect.application.command.impl;

import com.boot.architect.application.assembler.UserAssembler;
import com.boot.architect.application.command.IUserCommandService;
import com.boot.architect.application.command.dto.UserCreateCmd;
import com.boot.architect.application.command.dto.UserEditCmd;
import com.boot.architect.application.command.dto.UserListQuery;
import com.boot.architect.infrast.persist.mapper.UserMapper;
import com.boot.architect.infrast.persist.po.UserPo;
import com.cloud.arch.mybatis.extend.Query;
import com.cloud.arch.page.Pager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCommandService implements IUserCommandService {

    private final UserMapper    mapper;
    private final UserAssembler assembler;

    @Override
    public void create(UserCreateCmd command) {
        UserPo user = assembler.toUser(command);
        mapper.insert(user);
    }

    @Override
    public void editUser(UserEditCmd command) {
        UserPo user = mapper.selectOneById(command.getId());
        if (user != null) {
            UserPo newUser = new UserPo();
            newUser.setId(user.getId());
            newUser.setName(command.getName());
            newUser.setNickname(command.getNickname());
            newUser.setVersion(user.getVersion());
            mapper.update(user, true);
        }
    }

    @Override
    public void remove(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public UserPo getUser(String name) {
        return Query.of(mapper).eq(UserPo::getName, name).one();
    }

    @Override
    public UserPo getUser(Long id) {
        return mapper.selectOneById(id);
    }

    @Override
    public Pager<UserPo> userList(UserListQuery query) {
        return Query.of(mapper)
                    .eq(UserPo::getName, query.getName())
                    .eq(UserPo::getGender, query.getGender())
                    .pager(query.getPage(), query.getLimit());
    }

}
