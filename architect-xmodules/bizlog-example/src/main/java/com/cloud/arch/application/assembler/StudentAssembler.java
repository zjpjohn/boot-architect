package com.cloud.arch.application.assembler;

import com.cloud.arch.application.command.dto.StudentCreateCmd;
import com.cloud.arch.infrast.persist.po.StudentPo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StudentAssembler {

    StudentPo toPo(StudentCreateCmd cmd);

}
