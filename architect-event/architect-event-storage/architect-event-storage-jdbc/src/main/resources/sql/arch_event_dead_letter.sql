-- 死信表，结构与 arch_event 对齐，额外记录死信时间和原因
CREATE TABLE IF NOT EXISTS arch_event_dead_letter (
    id          BIGINT       NOT NULL COMMENT '事件ID',
    name        VARCHAR(256) NOT NULL COMMENT '消息 topic',
    filter      VARCHAR(128) DEFAULT '' COMMENT '消息过滤 tag',
    delay       BIGINT       DEFAULT 0 COMMENT '延迟时间(ms)',
    event       TEXT         NOT NULL COMMENT '消息内容 JSON',
    shard_key   VARCHAR(128) DEFAULT '' COMMENT '分片键',
    state       INT          DEFAULT 2 COMMENT '事件状态',
    version     INT          DEFAULT 0 COMMENT '重试次数',
    gmt_create  BIGINT       NOT NULL COMMENT '创建时间',
    dead_time   BIGINT       NOT NULL COMMENT '进入死信时间',
    dead_reason VARCHAR(512) DEFAULT '' COMMENT '死信原因',
    PRIMARY KEY (id),
    INDEX idx_dead_gmt_create (gmt_create),
    INDEX idx_dead_shard_key (shard_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事件死信表';
