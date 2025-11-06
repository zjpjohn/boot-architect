package com.cloud.arch.web.custom;

import com.cloud.arch.web.props.WebmvcProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.mvc.condition.RequestCondition;

public record VersionRequestCondition(RequestVersion version, WebmvcProperties.VersionConfig config)
        implements RequestCondition<VersionRequestCondition> {

    @Override
    public VersionRequestCondition combine(VersionRequestCondition other) {
        if (other.version.compareTo(this.version) > 0) {
            return new VersionRequestCondition(other.version, config);
        }
        return this;
    }

    @Override
    public VersionRequestCondition getMatchingCondition(HttpServletRequest request) {
        RequestVersion requestVersion = VersionConditionNegotiate.getVersion(request, config);
        if (requestVersion != null) {
            return VersionConditionNegotiate.negotiate(requestVersion, this);
        }
        return null;
    }

    @Override
    public int compareTo(VersionRequestCondition other, HttpServletRequest request) {
        return other.version.compareTo(this.version);
    }

}
