package com.sourceplusplus.agent;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.sourceplusplus.agent.inject.ClassFileTransformerImpl;
import com.sourceplusplus.agent.intercept.logger.SourceLoggerResolver;
import com.sourceplusplus.agent.sync.ArtifactTraceSubscriptionSync;
import com.sourceplusplus.api.client.SourceCoreClient;
import com.sourceplusplus.api.model.application.SourceApplication;
import com.sourceplusplus.api.model.config.SourceAgentConfig;
import com.sourceplusplus.api.model.info.SourceCoreInfo;
import com.sourceplusplus.api.model.integration.ConnectionType;
import com.sourceplusplus.api.model.integration.IntegrationConnection;
import com.sourceplusplus.api.model.integration.IntegrationInfo;
import io.vertx.core.json.JsonObject;
import org.apache.skywalking.apm.agent.SkyWalkingAgent;
import org.apache.skywalking.apm.agent.core.conf.Config;
import org.apache.skywalking.apm.agent.core.conf.RemoteDownstreamConfig;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.dictionary.DictionaryUtil;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Level;
import org.pmw.tinylog.Logger;
import org.pmw.tinylog.writers.FileWriter;

import java.io.*;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * todo: description
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.2.5
 * @since 0.1.0
 */
public class SourceAgent {

    public static final MetricRegistry METRIC_REGISTRY = new MetricRegistry();
    public static final ResourceBundle BUILD = ResourceBundle.getBundle("source-agent_build");

    private static final ScheduledExecutorService workScheduler = Executors.newScheduledThreadPool(1, r -> {
        Thread t = Executors.defaultThreadFactory().newThread(r);
        t.setDaemon(true);
        return t;
    });
    private static SourceCoreClient coreClient;
    private static Instrumentation instrumentation;
    private static ArtifactTraceSubscriptionSync traceSubscriptionSync;

    @SuppressWarnings("unused")
    public static void triggerStart(String artifactSignature) {
        if (!ArtifactTraceSubscriptionSync.TRACE_ARTIFACTS.contains(artifactSignature)) {
            return;
        }

        METRIC_REGISTRY.meter("agent.trigger.start-" + artifactSignature).mark();
        if (ContextManager.getGlobalTraceId().equals("N/A")) {
            ContextCarrier contextCarrier = new ContextCarrier();
            AbstractSpan firstEntrySpan = ContextManager.createEntrySpan(artifactSignature, contextCarrier);
        } else {
            ContextManager.createLocalSpan(artifactSignature);
        }
    }

    @SuppressWarnings("unused")
    public static void triggerEnd(String artifactSignature) {
        if (!ArtifactTraceSubscriptionSync.TRACE_ARTIFACTS.contains(artifactSignature)) {
            return;
        }

        if (!ContextManager.getGlobalTraceId().equals("N/A")) {
            METRIC_REGISTRY.meter("agent.trigger.end-" + artifactSignature).mark();
            ContextManager.stopSpan();
        }
    }

    @SuppressWarnings("unused")
    public static void triggerEnd(Throwable throwable, String artifactSignature) {
        if (!ArtifactTraceSubscriptionSync.TRACE_ARTIFACTS.contains(artifactSignature)) {
            return;
        }

        if (!ContextManager.getGlobalTraceId().equals("N/A")) {
            METRIC_REGISTRY.meter("agent.trigger.exception-" + artifactSignature).mark();
            ContextManager.activeSpan().errorOccurred().log(throwable);
            ContextManager.stopSpan();
        }
    }

    public static void agentmain(String agentArgs, Instrumentation instrumentation) throws Exception {
        premain(agentArgs, instrumentation);
    }

