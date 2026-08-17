package com.cloud.arch.operate.core;

import java.util.List;

public interface ILogStorage {

    void save(List<OperationLog> records);

}
