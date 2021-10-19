package com.sourceplusplus.protocol.artifact.trace

import com.google.common.io.Resources
import com.sourceplusplus.protocol.artifact.exception.LiveStackTrace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PythonStackTraceTest {

    @Test
    fun parsePythonStackTrace() {
        val stackTrace = LiveStackTrace.fromString(
            Resources.toString(Resources.getResource("pythonStackTrace.txt"), Charsets.UTF_8)
        )
        assertNotNull(stackTrace)
        assertEquals(23, stackTrace!!.elements.size)
        assertEquals(16, stackTrace.getElements(true).size)
    }
}
