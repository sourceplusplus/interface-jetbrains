package com.sourceplusplus.api.model.config;

/**
 * todo: description
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.2.0
 * @since 0.2.0
 */
public class SourceEnvironmentConfig {

    public volatile String apiHost;
    public volatile int apiPort;
    public volatile boolean apiSslEnabled;
    public volatile String apiKey = null;

    public String getSppUrl() {
        if (apiSslEnabled) {
            return "https://" + apiHost + ":" + apiPort;
        } else {
            return "http://" + apiHost + ":" + apiPort;
        }
    }
}
