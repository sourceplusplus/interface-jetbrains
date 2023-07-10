/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022-2023 CodeBrig, Inc.
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
package spp.jetbrains.sourcemarker.view.window

import com.codahale.metrics.Histogram
import com.codahale.metrics.SlidingWindowReservoir
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.ui.DarculaColors
import com.intellij.ui.JBColor
import com.intellij.ui.charts.*
import io.vertx.core.eventbus.MessageConsumer
import io.vertx.core.json.JsonObject
import spp.jetbrains.PluginUI
import spp.jetbrains.invokeLater
import spp.jetbrains.view.ResumableView
import spp.jetbrains.view.overlay.ValueDotPainter
import spp.protocol.artifact.metrics.MetricStep
import spp.protocol.artifact.metrics.MetricType
import spp.protocol.service.LiveViewService
import spp.protocol.view.LiveView
import spp.protocol.view.LiveViewEvent
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Insets
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import javax.swing.JComponent
import javax.swing.SwingConstants
import kotlin.math.ceil

//todo: MergingUpdateQueue that handles all the view updates atomically
/**
 * Displays a visual chart graph of the metric values supplied via [LiveView].
 *
 * @since 0.7.6
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class LiveViewChartWindowImpl(
    val project: Project,
    private val viewService: LiveViewService,
    var liveView: LiveView,
    private val entityName: String,
    private val labels: List<String>,
    private val consumerCreator: (LiveViewChartWindowImpl) -> MessageConsumer<JsonObject>
) : ResumableView {

    private val log = logger<LiveViewChartWindowImpl>()
    private var consumer: MessageConsumer<JsonObject>? = null
    private var step = MetricStep.MINUTE
    private var reservoirSize = 5
    private val keepTimeSize: Long
        get() = step.milliseconds.toLong() * timeSizeMultiplier()
    private var xStepSize: Long = (step.milliseconds).toLong()
    private val metricType = MetricType(liveView.viewConfig.viewMetrics.first())
    private var chartColor = defaultChartColor(metricType)
    private val timeFormat = SimpleDateFormat("h:mm a")
    private var histogramSize = 1000 //todo: optimize
    override var isRunning: Boolean = false
    private var histogram = Histogram(SlidingWindowReservoir(histogramSize))
    private val chart = singleLineChart(metricType, chartColor)
    val component: JComponent
        get() = chart.component
    override val refreshInterval: Int
        get() = liveView.viewConfig.refreshRateLimit
    private lateinit var hoverOverlay: ValueDotPainter

    override fun resume() {
        if (isRunning) return
        isRunning = true
        viewService.addLiveView(liveView).onSuccess {
            liveView = it
            consumer = consumerCreator.invoke(this)
        }.onFailure {
            log.error("Failed to resume live view", it)
        }

        getHistoricalData()
    }

    private fun getHistoricalData() {
        val stop = Instant.now()
        val start = stop.truncatedTo(ChronoUnit.SECONDS)
            .minusSeconds(60 + (getHistoricalMinutes() * 60).toLong())
        val step = step

        viewService.getHistoricalMetrics(
            liveView.entityIds.toList(),
            liveView.viewConfig.viewMetrics,
            step, start, stop, labels
        ).onSuccess {
            for (i in 0 until it.data.size()) {
                val stepBucket = step.bucketFormatter.format(start.plusSeconds((step.seconds * i).toLong())).toLong()
                val metricData = it.data.getJsonObject(i)
                addMetric(metricData.put("timeBucket", stepBucket))
            }
        }.onFailure {
            log.error("Failed to get historical metrics", it)
        }
    }

    override fun pause() {
        if (!isRunning) return
        isRunning = false
        consumer?.unregister()
        consumer = null
        liveView.subscriptionId?.let {
            if (project.isDisposed) {
                return //no need to remove view
            }
            viewService.removeLiveView(it).onFailure {
                log.error("Failed to pause live view", it)
            }
        }
    }

    override fun setRefreshInterval(interval: Int) {
        pause()
        liveView = liveView.copy(viewConfig = liveView.viewConfig.copy(refreshRateLimit = interval))
        resume()
    }

    fun getHistoricalMinutes(): Int {
        return reservoirSize
    }

    fun setHistoricalMinutes(historicalMinutes: Int) {
        step = if (historicalMinutes >= 720) MetricStep.HOUR else MetricStep.MINUTE
        reservoirSize = historicalMinutes
        histogram = Histogram(SlidingWindowReservoir(histogramSize))
        chart.clear()
        xStepSize = (step.milliseconds).toLong() * reservoirSize

        getHistoricalData()
    }

    private fun singleLineChart(metricType: MetricType, chartColor: Color) = lineChart<Long, Double> {
        val dataset = XYLineDataset<Long, Double>().apply {
            label = metricType.simpleName
            lineColor = chartColor
            fillColor = chartColor.transparent(0.5)
            smooth = false
        }
        hoverOverlay = ValueDotPainter(dataset, metricType)

        margins(marginInsets())
        ranges {
            yMin = 0.0
            yMax = 100.0
        }
        grid(makeGrid(dataset))
        overlays = listOf(
            titleOverlay("${metricType.simpleName} (${metricType.unitType}) - $entityName"),
            hoverOverlay
        )
        datasets = listOf(dataset)
    }

    private fun makeGrid(dataset: XYLineDataset<Long, Double>): Grid<Long, Double>.() -> Unit = {
        xLines = generator(xStepSize)
        xPainter {
            paintLine = if (value - (keepTimeSize / 25) == xOrigin) {
                false
            } else {
                val latestTime = Instant.ofEpochMilli(dataset.data.lastOrNull()?.x ?: System.currentTimeMillis())
                    .truncatedTo(ChronoUnit.MINUTES)
                val lineTime = Instant.ofEpochMilli(value).truncatedTo(ChronoUnit.MINUTES)
                val minutesBetween = ChronoUnit.MINUTES.between(latestTime, lineTime)

                if (latestTime == lineTime) {
                    true
                } else if (reservoirSize == 30 || reservoirSize == 60) {
                    (minutesBetween % 5).toInt() == 0
                } else if (reservoirSize == 120) {
                    (minutesBetween % 10).toInt() == 0
                } else if (reservoirSize == 240) {
                    (minutesBetween % 20).toInt() == 0
                } else if (reservoirSize == 480) {
                    (minutesBetween % 40).toInt() == 0
                } else if (reservoirSize >= 720) {
                    (minutesBetween % 60).toInt() == 0
                } else {
                    true
                }
            }
            majorLine = false
            label = if (hoverOverlay.mouseLocation != null) {
                ""
            } else if (value - (keepTimeSize / 25) == xOrigin) {
                ""
            } else {
                val latestTime = Instant.ofEpochMilli(dataset.data.lastOrNull()?.x ?: System.currentTimeMillis())
                    .truncatedTo(ChronoUnit.MINUTES)
                val lineTime = Instant.ofEpochMilli(value).truncatedTo(ChronoUnit.MINUTES)
                val minutesBetween = ChronoUnit.MINUTES.between(latestTime, lineTime)

                if (latestTime == lineTime) {
                    timeFormat.format(Date.from(Instant.ofEpochMilli(value)))
                } else if (reservoirSize == 30 || reservoirSize == 60) {
                    if ((minutesBetween % 5).toInt() == 0) {
                        timeFormat.format(Date.from(Instant.ofEpochMilli(value)))
                    } else ""
                } else if (reservoirSize == 120) {
                    if ((minutesBetween % 10).toInt() == 0) {
                        timeFormat.format(Date.from(Instant.ofEpochMilli(value)))
                    } else ""
                } else if (reservoirSize == 240) {
                    if ((minutesBetween % 20).toInt() == 0) {
                        timeFormat.format(Date.from(Instant.ofEpochMilli(value)))
                    } else ""
                } else if (reservoirSize == 480) {
                    if ((minutesBetween % 40).toInt() == 0) {
                        timeFormat.format(Date.from(Instant.ofEpochMilli(value)))
                    } else ""
                } else if (reservoirSize >= 720) {
                    if ((minutesBetween % 60).toInt() == 0) {
                        timeFormat.format(Date.from(Instant.ofEpochMilli(value)))
                    } else ""
                } else {
                    timeFormat.format(Date.from(Instant.ofEpochMilli(value)))
                }
            }
            verticalAlignment = SwingConstants.BOTTOM
            horizontalAlignment = SwingConstants.CENTER
        }
        yLines = generator(10.0)
        yPainter {
            majorLine = value == 100.0
            label = if (metricType.requiresConversion) {
                "${value.toInt()}%"
            } else {
                format(value)
            }
            verticalAlignment = SwingConstants.CENTER
            horizontalAlignment = SwingConstants.RIGHT
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
            top = 40
            left = 40
            bottom = 25
            right = 40
        }
    }

    private fun timeSizeMultiplier(): Int {
        return when (step) {
            MetricStep.SECOND -> reservoirSize * 60
            MetricStep.MINUTE -> reservoirSize
            MetricStep.HOUR -> reservoirSize / 60
            MetricStep.DAY -> reservoirSize / (60 * 24)
        }
    }

    fun addMetric(viewEvent: LiveViewEvent) {
        val rawMetrics = JsonObject(viewEvent.metricsData).put("timeBucket", viewEvent.timeBucket)
        addMetric(rawMetrics)
    }

    private fun addMetric(rawMetrics: JsonObject) = project.invokeLater {
        val metricValue = rawMetrics.getLong("value") ?: 0
        histogram.update(metricValue)

        if (metricType.requiresConversion) {
            chart.ranges.yMax = 100.0 //only for SLA
        } else {
            val step = ceil((histogram.snapshot.max / 10.0) / 5.0) * 5
            if (step >= 1) {
                chart.ranges.yMax = histogram.snapshot.max.toDouble() * 1.01
                if (chart.ranges.yMax == 0.0) chart.ranges.yMax = 1.01
                chart.grid.yLines = generator(step)
            } else {
                chart.ranges.yMax = 10.01
                chart.grid.yLines = generator(1.0)
            }
        }

        var timeBucket = rawMetrics.getLong("currentTime")
        if (timeBucket == null) {
            val bucket = rawMetrics.getValue("timeBucket").toString()
            val step = MetricStep.fromBucketFormat(bucket)
            timeBucket = Instant.from(step.bucketFormatter.parse(bucket)).toEpochMilli()
        }
        val newCoordinates = Coordinates.of(timeBucket, metricValue / metricType.unitConversion)
        val chartData = chart.datasets[0].data as MutableList<Coordinates<Long, Double>>
        val existingCoordinatesIndex = chartData.indexOfFirst { it.x >= timeBucket }
        if (existingCoordinatesIndex != -1) {
            if (chartData[existingCoordinatesIndex].x == timeBucket) {
                chartData[existingCoordinatesIndex] = newCoordinates
            } else {
                chartData.add(existingCoordinatesIndex, newCoordinates)
            }
        } else {
            chartData.add(newCoordinates)
        }

        chart.ranges.xMax = timeBucket
        chart.ranges.xMin = timeBucket - keepTimeSize
        chart.grid.xOrigin = chart.ranges.xMin - (keepTimeSize / 25)
        chart.update()
    }

    companion object {
        fun defaultChartColor(metricType: MetricType): Color {
            return if (metricType.metricId.contains("sla")) {
                PluginUI.purple
            } else if (metricType.metricId.contains("cpm")) {
                PluginUI.yellow
            } else {
                DarculaColors.BLUE
            }
        }

        private val suffix = arrayOf("", "k", "m", "b", "t")
        private const val MAX_LENGTH = 4

        private fun format(number: Double): String {
            var r: String = DecimalFormat("##0E0").format(number)
            r = r.replace("E[0-9]".toRegex(), suffix[Character.getNumericValue(r[r.length - 1]) / 3])
            while (r.length > MAX_LENGTH || r.matches("[0-9]+\\.[a-z]".toRegex())) {
                r = r.substring(0, r.length - 2) + r.substring(r.length - 1)
            }
            return r
        }
    }

    override fun dispose() {
        try {
            pause()
        } catch (e: Exception) {
            log.warn("Failed to dispose live view", e)
        }
    }
}
