import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.guava.GuavaModule
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.sourceplusplus.portal.extensions.*
import com.sourceplusplus.protocol.ProtocolAddress.Global.ActivityTabOpened
import com.sourceplusplus.protocol.ProtocolAddress.Global.ClickedDisplaySpanInfo
import com.sourceplusplus.protocol.ProtocolAddress.Global.ClickedDisplayTraceStack
import com.sourceplusplus.protocol.ProtocolAddress.Global.ClickedDisplayTraces
import com.sourceplusplus.protocol.ProtocolAddress.Global.ClickedViewAsExternalPortal
import com.sourceplusplus.protocol.ProtocolAddress.Global.ConfigurationTabOpened
import com.sourceplusplus.protocol.ProtocolAddress.Global.GetPortalConfiguration
import com.sourceplusplus.protocol.ProtocolAddress.Global.OverviewTabOpened
import com.sourceplusplus.protocol.ProtocolAddress.Global.SetActiveChartMetric
import com.sourceplusplus.protocol.ProtocolAddress.Global.TracesTabOpened
import com.sourceplusplus.protocol.ProtocolAddress.Portal.ClearActivity
import com.sourceplusplus.protocol.ProtocolAddress.Portal.DisplayArtifactConfiguration
import com.sourceplusplus.protocol.ProtocolAddress.Portal.UpdateEndpoints
import com.sourceplusplus.protocol.artifact.ArtifactConfiguration
import com.sourceplusplus.protocol.artifact.ArtifactInformation
import com.sourceplusplus.protocol.artifact.QueryTimeFrame
import com.sourceplusplus.protocol.artifact.metrics.BarTrendCard
import com.sourceplusplus.protocol.artifact.metrics.MetricType
import com.sourceplusplus.protocol.artifact.metrics.SplineChart
import com.sourceplusplus.protocol.artifact.metrics.SplineSeriesData
import com.sourceplusplus.protocol.artifact.trace.*
import com.sourceplusplus.protocol.portal.PortalConfiguration
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.ext.bridge.PermittedOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions
import io.vertx.ext.web.handler.sockjs.SockJSHandler
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.*
import java.util.concurrent.ThreadLocalRandom.current

var currentMetricType = MetricType.ResponseTime_Average

