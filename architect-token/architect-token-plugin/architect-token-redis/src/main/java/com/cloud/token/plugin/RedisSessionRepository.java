package com.cloud.token.plugin;

import com.cloud.token.session.ISessionRepository;
import com.cloud.token.session.Session;
import com.cloud.token.utils.TokenConstants;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.time.Duration;

@Slf4j
@AllArgsConstructor
public class RedisSessionRepository implements ISessionRepository {

    private final RedissonClient redissonClient;

    @Override
    public void save(String key, Object value, long timeout) {
        if (timeout == 0 || timeout <= TokenConstants.NO_EXPIRE_VALUE) {
            return;
        }
        RBucket<Object> bucket = redissonClient.getBucket(key);
        if (timeout == TokenConstants.NEVER_EXPIRE) {
            bucket.set(value);
            return;
        }
        bucket.set(value, Duration.ofSeconds(timeout));
    }

    @Override
    public boolean delete(String key) {
        return redissonClient.getBucket(key).delete();
    }

    @Override
    public void update(String key, Object value) {
        long ttl = this.ttl(key);
        if (ttl == TokenConstants.NO_EXPIRE_VALUE) {
            return;
        }
        this.save(key, value, ttl);
    }

    @Override
    public void refreshTtl(String key, long timeout) {
        long ttl = ttl(key);
        if (ttl == TokenConstants.NO_EXPIRE_VALUE || (ttl == TokenConstants.NEVER_EXPIRE
                && timeout == TokenConstants.NEVER_EXPIRE)) {
            //key存储对象不存在或者永久过期时间再次设置永久过期时间
            return;
        }
        redissonClient.getBucket(key).expire(Duration.ofSeconds(timeout));
    }

    @Override
    public long ttl(String key) {
        RBucket<Object> bucket     = redissonClient.getBucket(key);
        long            timeToLive = bucket.remainTimeToLive();
        return timeToLive < 0 ? timeToLive : timeToLive / 1000;
    }

    @Override
    public Object get(String key) {
        return redissonClient.getBucket(key).get();
    }

    @Override
    public void save(Session session, long timeout) {
        if (timeout == 0 || timeout <= TokenConstants.NO_EXPIRE_VALUE) {
            return;
        }
        RBucket<Session> bucket = redissonClient.getBucket(session.getSessionId());
        if (timeout == TokenConstants.NEVER_EXPIRE) {
            bucket.set(session);
            return;
        }
        bucket.set(session, Duration.ofSeconds(timeout));
    }

    @Override
    public void updateSession(Session session) {
        long ttl = this.ttl(session.getSessionId());
        if (ttl == TokenConstants.NO_EXPIRE_VALUE) {
            return;
        }
        this.save(session, ttl);
    }

    @Override
    public void refreshSessionTtl(String sessionId, long timeout) {
        this.refreshTtl(sessionId, timeout);
    }

    @Override
    public void deleteSession(String sessionId) {
        this.redissonClient.getBucket(sessionId).delete();
    }

    @Override
    public long sessionTtl(String sessionId) {
        return this.ttl(sessionId);
    }

    @Override
    public Session getSession(String sessionId) {
        RBucket<Session> bucket = this.redissonClient.getBucket(sessionId);
        return bucket.get();
    }

}
