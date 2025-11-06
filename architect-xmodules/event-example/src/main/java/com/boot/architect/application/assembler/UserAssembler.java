package com.boot.architect.application.assembler;

import com.boot.architect.application.command.dto.UserCreateCmd;
import com.boot.architect.domain.user.model.UserInfoDo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserAssembler {

    UserInfoDo toDo(UserCreateCmd cmd);

}
