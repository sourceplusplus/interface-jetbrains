package com.sourceplusplus.protocol

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class ProtocolAddress {
    class Global {
        companion object {
            //Portal
            const val ClosePortal = "ClosePortal"
            const val ClickedViewAsExternalPortal = "ClickedViewAsExternalPortal"
            const val KeepAlivePortal = "KeepAlivePortal" //todo: remove
            const val UpdatePortalArtifact = "UpdatePortalArtifact"
            const val CanOpenPortal = "CanOpenPortal"
            //const val OpenedPortal = "OpenedPortal"
            //const val ClosedPortal = "ClosedPortal"
            const val ChangedPortalArtifact = "ChangedPortalArtifact"

            //Portal - Overview
            const val OverviewTabOpened = "OverviewTabOpened"
            const val RefreshOverview = "RefreshOverview"

            //Portal - Activity
            const val ActivityTabOpened = "ActivityTabOpened"
            const val SetMetricTimeFrame = "SetMetricTimeFrame"
            const val SetActiveChartMetric = "SetActiveChartMetric"
            const val RefreshActivity = "RefreshActivity"
            const val ArtifactMetricUpdated = "ArtifactMetricUpdated"

            //Portal - Traces
            const val TracesTabOpened = "TracesTabOpened"
            const val ClickedDisplayTraces = "ClickedDisplayTraces"
            const val ClickedDisplayTraceStack = "ClickedDisplayTraceStack"
            const val ClickedDisplaySpanInfo = "ClickedDisplaySpanInfo"
            const val GetTraceStack = "GetTraceStack"
            const val RefreshTraces = "RefreshTraces"
            const val ArtifactTraceUpdated = "ArtifactTraceUpdated"
            const val QueryTraceStack = "QueryTraceStack"

            //Portal - Configuration
            const val ConfigurationTabOpened = "ConfigurationTabOpened"
            const val DisplayArtifactConfiguration = "DisplayArtifactConfiguration"
            const val UpdateArtifactEntryMethod = "UpdateArtifactEntryMethod"
            const val UpdateArtifactAutoSubscribe = "UpdateArtifactAutoSubscribe"
        }
    }

    @Suppress("FunctionName")
    class Portal {
        companion object {
            fun UpdateEndpoints(portalUuid: String): String {
                return "$portalUuid-UpdateEndpoints"
            }

            fun ClearActivity(portalUuid: String): String {
                return "$portalUuid-ClearActivity"
            }

            fun UpdateChart(portalUuid: String): String {
                return "$portalUuid-UpdateChart"
            }

            fun DisplayCard(portalUuid: String): String {
                return "$portalUuid-DisplayCard"
            }

            fun DisplayTraces(portalUuid: String): String {
                return "$portalUuid-DisplayTraces"
            }

            fun DisplayTraceStack(portalUuid: String): String {
                return "$portalUuid-DisplayTraceStack"
            }

            fun DisplayInnerTraceStack(portalUuid: String): String {
                return "$portalUuid-DisplayInnerTraceStack"
            }

            fun DisplaySpanInfo(portalUuid: String): String {
                return "$portalUuid-DisplaySpanInfo"
            }

            fun DisplayArtifactConfiguration(portalUuid: String): String {
                return "$portalUuid-DisplayArtifactConfiguration"
            }
        }
    }
}
