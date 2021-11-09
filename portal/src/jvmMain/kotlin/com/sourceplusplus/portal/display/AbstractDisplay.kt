package com.sourceplusplus.portal.display

import com.sourceplusplus.portal.SourcePortal
import spp.protocol.ProtocolAddress.Global.RefreshPortal
import spp.protocol.portal.PageType
import io.vertx.kotlin.coroutines.CoroutineVerticle
import org.slf4j.LoggerFactory

/**
 * Contains common portal tab functionality.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
abstract class AbstractDisplay(val thisTab: PageType) : CoroutineVerticle() {

    companion object {
        private val log = LoggerFactory.getLogger(AbstractDisplay::class.java)
    }

    override suspend fun start() {
        vertx.eventBus().consumer<Any>(RefreshPortal) {
            val portal = if (it.body() is String) {
                SourcePortal.getPortal(it.body() as String)
            } else {
                it.body() as SourcePortal
            }!!
            updateUI(portal)
        }
        log.info("{} started", javaClass.simpleName)
    }

    override suspend fun stop() {
        log.info("{} stopped", javaClass.simpleName)
    }

    abstract fun updateUI(portal: SourcePortal)
}