//todo: can re-write this by essentially writing a PortalEventListener equivalent
fun main() {
    DatabindCodec.mapper().registerModule(GuavaModule())
    DatabindCodec.mapper().registerModule(Jdk8Module())
    DatabindCodec.mapper().registerModule(JavaTimeModule())
    DatabindCodec.mapper().enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
    DatabindCodec.mapper().enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING)

    val module = SimpleModule()
    module.addSerializer(Instant::class.java, KSerializers.KotlinInstantSerializer())
    module.addDeserializer(Instant::class.java, KSerializers.KotlinInstantDeserializer())
    DatabindCodec.mapper().registerModule(module)

    val vertx = Vertx.vertx()
    val sockJSHandler = SockJSHandler.create(vertx)
    val portalBridgeOptions = SockJSBridgeOptions()
        .addInboundPermitted(PermittedOptions().setAddressRegex(".+"))
        .addOutboundPermitted(PermittedOptions().setAddressRegex(".+"))
    sockJSHandler.bridge(portalBridgeOptions)

    val router = Router.router(vertx)
    router.route("/eventbus/*").handler(sockJSHandler)

    vertx.createHttpServer().requestHandler(router).listen(8888, "localhost")

    vertx.eventBus().consumer<String>(ClickedViewAsExternalPortal) {
        it.reply(JsonObject().put("portalUuid", "null"))
    }

    vertx.eventBus().consumer<String>(GetPortalConfiguration) {
        it.reply(JsonObject.mapFrom(PortalConfiguration().copy(external = true)))
    }

    vertx.eventBus().consumer<Void>(OverviewTabOpened) {
        displayEndpoints(vertx)
    }

    vertx.eventBus().consumer<Void>(ActivityTabOpened) {
        updateCards(vertx)

        vertx.eventBus().publish(ClearActivity("null"), "")
        displayChart(vertx)
    }
    vertx.setPeriodic(2500) {
        updateCards(vertx)
        displayChart(vertx)
    }

    vertx.eventBus().consumer<JsonObject>(SetActiveChartMetric) {
        currentMetricType = MetricType.valueOf(it.body().getString("metricType"))
        updateCards(vertx)
        displayChart(vertx)
    }

    vertx.eventBus().consumer<Void>(ClickedDisplayTraces) {
        displayTraces(vertx)
    }
    vertx.eventBus().consumer<Void>(TracesTabOpened) {
        displayTraces(vertx)
    }

    vertx.eventBus().consumer<String>(ClickedDisplayTraceStack) {
        val traceSpans = mutableListOf<TraceSpanInfo>()
        for (i in 1..5) {
            val span = TraceSpan(
                artifactQualifiedName = UUID.randomUUID().toString(),
                parentSpanId = System.currentTimeMillis().toInt(),
                traceId = "100",
                segmentId = "100",
                spanId = 100,
                error = current().nextBoolean(),
                hasChildStack = false,
                startTime = Clock.System.now(),
                endTime = Clock.System.now(),
                component = "DATABASE",
                serviceCode = "SERVICE_CODE",
                type = listOf("Entry", "Exit", "Local", "UNRECOGNIZED").random()
            )
            val spanInfo = TraceSpanInfo(
                span = span,
                appUuid = "null",
                rootArtifactQualifiedName = UUID.randomUUID().toString(),
                operationName = UUID.randomUUID().toString(),
                timeTook = "10s",
                totalTracePercent = current().nextDouble(100.0)
            )
            traceSpans.add(spanInfo)
        }
        vertx.eventBus().displayTraceStack("null", traceSpans)
    }

    vertx.eventBus().consumer<Void>(ClickedDisplaySpanInfo) {
        val span = TraceSpan(
            traceId = "100-" + System.currentTimeMillis(),
            parentSpanId = System.currentTimeMillis().toInt(),
            spanId = System.currentTimeMillis().toInt(),
            segmentId = "100",
            startTime = Clock.System.now(),
            endTime = Clock.System.now(),
            serviceCode = "SERVICE_CODE",
            type = listOf("Entry", "Exit", "Local", "UNRECOGNIZED").random(),
            tags = mapOf(
                "thing1" to UUID.randomUUID().toString(),
                "thing2" to UUID.randomUUID().toString(),
                "thing3" to UUID.randomUUID().toString(),
                "thing4" to UUID.randomUUID().toString(),
                "thing5" to UUID.randomUUID().toString()
            ),
            logs = listOf(
                TraceSpanLogEntry(time = Clock.System.now(), data = UUID.randomUUID().toString())
            )
        )
        vertx.eventBus().displaySpanInfo("null", span)
    }

    vertx.eventBus().consumer<Void>(ConfigurationTabOpened) {
        vertx.eventBus().publish(
            DisplayArtifactConfiguration("null"), JsonObject.mapFrom(
                ArtifactInformation(
                    artifactQualifiedName = UUID.randomUUID().toString(),
                    createDate = Clock.System.now(),
                    lastUpdated = Clock.System.now(),
                    config = ArtifactConfiguration(
                        endpoint = current().nextBoolean(),
                        subscribeAutomatically = current().nextBoolean(),
                        endpointName = UUID.randomUUID().toString()
                    )
                )
            )
        )
    }
}

