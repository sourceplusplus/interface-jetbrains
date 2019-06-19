package com.sourceplusplus.agent.trace;

import com.sourceplusplus.agent.ActiveSourceAgentTest;
import com.sourceplusplus.agent.SourceAgent;
import com.sourceplusplus.api.model.application.SourceApplication;
import com.sourceplusplus.api.model.trace.ArtifactTraceSubscribeRequest;
import com.sourceplusplus.api.model.trace.TraceOrderType;
import com.sourceplusplus.api.model.trace.TraceQueryResult;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.2.0
 * @since 0.1.0
 */
public class TraceTest extends ActiveSourceAgentTest {

    @Test
    public void basicTraceTest() {
        SourceApplication application = coreClient.createApplication();
        usingAppUuid(application.appUuid());

        ArtifactTraceSubscribeRequest subscribeRequest = ArtifactTraceSubscribeRequest.builder()
                .addOrderTypes(TraceOrderType.LATEST_TRACES)
                .appUuid(application.appUuid())
                .artifactQualifiedName(JavaTestClass.class.getName() + ".staticMethod()").build();
        assertTrue(coreClient.subscribeToArtifact(subscribeRequest));
        coreClient.refreshStorage();
        SourceAgent.getTraceSubscriptionSync().run();

        for (int i = 0; i < 40; i++) {
            JavaTestClass.staticMethod();
        }
        //todo: force flush of traces to core (currently just looping long enough for it to happen organically)

        coreClient.searchForNewEndpoints();
        TraceQueryResult result = coreClient.getTraces(application.appUuid(),
                JavaTestClass.class.getName() + ".staticMethod()", TraceOrderType.LATEST_TRACES);
        assertNotNull(result);
        assertFalse(result.traces().isEmpty());
    }
}
