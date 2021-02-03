package com.sourceplusplus.protocol

/**
 * Contains all the public addresses which consume and send protocol messages.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
object ProtocolAddress {
    object Global { //todo: could probably rename to Plugin to indicate it's the module consuming message
        //Portal
        const val FindPortal = "FindAndOpenPortal"
        const val FindAndOpenPortal = "FindAndOpenPortal"
        const val OpenPortal = "OpenPortal"
        const val ClosePortal = "ClosePortal"
        const val ClickedViewAsExternalPortal = "ClickedViewAsExternalPortal"
        const val KeepAlivePortal = "KeepAlivePortal" //todo: remove
        const val UpdatePortalArtifact = "UpdatePortalArtifact"
        const val GetPortalConfiguration = "GetPortalConfiguration"
        const val SetCurrentPage = "SetCurrentPage"
        const val RefreshPortal = "RefreshPortal"
        const val CanOpenPortal = "CanOpenPortal"
        const val ChangedPortalArtifact = "ChangedPortalArtifact"
        const val CanNavigateToArtifact = "CanNavigateToArtifact"
        const val NavigateToArtifact = "NavigateToArtifact"
        const val GetPortalTranslations = "GetPortalTranslations"

        //Portal - Overview
        const val RefreshOverview = "RefreshOverview"
        const val SetOverviewTimeFrame = "SetOverviewTimeFrame"
        const val ClickedEndpointArtifact = "ClickedEndpointArtifact"

        //Portal - Activity
        const val ActivityTabOpened = "ActivityTabOpened"
        const val SetMetricTimeFrame = "SetMetricTimeFrame"
        const val SetActiveChartMetric = "SetActiveChartMetric"
        const val RefreshActivity = "RefreshActivity"
        const val ArtifactMetricsUpdated = "ArtifactMetricUpdated"

        //Portal - Traces
        const val SetTraceOrderType = "SetTraceOrderType"
        const val ClickedDisplayTraces = "ClickedDisplayTraces"
        const val ClickedDisplayTraceStack = "ClickedDisplayTraceStack"
        const val ClickedDisplayInnerTraceStack = "ClickedDisplayInnerTraceStack"
        const val ClickedDisplaySpanInfo = "ClickedDisplaySpanInfo"
        const val GetTraceStack = "GetTraceStack"
        const val RefreshTraces = "RefreshTraces"
        const val ArtifactTracesUpdated = "ArtifactTracesUpdated"
        const val TraceSpanUpdated = "TraceSpanUpdated"
        const val QueryTraceStack = "QueryTraceStack"
        const val ClickedStackTraceElement = "ClickedStackTraceElement"
        const val FetchMoreTraces = "FetchMoreTraces"

        //Portal - Logs
        const val SetLogOrderType = "SetLogOrderType"
        const val RefreshLogs = "RefreshLogs"
        const val ArtifactLogUpdated = "ArtifactLogUpdated"
        const val ClickedDisplayLog = "ClickedDisplayLog"
        const val ClickedDisplayLogs = "ClickedDisplayLogs"
        const val FetchMoreLogs = "FetchMoreLogs"

        //Portal - Configuration
        const val DisplayArtifactConfiguration = "DisplayArtifactConfiguration"
        const val UpdateArtifactEntryMethod = "UpdateArtifactEntryMethod"
        const val UpdateArtifactAutoSubscribe = "UpdateArtifactAutoSubscribe"
    }

    @Suppress("FunctionName")
    object Portal {
        fun RenderPage(portalUuid: String): String {
            return "$portalUuid-RenderPage"
        }

        fun UpdateEndpoints(portalUuid: String): String {
            return "$portalUuid-UpdateEndpoints"
        }

        fun ClearActivity(portalUuid: String): String {
            return "$portalUuid-ClearActivity"
        }

        fun DisplayActivity(portalUuid: String): String {
            return "$portalUuid-DisplayActivity"
        }

        fun DisplayTraces(portalUuid: String): String {
            return "$portalUuid-DisplayTraces"
        }

        fun DisplayLogs(portalUuid: String): String {
            return "$portalUuid-DisplayLogs"
        }

        fun DisplayLog(portalUuid: String): String {
            return "$portalUuid-DisplayLog"
        }

        fun DisplayTraceStack(portalUuid: String): String {
            return "$portalUuid-DisplayTraceStack"
        }

        fun DisplaySpanInfo(portalUuid: String): String {
            return "$portalUuid-DisplaySpanInfo"
        }

        fun UpdateTraceSpan(portalUuid: String): String {
            return "$portalUuid-UpdateTraceSpan"
        }

        fun DisplayArtifactConfiguration(portalUuid: String): String {
            return "$portalUuid-DisplayArtifactConfiguration"
        }
    }
}
