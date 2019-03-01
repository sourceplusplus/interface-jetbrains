package com.sourceplusplus.plugin.source.model

import groovy.transform.Immutable
import groovy.transform.TupleConstructor

@Immutable
@TupleConstructor
class SourceMethodAnnotation {
    final String qualifiedName
    final Map<String, Object> attributeMap
}
