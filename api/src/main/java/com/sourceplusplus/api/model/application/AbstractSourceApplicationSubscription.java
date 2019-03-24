package com.sourceplusplus.api.model.application;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sourceplusplus.api.model.SourceMessage;
import com.sourceplusplus.api.model.SourceStyle;
import com.sourceplusplus.api.model.artifact.SourceArtifactSubscriptionType;
import org.immutables.value.Value;

import java.util.Set;

/**
 * todo: description
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.1.2
 * @since 0.1.0
 */
@SourceStyle
@Value.Immutable
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@JsonSerialize(as = SourceApplicationSubscription.class)
@JsonDeserialize(as = SourceApplicationSubscription.class)
public interface AbstractSourceApplicationSubscription extends SourceMessage {

    String artifactQualifiedName();

    int subscribers();

    //double priority();

    Set<SourceArtifactSubscriptionType> types();

    @Value.Default
    default boolean automaticSubscription() {
        return false;
    }
}
