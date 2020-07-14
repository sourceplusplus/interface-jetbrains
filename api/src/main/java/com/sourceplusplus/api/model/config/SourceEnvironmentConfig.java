package com.sourceplusplus.api.model.config;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.sourceplusplus.api.client.SourceCoreClient;

import java.beans.Transient;
import java.util.Objects;

/**
 * Holds configuration necessary to connect to core.
 *
 * @version 0.3.2
 * @since 0.2.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class SourceEnvironmentConfig {

    public transient SourceCoreClient coreClient;
    public volatile String appUuid;
    public volatile String environmentName;
    public volatile String apiHost;
    public volatile int apiPort;
    public volatile boolean apiSslEnabled;
    public volatile String apiKey;
    public volatile boolean embedded;
    public volatile String applicationDomain;

    @Transient
    public String getSppUrl() {
        if (apiSslEnabled) {
            return "https://" + apiHost + ":" + apiPort;
        } else {
            return "http://" + apiHost + ":" + apiPort;
        }
    }

    @Override
    public String toString() {
        return String.format("Environment: %s (%s:%d)", environmentName, apiHost, apiPort);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SourceEnvironmentConfig that = (SourceEnvironmentConfig) o;
        return environmentName.equals(that.environmentName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(environmentName);
    }
}
