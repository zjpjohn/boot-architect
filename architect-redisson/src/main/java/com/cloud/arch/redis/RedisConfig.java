package com.cloud.arch.redis;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;
import org.redisson.config.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@Data
@ConfigurationProperties(prefix = "com.cloud.redis")
public class RedisConfig {

    //单点模式
    private Standalone  standalone;
    //集群模式配置
    private Cluster     cluster;
    //哨兵模式配置
    private Sentinel    sentinel;
    //主从模式配置
    private MasterSlave masterSlave;
    //复制模式配置
    private Replicated  replicated;

    @Data
    public abstract static class BaseServer {

        public static final String REDIS_PREFIX = "redis://";
        public static final String DELIMITER    = ",";

        private int    idleConnectionTimeout                 = 10000;
        private int    connectTimeout                        = 10000;
        private int    timeout                               = 3000;
        private int    pingConnectionInterval                = 0;
        private int    subscriptionConnectionMinimumIdleSize = 1;
        private int    subscriptionConnectionPoolSize        = 9;
        private long   dnsMonitoringInterval                 = 5000L;
        private String password;
        private String userName;

        protected Config config(Codec codec) {
            Config config = new Config();
            if (codec != null) {
                config.setCodec(codec);
            }
            if (StringUtils.hasText(getUserName())) {
                config.setUsername(getUserName());
            }
            if (StringUtils.hasText(getPassword())) {
                config.setPassword(getPassword());
            }
            return config;
        }

        protected String[] polishAddress(String address) {
            String[] addresses = address.split(DELIMITER);
            for (int i = 0; i < addresses.length; i++) {
                if (!addresses[i].startsWith(REDIS_PREFIX)) {
                    addresses[i] = REDIS_PREFIX + addresses[i];
                }
            }
            return addresses;
        }

        public abstract RedissonClient createClient(Codec codec);

    }


    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Cluster extends BaseServer {

        private int    slaveConnectionMinimumIdleSize  = 4;
        private int    slaveConnectionPoolSize         = 10;
        private int    masterConnectionMinimumIdleSize = 4;
        private int    masterConnectionPoolSize        = 10;
        private String nodeAddresses;
        private int    scanInterval                    = 1000;

        @Override
        public RedissonClient createClient(Codec codec) {
            Config   config    = config(codec);
            String[] addresses = polishAddress(nodeAddresses);
            config.useClusterServers()
                  .addNodeAddress(addresses)
                  .setTimeout(getTimeout())
                  .setMasterConnectionPoolSize(masterConnectionPoolSize)
                  .setSlaveConnectionPoolSize(slaveConnectionPoolSize)
                  .setMasterConnectionMinimumIdleSize(masterConnectionMinimumIdleSize)
                  .setSlaveConnectionMinimumIdleSize(slaveConnectionMinimumIdleSize)
                  .setPingConnectionInterval(getPingConnectionInterval())
                  .setConnectTimeout(getConnectTimeout());
            return Redisson.create(config);
        }

    }


    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class MasterSlave extends BaseServer {

        private int    slaveConnectionMinimumIdleSize  = 4;
        private int    slaveConnectionPoolSize         = 10;
        private int    masterConnectionMinimumIdleSize = 4;
        private int    masterConnectionPoolSize        = 10;
        private int    database                        = 0;
        private String slaveAddresses;
        private String masterAddress;

        @Override
        public RedissonClient createClient(Codec codec) {
            Config config = config(codec);
            String master = masterAddress;
            if (!master.startsWith(REDIS_PREFIX)) {
                master = REDIS_PREFIX + master;
            }
            String[] slaves = polishAddress(slaveAddresses);
            config.useMasterSlaveServers()
                  .setMasterAddress(master)
                  .addSlaveAddress(slaves)
                  .setTimeout(getTimeout())
                  .setMasterConnectionPoolSize(masterConnectionPoolSize)
                  .setMasterConnectionMinimumIdleSize(masterConnectionMinimumIdleSize)
                  .setPingConnectionInterval(getPingConnectionInterval())
                  .setSlaveConnectionPoolSize(slaveConnectionPoolSize)
                  .setSlaveConnectionMinimumIdleSize(slaveConnectionMinimumIdleSize);
            return Redisson.create(config);
        }

    }


    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Replicated extends BaseServer {

        private String address;
        private int    slaveConnectionMinimumIdleSize  = 4;
        private int    slaveConnectionPoolSize         = 10;
        private int    masterConnectionMinimumIdleSize = 4;
        private int    masterConnectionPoolSize        = 10;
        private int    scanInterval                    = 1000;
        private int    database                        = 0;

        @Override
        public RedissonClient createClient(Codec codec) {
            Config   config    = config(codec);
            String[] addresses = polishAddress(address);
            config.useReplicatedServers()
                  .addNodeAddress(addresses)
                  .setDatabase(database)
                  .setScanInterval(scanInterval)
                  .setTimeout(getTimeout())
                  .setMasterConnectionPoolSize(masterConnectionPoolSize)
                  .setMasterConnectionMinimumIdleSize(masterConnectionMinimumIdleSize)
                  .setPingConnectionInterval(getPingConnectionInterval())
                  .setSlaveConnectionPoolSize(slaveConnectionPoolSize)
                  .setSlaveConnectionMinimumIdleSize(slaveConnectionMinimumIdleSize);
            return Redisson.create(config);
        }

    }


    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Sentinel extends BaseServer {

        private int    slaveConnectionMinimumIdleSize  = 4;
        private int    slaveConnectionPoolSize         = 10;
        private int    masterConnectionMinimumIdleSize = 4;
        private int    masterConnectionPoolSize        = 10;
        private int    database                        = 0;
        private String sentinelAddresses;
        private String masterName;

        @Override
        public RedissonClient createClient(Codec codec) {
            Config   config    = config(codec);
            String[] addresses = polishAddress(sentinelAddresses);
            config.useSentinelServers()
                  .addSentinelAddress(addresses)
                  .setMasterName(masterName)
                  .setTimeout(getTimeout())
                  .setMasterConnectionPoolSize(masterConnectionPoolSize)
                  .setSlaveConnectionPoolSize(slaveConnectionPoolSize)
                  .setMasterConnectionMinimumIdleSize(masterConnectionMinimumIdleSize)
                  .setSlaveConnectionMinimumIdleSize(slaveConnectionMinimumIdleSize)
                  .setPingConnectionInterval(getPingConnectionInterval())
                  .setDatabase(database);
            return Redisson.create(config);
        }

    }


    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Standalone extends BaseServer {

        private String address;
        private int    connectionMinimumIdleSize = 4;
        private int    connectionPoolSize        = 10;
        private int    database                  = 0;

        @Override
        public RedissonClient createClient(Codec codec) {
            Config config = config(codec);
            String server = address;
            if (!server.startsWith(REDIS_PREFIX)) {
                server = REDIS_PREFIX + server;
            }
            config.useSingleServer()
                  .setAddress(server)
                  .setTimeout(getTimeout())
                  .setConnectionPoolSize(connectionPoolSize)
                  .setConnectionMinimumIdleSize(connectionMinimumIdleSize)
                  .setPingConnectionInterval(getPingConnectionInterval())
                  .setDatabase(database);
            return Redisson.create(config);
        }

    }

}
