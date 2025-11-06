package com.boot.architect.application.assembler;

import com.boot.architect.application.command.dto.StudentCreateCmd;
import com.boot.architect.application.command.dto.StudentEditCmd;
import com.boot.architect.infrast.persist.po.StudentPo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StudentAssembler {


    StudentPo toPo(StudentCreateCmd command);

    StudentPo toPo(StudentEditCmd command);


}
