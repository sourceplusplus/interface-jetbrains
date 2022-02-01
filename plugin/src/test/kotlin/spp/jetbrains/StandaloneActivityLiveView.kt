/*
 * Source++, the open-source live coding platform.
 * Copyright (C) 2022 CodeBrig, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package spp.jetbrains

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.module.Module
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.LanguageLevelModuleExtension
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.pom.java.LanguageLevel
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import io.vertx.core.Promise
import io.vertx.core.json.Json
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import spp.jetbrains.marker.SourceMarker
import spp.jetbrains.marker.source.mark.api.SourceMark
import spp.jetbrains.portal.SourcePortal
import spp.jetbrains.sourcemarker.SourceMarkerPlugin
import spp.jetbrains.sourcemarker.SourceMarkerPlugin.vertx
import spp.jetbrains.sourcemarker.mark.SourceMarkKeys
import spp.jetbrains.sourcemarker.settings.SourceMarkerConfig
import spp.protocol.ProtocolAddress.Global.RefreshActivity
import spp.protocol.SourceMarkerServices
import spp.protocol.artifact.ArtifactQualifiedName
import spp.protocol.artifact.ArtifactType
import spp.protocol.instrument.LiveSourceLocation
import spp.protocol.portal.PortalConfiguration
import spp.protocol.view.LiveViewConfig
import spp.protocol.view.LiveViewSubscription
import java.io.File
import kotlin.system.exitProcess

class StandaloneActivityLiveView : LightJavaCodeInsightFixtureTestCase() {

    @BeforeEach
    public override fun setUp() {
        assumeTrue((System.getenv("STANDALONE_ENABLED")?.toBoolean() ?: false))
        super.setUp()
    }

    override fun getTestDataPath() = "src/test/testData/spp-example-web-app/src/main/java"

    override fun getProjectDescriptor(): LightProjectDescriptor {
        return object : DefaultLightProjectDescriptor() {
            override fun getSdk(): Sdk = JavaSdk.getInstance().createJdk(
                "jdk-1.8", "/usr/lib/jvm/java-1.8.0-openjdk-amd64/jre"
            )

            override fun configureModule(module: Module, model: ModifiableRootModel, contentEntry: ContentEntry) {
                super.configureModule(module, model, contentEntry)
                model.getModuleExtension(LanguageLevelModuleExtension::class.java).languageLevel = LanguageLevel.JDK_1_8

                myFixture.copyDirectoryToProject("", "")
                val modulePath = testDataPath
                val moduleDir = LocalFileSystem.getInstance().findFileByPath(
                    modulePath.replace(File.separatorChar, '/')
                )!!
                PsiTestUtil.removeAllRoots(module, sdk)
                PsiTestUtil.addContentRoot(module, moduleDir)
                PsiTestUtil.addLibrary(
                    model,
                    "test-lib",
                    "src/test/testData/spp-example-web-app/lib/",
                    "spring-web-5.2.2.RELEASE.jar",
                    "spring-data-commons-2.2.3.RELEASE.jar",
                    "apm-toolkit-trace-8.5.0.jar",
                    "slf4j-api-1.7.29.jar"
                )
            }
        }
    }

    @Test
    fun test() {
        assumeTrue((System.getenv("STANDALONE_ENABLED")?.toBoolean() ?: false))
        val projectSettings = PropertiesComponent.getInstance(project)
        val pluginConfig = SourceMarkerConfig(
            accessToken = "change-me",
            certificatePins = listOf(
                "47:09:B8:64:03:06:5C:1A:25:D5:9B:95:CD:0F:8B:DD:5C:BA:7C:89:48:F0:37:14:E2:21:9D:E1:45:64:11:2C"
            )
        )
        projectSettings.setValue("sourcemarker_plugin_config", Json.encode(pluginConfig))

        val className = "spp.example.webapp.controller.WebappController"
        val artifactName = "$className.getUser(long)"
        val portalUuid = "5471535f-2a5f-4ed2-bfaf-65345c59fd7b"
        println(
            "Portal UUID: " + SourcePortal.register(
                portalUuid,
                ArtifactQualifiedName(artifactName, type = ArtifactType.METHOD),
                PortalConfiguration(external = true)
            )
        )
        val portal = SourcePortal.getPortal(portalUuid)!!

        vertx.sharedData().getLocalMap<String, Int>("portal")["bridge.port"] = 8888
        runBlocking {
            SourceMarkerPlugin.init(project)
        }

        myFixture.testHighlighting(
            false,
            false,
            false,
            "spp/example/webapp/controller/WebappController.java"
        )

        GlobalScope.launch(vertx.dispatcher()) {
            vertx.eventBus().send(RefreshActivity, portal)
            delay(5000)

            val sourceMark = SourceMarker.getSourceMark(
                portal.viewingArtifact, SourceMark.Type.GUTTER
            ) ?: return@launch
            val endpointName = sourceMark.getUserData(
                SourceMarkKeys.ENDPOINT_DETECTOR
            )?.getOrFindEndpointName(sourceMark) ?: return@launch
            SourceMarkerServices.Instance.liveView!!.addLiveViewSubscription(
                LiveViewSubscription(
                    null,
                    listOf(endpointName),
                    sourceMark.artifactQualifiedName,
                    LiveSourceLocation(sourceMark.artifactQualifiedName.identifier, -1),
                    LiveViewConfig("ACTIVITY", listOf("endpoint_cpm", "endpoint_avg", "endpoint_sla"))
                )
            ).onComplete {
                if (it.succeeded()) {
                    println(it)
                } else {
                    it.cause().printStackTrace()
                    exitProcess(-1)
                }
            }
        }

        runBlocking { Promise.promise<Nothing>().future().await() }
    }
}
