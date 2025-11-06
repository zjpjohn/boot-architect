package com.boot.architect.application.command;

import com.boot.architect.application.command.dto.StudentCreateCmd;
import com.boot.architect.application.command.dto.StudentEditCmd;

public interface IStudentCommandService {

    void createStudent(StudentCreateCmd cmd);

    void editStudent(StudentEditCmd cmd);

}
