package com.sourceplusplus.marker.source.mark.api.event

/**
 * todo: description.
 *
 * @since 0.0.1
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
enum class SourceMarkEventCode(private val code: Int) : IEventCode {
    MARK_ADDED(1000),
    MARK_REMOVED(1001),
    NAME_CHANGED(1002);

    override fun code(): Int {
        return this.code
    }
}