fun displayEndpoints(vertx: Vertx) {
    vertx.eventBus().send(
        UpdateEndpoints("null"), JsonObject(
            """
{
   "appUuid":"null",
   "timeFrame":"LAST_5_MINUTES",
   "start":1602623397340,
   "stop":1602623697340,
   "step":"MINUTE",
   "endpointMetrics":[
      {
         "endpointType": "HTTP",
         "artifactQualifiedName":{
            "identifier":"spp.example.webapp.controller.WebappController.getUser(long)",
            "commitId":"todo",
            "type":"ENDPOINT",
            "lineNumber":null,
            "operationName":"{GET}/users/{id}"
         },
         "artifactSummarizedMetrics":[
            {
               "metricType":"Throughput_Average",
               "value":"98/sec"
            },
            {
               "metricType":"ResponseTime_Average",
               "value":"0ms"
            },
            {
               "metricType":"ServiceLevelAgreement_Average",
               "value":"99.9%"
            }
         ]
      },
      {
         "endpointType": "HTTP",
         "artifactQualifiedName":{
            "identifier":"spp.example.webapp.controller.WebappController.createUser(java.lang.String,java.lang.String)",
            "commitId":"todo",
            "type":"ENDPOINT",
            "lineNumber":null,
            "operationName":"{POST}/users"
         },
         "artifactSummarizedMetrics":[
            {
               "metricType":"Throughput_Average",
               "value":"4/sec"
            },
            {
               "metricType":"ResponseTime_Average",
               "value":"0ms"
            },
            {
               "metricType":"ServiceLevelAgreement_Average",
               "value":"100.0%"
            }
         ]
      },
      {
         "endpointType": "HTTP",
         "artifactQualifiedName":{
            "identifier":"spp.example.webapp.controller.WebappController.throwsException()",
            "commitId":"todo",
            "type":"ENDPOINT",
            "lineNumber":null,
            "operationName":"{GET}/throws-exception"
         },
         "artifactSummarizedMetrics":[
            {
               "metricType":"Throughput_Average",
               "value":"59/min"
            },
            {
               "metricType":"ResponseTime_Average",
               "value":"1ms"
            },
            {
               "metricType":"ServiceLevelAgreement_Average",
               "value":"0%"
            }
         ]
      },
      {
         "endpointType": "HTTP",
         "artifactQualifiedName":{
            "identifier":"spp.example.webapp.controller.WebappController.userList()",
            "commitId":"todo",
            "type":"ENDPOINT",
            "lineNumber":null,
            "operationName":"{GET}/users"
         },
         "artifactSummarizedMetrics":[
            {
               "metricType":"Throughput_Average",
               "value":"49/sec"
            },
            {
               "metricType":"ResponseTime_Average",
               "value":"15ms"
            },
            {
               "metricType":"ServiceLevelAgreement_Average",
               "value":"100.0%"
            }
         ]
      }
   ]
}
""".trimIndent()
        )
    )
}

fun displayChart(vertx: Vertx) {
    val seriesData =
        SplineSeriesData(
            0,
            listOf(
                Instant.fromEpochMilliseconds(
                    java.time.Instant.now().minusSeconds(10).toEpochMilli()
                ),
                Clock.System.now()
            ),
            listOf(current().nextDouble(10.0), current().nextDouble(10.0))
        )
    val splineChart = SplineChart(currentMetricType, QueryTimeFrame.LAST_15_MINUTES, listOf(seriesData))
    vertx.eventBus().updateChart("null", splineChart)
}

fun displayTraces(vertx: Vertx) {
    val traces = mutableListOf<Trace>()
    for (i in 1..20) {
        val trace = Trace(
            traceIds = listOf("${current().nextInt()}.${current().nextInt()}"),
            operationNames = listOf(UUID.randomUUID().toString()),
            prettyDuration = "10s",
            duration = 10000,
            error = current().nextBoolean(),
            start = Clock.System.now()
        )
        traces.add(trace)
    }

    val tracesResult = TraceResult(
        appUuid = "null",
        artifactQualifiedName = UUID.randomUUID().toString(),
        artifactSimpleName = UUID.randomUUID().toString(),
        start = Clock.System.now(),
        stop = Clock.System.now(),
        total = traces.size,
        traces = traces.toList(),
        orderType = TraceOrderType.LATEST_TRACES
    )
    vertx.eventBus().displayTraces("null", tracesResult)
}

fun updateCards(vertx: Vertx) {
    val throughputAverageCard =
        BarTrendCard(
            meta = "throughput_average",
            header = current().nextInt(100).toString(),
            barGraphData = emptyList()
        )
    val responseTimeAverageCard =
        BarTrendCard(
            meta = "responsetime_average",
            header = current().nextInt(100).toString(),
            barGraphData = emptyList()
        )
    val slaAverageCard =
        BarTrendCard(
            meta = "servicelevelagreement_average",
            header = current().nextInt(100).toString(),
            barGraphData = emptyList()
        )
    vertx.eventBus().displayCard("null", throughputAverageCard)
    vertx.eventBus().displayCard("null", responseTimeAverageCard)
    vertx.eventBus().displayCard("null", slaAverageCard)
}

class KSerializers {
    class KotlinInstantSerializer : JsonSerializer<Instant>() {
        override fun serialize(value: Instant, jgen: JsonGenerator, provider: SerializerProvider) =
            jgen.writeNumber(value.toEpochMilliseconds())
    }

    class KotlinInstantDeserializer : JsonDeserializer<Instant>() {
        override fun deserialize(p: JsonParser, p1: DeserializationContext): Instant =
            Instant.fromEpochMilliseconds((p.codec.readTree(p) as JsonNode).longValue())
    }
}
