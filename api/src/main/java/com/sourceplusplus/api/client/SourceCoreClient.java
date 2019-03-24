package com.sourceplusplus.api.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sourceplusplus.api.model.application.SourceApplication;
import com.sourceplusplus.api.model.application.SourceApplicationSubscription;
import com.sourceplusplus.api.model.artifact.*;
import com.sourceplusplus.api.model.info.SourceCoreInfo;
import com.sourceplusplus.api.model.metric.ArtifactMetricUnsubscribeRequest;
import com.sourceplusplus.api.model.trace.*;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import okhttp3.*;

import java.net.URLEncoder;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Used to communicate with Source++ Core.
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.1.2
 * @since 0.1.0
 */
public class SourceCoreClient implements SourceClient {

    private final static String SPP_API_VERSION = System.getenv().getOrDefault(
            "SPP_API_VERSION", System.getProperty("SPP_API_VERSION", "v1"));

    private static final String PING_ENDPOINT = "/ping";
    private static final String INFO_ENDPOINT = String.format("/%s/info", SPP_API_VERSION);
    private static final String REGISTER_IP_ENDPOINT = String.format("/%s/registerIP", SPP_API_VERSION);
    private static final String REFRESH_STORAGE = String.format(
            "/%s/admin/storage/refresh", SPP_API_VERSION);
    private static final String SEARCH_FOR_NEW_ENDPOINTS = String.format(
            "/%s/admin/integrations/skywalking/searchForNewEndpoints", SPP_API_VERSION);
    private static final String CREATE_APPLICATION_ENDPOINT = String.format(
            "/%s/applications", SPP_API_VERSION);
    private static final String GET_APPLICATION_SUBSCRIPTIONS_ENDPOINT = String.format(
            "/%s/applications/:appUuid/subscriptions?includeAutomatic=:includeAutomatic", SPP_API_VERSION);
    private static final String GET_SUBSCRIBER_APPLICATION_SUBSCRIPTIONS_ENDPOINT = String.format(
            "/%s/applications/:appUuid/subscribers/:subscriberUuid/subscriptions", SPP_API_VERSION);
    private static final String REFRESH_SUBSCRIBER_APPLICATION_SUBSCRIPTIONS_ENDPOINT = String.format(
            "/%s/applications/:appUuid/subscribers/:subscriberUuid/subscriptions/refresh", SPP_API_VERSION);
    private static final String UPDATE_APPLICATION_ENDPOINT = String.format(
            "/%s/applications/:appUuid", SPP_API_VERSION);
    private static final String GET_APPLICATION_ENDPOINT = String.format(
            "/%s/applications/:appUuid", SPP_API_VERSION);
    private static final String GET_APPLICATIONS_ENDPOINT = String.format(
            "/%s/applications", SPP_API_VERSION);
    private static final String CREATE_SOURCE_ARTIFACT_ENDPOINT = String.format(
            "/%s/applications/:appUuid/artifacts/:artifactQualifiedName", SPP_API_VERSION);
    private static final String GET_SOURCE_ARTIFACT_ENDPOINT = CREATE_SOURCE_ARTIFACT_ENDPOINT;
    private static final String CONFIGURE_SOURCE_ARTIFACT_ENDPOINT = String.format(
            CREATE_SOURCE_ARTIFACT_ENDPOINT + "/config", SPP_API_VERSION);
    private static final String UNSUBSCRIBE_SOURCE_ARTIFACT_ENDPOINT = String.format(
            CREATE_SOURCE_ARTIFACT_ENDPOINT + "/unsubscribe", SPP_API_VERSION);
    private static final String SUBSCRIBE_SOURCE_ARTIFACT_METRICS_ENDPOINT = String.format(
            CREATE_SOURCE_ARTIFACT_ENDPOINT + "/metrics/subscribe", SPP_API_VERSION);
    private static final String UNSUBSCRIBE_SOURCE_ARTIFACT_METRICS_ENDPOINT = String.format(
            CREATE_SOURCE_ARTIFACT_ENDPOINT + "/metrics/unsubscribe", SPP_API_VERSION);
    private static final String SUBSCRIBE_SOURCE_ARTIFACT_TRACES_ENDPOINT = String.format(
            CREATE_SOURCE_ARTIFACT_ENDPOINT + "/traces/subscribe", SPP_API_VERSION);
    private static final String UNSUBSCRIBE_SOURCE_ARTIFACT_TRACES_ENDPOINT = String.format(
            CREATE_SOURCE_ARTIFACT_ENDPOINT + "/traces/unsubscribe", SPP_API_VERSION);
    private static final String GET_SOURCE_ARTIFACT_CONFIGURATION_ENDPOINT = CONFIGURE_SOURCE_ARTIFACT_ENDPOINT;
    private static final String GET_TRACES_ENDPOINT = String.format(
            "/%s/applications/:appUuid/artifacts/:artifactQualifiedName/traces?orderType=:orderType", SPP_API_VERSION);
    private static final String GET_TRACE_SPANS_ENDPOINT = String.format(
            "/%s/applications/:appUuid/artifacts/:artifactQualifiedName/traces/:traceId/spans" +
                    "?followExit=:followExit&oneLevelDeep=:oneLevelDeep&segmentId=:segmentId&spanId=:spanId", SPP_API_VERSION);

