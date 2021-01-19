package com.sourceplusplus.portal.template

import com.sourceplusplus.portal.PortalBundle.translate
import com.sourceplusplus.portal.model.ArtifactConfigType
import com.sourceplusplus.portal.model.ArtifactInfoType
import kotlinx.html.*
import org.w3c.dom.HTMLElement

fun TagConsumer<HTMLElement>.configurationContent(block: FlowContent.() -> Unit) {
    div("pusher background_color") {
        block()
    }
}

fun TagConsumer<HTMLElement>.configurationTable(block: FlowContent.() -> Unit) {
    div("ui grid marginlefting min_height") {
        block()
    }
}

fun TagConsumer<HTMLElement>.artifactConfiguration(vararg artifactConfigTypes: ArtifactConfigType = arrayOf()) {
    div("six wide column min_height") {
        div("ui form") {
            div("ui segment secondary_background_color") {
                for (artifactConfigType in artifactConfigTypes) {
                    div("field") {
                        div("ui toggle checkbox") {
                            id = "${artifactConfigType.id}_toggle"
                            input {
                                id = "${artifactConfigType.id}_checkbox"
                                type = InputType.checkBox
                            }
                            label("secondary_background_color") { + translate(artifactConfigType.description) }
                        }
                    }
                }
            }
        }
    }
}

fun TagConsumer<HTMLElement>.artifactInformation(vararg artifactInfoTypes: ArtifactInfoType = arrayOf()) {
    div("ten wide column min_height") {
        div("ui segments secondary_background_color margin_right_white") {
            table("ui small very compact very basic collapsing celled table margin_left_no_padding") {
                tbody {
                    for (artifactInfoType in artifactInfoTypes) {
                        tr {
                            td {
                                h4("ui header secondary_background_color") {
                                    div("content") { + translate(artifactInfoType.description) }
                                }
                            }
                            td("secondary_background_color") {
                                id = "artifact_${artifactInfoType.id}"
                            }
                        }
                    }
                }
            }
        }
    }
}
