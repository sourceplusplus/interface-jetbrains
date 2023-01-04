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
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.ui.DarculaColors
import com.intellij.ui.JBColor
import com.intellij.ui.charts.*
import io.vertx.core.eventbus.MessageConsumer
import io.vertx.core.json.JsonObject
import spp.jetbrains.PluginUI
import spp.jetbrains.UserData
import spp.jetbrains.sourcemarker.view.overlay.ValueDotPainter
import spp.jetbrains.view.ResumableView
import spp.protocol.artifact.metrics.MetricStep
import spp.protocol.artifact.metrics.MetricType
import spp.protocol.view.LiveView
import spp.protocol.view.LiveViewEvent
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Insets
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*
import javax.swing.JComponent
import javax.swing.SwingConstants

/**
 * todo: description.
 *
 * @since 0.7.6
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class LiveViewChartWindow(
    val project: Project,
    var liveView: LiveView,
    private val entityName: String,
    private val consumerCreator: (LiveViewChartWindow) -> MessageConsumer<JsonObject>
) : ResumableView {

    private val log = logger<LiveViewChartWindow>()
    private val viewService = UserData.liveViewService(project)!!
    private var consumer: MessageConsumer<JsonObject>? = null
    private var step = MetricStep.MINUTE
    private var reservoirSize = 5
    private val keepTimeSize: Int
        get() = step.milliseconds * reservoirSize
    private var xStepSize = step.milliseconds
    private val metricType = MetricType(liveView.viewConfig.viewMetrics.first())
    private var chartColor = defaultChartColor(metricType)
    private val timeFormat = SimpleDateFormat("h:mm a")
    private val timeWithSecondsFormat = SimpleDateFormat("h:mm:ss a")
    private val dateFormat: SimpleDateFormat
        get() = if (metricType.isRealtime) {
            timeWithSecondsFormat
        } else {
            timeFormat
        }

    private var histogramSize = 1000 //todo: optimize
    override var isRunning: Boolean = false
    private var histogram = Histogram(SlidingWindowReservoir(histogramSize))
    private val chart = singleLineChart(metricType, chartColor)
    val component: JComponent
        get() = chart.component
    override val refreshInterval: Int
        get() = liveView.viewConfig.refreshRateLimit

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
        val reservoirSize = if (step == MetricStep.MINUTE) 60 else 24
        val stop = Instant.now()
        val start = stop.minusSeconds((reservoirSize * step.seconds).toLong())
        viewService.getHistoricalMetrics(
            liveView.entityIds.toList(),
            liveView.viewConfig.viewMetrics,
            step, start, stop
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
        step = if (historicalMinutes >= 720) {
            MetricStep.HOUR
        } else {
            MetricStep.MINUTE
        }

        reservoirSize = historicalMinutes
        histogram = Histogram(SlidingWindowReservoir(histogramSize))
        (chart.datasets[0].data as MutableList<Coordinates<Long, Double>>).clear()
        xStepSize = step.milliseconds * reservoirSize

        getHistoricalData()
    }

    private fun singleLineChart(metricType: MetricType, chartColor: Color) = lineChart<Long, Double> {
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
            xLines = generator(xStepSize.toLong())
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
            titleOverlay("${metricType.simpleName} (${metricType.unitType}) - $entityName"),
            ValueDotPainter(dataset, metricType)
        )
        datasets = listOf(dataset)
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

    fun addMetric(viewEvent: LiveViewEvent) {
        val rawMetrics = JsonObject(viewEvent.metricsData)
        addMetric(rawMetrics)
    }

    private fun addMetric(rawMetrics: JsonObject) = ApplicationManager.getApplication().invokeLater {
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

        val timeBucket = rawMetrics.getLong("currentTime")
            ?: Instant.from(step.bucketFormatter.parse(rawMetrics.getLong("timeBucket").toString())).toEpochMilli()
        val newCoordinates = Coordinates.of(timeBucket, metricValue / metricType.unitConversion)
        val chartData = chart.datasets[0].data as MutableList<Coordinates<Long, Double>>
        val existingCoordinatesIndex = chartData.indexOfFirst { it.x == timeBucket }
        if (existingCoordinatesIndex != -1) {
            chartData[existingCoordinatesIndex] = newCoordinates
        } else {
            chartData.add(newCoordinates)
        }

        chart.ranges.xMax = timeBucket
        chart.ranges.xMin = timeBucket - keepTimeSize
        chart.grid.xOrigin = chart.ranges.xMin - 200
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
        private val MAX_LENGTH = 3

        private fun format(number: Double): String {
            var r: String = DecimalFormat("##0E0").format(number)
            r = r.replace("E[0-9]".toRegex(), suffix[Character.getNumericValue(r[r.length - 1]) / 3])
            while (r.length > MAX_LENGTH || r.matches("[0-9]+\\.[a-z]".toRegex())) {
                r = r.substring(0, r.length - 2) + r.substring(r.length - 1)
            }
            return r
        }
    }

    override fun dispose() = Unit
}