    private final OkHttpClient client = new OkHttpClient();
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final String sppUrl;
    private String apiKey;

    public SourceCoreClient(String host, int port, boolean ssl) {
        if (ssl) {
            this.sppUrl = "https://" + Objects.requireNonNull(host) + ":" + port;
        } else {
            this.sppUrl = "http://" + Objects.requireNonNull(host) + ":" + port;
        }
        SourceClient.initMappers();
    }

    public SourceCoreClient(String sppUrl) {
        this.sppUrl = Objects.requireNonNull(sppUrl);
        SourceClient.initMappers();
    }

    public void ping(Handler<AsyncResult<Boolean>> handler) {
        try {
            String url = sppUrl + PING_ENDPOINT;
            Request request = new Request.Builder().url(url).get().build();

            OkHttpClient timeOutClient = client.newBuilder()
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .writeTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(5, TimeUnit.SECONDS)
                    .build();
            try (Response response = timeOutClient.newCall(request).execute()) {
                if (response.code() == 200) {
                    handler.handle(Future.succeededFuture(true));
                } else {
                    handler.handle(Future.failedFuture(response.message()));
                }
            } catch (Exception e) {
                handler.handle(Future.failedFuture(e));
            }
        } catch (Exception e) {
            handler.handle(Future.failedFuture(e));
        }
    }

    public void info(Handler<AsyncResult<SourceCoreInfo>> handler) {
        String url = sppUrl + INFO_ENDPOINT;
        Request.Builder request = new Request.Builder().url(url).get();
        addHeaders(request);

        try (Response response = client.newCall(request.build()).execute()) {
            handler.handle(Future.succeededFuture(Json.decodeValue(response.body().string(), SourceCoreInfo.class)));
        } catch (Exception e) {
            handler.handle(Future.failedFuture(e));
        }
    }

