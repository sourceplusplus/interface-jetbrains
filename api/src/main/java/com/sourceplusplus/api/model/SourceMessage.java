package com.sourceplusplus.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sourceplusplus.api.client.SourceClient;
import com.sourceplusplus.api.model.application.SourceApplication;
import com.sourceplusplus.api.model.artifact.SourceArtifact;
import com.sourceplusplus.api.model.artifact.SourceArtifactConfig;
import com.sourceplusplus.api.model.artifact.SourceArtifactUnsubscribeRequest;
import com.sourceplusplus.api.model.info.SourceCoreInfo;
import com.sourceplusplus.api.model.internal.ApplicationArtifact;
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

    static <T> MessageCodec<T, T> messageCodec(Class<T> type) {
        return new MessageCodec<T, T>() {

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
        };
    }

    static void registerCodecs(@NotNull Vertx vertx) {
        SourceClient.initMappers();

        vertx.eventBus().registerDefaultCodec(SourceCoreInfo.class, SourceMessage.messageCodec(SourceCoreInfo.class));
        vertx.eventBus().registerDefaultCodec(SourceApplication.class, SourceMessage.messageCodec(SourceApplication.class));
        vertx.eventBus().registerDefaultCodec(ArtifactMetrics.class, SourceMessage.messageCodec(ArtifactMetrics.class));
        vertx.eventBus().registerDefaultCodec(ArtifactMetricResult.class, SourceMessage.messageCodec(ArtifactMetricResult.class));
        vertx.eventBus().registerDefaultCodec(SourceArtifactConfig.class, SourceMessage.messageCodec(SourceArtifactConfig.class));
        vertx.eventBus().registerDefaultCodec(SourceArtifact.class, SourceMessage.messageCodec(SourceArtifact.class));
        vertx.eventBus().registerDefaultCodec(ApplicationArtifact.class, SourceMessage.messageCodec(ApplicationArtifact.class));
        vertx.eventBus().registerDefaultCodec(TraceSpan.class, SourceMessage.messageCodec(TraceSpan.class));
        vertx.eventBus().registerDefaultCodec(ArtifactMetricSubscribeRequest.class, SourceMessage.messageCodec(ArtifactMetricSubscribeRequest.class));
        vertx.eventBus().registerDefaultCodec(ArtifactMetricUnsubscribeRequest.class, SourceMessage.messageCodec(ArtifactMetricUnsubscribeRequest.class));
        vertx.eventBus().registerDefaultCodec(ArtifactTraceSubscribeRequest.class, SourceMessage.messageCodec(ArtifactTraceSubscribeRequest.class));
        vertx.eventBus().registerDefaultCodec(ArtifactTraceUnsubscribeRequest.class, SourceMessage.messageCodec(ArtifactTraceUnsubscribeRequest.class));
        vertx.eventBus().registerDefaultCodec(SourceArtifactUnsubscribeRequest.class, SourceMessage.messageCodec(SourceArtifactUnsubscribeRequest.class));
        vertx.eventBus().registerDefaultCodec(TimeFramedMetricType.class, SourceMessage.messageCodec(TimeFramedMetricType.class));
        vertx.eventBus().registerDefaultCodec(ArtifactTraceResult.class, SourceMessage.messageCodec(ArtifactTraceResult.class));
        vertx.eventBus().registerDefaultCodec(TraceQuery.class, SourceMessage.messageCodec(TraceQuery.class));
        vertx.eventBus().registerDefaultCodec(TraceQueryResult.class, SourceMessage.messageCodec(TraceQueryResult.class));
        vertx.eventBus().registerDefaultCodec(Trace.class, SourceMessage.messageCodec(Trace.class));
        vertx.eventBus().registerDefaultCodec(TraceSpanStackQuery.class, SourceMessage.messageCodec(TraceSpanStackQuery.class));
        vertx.eventBus().registerDefaultCodec(TraceSpanStackQueryResult.class, SourceMessage.messageCodec(TraceSpanStackQueryResult.class));
    }
}
