package com.sourceplusplus.sourcemarker.service.hindsight.breakpoint

import com.intellij.util.xml.Attribute
import com.sourceplusplus.protocol.artifact.debugger.SourceLocation
import org.jetbrains.java.debugger.breakpoints.properties.JavaBreakpointProperties

/**
 * todo: description.
 *
 * @since 0.2.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
class HindsightBreakpointProperties : JavaBreakpointProperties<HindsightBreakpointProperties>() {

    private var active: Boolean = false
    private var location: SourceLocation? = null

    @Attribute("active")
    fun getActive(): Boolean {
        return active
    }

    @Attribute("active")
    fun setActive(active: Boolean) {
        this.active = active
    }

    @Attribute("location")
    fun getLocation(): SourceLocation {
        return location!!
    }

    @Attribute("location")
    fun setLocation(location: SourceLocation) {
        this.location = location
    }

    override fun getState(): HindsightBreakpointProperties? {
        return super.getState()
    }

    override fun loadState(state: HindsightBreakpointProperties) {
        super.loadState(state)

        active = state.active
        location = state.location
    }
}
