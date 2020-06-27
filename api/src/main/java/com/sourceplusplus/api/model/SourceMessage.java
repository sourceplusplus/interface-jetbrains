package com.sourceplusplus.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sourceplusplus.api.client.SourceClient;
import com.sourceplusplus.api.model.application.SourceApplication;
import com.sourceplusplus.api.model.artifact.SourceArtifact;
import com.sourceplusplus.api.model.artifact.SourceArtifactConfig;
import com.sourceplusplus.api.model.artifact.SourceArtifactUnsubscribeRequest;
import com.sourceplusplus.api.model.info.SourceCoreInfo;
import com.sourceplusplus.api.model.integration.IntegrationInfo;
import com.sourceplusplus.api.model.metric.*;
import com.sourceplusplus.api.model.trace.*;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * Base message for all Source++ API messages.
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.2.6
 * @since 0.1.0
 */
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public interface SourceMessage extends Serializable {

    static void registerCodecs(@NotNull Vertx vertx) {
        SourceClient.initMappers();

        registerCodec(vertx, SourceCoreInfo.class);
        registerCodec(vertx, SourceApplication.class);
        registerCodec(vertx, ArtifactMetrics.class);
        registerCodec(vertx, ArtifactMetricResult.class);
        registerCodec(vertx, SourceArtifactConfig.class);
        registerCodec(vertx, SourceArtifact.class);
        registerCodec(vertx, TraceSpan.class);
        registerCodec(vertx, ArtifactMetricSubscribeRequest.class);
        registerCodec(vertx, ArtifactMetricUnsubscribeRequest.class);
        registerCodec(vertx, ArtifactTraceSubscribeRequest.class);
        registerCodec(vertx, ArtifactTraceUnsubscribeRequest.class);
        registerCodec(vertx, SourceArtifactUnsubscribeRequest.class);
        registerCodec(vertx, TimeFramedMetricType.class);
        registerCodec(vertx, ArtifactTraceResult.class);
        registerCodec(vertx, TraceQuery.class);
        registerCodec(vertx, TraceQueryResult.class);
        registerCodec(vertx, Trace.class);
        registerCodec(vertx, TraceSpanStackQuery.class);
        registerCodec(vertx, TraceSpanStackQueryResult.class);
        registerCodec(vertx, IntegrationInfo.class);
    }

    static <T> void registerCodec(@NotNull Vertx vertx, @NotNull Class<T> type) {
        if (vertx.sharedData().getLocalMap("registered_source_message_codecs")
                .putIfAbsent(type.getCanonicalName(), true) == null) {
            vertx.eventBus().registerDefaultCodec(type, new SourceMessageCodec<>(type));
        }
    }

    class SourceMessageCodec<T> implements MessageCodec<T, T> {
        private final Class<T> type;

        SourceMessageCodec(Class<T> type) {
            this.type = type;
        }

        @Override
        public void encodeToWire(Buffer buffer, T o) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public T decodeFromWire(int pos, Buffer buffer) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public T transform(T o) {
            return o;
        }

        @Override
        public String name() {
            return type.getSimpleName();
        }

        @Override
        public byte systemCodecID() {
            return -1;
        }

        public Class<T> type() {
            return type;
        }
    }
}
