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
    private var finished: Boolean = false
    private var location: SourceLocation? = null
    private var breakpointId: String? = null
    private var hindsightCondition: String? = null

    @Attribute("active")
    fun getActive(): Boolean {
        return active
    }

    @Attribute("active")
    fun setActive(active: Boolean) {
        this.active = active
    }

    @Attribute("finished")
    fun getFinished(): Boolean {
        return finished
    }

    @Attribute("finished")
    fun setFinished(finished: Boolean) {
        this.finished = finished
    }

    @Attribute("location")
    fun getLocation(): SourceLocation? {
        return location
    }

    @Attribute("location")
    fun setLocation(location: SourceLocation) {
        this.location = location
    }

    @Attribute("breakpointId")
    fun getBreakpointId(): String? {
        return breakpointId
    }

    @Attribute("breakpointId")
    fun setBreakpointId(breakpointId: String) {
        this.breakpointId = breakpointId
    }

    @Attribute("hindsightCondition")
    fun getHindsightCondition(): String? {
        return hindsightCondition
    }

    @Attribute("hindsightCondition")
    fun setHindsightCondition(hindsightCondition: String) {
        this.hindsightCondition = hindsightCondition
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
