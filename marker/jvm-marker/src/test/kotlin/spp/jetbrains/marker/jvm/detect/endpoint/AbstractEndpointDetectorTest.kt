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
                    "public enum RequestMethod {\n" +
                    "\tGET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS, TRACE\n" +
                    "}\n"
        )
        myFixture.addClass(
            "package org.springframework.web.bind.annotation;\n" +
                    "public @interface RequestMapping {\n" +
                    "   String name() default \"\";\n" +
                    "   String[] value() default {};\n" +
                    "   String[] path() default {};\n" +
                    "   RequestMethod[] method() default {};\n" +
                    "   String[] params() default {};\n" +
                    "   String[] headers() default {};\n" +
                    "   String[] consumes() default {};\n" +
                    "   String[] produces() default {};\n" +
                    "}\n"
        )
        myFixture.addClass(
            "package org.springframework.web.bind.annotation;\n" +
                    "public @interface GetMapping {\n" +
                    "   String name() default \"\";\n" +
                    "   String[] value() default {};\n" +
                    "   String[] path() default {};\n" +
                    "   String[] params() default {};\n" +
                    "   String[] headers() default {};\n" +
                    "   String[] consumes() default {};\n" +
                    "   String[] produces() default {};\n" +
                    "}\n"
        )
        myFixture.addClass(
            "package org.apache.skywalking.apm.toolkit.trace;\n" +
                    "public @interface Trace {\n" +
                    "    String operationName() default \"\";\n" +
                    "}\n"
        )
        myFixture.addClass(
            "package io.micronaut.http.annotation;\n" +
                    "public @interface Controller {\n" +
                    "    String value() default \"/\";\n" +
                    "    String[] produces() default \"application/json\";\n" +
                    "    String[] consumes() default \"application/json\";\n" +
                    "    String port() default \"\";\n" +
                    "}\n"
        )
        myFixture.addClass(
            "package io.micronaut.http.annotation;\n" +
                    "public @interface Get {\n" +
                    "    String value() default \"/\";\n" +
                    "    String uri() default \"/\";\n" +
                    "    String[] uris() default {\"/\"};\n" +
                    "    String[] produces() default {};\n" +
                    "    String[] consumes() default {};\n" +
                    "    String[] processes() default {};\n" +
                    "    boolean single() default false;\n" +
                    "    boolean headRoute() default true;\n" +
                    "}\n"
        )
    }

    override fun tearDown() {
        super.tearDown()
        TestApplicationManager.getInstance().setDataProvider(null)
    }
}
