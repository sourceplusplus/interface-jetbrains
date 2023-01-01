/*
 * Source++, the continuous feedback platform for developers.
 * Copyright (C) 2022-2023 CodeBrig, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package spp.jetbrains.marker.jvm.detect.endpoint

import com.intellij.testFramework.TestApplicationManager
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase

abstract class AbstractEndpointDetectorTest : LightJavaCodeInsightFixtureTestCase() {

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

    override fun tearDown() {
        super.tearDown()
        TestApplicationManager.getInstance().setDataProvider(null)
    }
}
