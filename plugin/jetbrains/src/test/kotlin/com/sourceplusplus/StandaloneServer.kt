package com.sourceplusplus

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.LanguageLevelModuleExtension
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.pom.java.LanguageLevel
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import com.sourceplusplus.marker.SourceMarker
import com.sourceplusplus.portal.SourcePortal
import com.sourceplusplus.protocol.portal.PortalConfiguration
import com.sourceplusplus.sourcemarker.SourceMarkerPlugin
import io.vertx.core.Promise
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.cli.common.toBooleanLenient
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File

@RunWith(JUnit4::class)
class StandaloneServer : LightJavaCodeInsightFixtureTestCase() {

    @Before
    public override fun setUp() {
        assumeTrue((System.getenv("STANDALONE_ENABLED")?.toBooleanLenient() ?: false))
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
        assumeTrue((System.getenv("STANDALONE_ENABLED")?.toBooleanLenient() ?: false))
        val className = "spp.example.webapp.controller.WebappController"
        val artifactName = "$className.getUser(long)"
        val portalUuid = "5471535f-2a5f-4ed2-bfaf-65345c59fd7b"
        println(
            "Portal UUID: " + SourcePortal.register(
                portalUuid,
                "null",
                artifactName,
                PortalConfiguration(external = true)
            )
        )

        SourceMarkerPlugin.vertx.sharedData().getLocalMap<String, Int>("portal")["bridge.port"] = 8888
        runBlocking {
            SourceMarkerPlugin.init(project)
        }

        invokeLater {
            runReadAction {
                val testFile = JavaPsiFacade.getInstance(project).findClass(
                    className,
                    GlobalSearchScope.allScope(project)
                )!!.containingFile
                val fileMarker = SourceMarker.getSourceFileMarker(testFile)
                assertNotNull(fileMarker)

                myFixture.testHighlighting(
                    false,
                    false,
                    false,
                    "spp/example/webapp/controller/WebappController.java"
                )
            }
        }

        runBlocking { Promise.promise<Nothing>().future().await() }
    }
}
