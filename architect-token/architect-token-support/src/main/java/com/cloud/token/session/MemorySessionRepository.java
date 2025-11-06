package com.cloud.token.session;

import com.cloud.token.dual.IDualSafeRepository;
import com.cloud.token.muted.IMutedRepository;
import com.cloud.token.utils.CommonUtils;
import com.cloud.token.utils.TokenConstants;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class MemorySessionRepository implements ISessionRepository, IDualSafeRepository, IMutedRepository {

    private static final long DEFAULT_UNTIL_TIME = TimeUnit.SECONDS.toMillis(30);

    private final Map<String, Object>         dataMap     = Maps.newConcurrentMap();
    private final Map<String, Long>           expireMap   = Maps.newConcurrentMap();
    private final LinkedBlockingQueue<String> expireQueue = new LinkedBlockingQueue<>();
    private final AtomicBoolean               started     = new AtomicBoolean(false);

    private Thread expireWorker;

    /**
     * 检测并清理过期key
     */
    public void checkAndClearTimeout(String key) {
        Long expireAt = this.expireMap.get(key);
        if (expireAt != null
                && !expireAt.equals(TokenConstants.NEVER_EXPIRE)
                && expireAt < System.currentTimeMillis()) {
            this.expireQueue.add(key);
        }
    }

    /**
     * 获取指定key的剩余存活时间
     */
    long keyTtl(String key) {
        //惰性检查
        this.checkAndClearTimeout(key);
        Long expireAt = expireMap.get(key);
        if (expireAt == null) {
            return TokenConstants.NO_EXPIRE_VALUE;
        }
        if (expireAt.equals(TokenConstants.NEVER_EXPIRE)) {
            return TokenConstants.NEVER_EXPIRE;
        }
        long timeout = (expireAt - System.currentTimeMillis()) / 1000;
        if (timeout < 0) {
            this.expireQueue.add(key);
            return TokenConstants.NO_EXPIRE_VALUE;
        }
        return timeout;
    }

    @Override
    public void save(String key, Object value, long timeout) {
        if (timeout == 0 || timeout <= TokenConstants.NO_EXPIRE_VALUE) {
            return;
        }
        this.dataMap.put(key, value);
        long expireAt = CommonUtils.expireAt(timeout, TimeUnit.SECONDS);
        this.expireMap.put(key, expireAt);
    }

    @Override
    public boolean delete(String key) {
        Object value = this.dataMap.remove(key);
        if (value != null) {
            this.expireMap.remove(key);
        }
        return value != null;
    }

    @Override
    public void update(String key, Object loginId) {
        long ttl = keyTtl(key);
        if (ttl == TokenConstants.NO_EXPIRE_VALUE) {
            return;
        }
        dataMap.put(key, loginId);
    }

    @Override
    public void refreshTtl(String key, long timeout) {
        long ttl = keyTtl(key);
        if (ttl == TokenConstants.NO_EXPIRE_VALUE || ttl == TokenConstants.NEVER_EXPIRE) {
            return;
        }
        long expireAt = CommonUtils.expireAt(timeout, TimeUnit.SECONDS);
        expireMap.put(key, expireAt);
    }

    @Override
    public long ttl(String key) {
        return keyTtl(key);
    }

    @Override
    public Object get(String key) {
        checkAndClearTimeout(key);
        return dataMap.get(key);
    }

    @Override
    public void save(Session session, long timeout) {
        if (timeout == 0 || timeout <= TokenConstants.NO_EXPIRE_VALUE) {
            return;
        }
        String sessionId = session.getSessionId();
        dataMap.put(sessionId, sessionId);
        long expireAt = CommonUtils.expireAt(timeout, TimeUnit.SECONDS);
        expireMap.put(sessionId, expireAt);
    }

    @Override
    public void updateSession(Session session) {
        String sessionId = session.getSessionId();
        long   ttl       = keyTtl(sessionId);
        if (ttl == TokenConstants.NO_EXPIRE_VALUE) {
            return;
        }
        dataMap.put(sessionId, session);
    }

    @Override
    public void refreshSessionTtl(String sessionId, long timeout) {
        this.refreshTtl(sessionId, timeout);
    }

    @Override
    public void deleteSession(String sessionId) {
        this.dataMap.remove(sessionId);
        this.expireMap.remove(sessionId);
    }

    @Override
    public long sessionTtl(String sessionId) {
        return keyTtl(sessionId);
    }

    @Override
    public Session getSession(String sessionId) {
        checkAndClearTimeout(sessionId);
        return (Session) dataMap.get(sessionId);
    }

    @Override
    public void save(String key, long timeout) {
        if (timeout == 0 || timeout <= TokenConstants.NO_EXPIRE_VALUE) {
            return;
        }
        this.dataMap.put(key, TokenConstants.AUTH_SAFE_VALUE);
        long expireAt = CommonUtils.expireAt(timeout, TimeUnit.SECONDS);
        this.expireMap.put(key, expireAt);
    }

    @Override
    public boolean isSafe(String key) {
        this.checkAndClearTimeout(key);
        String value = (String) this.dataMap.get(key);
        return StringUtils.isNotBlank(value);
    }

    @Override
    public void save(String key, int level, long timeout) {
        if (timeout == 0 || timeout <= TokenConstants.NO_EXPIRE_VALUE) {
            return;
        }
        this.dataMap.put(key, level);
        long expireAt = CommonUtils.expireAt(timeout, TimeUnit.HOURS);
        this.expireMap.put(key, expireAt);
    }

    @Override
    public boolean isMuted(String key, int level) {
        int forbidden = this.getMuted(key);
        if (forbidden == TokenConstants.NO_MUTED_LEVEL) {
            return false;
        }
        return level >= forbidden;
    }

    @Override
    public int getMuted(String key) {
        this.checkAndClearTimeout(key);
        Integer value = (Integer) this.dataMap.get(key);
        if (value == null) {
            return TokenConstants.NO_MUTED_LEVEL;
        }
        return value;
    }

    @Override
    public boolean cancel(String key) {
        int level = this.getMuted(key);
        if (level == TokenConstants.NO_MUTED_LEVEL) {
            return false;
        }
        this.dataMap.remove(key);
        this.expireMap.remove(key);
        return true;
    }

    @Override
    public void initialize() {
        this.expireWorker = new Thread(() -> {
            do {
                try {
                    String key = this.expireQueue.poll(DEFAULT_UNTIL_TIME, TimeUnit.MILLISECONDS);
                    if (key != null) {
                        this.dataMap.remove(key);
                    }
                } catch (InterruptedException e) {
                    log.warn(e.getMessage(), e);
                }
            }
            while (started.get());
        });
        this.expireWorker.start();
    }

    @Override
    public void destroy() {
        started.set(false);
        if (!this.expireWorker.isInterrupted()) {
            this.expireWorker.interrupt();
        }
    }

}
