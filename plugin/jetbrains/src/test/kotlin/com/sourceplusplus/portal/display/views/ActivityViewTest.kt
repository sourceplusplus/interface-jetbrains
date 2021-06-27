package com.sourceplusplus.portal.display.views

import com.sourceplusplus.portal.SourcePortal
import com.sourceplusplus.protocol.artifact.QueryTimeFrame
import com.sourceplusplus.protocol.artifact.metrics.ArtifactMetricResult
import com.sourceplusplus.protocol.artifact.metrics.ArtifactMetrics
import com.sourceplusplus.protocol.artifact.metrics.MetricType
import com.sourceplusplus.protocol.portal.PortalConfiguration
import kotlinx.datetime.toKotlinInstant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class ActivityViewTest {

    @Test
    fun startPush1Minute() {
        val className = "spp.example.webapp.controller.WebappController"
        val artifactName = "$className.getUser(long)"
        val portalUuid = "5471535f-2a5f-4ed2-bfaf-65345c59fd7b"
        println(
            "Portal UUID: " + SourcePortal.register(
                portalUuid,
                "null",
                artifactName,
                PortalConfiguration(external = true)
            )
        )
        val portal = SourcePortal.getPortal(portalUuid)!!

        val now = ZonedDateTime.now().truncatedTo(ChronoUnit.MINUTES)
        val endTime = now.plusSeconds(60).toInstant()
        val startTime = now.toInstant()
        val pushResult = ArtifactMetricResult(
            "null",
            portal.viewingPortalArtifact,
            QueryTimeFrame.LAST_MINUTE,
            MetricType.Throughput_Average,
            startTime.toKotlinInstant(),
            endTime.toKotlinInstant(),
            "MINUTE",
            listOf(
                ArtifactMetrics(
                    MetricType.Throughput_Average,
                    listOf(1.0)
                )
            ),
            true
        )
        portal.activityView.cacheMetricResult(pushResult)

        val metricResult = portal.activityView.metricResultCache
            .get(portal.viewingPortalArtifact)!!.get(QueryTimeFrame.LAST_MINUTE)
        assertNotNull(metricResult)
        assertEquals(1, metricResult!!.artifactMetrics.size)
        assertEquals(listOf(1.0), metricResult.artifactMetrics.first().values)
        assertEquals(startTime.toKotlinInstant(), metricResult.start)
        assertEquals(endTime.toKotlinInstant(), metricResult.stop)
    }

    @Test
    fun startPush1MinuteUpdated() {
        val className = "spp.example.webapp.controller.WebappController"
        val artifactName = "$className.getUser(long)"
        val portalUuid = "5471535f-2a5f-4ed2-bfaf-65345c59fd7b"
        println(
            "Portal UUID: " + SourcePortal.register(
                portalUuid,
                "null",
                artifactName,
                PortalConfiguration(external = true)
            )
        )
        val portal = SourcePortal.getPortal(portalUuid)!!

        val now = ZonedDateTime.now().truncatedTo(ChronoUnit.MINUTES)
        val endTime = now.plusSeconds(60).toInstant()
        val startTime = now.toInstant()
        val pushResult = ArtifactMetricResult(
            "null",
            portal.viewingPortalArtifact,
            QueryTimeFrame.LAST_MINUTE,
            MetricType.Throughput_Average,
            startTime.toKotlinInstant(),
            endTime.toKotlinInstant(),
            "MINUTE",
            listOf(
                ArtifactMetrics(
                    MetricType.Throughput_Average,
                    listOf(1.0)
                )
            ),
            true
        )
        portal.activityView.cacheMetricResult(pushResult)

        val pushResult2 = ArtifactMetricResult(
            "null",
            portal.viewingPortalArtifact,
            QueryTimeFrame.LAST_MINUTE,
            MetricType.Throughput_Average,
            startTime.toKotlinInstant(),
            endTime.toKotlinInstant(),
            "MINUTE",
            listOf(
                ArtifactMetrics(
                    MetricType.Throughput_Average,
                    listOf(2.0)
                )
            ),
            true
        )
        portal.activityView.cacheMetricResult(pushResult2)

        val metricResult = portal.activityView.metricResultCache
            .get(portal.viewingPortalArtifact)!!.get(QueryTimeFrame.LAST_MINUTE)
        assertNotNull(metricResult)
        assertEquals(1, metricResult!!.artifactMetrics.size)
        assertEquals(listOf(2.0), metricResult.artifactMetrics.first().values)
        assertEquals(startTime.toKotlinInstant(), metricResult.start)
        assertEquals(endTime.toKotlinInstant(), metricResult.stop)
    }

    @Test
    fun startPush1MinuteUpdatedAfter() {
        val className = "spp.example.webapp.controller.WebappController"
        val artifactName = "$className.getUser(long)"
        val portalUuid = "5471535f-2a5f-4ed2-bfaf-65345c59fd7b"
        println(
            "Portal UUID: " + SourcePortal.register(
                portalUuid,
                "null",
                artifactName,
                PortalConfiguration(external = true)
            )
        )
        val portal = SourcePortal.getPortal(portalUuid)!!

        val now = ZonedDateTime.now().truncatedTo(ChronoUnit.MINUTES)
        val endTime = now.plusSeconds(60).toInstant()
        val startTime = now.toInstant()
        val pushResult = ArtifactMetricResult(
            "null",
            portal.viewingPortalArtifact,
            QueryTimeFrame.LAST_MINUTE,
            MetricType.Throughput_Average,
            startTime.toKotlinInstant(),
            endTime.toKotlinInstant(),
            "MINUTE",
            listOf(
                ArtifactMetrics(
                    MetricType.Throughput_Average,
                    listOf(1.0)
                )
            ),
            true
        )
        portal.activityView.cacheMetricResult(pushResult)

        val pushResult2 = ArtifactMetricResult(
            "null",
            portal.viewingPortalArtifact,
            QueryTimeFrame.LAST_MINUTE,
            MetricType.Throughput_Average,
            startTime.plusSeconds(60).toKotlinInstant(),
            endTime.plusSeconds(60).toKotlinInstant(),
            "MINUTE",
            listOf(
                ArtifactMetrics(
                    MetricType.Throughput_Average,
                    listOf(2.0)
                )
            ),
            true
        )
        portal.activityView.cacheMetricResult(pushResult2)

        val metricResult = portal.activityView.metricResultCache
            .get(portal.viewingPortalArtifact)!!.get(QueryTimeFrame.LAST_MINUTE)
        assertNotNull(metricResult)
        assertEquals(1, metricResult!!.artifactMetrics.size)
        assertEquals(listOf(2.0), metricResult.artifactMetrics.first().values)
        assertEquals(startTime.plusSeconds(60).toKotlinInstant(), metricResult.start)
        assertEquals(endTime.plusSeconds(60).toKotlinInstant(), metricResult.stop)
    }

    @Test
    fun push1MinuteAfter() {
        val className = "spp.example.webapp.controller.WebappController"
        val artifactName = "$className.getUser(long)"
        val portalUuid = "5471535f-2a5f-4ed2-bfaf-65345c59fd7b"
        println(
            "Portal UUID: " + SourcePortal.register(
                portalUuid,
                "null",
                artifactName,
                PortalConfiguration(external = true)
            )
        )
        val portal = SourcePortal.getPortal(portalUuid)!!

        val now = ZonedDateTime.now().truncatedTo(ChronoUnit.MINUTES)
        val endTime = now.toInstant()
        val startTime = now.minusMinutes(portal.activityView.timeFrame.minutes.toLong()).toInstant()
        val originalResult = ArtifactMetricResult(
            "null",
            portal.viewingPortalArtifact,
            QueryTimeFrame.LAST_5_MINUTES,
            MetricType.Throughput_Average,
            startTime.toKotlinInstant(),
            endTime.toKotlinInstant(),
            "MINUTE",
            listOf(
                ArtifactMetrics(
                    MetricType.Throughput_Average,
                    listOf(1.0, 2.0, 3.0, 4.0, 5.0)
                )
            )
        )
        portal.activityView.cacheMetricResult(originalResult)

        val updatedStartTime = now.plusMinutes(1).toInstant()
        val pushResult = ArtifactMetricResult(
            "null",
            portal.viewingPortalArtifact,
            QueryTimeFrame.LAST_MINUTE,
            MetricType.Throughput_Average,
            endTime.toKotlinInstant(),
            updatedStartTime.toKotlinInstant(),
            "MINUTE",
            listOf(
                ArtifactMetrics(
                    MetricType.Throughput_Average,
                    listOf(6.0)
                )
            ),
            true
        )
        portal.activityView.cacheMetricResult(pushResult)

        val metricResult = portal.activityView.metricResult
        assertNotNull(metricResult)
        assertEquals(1, metricResult!!.artifactMetrics.size)
        assertEquals(listOf(2.0, 3.0, 4.0, 5.0, 6.0), metricResult.artifactMetrics.first().values)
        assertEquals(startTime.plusSeconds(60).toKotlinInstant(), metricResult.start)
        assertEquals(endTime.plusSeconds(60).toKotlinInstant(), metricResult.stop)
    }

    @Test
    fun push1MinuteDuring() {
        val className = "spp.example.webapp.controller.WebappController"
        val artifactName = "$className.getUser(long)"
        val portalUuid = "5471535f-2a5f-4ed2-bfaf-65345c59fd7b"
        println(
            "Portal UUID: " + SourcePortal.register(
                portalUuid,
                "null",
                artifactName,
                PortalConfiguration(external = true)
            )
        )
        val portal = SourcePortal.getPortal(portalUuid)!!

        val now = ZonedDateTime.now().truncatedTo(ChronoUnit.MINUTES)
        val endTime = now.toInstant()
        val startTime = now.minusMinutes(portal.activityView.timeFrame.minutes.toLong()).toInstant()
        val originalResult = ArtifactMetricResult(
            "null",
            portal.viewingPortalArtifact,
            QueryTimeFrame.LAST_5_MINUTES,
            MetricType.Throughput_Average,
            startTime.toKotlinInstant(),
            endTime.toKotlinInstant(),
            "MINUTE",
            listOf(
                ArtifactMetrics(
                    MetricType.Throughput_Average,
                    listOf(1.0, 2.0, 3.0, 4.0, 5.0)
                )
            )
        )
        portal.activityView.cacheMetricResult(originalResult)

        val updatedStartTime = startTime.plusSeconds(60)
        val pushResult = ArtifactMetricResult(
            "null",
            portal.viewingPortalArtifact,
            QueryTimeFrame.LAST_MINUTE,
            MetricType.Throughput_Average,
            startTime.toKotlinInstant(),
            updatedStartTime.toKotlinInstant(),
            "MINUTE",
            listOf(
                ArtifactMetrics(
                    MetricType.Throughput_Average,
                    listOf(6.0)
                )
            ),
            true
        )
        portal.activityView.cacheMetricResult(pushResult)

        val metricResult = portal.activityView.metricResult
        assertNotNull(metricResult)
        assertEquals(1, metricResult!!.artifactMetrics.size)
        assertEquals(listOf(6.0, 2.0, 3.0, 4.0, 5.0), metricResult.artifactMetrics.first().values)
        assertEquals(startTime.toKotlinInstant(), metricResult.start)
        assertEquals(endTime.toKotlinInstant(), metricResult.stop)
    }

    @Test
    fun push1MinuteBefore() {
        val className = "spp.example.webapp.controller.WebappController"
        val artifactName = "$className.getUser(long)"
        val portalUuid = "5471535f-2a5f-4ed2-bfaf-65345c59fd7b"
        println(
            "Portal UUID: " + SourcePortal.register(
                portalUuid,
                "null",
                artifactName,
                PortalConfiguration(external = true)
            )
        )
        val portal = SourcePortal.getPortal(portalUuid)!!

        val now = ZonedDateTime.now().truncatedTo(ChronoUnit.MINUTES)
        val endTime = now.toInstant()
        val startTime = now.minusMinutes(portal.activityView.timeFrame.minutes.toLong()).toInstant()
        val originalResult = ArtifactMetricResult(
            "null",
            portal.viewingPortalArtifact,
            QueryTimeFrame.LAST_5_MINUTES,
            MetricType.Throughput_Average,
            startTime.toKotlinInstant(),
            endTime.toKotlinInstant(),
            "MINUTE",
            listOf(
                ArtifactMetrics(
                    MetricType.Throughput_Average,
                    listOf(1.0, 2.0, 3.0, 4.0, 5.0)
                )
            )
        )
        portal.activityView.cacheMetricResult(originalResult)

        val updatedEndTime = startTime.minusSeconds(60)
        val pushResult = ArtifactMetricResult(
            "null",
            portal.viewingPortalArtifact,
            QueryTimeFrame.LAST_MINUTE,
            MetricType.Throughput_Average,
            updatedEndTime.toKotlinInstant(),
            startTime.toKotlinInstant(),
            "MINUTE",
            listOf(
                ArtifactMetrics(
                    MetricType.Throughput_Average,
                    listOf(6.0)
                )
            ),
            true
        )
        portal.activityView.cacheMetricResult(pushResult)

        val metricResult = portal.activityView.metricResult
        assertNotNull(metricResult)
        assertEquals(1, metricResult!!.artifactMetrics.size)
        assertEquals(listOf(1.0, 2.0, 3.0, 4.0, 5.0), metricResult.artifactMetrics.first().values)
        assertEquals(startTime.toKotlinInstant(), metricResult.start)
        assertEquals(endTime.toKotlinInstant(), metricResult.stop)
    }
}
