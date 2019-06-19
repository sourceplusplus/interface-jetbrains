package com.sourceplusplus.api.model.config;

import com.sourceplusplus.api.client.SourceCoreClient;

import java.util.Objects;

/**
 * todo: description
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.2.0
 * @since 0.2.0
 */
public class SourceEnvironmentConfig {

    public transient SourceCoreClient coreClient;
    public volatile String appUuid;
    public volatile String environmentName;
    public volatile String apiHost;
    public volatile int apiPort;
    public volatile boolean apiSslEnabled;
    public volatile String apiKey;

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
        return apiPort == that.apiPort &&
                apiSslEnabled == that.apiSslEnabled &&
                Objects.equals(environmentName, that.environmentName) &&
                Objects.equals(apiHost, that.apiHost) &&
                (((apiKey == null || apiKey.isEmpty()) && (that.apiKey == null || that.apiKey.isEmpty()))
                        || Objects.equals(apiKey, that.apiKey));
    }

    @Override
    public int hashCode() {
        return Objects.hash(environmentName);
    }
}