    public static void premain(String agentArgs, Instrumentation instrumentation) throws Exception {
        if (isAgentInitialized()) {
            throw new IllegalStateException("Source++ Agent already initialized");
        } else {
            loadConfiguration();
            if (SourceAgentConfig.current.logLocation != null) {
                File logFile = new File(SourceAgentConfig.current.logLocation, "source-agent.log");
                Configurator.defaultConfig()
                        .writer(new FileWriter(logFile.getAbsolutePath()))
                        .level(Level.valueOf(SourceAgentConfig.current.logLevel))
                        .formatPattern("{level} {date:yyyy-MM-dd HH:mm:ss.S} {class_name} : {message}")
                        .activate();

                if (SourceAgentConfig.current.logMetrics) {
                    File metricsFile = new File(SourceAgentConfig.current.logLocation, "source-agent.metrics");
                    ConsoleReporter reporter = ConsoleReporter.forRegistry(METRIC_REGISTRY)
                            .convertRatesTo(TimeUnit.SECONDS)
                            .convertDurationsTo(TimeUnit.MILLISECONDS)
                            .outputTo(new PrintStream(new FileOutputStream(metricsFile, true)))
                            .build();
                    reporter.start(1, TimeUnit.MINUTES);
                }
            }
            SourceAgent.instrumentation = instrumentation;
            Logger.info("Build: " + BUILD.getString("build_date"));

            SourceCoreInfo coreInfo = null;
            if (!SourceAgentConfig.current.manualSetupMode) {
                coreClient = new SourceCoreClient(SourceAgentConfig.current.apiHost, SourceAgentConfig.current.apiPort,
                        SourceAgentConfig.current.apiSslEnabled);
                if (SourceAgentConfig.current.apiKey != null) {
                    coreClient.setApiKey(SourceAgentConfig.current.apiKey);
                }
                coreClient.registerIP();
                coreInfo = coreClient.info();
            }
            Logger.info("Source++ Agent initialized");

            if (SourceAgentConfig.current.testMode) {
                ClassFileTransformer sTransformer = new ClassFileTransformerImpl(SourceAgentConfig.current.packages);
                instrumentation.addTransformer(sTransformer, true);
                traceSubscriptionSync = new ArtifactTraceSubscriptionSync(coreClient);
            } else if ((SourceAgentConfig.current.appUuid != null || SourceAgentConfig.current.appName != null)
                    || SourceAgentConfig.current.manualSetupMode) {
                if (!SourceAgentConfig.current.manualSetupMode) {
                    Logger.info("Getting Source++ application");
                    SourceApplication application;
                    if (SourceAgentConfig.current.appUuid != null) {
                        Logger.info("Using App UUID to get application");
                        application = coreClient.getApplication(SourceAgentConfig.current.appUuid);
                    } else {
                        Logger.info("Using App Name to find application");
                        application = coreClient.findApplicationByName(SourceAgentConfig.current.appName);
                    }
                    if (application != null) {
                        SourceAgentConfig.current.appUuid = application.appUuid();
                        Logger.info(String.format("Found application. App Name: %s - App UUID: %s",
                                application.appName(), application.appUuid()));

                        if (application.agentConfig() != null) {
                            overrideSourceAgentConfig(application.agentConfig());
                        }
                    } else {
                        throw new IllegalStateException("Could not find application: " + SourceAgentConfig.current.appUuid);
                    }
                }

                ClassFileTransformer sTransformer = new ClassFileTransformerImpl(SourceAgentConfig.current.packages);
                instrumentation.addTransformer(sTransformer, true);
                if (!SourceAgentConfig.current.manualSetupMode) {
                    startArtifactTraceSubscriptionSync(coreClient);
                }
            } else {
                throw new IllegalStateException("Source++ Agent configuration is missing app_uuid/app_name");
            }

            if (!SourceAgentConfig.current.manualSetupMode) {
                bootIntegrations(coreInfo);
            }
            Logger.info("Source++ Agent successfully started");
        }
    }

    public static void overrideSourceAgentConfig(SourceAgentConfig agentConfig) {
        Logger.info("Overriding Source++ Agent configuration with: " + agentConfig);
        SourceAgentConfig.current.applyConfig(agentConfig.toJsonObject());
    }

    public static void startArtifactTraceSubscriptionSync() {
        coreClient = new SourceCoreClient(SourceAgentConfig.current.apiHost, SourceAgentConfig.current.apiPort,
                SourceAgentConfig.current.apiSslEnabled);
        if (SourceAgentConfig.current.apiKey != null) {
            coreClient.setApiKey(SourceAgentConfig.current.apiKey);
        }
        coreClient.registerIP();
        startArtifactTraceSubscriptionSync(coreClient);
    }

    private static void startArtifactTraceSubscriptionSync(SourceCoreClient coreClient) {
        Thread daemonThread = new Thread(() -> {
            workScheduler.scheduleAtFixedRate(traceSubscriptionSync = new ArtifactTraceSubscriptionSync(coreClient),
                    0, ArtifactTraceSubscriptionSync.WORK_SYNC_DELAY, MILLISECONDS);
        });
        daemonThread.setDaemon(true);
        daemonThread.start();
    }

    public static void bootIntegrations(SourceCoreInfo coreInfo) {
        Logger.info("Booting Source++ integrations");
        coreInfo.activeIntegrations().parallelStream().forEach(info -> {
            switch (info.id()) {
                case "apache_skywalking":
                    bootApacheSkyWalking(info);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid integration: " + info.id());
            }
        });
    }

