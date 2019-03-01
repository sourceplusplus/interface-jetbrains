package com.sourceplusplus.plugin.source.navigate

import io.vertx.core.AbstractVerticle

abstract class ArtifactNavigator extends AbstractVerticle {

    public static final String CAN_NAVIGATE_TO_ARTIFACT = "CanNavigateToArtifact"
    public static final String NAVIGATE_TO_ARTIFACT = "NavigateToArtifact"

}
