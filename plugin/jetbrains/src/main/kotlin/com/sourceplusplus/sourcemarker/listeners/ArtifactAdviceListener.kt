package com.sourceplusplus.sourcemarker.listeners

import com.sourceplusplus.marker.plugin.SourceMarkerPlugin
import com.sourceplusplus.marker.source.mark.api.key.SourceKey
import com.sourceplusplus.protocol.advice.AdviceListener
import com.sourceplusplus.protocol.advice.ArtifactAdvice
import com.sourceplusplus.sourcemarker.actions.PluginSourceMarkPopupAction

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class ArtifactAdviceListener : AdviceListener {

    //todo: pending advice
    val ARTIFACT_ADVICE = SourceKey<MutableList<ArtifactAdvice>>("ARTIFACT_ADVICE")

    override fun advised(advice: ArtifactAdvice) {
        //todo: get sourcemark by endpoint name smarter
        //todo: atm endpoint name only added to sourcemark when portal is viewed
        val operationName = advice.artifact.identifier
        val sourceMark = SourceMarkerPlugin.getSourceMarks().firstOrNull {
            val endpointName = it.getUserData(PluginSourceMarkPopupAction.ENDPOINT_NAME)
            endpointName == operationName
        }
        if (sourceMark != null) {
            sourceMark.putUserData(ARTIFACT_ADVICE, mutableListOf())
            sourceMark.getUserData(ARTIFACT_ADVICE)!!.add(advice)
            sourceMark.getUserData(PluginSourceMarkPopupAction.SOURCE_PORTAL)!!.advice.add(advice)
            println("added advice")
        }
    }
}