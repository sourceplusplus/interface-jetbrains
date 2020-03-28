package com.sourceplusplus.plugin.source.model

import groovy.transform.Immutable
import groovy.transform.TupleConstructor

/**
 * todo: description
 *
 * @version 0.2.3
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Immutable
@TupleConstructor
class SourceMethodAnnotation {
    final String qualifiedName
    final Map<String, Object> attributeMap
}
