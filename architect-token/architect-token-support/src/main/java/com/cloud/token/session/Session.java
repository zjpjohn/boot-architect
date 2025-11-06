package com.cloud.token.session;

import com.cloud.token.utils.CommonUtils;
import com.cloud.token.utils.TokenConstants;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class Session {

    /**
     * session会话所属类型
     */
    private String               type;
    /**
     * session会话所属业务域
     */
    private String               realm;
    /**
     * session会话所属登录用户标识
     */
    private Object               loginId;
    /**
     * 当前会话token,当为token-session时存在值
     */
    private String               token;
    /**
     * 当前会话创建时间
     */
    private long                 createTime;
    /**
     * session会话绑定的token属性集合
     */
    private List<TokenAttribute> attr  = Lists.newArrayList();
    /**
     * 会话扩展数据
     */
    private Map<String, Object>  extra = Maps.newConcurrentMap();

    public Session(String type, String realm, Object loginId) {
        this(type, realm, loginId, null);
    }

    public Session(String type, String realm, Object loginId, String token) {
        this.type       = type;
        this.realm      = realm;
        this.loginId    = loginId;
        this.token      = token;
        this.createTime = System.currentTimeMillis();
    }

    /**
     * 获取当前会话的sessionId
     */
    public String getSessionId() {
        return CommonUtils.sessionId(TokenConstants.SESSION_PREFIX, realm, this.loginId);
    }

    public List<TokenAttribute> copyAttrs() {
        return Lists.newArrayList(this.attr);
    }

    public List<TokenAttribute> attrsByDevice(String device) {
        if (StringUtils.isBlank(device)) {
            return Lists.newArrayList(this.attr);
        }
        return attr.stream().filter(a -> device.equals(a.getDevice())).collect(Collectors.toList());
    }

    public List<String> tokensByDevice(String device) {
        return this.attr.stream()
                        .map(TokenAttribute::getDevice)
                        .filter(e -> StringUtils.isBlank(device) || device.equals(e))
                        .collect(Collectors.toList());
    }

    public TokenAttribute getAttr(String token) {
        return this.attr.stream().filter(e -> e.getToken().equals(token)).findFirst().orElse(null);
    }

    public Session appendAttr(TokenAttribute attribute) {
        TokenAttribute oldAttr = this.getAttr(attribute.getToken());
        if (oldAttr == null) {
            this.attr.add(attribute);
            return this;
        }
        oldAttr.setDevice(attribute.getDevice());
        oldAttr.setAttr(attribute.getAttr());
        return this;
    }

    public Session bindDevice(String token, String device) {
        return appendAttr(new TokenAttribute(token, device));
    }

    public boolean removeAttr(String token) {
        return Optional.ofNullable(this.getAttr(token)).map(e -> this.attr.remove(e)).orElse(false);
    }

    public Session putExtra(String key, Object value) {
        this.extra.put(key, value);
        return this;
    }

    public Object getExtra(String key) {
        return this.extra.get(key);
    }

    public Session putExtraIfAbsent(String key, Object value) {
        Object oldVal = this.extra.get(key);
        if (oldVal == null || oldVal.equals("")) {
            this.extra.put(key, value);
        }
        return this;
    }

    public boolean removeExtra(String key) {
        return this.extra.remove(key) != null;
    }

}
