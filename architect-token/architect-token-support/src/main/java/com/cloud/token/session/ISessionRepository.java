package com.cloud.token.session;

public interface ISessionRepository {

    default void initialize() {
    }

    default void destroy() {
    }

    /**
     * 保存token-loginId映射关系
     */
    void save(String key, Object value, long timeout);

    /**
     * 删除token-loginId映射关系
     */
    boolean delete(String key);

    /**
     * 更新token-loginId映射关系
     */
    void update(String key, Object value);

    /**
     * 更新token-loginId映射过期时间
     */
    void refreshTtl(String key, long timeout);

    /**
     * 查询token-loginId映射关系剩余存活时间
     */
    long ttl(String key);

    /**
     * 根据token查询loginId
     */
    Object get(String key);

    /**
     * 保存会话信息
     */
    void save(Session session, long timeout);

    /**
     * 更新会话信息
     */
    void updateSession(Session session);

    /**
     * 更新会话存活时间
     */
    void refreshSessionTtl(String sessionId, long timeout);

    /**
     * 删除指定登录标识的会话信息
     */
    void deleteSession(String sessionId);

    /**
     * 查询会话的剩余存活时间
     */
    long sessionTtl(String sessionId);

    /**
     * 查询指定账户的会话信息
     */
    Session getSession(String sessionId);

}
