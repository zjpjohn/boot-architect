package com.cloud.arch.hotkey.config;

public interface ConfigConstant {
    /**
     * 所有的app名字，存这里
     */
    String appsPath             = "/thales/apps/";
    /**
     * 所有的workers，存这里
     */
    String workersPath          = "/thales/workers/";
    /**
     * dashboard的ip存这里
     */
    String dashboardPath        = "/thales/dashboard/";
    /**
     * 该app所有的workers地址的path。需要手工分配，默认每个app都用所有的worker
     */
    String appWorkerPath        = null;
    /**
     * 所有的客户端规则（譬如哪个app的哪些前缀的才参与计算）
     */
    String rulePath             = "/thales/rules/";
    /**
     * 白名单路径，白名单的不参与热key计算，如 /thales/whiteList/app1 -> key1,key2,key3
     */
    String whiteListPath        = "/thales/whiteList/";
    /**
     * 客户端数量，如/jd/count/cartsoa = 2900
     */
    String clientCountPath      = "/thales/count/";
    /**
     * 每个app的热key放这里。格式如：jd/hotkeys/app1/userA
     */
    String hotKeyPath           = "/thales/hotkeys/";
    /**
     * 每个app的热key记录放这里，供控制台监听入库用。格式如：thales/records/app1/userA
     */
    String hotKeyRecordPath     = "/thales/keyRecords/";
    /**
     * caffeine的size
     */
    String caffeineSizePath     = "/thales/caffeineSize/";
    /**
     * totalReceiveKeyCount该worker接收到的key总量，每10秒上报一次
     */
    String totalReceiveKeyCount = "/thales/totalKeyCount/";
    /**
     * bufferPool直接内存
     */
    String bufferPoolPath       = "/thales/bufferPool/";

    /**
     * 存放客户端hotKey访问次数和总访问次数的path
     */
    String keyHitCountPath = "/thales/keyHitCount/";
    /**
     * 是否开启日志
     */
    String logToggle       = "/thales/logOn";

    /**
     * 清理历史数据的配置的path
     * time unit : day
     */
    String clearCfgPath = "/thales/clearCfg/";

    /**
     * app配置
     */
    String appCfgPath = "/thales/appCfg/";

}
