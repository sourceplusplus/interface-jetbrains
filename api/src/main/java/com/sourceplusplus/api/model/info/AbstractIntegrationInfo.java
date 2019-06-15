package com.sourceplusplus.api.model.info;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sourceplusplus.api.model.SourceMessage;
import com.sourceplusplus.api.model.SourceStyle;
import org.immutables.value.Value;

import java.io.Serializable;

/**
 * todo: description
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.2.0
 * @since 0.1.1
 */
@SourceStyle
@Value.Immutable
@JsonSerialize(as = IntegrationInfo.class)
@JsonDeserialize(as = IntegrationInfo.class)
public interface AbstractIntegrationInfo extends SourceMessage {

    String name();

    IntegrationCategory category();

    String version();

    ConnectionInfo connection();

    class ConnectionInfo implements Serializable {
        private final String host;
        private final int port;

        public ConnectionInfo(String host, int port) {
            this.host = host;
            this.port = port;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }
    }
}
