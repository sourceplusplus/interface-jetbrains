package com.sourceplusplus.agent.sync;

import com.sourceplusplus.api.client.SourceCoreClient;
import com.sourceplusplus.api.model.application.SourceApplicationSubscription;
import com.sourceplusplus.api.model.artifact.SourceArtifactSubscriptionType;
import com.sourceplusplus.api.model.config.SourceAgentConfig;
import org.pmw.tinylog.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * todo: description
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.2.3
 * @since 0.1.0
 */
public class ArtifactTraceSubscriptionSync implements Runnable {

    public static final Set<String> TRACE_ARTIFACTS = ConcurrentHashMap.newKeySet();
    public final static int WORK_SYNC_DELAY = 5000;
    private final SourceCoreClient coreClient;
    private static boolean enabled = true;
    private static AtomicBoolean readyForWork = new AtomicBoolean(true);

    public ArtifactTraceSubscriptionSync(SourceCoreClient coreClient) {
        this.coreClient = Objects.requireNonNull(coreClient);
    }

    @Override
    public void run() {
        if (enabled && readyForWork.getAndSet(false)) {
            coreClient.registerIP(); //in-case core went down
            Logger.trace("Getting application subscriptions. - App UUID: " + SourceAgentConfig.current.appUuid);
            List<SourceApplicationSubscription> subscriptions = coreClient.getApplicationSubscriptions(
                    SourceAgentConfig.current.appUuid, false);

            Set<String> currentTraceSubscriptions = new HashSet<>();
            for (SourceApplicationSubscription subscription : subscriptions) {
                if (subscription.types().contains(SourceArtifactSubscriptionType.TRACES)
                        || subscription.forceSubscription()) {
                    currentTraceSubscriptions.add(subscription.artifactQualifiedName());
                    if (!TRACE_ARTIFACTS.contains(subscription.artifactQualifiedName())) {
                        Logger.info("Added traced artifact: " + subscription.artifactQualifiedName());
                    }
                }
            }
            TRACE_ARTIFACTS.addAll(currentTraceSubscriptions);
            TRACE_ARTIFACTS.removeIf(s -> {
                if (!currentTraceSubscriptions.contains(s)) {
                    Logger.info("Removed traced artifact: " + s);
                    return true;
                } else {
                    return false;
                }
            });
            Logger.trace("Tracing artifacts: " + String.join(",", TRACE_ARTIFACTS));

            readyForWork.set(true);
        }
    }

    public static void disable() {
        enabled = false;
    }

    public static void enable() {
        enabled = true;
    }
}
