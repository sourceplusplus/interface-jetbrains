package com.sourceplusplus.sourcemarker.psi.endpoint

import com.intellij.openapi.application.ApplicationManager
import com.sourceplusplus.sourcemarker.psi.EndpointDetector
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.runBlocking
import org.intellij.lang.annotations.Language
import org.jetbrains.uast.UFile
import org.jetbrains.uast.toUElement
import org.junit.jupiter.api.Test

class JavaEndpointDetectorTest : EndpointDetectorTest() {

    @Test
    fun `SpringMVC RequestMapping method`() {
        @Language("Java") val code = """
                    import org.springframework.web.bind.annotation.RequestMapping;
                    public class TestController {
                        @RequestMapping(value = "/doGet", method = RequestMethod.GET)
                        public void doGet() {}
                    }
                """.trimIndent()
        val uFile = myFixture.configureByText("TestController.java", code).toUElement() as UFile

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, uFile.classes.size)
            assertEquals(1, uFile.classes[0].methods.size)

            runBlocking {
                val result = EndpointDetector().determineEndpointName(uFile.classes[0].methods[0]).await()
                assertTrue(result.isPresent)
                assertEquals("{GET}/doGet", result.get().name)
            }
        }
    }

    @Test
    fun `SpringMVC RequestMapping method with static import`() {
        @Language("Java") val code = """
                    import org.springframework.web.bind.annotation.RequestMapping;
                    import static org.springframework.web.bind.annotation.RequestMethod.*;
                    public class TestController {
                        @RequestMapping(method = GET, value = "/doGet")
                        public void doGet() {}
                    }
                """.trimIndent()
        val uFile = myFixture.configureByText("TestController.java", code).toUElement() as UFile

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, uFile.classes.size)
            assertEquals(1, uFile.classes[0].methods.size)

            runBlocking {
                val result = EndpointDetector().determineEndpointName(uFile.classes[0].methods[0]).await()
                assertTrue(result.isPresent)
                assertEquals("{GET}/doGet", result.get().name)
            }
        }
    }

    @Test
    fun `SpringMVC RequestMapping method with no value`() {
        @Language("Java") val code = """
                    import org.springframework.web.bind.annotation.RequestMapping;
                    import static org.springframework.web.bind.annotation.RequestMethod.*;
                    public class TestController {
                        @RequestMapping(method = GET)
                        public void doGet() {}
                    }
                """.trimIndent()
        val uFile = myFixture.configureByText("TestController.java", code).toUElement() as UFile

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, uFile.classes.size)
            assertEquals(1, uFile.classes[0].methods.size)

            runBlocking {
                val result = EndpointDetector().determineEndpointName(uFile.classes[0].methods[0]).await()
                assertTrue(result.isPresent)
                assertEquals("{GET}", result.get().name)
            }
        }
    }

    @Test
    fun `SpringMVC RequestMapping method with class request mapping`() {
        @Language("Java") val code = """
                    import org.springframework.web.bind.annotation.RequestMapping;
                    import static org.springframework.web.bind.annotation.RequestMethod.*;
                    @RequestMapping("/todos")
                    public class TestController {
                        @RequestMapping(method = GET)
                        public void doGet() {}
                    }
                """.trimIndent()
        val uFile = myFixture.configureByText("TestController.java", code).toUElement() as UFile

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, uFile.classes.size)
            assertEquals(1, uFile.classes[0].methods.size)

            runBlocking {
                val result = EndpointDetector().determineEndpointName(uFile.classes[0].methods[0]).await()
                assertTrue(result.isPresent)
                assertEquals("{GET}/todos", result.get().name)
            }
        }
    }

    @Test
    fun `SpringMVC RequestMapping method with class request mapping 2`() {
        @Language("Java") val code = """
                    import org.springframework.web.bind.annotation.RequestMapping;
                    import static org.springframework.web.bind.annotation.RequestMethod.*;
                    @RequestMapping("/todos/")
                    public class TestController {
                        @RequestMapping(method = GET)
                        public void doGet() {}
                    }
                """.trimIndent()
        val uFile = myFixture.configureByText("TestController.java", code).toUElement() as UFile

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, uFile.classes.size)
            assertEquals(1, uFile.classes[0].methods.size)

            runBlocking {
                val result = EndpointDetector().determineEndpointName(uFile.classes[0].methods[0]).await()
                assertTrue(result.isPresent)
                assertEquals("{GET}/todos", result.get().name)
            }
        }
    }

    @Test
    fun `SpringMVC RequestMapping method with class request mapping 3`() {
        @Language("Java") val code = """
                    import org.springframework.web.bind.annotation.RequestMapping;
                    import static org.springframework.web.bind.annotation.RequestMethod.*;
                    @RequestMapping("/todos")
                    public class TestController {
                        @RequestMapping(method = GET, value = "/doGet")
                        public void doGet() {}
                    }
                """.trimIndent()
        val uFile = myFixture.configureByText("TestController.java", code).toUElement() as UFile

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, uFile.classes.size)
            assertEquals(1, uFile.classes[0].methods.size)

            runBlocking {
                val result = EndpointDetector().determineEndpointName(uFile.classes[0].methods[0]).await()
                assertTrue(result.isPresent)
                assertEquals("{GET}/todos/doGet", result.get().name)
            }
        }
    }

    @Test
    fun `SpringMVC GetMapping method`() {
        @Language("Java") val code = """
                    import org.springframework.web.bind.annotation.GetMapping;
                    public class TestController {
                        @GetMapping(name = "/doGet")
                        public void doGet() {}
                    }
                """.trimIndent()
        val uFile = myFixture.configureByText("TestController.java", code).toUElement() as UFile

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, uFile.classes.size)
            assertEquals(1, uFile.classes[0].methods.size)

            runBlocking {
                val result = EndpointDetector().determineEndpointName(uFile.classes[0].methods[0]).await()
                assertTrue(result.isPresent)
                assertEquals("{GET}/", result.get().name)
            }
        }
    }

    @Test
    fun `SpringMVC GetMapping method_value`() {
        @Language("Java") val code = """
                    import org.springframework.web.bind.annotation.GetMapping;
                    public class TestController {
                        @GetMapping(value = "/doGet")
                        public void doGet() {}
                    }
                """.trimIndent()
        val uFile = myFixture.configureByText("TestController.java", code).toUElement() as UFile

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, uFile.classes.size)
            assertEquals(1, uFile.classes[0].methods.size)

            runBlocking {
                val result = EndpointDetector().determineEndpointName(uFile.classes[0].methods[0]).await()
                assertTrue(result.isPresent)
                assertEquals("{GET}/doGet", result.get().name)
            }
        }
    }

    @Test
    fun `SpringMVC GetMapping method_path`() {
        @Language("Java") val code = """
                    import org.springframework.web.bind.annotation.GetMapping;
                    public class TestController {
                        @GetMapping(path = "/doGet")
                        public void doGet() {}
                    }
                """.trimIndent()
        val uFile = myFixture.configureByText("TestController.java", code).toUElement() as UFile

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, uFile.classes.size)
            assertEquals(1, uFile.classes[0].methods.size)

            runBlocking {
                val result = EndpointDetector().determineEndpointName(uFile.classes[0].methods[0]).await()
                assertTrue(result.isPresent)
                assertEquals("{GET}/doGet", result.get().name)
            }
        }
    }

    @Test
    fun `SkyWalking Trace with operation name`() {
        @Language("Java") val code = """
                    import org.apache.skywalking.apm.toolkit.trace.Trace;
                    public class TestController {
                        @Trace(operationName = "doGet")
                        public void doGet() {}
                    }
                """.trimIndent()
        val uFile = myFixture.configureByText("TestController.java", code).toUElement() as UFile

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, uFile.classes.size)
            assertEquals(1, uFile.classes[0].methods.size)

            runBlocking {
                val result = EndpointDetector().determineEndpointName(uFile.classes[0].methods[0]).await()
                assertTrue(result.isPresent)
                assertEquals("doGet", result.get().name)
            }
        }
    }

    @Test
    fun `SkyWalking Trace no operation name`() {
        @Language("Java") val code = """
                    import org.apache.skywalking.apm.toolkit.trace.Trace;
                    public class TestController {
                        @Trace
                        public void doGet() {}
                    }
                """.trimIndent()
        val uFile = myFixture.configureByText("TestController.java", code).toUElement() as UFile

        ApplicationManager.getApplication().runReadAction {
            assertEquals(1, uFile.classes.size)
            assertEquals(1, uFile.classes[0].methods.size)

            runBlocking {
                val result = EndpointDetector().determineEndpointName(uFile.classes[0].methods[0]).await()
                assertTrue(result.isPresent)
                assertEquals("TestController.doGet", result.get().name)
            }
        }
    }
}