    private static void bootApacheSkyWalking(IntegrationInfo info) {
        Logger.info("Booting Apache SkyWalking");
        LogManager.setLogResolver(new SourceLoggerResolver());

        IntegrationConnection connection = info.connections().get(ConnectionType.gRPC);
        if ("localhost".equals(connection.getHost()) || "127.0.0.1".equals(connection.getHost())
                || "skywalking-oap".equals(connection.getHost())) {
            Config.Collector.BACKEND_SERVICE = SourceAgentConfig.current.apiHost + ":" + connection.getPort();
        } else {
            Config.Collector.BACKEND_SERVICE = connection.getHost() + ":" + connection.getPort();
        }
        Config.Collector.GRPC_CHANNEL_CHECK_INTERVAL = Integer.MAX_VALUE;
        Config.Agent.IS_OPEN_DEBUGGING_CLASS = SourceAgentConfig.current.outputEnhancedClasses;
        Config.Agent.SAMPLE_N_PER_3_SECS = SourceAgentConfig.current.sampleNPer3Secs;
        Config.Agent.SPAN_LIMIT_PER_SEGMENT = SourceAgentConfig.current.spanLimitPerSegment;
        Config.Plugin.SpringMVC.USE_QUALIFIED_NAME_AS_ENDPOINT_NAME = true;
        Config.Plugin.Toolkit.USE_QUALIFIED_NAME_AS_OPERATION_NAME = true;
        System.setProperty("skywalking.logging.level", SourceAgentConfig.current.logLevel);
        Logger.info("Using SkyWalking host: " + Config.Collector.BACKEND_SERVICE);

        if (SourceAgentConfig.current.testMode) {
            Logger.info("Test mode enabled");
            System.setProperty("skywalking.logging.level", SourceAgentConfig.current.logLevel);
            System.setProperty("skywalking.agent.application_code", "test_mode");
            System.setProperty("skywalking.agent.service_name", "test_mode");
            try {
                SkyWalkingAgent.premain(null, SourceAgent.instrumentation);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            if (SourceAgentConfig.current.skywalkingEnabled) {
                if (SourceAgentConfig.current.appUuid == null) {
                    throw new RuntimeException("Missing application UUID in Source++ Agent configuration");
                }
                Config.Agent.SERVICE_NAME = SourceAgentConfig.current.appUuid;
                System.setProperty("skywalking.agent.application_code", SourceAgentConfig.current.appUuid);
                try {
                    SkyWalkingAgent.premain(null, SourceAgent.instrumentation);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                Logger.info("Waiting for Apache SkyWalking to finish setup");
                while (RemoteDownstreamConfig.Agent.SERVICE_ID == DictionaryUtil.nullValue()
                        || RemoteDownstreamConfig.Agent.SERVICE_INSTANCE_ID == DictionaryUtil.nullValue()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignore) {
                    }
                }
                Logger.info("Apache SkyWalking initialized");
            }
        }
    }

    @Contract(pure = true)
    public static boolean isAgentInitialized() {
        return instrumentation != null;
    }

    @NotNull
    @Contract(pure = true)
    public static SourceCoreClient coreClient() {
        return coreClient;
    }

    @Contract(pure = true)
    public static ArtifactTraceSubscriptionSync getTraceSubscriptionSync() {
        return traceSubscriptionSync;
    }

    private static void loadConfiguration() {
        InputStream configInputStream;
        String environmentConfigFile = System.getenv("SOURCE_AGENT_CONFIG");
        if (environmentConfigFile != null) {
            configInputStream = getConfigInputStream(environmentConfigFile);
        } else {
            configInputStream = getConfigInputStream(System.getProperty("SOURCE_AGENT_CONFIG"));
        }
        String configData = convertStreamToString(configInputStream);
        SourceAgentConfig.current.applyConfig(new JsonObject(configData));
        try {
            configInputStream.close();
        } catch (IOException ignore) {
        }
    }

    private static InputStream getConfigInputStream(String configFileLocationOrName) {
        if (configFileLocationOrName != null) {
            File configFile = new File(configFileLocationOrName);
            if (configFile.exists()) {
                try {
                    return new FileInputStream(configFile);
                } catch (FileNotFoundException e) {
                    Logger.error(e, "Failed to find agent config file: " + configFile);
                    throw new RuntimeException(e);
                }
            } else {
                return Thread.currentThread().getContextClassLoader()
                        .getResourceAsStream(configFileLocationOrName);
            }
        } else {
            return Thread.currentThread().getContextClassLoader().getResourceAsStream("source-agent.json");
        }
    }

    private static String convertStreamToString(InputStream is) {
        Scanner s = new Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}
