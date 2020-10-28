package com.sourceplusplus.portal.model

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
enum class EndpointTableType(override val description: String) : TableType {
    NAME("Name"),
    TYPE("Type"),
    AVG_THROUGHPUT("Throughput"),
    AVG_RESPONSE_TIME("Response"),
    AVG_SLA("SLA");

    override val isCentered: Boolean = false
}
