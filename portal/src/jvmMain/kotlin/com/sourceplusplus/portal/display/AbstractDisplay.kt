package com.sourceplusplus.portal.display

import com.sourceplusplus.portal.SourcePortal
import com.sourceplusplus.protocol.portal.PageType
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
        log.info("{} started", javaClass.simpleName)
    }

    override suspend fun stop() {
        log.info("{} stopped", javaClass.simpleName)
    }

    abstract fun updateUI(portal: SourcePortal)
}
