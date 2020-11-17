package com.sourceplusplus.portal.model

/**
 * todo: description.
 *
 * @since 0.1.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
enum class TraceTableType(
    override val description: String,
    override val isCentered: Boolean
) : TableType {
    OPERATION("Operation", false),
    COMPONENT("", true),
    OCCURRED("Occurred", true),
    EXEC("Exec", true),
    EXEC_PCT("Exec (%)", true)
}
