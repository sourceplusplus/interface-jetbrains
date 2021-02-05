package com.sourceplusplus.portal.model

/**
 * todo: description.
 *
 * @since 0.2.0
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
enum class LogTableType(
    override val isCentered: Boolean
) : TableType {
    OPERATION(false),
    PATTERN(false),
    LEVEL(false),
    OCCURRED(true);

    override val description = name.toLowerCase().capitalize()
}
