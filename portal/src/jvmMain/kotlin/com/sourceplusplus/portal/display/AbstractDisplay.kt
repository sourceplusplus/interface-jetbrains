package com.sourceplusplus.portal.display

import com.sourceplusplus.portal.SourcePortal
import com.sourceplusplus.protocol.portal.PageType
import io.vertx.kotlin.coroutines.CoroutineVerticle

/**
 * Contains common portal tab functionality.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
abstract class AbstractDisplay(val thisTab: PageType) : CoroutineVerticle() {

    abstract fun updateUI(portal: SourcePortal)
}
