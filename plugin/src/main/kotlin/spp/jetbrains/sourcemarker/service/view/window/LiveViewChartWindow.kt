/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022 CodeBrig, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package spp.jetbrains.sourcemarker.service.view.window

import com.codahale.metrics.Histogram
import com.codahale.metrics.SlidingWindowReservoir
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.DarculaColors
import com.intellij.ui.JBColor
import com.intellij.ui.charts.*
import com.intellij.ui.components.JBTabbedPane
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import spp.jetbrains.PluginUI
import spp.jetbrains.UserData
import spp.jetbrains.sourcemarker.service.view.overlay.ValueDotPainter
import spp.protocol.artifact.metrics.MetricType
import spp.protocol.artifact.metrics.MetricType.Companion.Endpoint_RespTime_Percentiles
import spp.protocol.service.SourceServices.Subscribe.toLiveViewSubscriberAddress
import spp.protocol.view.LiveView
import spp.protocol.view.LiveViewConfig
import spp.protocol.view.LiveViewEvent
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Insets
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatterBuilder
import java.util.*
import javax.swing.JComponent
import javax.swing.SwingConstants

/**
 * todo: description.
 *
 * @since 0.7.6
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
open class LiveViewChartWindow(val project: Project) : Disposable {

    private val log = logger<LiveViewChartWindow>()
    private val formatter = DateTimeFormatterBuilder()
        .appendPattern("yyyyMMddHHmm")
        .toFormatter()
        .withZone(ZoneOffset.UTC)

    open val layoutComponent: JComponent
        get() = tabbedPane

    val tabbedPane = JBTabbedPane()
    var refreshInterval = 500
    val keepSize = 10 * 6 * 5
    val keepTimeSize = 10_000L * 6 * 5
    val xStepSize = 10_000L * 6
    val dateFormat = SimpleDateFormat("h:mm:ss a")

    private fun singleLineChart(
        entityId: String,
        metricType: MetricType,
        chartColor: Color
    ) = lineChart<Long, Double> {
        val dataset = XYLineDataset<Long, Double>().apply {
            label = metricType.simpleName
            lineColor = chartColor
            fillColor = chartColor.transparent(0.5)
            smooth = true
        }
        margins(marginInsets())
        ranges {
            yMin = 0.0
            yMax = 100.0
        }
        grid {
            xLines = generator(xStepSize)
            xPainter {
                label = if (value - 200 == xOrigin) "" else dateFormat.format(Date.from(Instant.ofEpochMilli(value)))
                verticalAlignment = SwingConstants.BOTTOM
                horizontalAlignment = SwingConstants.CENTER
            }
            yLines = generator(10.0)
            yPainter {
                majorLine = value == 100.0
                label = "${value.toInt()}" + if (metricType.requiresConversion) "%" else ""
                verticalAlignment = SwingConstants.CENTER
                horizontalAlignment = SwingConstants.RIGHT
            }
        }
        overlays = listOf(
            titleOverlay("${metricType.simpleName} (${metricType.unitType}) - $entityId"),
            ValueDotPainter(dataset, metricType)
        )
        datasets = listOf(dataset)
    }

    fun respTimePercentileChart(entityId: String, metricType: MetricType) = lineChart<Long, Long> {
        margins(marginInsets())
        ranges {
            yMin = 0L
            yMax = 100L
        }
        grid {
            xLines = generator(xStepSize)
            xPainter {
                label = if (value - 200 == xOrigin) "" else dateFormat.format(Date.from(Instant.ofEpochMilli(value)))
                verticalAlignment = SwingConstants.BOTTOM
                horizontalAlignment = SwingConstants.CENTER
            }
            yLines = generator(10L)
            yPainter {
                majorLine = value == 100L
                label = value.toInt().toString()
                verticalAlignment = SwingConstants.CENTER
                horizontalAlignment = SwingConstants.RIGHT
            }
        }
        overlays = listOf(
            titleOverlay("${metricType.simpleName} (${metricType.unitType}) - $entityId")
        )
        datasets {
            dataset {
                label = "Resp Time"
                lineColor = PluginUI.green
                smooth = true
            }
            dataset {
                label = "Resp Time"
                lineColor = PluginUI.purple
                smooth = true
            }
            dataset {
                label = "Resp Time"
                lineColor = JBColor.ORANGE
                smooth = true
            }
            dataset {
                label = "Resp Time"
                lineColor = DarculaColors.RED
                smooth = true
            }
            dataset {
                label = "Resp Time"
                lineColor = DarculaColors.BLUE
                smooth = true
            }
        }
    }

    private fun titleOverlay(label: String) = object : Overlay<ChartWrapper>() {
        override fun paintComponent(g: Graphics2D) {
            g.color = JBColor.foreground()
            g.font = g.font.deriveFont(10f)
            val w = g.fontMetrics.stringWidth(label)
            g.drawString(label, (chart.width - w) / 2, 30)
        }
    }

    private fun marginInsets(): Insets.() -> Unit {
        return {
            top = 30
            left = 55
            bottom = 30
            right = 50
        }
    }

    fun setupRespTimePercentileChart(
        entityId: String,
        vertx: Vertx,
        metricType: MetricType = Endpoint_RespTime_Percentiles.asRealtime()
    ): XYLineChart<Long, Long> {
        val chart = respTimePercentileChart(entityId, metricType)
        val histogram = Histogram(SlidingWindowReservoir(keepSize))

        UserData.liveViewService(project)!!.addLiveView(
            LiveView(
                entityIds = mutableSetOf(entityId),
                viewConfig = LiveViewConfig("ACTIVITY_VIEW", listOf(metricType.metricId), refreshInterval)
            )
        ).onSuccess { liveView ->
            val consumer = vertx.eventBus().consumer<JsonObject>(
                toLiveViewSubscriberAddress("system")
            )
            consumer.handler {
                val liveViewEvent = LiveViewEvent(it.body())
                if (liveView.subscriptionId != liveViewEvent.subscriptionId) return@handler

                val rawMetrics = JsonObject(liveViewEvent.metricsData)
                val rawValues = rawMetrics.getJsonArray("value") ?: rawMetrics.getJsonArray("values")
                val metricValues = rawValues.map { it.toString().toLong() }
                metricValues.forEach { histogram.update(it) }

                val step = histogram.snapshot.max / 10L
                if (step >= 1) {
                    chart.ranges.yMax = histogram.snapshot.max
                    chart.grid.yLines = generator(step)
                } else {
                    chart.ranges.yMax = 10L
                    chart.grid.yLines = generator(1L)
                }

                val timeBucket =
                    System.currentTimeMillis() //Instant.from(formatter.parse(rawMetrics.getLong("timeBucket").toString())).toEpochMilli() //System.currentTimeMillis()
                metricValues.forEachIndexed { i: Int, value: Long ->
                    chart.datasets[i].add(Coordinates.of(timeBucket, value))
                }
                chart.ranges.xMax = timeBucket
                chart.ranges.xMin = timeBucket - keepTimeSize
                chart.grid.xOrigin = chart.ranges.xMin - 200

                chart.update()
            }
            Disposer.register(this) { consumer.unregister() }
        }.onFailure {
            log.error("Failed to add live view", it)
        }

        return chart
    }

    fun setupSingleLineChart(
        entityId: String,
        vertx: Vertx,
        metricType: MetricType,
        chartColor: Color = defaultChartColor(metricType)
    ): XYLineChart<Long, Double> {
        val chart = singleLineChart(entityId, metricType, chartColor)
        val histogram = Histogram(SlidingWindowReservoir(keepSize))

        UserData.liveViewService(project)!!.addLiveView(
            LiveView(
                entityIds = mutableSetOf(entityId),
                viewConfig = LiveViewConfig("ACTIVITY_CHART", listOf(metricType.metricId), refreshInterval)
            )
        ).onSuccess { liveView ->
            val consumer = vertx.eventBus().consumer<JsonObject>(
                toLiveViewSubscriberAddress("system")
            )
            consumer.handler {
                val liveViewEvent = LiveViewEvent(it.body())
                if (liveView.subscriptionId != liveViewEvent.subscriptionId) return@handler
                val rawMetrics = JsonObject(liveViewEvent.metricsData)
                val metricValue = rawMetrics.getLong("value")
                histogram.update(metricValue)

                if (metricType.requiresConversion) {
                    //only for SLA
                    chart.ranges.yMax = (histogram.snapshot.max / metricType.unitConversion) * 1.01
                } else {
                    val step = histogram.snapshot.max / 10.0
                    if (step >= 1) {
                        chart.ranges.yMax = histogram.snapshot.max.toDouble() * 1.01
                        chart.grid.yLines = generator(step)
                    } else {
                        chart.ranges.yMax = 10.01
                        chart.grid.yLines = generator(1.0)
                    }
                }

                val timeBucket =
                    System.currentTimeMillis() //Instant.from(formatter.parse(rawMetrics.getLong("timeBucket").toString())).toEpochMilli() //System.currentTimeMillis()
                chart.datasets[0].add(Coordinates.of(timeBucket, metricValue / metricType.unitConversion))
                chart.ranges.xMax = timeBucket
                chart.ranges.xMin = timeBucket - keepTimeSize
                chart.grid.xOrigin = chart.ranges.xMin - 200

                chart.update()
            }
            Disposer.register(this) { consumer.unregister() }
        }.onFailure {
            log.error("Failed to add live view", it)
        }

        return chart
    }

    private fun defaultChartColor(metricType: MetricType): Color {
        return if (metricType.requiresConversion) {
            PluginUI.purple
        } else {
            DarculaColors.BLUE
        }
    }

    private val suffix = arrayOf("", "k", "m", "b", "t")
    private val MAX_LENGTH = 3

    private fun format(number: Double): String {
        var r: String = DecimalFormat("##0E0").format(number)
        r = r.replace("E[0-9]".toRegex(), suffix[Character.getNumericValue(r[r.length - 1]) / 3])
        while (r.length > MAX_LENGTH || r.matches("[0-9]+\\.[a-z]".toRegex())) {
            r = r.substring(0, r.length - 2) + r.substring(r.length - 1)
        }
        return r
    }

    override fun dispose() = Unit
}
