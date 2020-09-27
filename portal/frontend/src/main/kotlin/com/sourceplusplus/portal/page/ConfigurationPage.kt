package com.sourceplusplus.portal.page

import com.sourceplusplus.portal.template.*
import com.sourceplusplus.protocol.artifact.ArtifactConfigType.AUTO_SUBSCRIBE
import com.sourceplusplus.protocol.artifact.ArtifactConfigType.ENTRY_METHOD
import com.sourceplusplus.protocol.artifact.ArtifactInfoType.*
import com.sourceplusplus.protocol.artifact.trace.TraceOrderType.*
import com.sourceplusplus.protocol.portal.PageType.*
import kotlinx.browser.document
import kotlinx.html.dom.append
import org.w3c.dom.Element

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class ConfigurationPage {
    fun renderPage() {
        console.log("Rendering Configuration page")
        val root: Element = document.getElementById("root")!!
        root.innerHTML = ""

        root.append {
            portalNav {
                navItem(OVERVIEW)
                navItem(TRACES) {
                    navSubItem(LATEST_TRACES, SLOWEST_TRACES, FAILED_TRACES)
                }
                navItem(CONFIGURATION, isActive = true)
            }
            configurationContent {
                navBar(false) {
                    rightAlign {
                        externalPortalButton()
                    }
                }
                configurationTable {
                    artifactConfiguration(ENTRY_METHOD, AUTO_SUBSCRIBE)
                    artifactInformation(QUALIFIED_NAME, CREATE_DATE, LAST_UPDATED, ENDPOINT)
                }
            }
        }
    }
}
