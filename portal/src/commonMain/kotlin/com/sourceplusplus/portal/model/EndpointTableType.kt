package com.sourceplusplus.portal.model

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
enum class EndpointTableType : TableType {
    NAME,
    TYPE,
    AVG_THROUGHPUT,
    AVG_RESPONSE_TIME,
    AVG_SLA;

    override val isCentered: Boolean = false
    override val description = name
}
