package com.cloud.arch.hotkey.net.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TotalCount {

    private long totalReceiveCount;
    private long totalDealCount;

}