    public void registerIP() {
        String url = sppUrl + REGISTER_IP_ENDPOINT;
        Request.Builder request = new Request.Builder().url(url).get();
        addHeaders(request);

        try (Response response = client.newCall(request.build()).execute()) {
            if (!response.isSuccessful()) {
                throw new IllegalStateException("Failed to register IP");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean refreshStorage() {
        String url = sppUrl + REFRESH_STORAGE;
        Request.Builder request = new Request.Builder().url(url).get();
        addHeaders(request);

        try (Response response = client.newCall(request.build()).execute()) {
            if (response.isSuccessful()) {
                return true;
            } else {
                throw new IllegalStateException("Unknown response: " + response.body().string());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void searchForNewEndpoints(Handler<AsyncResult<Boolean>> handler) {
        String url = sppUrl + SEARCH_FOR_NEW_ENDPOINTS;
        Request.Builder request = new Request.Builder().url(url).get();
        addHeaders(request);

        try (Response response = client.newCall(request.build()).execute()) {
            if (response.isSuccessful()) {
                handler.handle(Future.succeededFuture(response.isSuccessful()));
            } else {
                handler.handle(Future.failedFuture(response.body().string()));
            }
        } catch (Exception e) {
            handler.handle(Future.failedFuture(e));
        }
    }

    public boolean searchForNewEndpoints() {
        String url = sppUrl + SEARCH_FOR_NEW_ENDPOINTS;
        Request.Builder request = new Request.Builder().url(url).get();
        addHeaders(request);

        try (Response response = client.newCall(request.build()).execute()) {
            if (response.isSuccessful()) {
                return true;
            } else {
                throw new IllegalStateException("Unknown response: " + response.body().string());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void createApplication(Handler<AsyncResult<SourceApplication>> handler) {
        createApplication(SourceApplication.builder().isCreateRequest(true).build(), handler);
    }

    public void createApplication(SourceApplication createRequest, Handler<AsyncResult<SourceApplication>> handler) {
        String url = sppUrl + CREATE_APPLICATION_ENDPOINT;
        Request.Builder request = new Request.Builder().url(url)
                .post(RequestBody.create(JSON, Json.encode(createRequest)));
        addHeaders(request);

        try (Response response = client.newCall(request.build()).execute()) {
            handler.handle(Future.succeededFuture(Json.decodeValue(response.body().string(), SourceApplication.class)));
        } catch (Exception e) {
            handler.handle(Future.failedFuture(e));
        }
    }

    public SourceApplication createApplication() {
        return createApplication(SourceApplication.builder().isCreateRequest(true).build());
    }

    public SourceApplication createApplication(SourceApplication createRequest) {
        String url = sppUrl + CREATE_APPLICATION_ENDPOINT;
        Request.Builder request = new Request.Builder().url(url)
                .post(RequestBody.create(JSON, Json.encode(createRequest)));
        addHeaders(request);

        try (Response response = client.newCall(request.build()).execute()) {
            return Json.decodeValue(response.body().string(), SourceApplication.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void updateApplication(SourceApplication updateRequest, Handler<AsyncResult<SourceApplication>> handler) {
        String url = sppUrl + UPDATE_APPLICATION_ENDPOINT.replace(":appUuid", updateRequest.appUuid());
        Request.Builder request = new Request.Builder().url(url)
                .put(RequestBody.create(JSON, Json.encode(updateRequest)));
        addHeaders(request);

        try (Response response = client.newCall(request.build()).execute()) {
            handler.handle(Future.succeededFuture(Json.decodeValue(response.body().string(), SourceApplication.class)));
        } catch (Exception e) {
            handler.handle(Future.failedFuture(e));
        }
    }

    public SourceApplication getApplication(String appUuid) {
        String url = sppUrl + GET_APPLICATION_ENDPOINT.replace(":appUuid", appUuid);
        Request.Builder request = new Request.Builder().url(url).get();
        addHeaders(request);

        try (Response response = client.newCall(request.build()).execute()) {
            if (response.isSuccessful()) {
                return Json.decodeValue(response.body().string(), SourceApplication.class);
            } else if (response.code() == 404) {
                return null;
            } else {
                throw new IllegalStateException("Unknown response: " + response.body().string());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void getApplication(String appUuid, Handler<AsyncResult<Optional<SourceApplication>>> handler) {
        String url = sppUrl + GET_APPLICATION_ENDPOINT.replace(":appUuid", appUuid);
        Request.Builder request = new Request.Builder().url(url).get();
        addHeaders(request);

        try (Response response = client.newCall(request.build()).execute()) {
            if (response.isSuccessful()) {
                handler.handle(Future.succeededFuture(Optional.of(Json.decodeValue(response.body().string(),
                        SourceApplication.class))));
            } else {
                handler.handle(Future.succeededFuture(Optional.empty()));
            }
        } catch (Exception e) {
            handler.handle(Future.failedFuture(e));
        }
    }

    public void getApplications(Handler<AsyncResult<List<SourceApplication>>> handler) {
        String url = sppUrl + GET_APPLICATIONS_ENDPOINT;
        Request.Builder request = new Request.Builder().url(url).get();
        addHeaders(request);

        try (Response response = client.newCall(request.build()).execute()) {
            handler.handle(Future.succeededFuture(Json.decodeValue(response.body().string(),
                    new TypeReference<List<SourceApplication>>() {
                    })));
        } catch (Exception e) {
            handler.handle(Future.failedFuture(e));
        }
    }

    public void getSubscriberApplicationSubscriptions(String appUuid,
                                                      Handler<AsyncResult<List<SourceArtifactSubscription>>> handler) {
        String url = sppUrl + GET_SUBSCRIBER_APPLICATION_SUBSCRIPTIONS_ENDPOINT
                .replace(":appUuid", appUuid)
                .replace(":subscriberUuid", CLIENT_ID);
        Request.Builder request = new Request.Builder().url(url).get();
        addHeaders(request);

        try (Response response = client.newCall(request.build()).execute()) {
            if (response.isSuccessful()) {
                handler.handle(Future.succeededFuture(Json.decodeValue(response.body().string(),
                        new TypeReference<List<SourceArtifactSubscription>>() {
                        })));
            } else {
                handler.handle(Future.failedFuture(response.message()));
            }
        } catch (Exception e) {
            handler.handle(Future.failedFuture(e));
        }
    }

    public void refreshSubscriberApplicationSubscriptions(String appUuid, Handler<AsyncResult<Void>> handler) {
        String url = sppUrl + REFRESH_SUBSCRIBER_APPLICATION_SUBSCRIPTIONS_ENDPOINT
                .replace(":appUuid", appUuid)
                .replace(":subscriberUuid", CLIENT_ID);
        Request.Builder request = new Request.Builder().url(url).put(RequestBody.create(null, new byte[0]));
        addHeaders(request);

        try (Response response = client.newCall(request.build()).execute()) {
            if (response.isSuccessful()) {
                handler.handle(Future.succeededFuture());
            } else {
                handler.handle(Future.failedFuture(response.message()));
            }
        } catch (Exception e) {
            handler.handle(Future.failedFuture(e));
        }
    }

    public List<SourceApplicationSubscription> getApplicationSubscriptions(String appUuid, boolean includeAutomatic) {
        String url = sppUrl + GET_APPLICATION_SUBSCRIPTIONS_ENDPOINT
                .replace(":appUuid", appUuid)
                .replace(":includeAutomatic", Boolean.toString(includeAutomatic));
        Request.Builder request = new Request.Builder().url(url).get();
        addHeaders(request);

        try (Response response = client.newCall(request.build()).execute()) {
            return Json.decodeValue(response.body().string(),
                    new TypeReference<List<SourceApplicationSubscription>>() {
                    });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void getApplicationSubscriptions(String appUuid, boolean includeAutomatic,
                                            Handler<AsyncResult<List<SourceApplicationSubscription>>> handler) {
        String url = sppUrl + GET_APPLICATION_SUBSCRIPTIONS_ENDPOINT
                .replace(":appUuid", appUuid)
                .replace(":includeAutomatic", Boolean.toString(includeAutomatic));
        Request.Builder request = new Request.Builder().url(url).get();
        addHeaders(request);

        try (Response response = client.newCall(request.build()).execute()) {
            List<SourceApplicationSubscription> subscribers = Json.decodeValue(response.body().string(),
                    new TypeReference<List<SourceApplicationSubscription>>() {
                    });
            handler.handle(Future.succeededFuture(subscribers));
        } catch (Exception e) {
            handler.handle(Future.failedFuture(e));
        }
    }

    public void createArtifact(String appUuid, SourceArtifact createRequest,
                               Handler<AsyncResult<SourceArtifact>> handler) {
        String url = sppUrl + CREATE_SOURCE_ARTIFACT_ENDPOINT
                .replace(":appUuid", appUuid)
                .replace(":artifactQualifiedName", URLEncoder.encode(createRequest.artifactQualifiedName()));
        Request.Builder request = new Request.Builder().url(url)
                .post(RequestBody.create(JSON, Json.encode(createRequest)));
        addHeaders(request);

        try (Response response = client.newCall(request.build()).execute()) {
            SourceArtifact artifact = Json.decodeValue(response.body().string(), SourceArtifact.class);
            artifact = artifact.withAppUuid(appUuid).withArtifactQualifiedName(createRequest.artifactQualifiedName());
            handler.handle(Future.succeededFuture(artifact));
        } catch (Exception e) {
            handler.handle(Future.failedFuture(e));
        }
    }

    public void getArtifact(String appUuid, String artifactQualifiedName,
                            Handler<AsyncResult<SourceArtifact>> handler) {
        String url = sppUrl + GET_SOURCE_ARTIFACT_ENDPOINT
                .replace(":appUuid", appUuid)
                .replace(":artifactQualifiedName", URLEncoder.encode(artifactQualifiedName));
        Request.Builder request = new Request.Builder().url(url).get();
        addHeaders(request);

        try (Response response = client.newCall(request.build()).execute()) {
            SourceArtifact artifact = Json.decodeValue(response.body().string(), SourceArtifact.class);
            artifact = artifact.withAppUuid(appUuid).withArtifactQualifiedName(artifactQualifiedName);
            handler.handle(Future.succeededFuture(artifact));
        } catch (Exception e) {
            handler.handle(Future.failedFuture(e));
        }
    }

    public void createArtifactConfig(String appUuid, String artifactQualifiedName, SourceArtifactConfig createRequest,
                                     Handler<AsyncResult<SourceArtifactConfig>> handler) {
        String url = sppUrl + CONFIGURE_SOURCE_ARTIFACT_ENDPOINT
                .replace(":appUuid", appUuid)
                .replace(":artifactQualifiedName", URLEncoder.encode(artifactQualifiedName));
        Request.Builder request = new Request.Builder().url(url)
                .put(RequestBody.create(JSON, Json.encode(createRequest)));
        addHeaders(request);

        try (Response response = client.newCall(request.build()).execute()) {
            handler.handle(Future.succeededFuture(Json.decodeValue(response.body().string(), SourceArtifactConfig.class)));
        } catch (Exception e) {
            handler.handle(Future.failedFuture(e));
        }
    }

    public SourceArtifactConfig createArtifactConfig(String appUuid, String artifactQualifiedName,
                                                     SourceArtifactConfig createRequest) {
        String url = sppUrl + CONFIGURE_SOURCE_ARTIFACT_ENDPOINT
                .replace(":appUuid", appUuid)
                .replace(":artifactQualifiedName", URLEncoder.encode(artifactQualifiedName));
        Request.Builder request = new Request.Builder().url(url)
                .put(RequestBody.create(JSON, Json.encode(createRequest)));
        addHeaders(request);

        try (Response response = client.newCall(request.build()).execute()) {
            return Json.decodeValue(response.body().string(), SourceArtifactConfig.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void subscribeToArtifact(ArtifactSubscribeRequest subscribeRequest, Handler<AsyncResult<Boolean>> handler) {
        final String subscribeEndpoint;
        switch (subscribeRequest.getType()) {
            case METRICS:
                subscribeEndpoint = SUBSCRIBE_SOURCE_ARTIFACT_METRICS_ENDPOINT;
                break;
            case TRACES:
                subscribeEndpoint = SUBSCRIBE_SOURCE_ARTIFACT_TRACES_ENDPOINT;
                break;
            default:
                throw new IllegalStateException("Invalid subscription request type: " + subscribeRequest.getType());
        }
        String url = sppUrl + subscribeEndpoint
                .replace(":appUuid", subscribeRequest.appUuid())
                .replace(":artifactQualifiedName", URLEncoder.encode(subscribeRequest.artifactQualifiedName()));
        Request.Builder request = new Request.Builder().url(url)
                .put(RequestBody.create(JSON, Json.encode(subscribeRequest)));
        addHeaders(request);

        try (Response response = client.newCall(request.build()).execute()) {
            if (response.isSuccessful()) {
                handler.handle(Future.succeededFuture(true));
            } else {
                handler.handle(Future.failedFuture(response.message()));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean subscribeToArtifact(ArtifactSubscribeRequest subscribeRequest) {
        final String subscribeEndpoint;
        switch (subscribeRequest.getType()) {
            case METRICS:
                subscribeEndpoint = SUBSCRIBE_SOURCE_ARTIFACT_METRICS_ENDPOINT;
                break;
            case TRACES:
                subscribeEndpoint = SUBSCRIBE_SOURCE_ARTIFACT_TRACES_ENDPOINT;
                break;
            default:
                throw new IllegalStateException("Invalid subscription request type: " + subscribeRequest.getType());
        }
        String url = sppUrl + subscribeEndpoint
                .replace(":appUuid", subscribeRequest.appUuid())
                .replace(":artifactQualifiedName", URLEncoder.encode(subscribeRequest.artifactQualifiedName()));
        Request.Builder request = new Request.Builder().url(url)
                .put(RequestBody.create(JSON, Json.encode(subscribeRequest)));
        addHeaders(request);

        try (Response response = client.newCall(request.build()).execute()) {
            if (response.isSuccessful()) {
                return true;
            } else {
                throw new IllegalStateException("Unknown response: " + response.body().string());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void unsubscribeFromArtifact(SourceArtifactUnsubscribeRequest unsubscribeRequest,
                                        Handler<AsyncResult<Boolean>> handler) {
        String url = sppUrl + UNSUBSCRIBE_SOURCE_ARTIFACT_ENDPOINT
                .replace(":appUuid", unsubscribeRequest.appUuid())
                .replace(":artifactQualifiedName", URLEncoder.encode(unsubscribeRequest.artifactQualifiedName()));
        Request.Builder request = new Request.Builder().url(url)
                .put(RequestBody.create(JSON, Json.encode(unsubscribeRequest)));
        addHeaders(request);

        try (Response response = client.newCall(request.build()).execute()) {
            if (response.isSuccessful()) {
                handler.handle(Future.succeededFuture(true));
            } else {
                handler.handle(Future.failedFuture(response.message()));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void unsubscribeFromArtifactTraces(ArtifactTraceUnsubscribeRequest unsubscribeRequest,
                                              Handler<AsyncResult<Boolean>> handler) {
        String url = sppUrl + UNSUBSCRIBE_SOURCE_ARTIFACT_TRACES_ENDPOINT
                .replace(":appUuid", unsubscribeRequest.appUuid())
                .replace(":artifactQualifiedName", URLEncoder.encode(unsubscribeRequest.artifactQualifiedName()));
        Request.Builder request = new Request.Builder().url(url)
                .put(RequestBody.create(JSON, Json.encode(unsubscribeRequest)));
        addHeaders(request);

        try (Response response = client.newCall(request.build()).execute()) {
            if (response.isSuccessful()) {
                handler.handle(Future.succeededFuture(true));
            } else {
                handler.handle(Future.failedFuture(response.message()));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void unsubscribeFromArtifactMetrics(ArtifactMetricUnsubscribeRequest unsubscribeRequest,
                                               Handler<AsyncResult<Boolean>> handler) {
        String url = sppUrl + UNSUBSCRIBE_SOURCE_ARTIFACT_METRICS_ENDPOINT
                .replace(":appUuid", unsubscribeRequest.appUuid())
                .replace(":artifactQualifiedName", URLEncoder.encode(unsubscribeRequest.artifactQualifiedName()));
        Request.Builder request = new Request.Builder().url(url)
                .put(RequestBody.create(JSON, Json.encode(unsubscribeRequest)));
        addHeaders(request);

        try (Response response = client.newCall(request.build()).execute()) {
            if (response.isSuccessful()) {
                handler.handle(Future.succeededFuture(true));
            } else {
                handler.handle(Future.failedFuture(response.message()));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void getArtifactConfig(String appUuid, String artifactQualifiedName,
                                  Handler<AsyncResult<SourceArtifactConfig>> handler) {
        String url = sppUrl + GET_SOURCE_ARTIFACT_CONFIGURATION_ENDPOINT
                .replace(":appUuid", appUuid)
                .replace(":artifactQualifiedName", URLEncoder.encode(artifactQualifiedName));
        Request.Builder request = new Request.Builder().url(url).get();
        addHeaders(request);

        try (Response response = client.newCall(request.build()).execute()) {
            handler.handle(Future.succeededFuture(Json.decodeValue(response.body().string(), SourceArtifactConfig.class)));
        } catch (Exception e) {
            handler.handle(Future.failedFuture(e));
        }
    }

    public TraceQueryResult getTraces(String appUuid, String artifactQualifiedName, TraceOrderType orderType) {
        String url = sppUrl + GET_TRACES_ENDPOINT
                .replace(":appUuid", appUuid)
                .replace(":artifactQualifiedName", artifactQualifiedName)
                .replace(":orderType", orderType.toString());
        Request.Builder request = new Request.Builder().url(url).get();
        addHeaders(request);

        try (Response response = client.newCall(request.build()).execute()) {
            return Json.decodeValue(response.body().string(), TraceQueryResult.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void getTraceSpans(String appUuid, String artifactQualifiedName, TraceSpanStackQuery traceSpanQuery,
                              Handler<AsyncResult<TraceSpanStackQueryResult>> handler) {
        String url = sppUrl + GET_TRACE_SPANS_ENDPOINT
                .replace(":appUuid", appUuid)
                .replace(":artifactQualifiedName", artifactQualifiedName)
                .replace(":traceId", traceSpanQuery.traceId())
                .replace(":segmentId", (traceSpanQuery.segmentId() == null) ? "" : traceSpanQuery.segmentId() + "")
                .replace(":spanId", (traceSpanQuery.spanId() == null) ? "" : traceSpanQuery.spanId() + "")
                .replace(":oneLevelDeep", Boolean.toString(traceSpanQuery.oneLevelDeep()))
                .replace(":followExit", Boolean.toString(traceSpanQuery.followExit()));
        Request.Builder request = new Request.Builder().url(url).get();
        addHeaders(request);

        try (Response response = client.newCall(request.build()).execute()) {
            handler.handle(Future.succeededFuture(Json.decodeValue(response.body().string(), TraceSpanStackQueryResult.class)));
        } catch (Exception e) {
            handler.handle(Future.failedFuture(e));
        }
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    private void addHeaders(Request.Builder request) {
        if (apiKey != null) {
            request.addHeader("Authorization", "Bearer " + apiKey);
        }
    }
}
