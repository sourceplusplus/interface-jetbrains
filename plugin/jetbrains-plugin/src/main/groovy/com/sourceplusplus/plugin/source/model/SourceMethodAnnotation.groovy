package com.sourceplusplus.plugin.source.model

import groovy.transform.Immutable
import groovy.transform.TupleConstructor

/**
 * Turns @ annotations into a single format.
 *
 * @version 0.3.1
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Immutable
@TupleConstructor
class SourceMethodAnnotation {
    final String qualifiedName
    final Map<String, Object> attributeMap
}
