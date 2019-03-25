package com.sourceplusplus.agent;

import com.sourceplusplus.agent.inject.ClassFileTransformerImpl;
import com.sourceplusplus.agent.intercept.logger.SourceLoggerResolver;
import com.sourceplusplus.agent.sync.ArtifactTraceSubscriptionSync;
import com.sourceplusplus.api.client.SourceCoreClient;
import com.sourceplusplus.api.model.application.SourceApplication;
import com.sourceplusplus.api.model.config.SourceAgentConfig;
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

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * todo: description
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.1.3
 * @since 0.1.0
 */
public class SourceAgent {

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
        Logger.trace("Try trigger start: " + artifactSignature);
        if (!ArtifactTraceSubscriptionSync.TRACE_ARTIFACTS.contains(artifactSignature)) {
            return;
        }

        Logger.debug("Trigger start: " + artifactSignature);
        if (ContextManager.getGlobalTraceId().equals("N/A")) {
            ContextCarrier contextCarrier = new ContextCarrier();
            AbstractSpan firstEntrySpan = ContextManager.createEntrySpan(artifactSignature, contextCarrier);
        } else {
            ContextManager.createLocalSpan(artifactSignature);
        }
    }

    @SuppressWarnings("unused")
    public static void triggerEnd(String artifactSignature) {
        Logger.trace("Try trigger end: " + artifactSignature);
        if (!ArtifactTraceSubscriptionSync.TRACE_ARTIFACTS.contains(artifactSignature)) {
            return;
        }

        if (!ContextManager.getGlobalTraceId().equals("N/A")) {
            Logger.debug("Trigger end: " + artifactSignature);
            ContextManager.stopSpan();
        }
    }

    @SuppressWarnings("unused")
    public static void triggerEnd(Throwable throwable, String artifactSignature) {
        Logger.trace("Try trigger exception end: " + artifactSignature);
        if (!ArtifactTraceSubscriptionSync.TRACE_ARTIFACTS.contains(artifactSignature)) {
            return;
        }

        if (!ContextManager.getGlobalTraceId().equals("N/A")) {
            Logger.debug("Trigger exception end: " + artifactSignature);
            ContextManager.activeSpan().errorOccurred().log(throwable);
            ContextManager.stopSpan();
        }
    }

    public static void agentmain(String agentArgs, Instrumentation instrumentation) {
        premain(agentArgs, instrumentation);
    }

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        if (isAgentInitialized()) {
            throw new IllegalStateException("Source++ Agent already initialized");
        } else {
            loadConfiguration();
            if (SourceAgentConfig.current.logLocation != null) {
                File logFile = new File(SourceAgentConfig.current.logLocation, "source-agent.log");
                Configurator.defaultConfig()
                        .writer(new FileWriter(logFile.getAbsolutePath()))
                        .level(Level.valueOf(SourceAgentConfig.current.logLevel))
                        .formatPattern("[AGENT] - {message}")
                        .activate();
            }
            SourceAgent.instrumentation = instrumentation;
            Logger.info("Build: " + BUILD.getString("build_date"));

            coreClient = new SourceCoreClient(
                    SourceAgentConfig.current.apiHost, SourceAgentConfig.current.apiPort, SourceAgentConfig.current.apiSslEnabled);
            if (SourceAgentConfig.current.apiKey != null) {
                coreClient.setApiKey(SourceAgentConfig.current.apiKey);
            }
            coreClient.registerIP();
            Logger.info("Source++ Agent initialized");

            LogManager.setLogResolver(new SourceLoggerResolver());
            Config.Collector.GRPC_CHANNEL_CHECK_INTERVAL = Integer.MAX_VALUE;
            Config.Agent.IS_OPEN_DEBUGGING_CLASS = SourceAgentConfig.current.outputEnhancedClasses;
            Config.Agent.SAMPLE_N_PER_3_SECS = SourceAgentConfig.current.sampleNPer3Secs;
            Config.Agent.SPAN_LIMIT_PER_SEGMENT = SourceAgentConfig.current.spanLimitPerSegment;
            Config.Collector.BACKEND_SERVICE = SourceAgentConfig.current.backendService;
            System.setProperty("skywalking.logging.level", SourceAgentConfig.current.logLevel);
            Logger.info("Using SkyWalking host: " + SourceAgentConfig.current.backendService);

            if (SourceAgentConfig.current.testMode) {
                Logger.info("Test mode enabled");
                System.setProperty("skywalking.logging.level", SourceAgentConfig.current.logLevel);
                System.setProperty("skywalking.agent.application_code", "test_mode");
                System.setProperty("skywalking.agent.service_name", "test_mode");
                Config.Collector.BACKEND_SERVICE = SourceAgentConfig.current.backendService;
                SkyWalkingAgent.premain(null, SourceAgent.instrumentation);
            } else {
                if (SourceAgentConfig.current.skywalkingEnabled) {
                    if (SourceAgentConfig.current.appUuid == null) {
                        throw new RuntimeException("Missing application UUID in Source++ Agent configuration");
                    }
                    Config.Agent.SERVICE_NAME = SourceAgentConfig.current.appUuid;
                    System.setProperty("skywalking.agent.application_code", SourceAgentConfig.current.appUuid);
                    SkyWalkingAgent.premain(null, SourceAgent.instrumentation);

                    Logger.info("Waiting for Apache SkyWalking to finish setup");
                    while (true) {
                        if (RemoteDownstreamConfig.Agent.SERVICE_ID != DictionaryUtil.nullValue()
                                && RemoteDownstreamConfig.Agent.SERVICE_INSTANCE_ID != DictionaryUtil.nullValue()) {
                            break;
                        }
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                        }
                    }
                    Logger.info("Apache SkyWalking initialized");
                }
            }

            if (SourceAgentConfig.current.testMode) {
                ClassFileTransformer sTransformer = new ClassFileTransformerImpl(SourceAgentConfig.current.packages);
                instrumentation.addTransformer(sTransformer, true);
                traceSubscriptionSync = new ArtifactTraceSubscriptionSync(coreClient);
            } else if (SourceAgentConfig.current.appUuid != null) {
                Logger.info("Getting Source++ application");
                SourceApplication application = coreClient.getApplication(SourceAgentConfig.current.appUuid);
                if (application != null) {
                    Logger.warn(String.format("Found application. App name: %s - App uuid: %s",
                            application.appName(), application.appUuid()));

                    if (application.agentConfig() != null) {
                        Logger.info("Overriding agent config with: " + application.agentConfig());
                        SourceAgentConfig.current.applyConfig(application.agentConfig().toJsonObject());
                        Config.Agent.SPAN_LIMIT_PER_SEGMENT = SourceAgentConfig.current.spanLimitPerSegment;
                    }
                    ClassFileTransformer sTransformer = new ClassFileTransformerImpl(SourceAgentConfig.current.packages);
                    instrumentation.addTransformer(sTransformer, true);

                    Thread daemonThread = new Thread(() -> {
                        workScheduler.scheduleAtFixedRate(traceSubscriptionSync = new ArtifactTraceSubscriptionSync(coreClient),
                                0, ArtifactTraceSubscriptionSync.WORK_SYNC_DELAY, MILLISECONDS);
                    });
                    daemonThread.setDaemon(true);
                    daemonThread.start();
                } else {
                    throw new IllegalStateException("Count not find application: " + SourceAgentConfig.current.appUuid);
                }
            } else {
                throw new IllegalStateException("Source++ Agent configuration is missing appUuid");
            }
            Logger.info("Source++ Agent successfully started");
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
        String environmentConfigFile = System.getenv("SOURCE_CONFIG");
        if (environmentConfigFile != null) {
            configInputStream = getConfigInputStream(environmentConfigFile);
        } else {
            configInputStream = getConfigInputStream(System.getProperty("SOURCE_CONFIG"));
        }
        String configData = convertStreamToString(configInputStream);
        SourceAgentConfig.current.applyConfig(new JsonObject(configData));
        try {
            configInputStream.close();
        } catch (IOException e) {
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
