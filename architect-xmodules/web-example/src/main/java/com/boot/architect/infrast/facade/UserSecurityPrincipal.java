package com.boot.architect.infrast.facade;

import com.cloud.arch.web.support.GrantedPrincipal;
import com.cloud.arch.web.support.SecurityPrincipal;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class UserSecurityPrincipal implements SecurityPrincipal {

    private final Map<String, Set<String>> permits = Maps.newHashMap();
    private final Map<String, Set<String>> roles   = Maps.newHashMap();

    public UserSecurityPrincipal() {
        permits.put("123456789", Sets.newHashSet("read"));
        permits.put("12345678", Sets.newHashSet("read", "write"));
        roles.put("123456789", Sets.newHashSet("role1", "role2"));
        roles.put("12345678", Sets.newHashSet("role2"));
    }

    @Override
    public String domain() {
        return "user";
    }

    @Override
    public GrantedPrincipal principal(String identity) {
        Set<String> permits = this.permits.getOrDefault(identity, Sets.newHashSet());
        Set<String> roles   = this.roles.getOrDefault(identity, Sets.newHashSet());
        return new GrantedPrincipal(roles, permits);
    }

}
