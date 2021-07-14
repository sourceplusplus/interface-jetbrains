package com.sourceplusplus.sourcemarker.psi.endpoint

import com.intellij.testFramework.TestApplicationManager
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

abstract class EndpointDetectorTest : LightJavaCodeInsightFixtureTestCase() {

    @BeforeEach
    public override fun setUp() {
        super.setUp()

        myFixture.addClass(
            "package org.springframework.web.bind.annotation;\n" +
                    "\n" +
                    "public enum RequestMethod {\n" +
                    "\tGET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS, TRACE\n" +
                    "}\n"
        )
        myFixture.addClass(
            "package org.springframework.web.bind.annotation;\n" +
                    "\n" +
                    "import java.lang.annotation.Documented;\n" +
                    "import java.lang.annotation.ElementType;\n" +
                    "import java.lang.annotation.Retention;\n" +
                    "import java.lang.annotation.RetentionPolicy;\n" +
                    "import java.lang.annotation.Target;\n" +
                    "\n" +
                    "import org.springframework.core.annotation.AliasFor;\n" +
                    "\n" +
                    "@Target({ElementType.TYPE, ElementType.METHOD})\n" +
                    "@Retention(RetentionPolicy.RUNTIME)\n" +
                    "@Documented\n" +
                    "@Mapping\n" +
                    "public @interface RequestMapping {\n" +
                    "\tString name() default \"\";\n" +
                    "\t@AliasFor(\"path\")\n" +
                    "\tString[] value() default {};\n" +
                    "\t@AliasFor(\"value\")\n" +
                    "\tString[] path() default {};\n" +
                    "\tRequestMethod[] method() default {};\n" +
                    "\tString[] params() default {};\n" +
                    "\tString[] headers() default {};\n" +
                    "\tString[] consumes() default {};\n" +
                    "\tString[] produces() default {};\n" +
                    "}\n"
        )
        myFixture.addClass(
            "package org.springframework.web.bind.annotation;\n" +
                    "\n" +
                    "import java.lang.annotation.Documented;\n" +
                    "import java.lang.annotation.ElementType;\n" +
                    "import java.lang.annotation.Retention;\n" +
                    "import java.lang.annotation.RetentionPolicy;\n" +
                    "import java.lang.annotation.Target;\n" +
                    "\n" +
                    "import org.springframework.core.annotation.AliasFor;\n" +
                    "\n" +
                    "@Target(ElementType.METHOD)\n" +
                    "@Retention(RetentionPolicy.RUNTIME)\n" +
                    "@Documented\n" +
                    "@RequestMapping(method = RequestMethod.GET)\n" +
                    "public @interface GetMapping {\n" +
                    "\n" +
                    "\t@AliasFor(annotation = RequestMapping.class)\n" +
                    "\tString name() default \"\";\n" +
                    "\t@AliasFor(annotation = RequestMapping.class)\n" +
                    "\tString[] value() default {};\n" +
                    "\t@AliasFor(annotation = RequestMapping.class)\n" +
                    "\tString[] path() default {};\n" +
                    "\t@AliasFor(annotation = RequestMapping.class)\n" +
                    "\tString[] params() default {};\n" +
                    "\t@AliasFor(annotation = RequestMapping.class)\n" +
                    "\tString[] headers() default {};\n" +
                    "\t@AliasFor(annotation = RequestMapping.class)\n" +
                    "\tString[] consumes() default {};\n" +
                    "\t@AliasFor(annotation = RequestMapping.class)\n" +
                    "\tString[] produces() default {};\n" +
                    "}\n"
        )
        myFixture.addClass(
            "package org.apache.skywalking.apm.toolkit.trace;\n" +
                    "\n" +
                    "import java.lang.annotation.ElementType;\n" +
                    "import java.lang.annotation.Retention;\n" +
                    "import java.lang.annotation.RetentionPolicy;\n" +
                    "import java.lang.annotation.Target;\n" +
                    "\n" +
                    "@Target({ElementType.METHOD})\n" +
                    "@Retention(RetentionPolicy.RUNTIME)\n" +
                    "public @interface Trace {\n" +
                    "    String operationName() default \"\";\n" +
                    "}\n"
        )
    }

    @AfterEach
    override fun tearDown() {
        super.tearDown()
        TestApplicationManager.getInstance().setDataProvider(null)
    }
}
