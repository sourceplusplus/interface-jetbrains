package com.sourceplusplus.core.integration

import io.vertx.core.AbstractVerticle

/**
 * todo: description
 *
 * @version 0.2.0
 * @since 0.2.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
abstract class SourceIntegration extends AbstractVerticle {

    abstract void enableIntegration()

    abstract void disableIntegration()
}
