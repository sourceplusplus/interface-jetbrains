import org.jetbrains.changelog.markdownToHTML
import java.net.URL

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij") version "1.5.3"
    id("org.jetbrains.changelog") version "1.3.1"
    id("maven-publish")
}

val joorVersion: String by project
val jacksonVersion: String by project
val vertxVersion: String by project
val kotlinVersion = ext.get("kotlinVersion")
val projectVersion: String by project

// Import variables from gradle.properties file
val pluginGroup: String by project
val pluginName: String by project
val pluginSinceBuild: String by project
val pluginVerifierIdeVersions: String by project

val platformType: String by project
val ideVersion: String by project
val platformPlugins: String by project
val platformDownloadSources: String by project

group = pluginGroup
version = projectVersion

intellij {
    pluginName.set("interface-jetbrains")
    version.set(ideVersion)
    type.set(platformType)
    downloadSources.set(platformDownloadSources.toBoolean())
    updateSinceUntilBuild.set(false)

    plugins.set(platformPlugins.split(',').map(String::trim).filter(String::isNotEmpty).toMutableList())
    //plugins.add("com.intellij.zh:202.413") //test chinese locale
}
tasks.getByName("buildSearchableOptions").onlyIf { false } //todo: figure out how to remove
tasks.getByName<JavaExec>("runIde") {
    //systemProperty("sourcemarker.debug.unblocked_threads", true)
    systemProperty("ide.enable.slow.operations.in.edt", false)
    systemProperty("ide.browser.jcef.contextMenu.devTools.enabled", true)
}

changelog {
    version.set(projectVersion)
}

dependencies {
    if (findProject(":interfaces:jetbrains") != null) {
        implementation(project(":interfaces:jetbrains:commander"))
        implementation(project(":interfaces:jetbrains:commander:kotlin-compiler-wrapper"))
        implementation(project(":interfaces:jetbrains:marker"))
        implementation(project(":interfaces:jetbrains:marker:jvm-marker"))
        implementation(project(":interfaces:jetbrains:marker:py-marker"))
        implementation(project(":interfaces:jetbrains:monitor"))
        implementation(project(":interfaces:booster-ui"))
        implementation(project(":protocol"))
    } else {
        implementation(project(":commander"))
        implementation(project(":commander:kotlin-compiler-wrapper"))
        implementation(project(":marker"))
        implementation(project(":marker:jvm-marker"))
        implementation(project(":marker:py-marker"))
        implementation(project(":monitor"))
        implementation("com.github.sourceplusplus:interface-booster-ui:cc4bb6dce0")
        implementation("com.github.sourceplusplus.protocol:protocol:$projectVersion")
    }

    implementation("org.jooq:joor:$joorVersion")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:$jacksonVersion")
    implementation("org.apache.commons:commons-text:1.9")
    implementation("com.github.sh5i:git-stein:v0.5.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinVersion")
    implementation("io.vertx:vertx-core:$vertxVersion")
    implementation("io.vertx:vertx-lang-kotlin:$vertxVersion")
    implementation("io.vertx:vertx-lang-kotlin-coroutines:$vertxVersion")
    implementation("io.vertx:vertx-web:$vertxVersion")
    implementation("io.vertx:vertx-service-discovery:$vertxVersion")
    implementation("io.vertx:vertx-service-proxy:$vertxVersion")
    implementation("io.vertx:vertx-tcp-eventbus-bridge:$vertxVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.13.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.13.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-guava:2.13.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.2")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation("io.dropwizard.metrics:metrics-core:4.2.9")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.3.2")
    implementation("org.jooq:jooq:3.16.6")
    implementation("org.eclipse.mylyn.github:org.eclipse.egit.github.core:2.1.5")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("eu.geekplace.javapinning:java-pinning-core:1.2.0")
    implementation("ch.qos.logback:logback-core:1.2.11")
    implementation("ch.qos.logback:logback-classic:1.2.11")
    implementation("info.debatty:java-string-similarity:2.0.0")

    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.8.2")
}

tasks {
    patchPluginXml {
        version.set(projectVersion)
        sinceBuild.set(pluginSinceBuild)

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        pluginDescription.set(
            File(file(projectDir).parent, "./README.md").readText().lines().run {
                val start = "<!-- Plugin description -->"
                val end = "<!-- Plugin description end -->"

                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md file:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end))
            }.joinToString("\n").run { markdownToHTML(this) }
        )

        // Get the latest available change notes from the changelog file
        changeNotes.set(project.provider {
            val pluginChangesHeader = "### [JetBrains Plugin](https://github.com/sourceplusplus/interface-jetbrains)\n"
            val fullChangelog = URL("https://raw.githubusercontent.com/sourceplusplus/documentation/master/docs/changelog/README.md")
                .readText()
            if (fullChangelog.contains(pluginChangesHeader)) {
                val changelog = fullChangelog.substringAfter(pluginChangesHeader)
                    .substringBefore("\n### ").substringBefore("\n## ")
                    .trim()
                markdownToHTML(changelog)
            } else {
                markdownToHTML("")
            }
        })
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
        channels.set(listOf(projectVersion.split('-').getOrElse(1) { "default" }.split('.').first()))
    }
    runPluginVerifier {
        ideVersions.set(pluginVerifierIdeVersions.split(",").map { it.trim() })
    }

    prepareSandbox {
        dependsOn("getKotlinCompilerWrapper")
    }

    register<Copy>("getKotlinCompilerWrapper") {
        dependsOn(":commander:kotlin-compiler-wrapper:installDist")
        val wrapperBuildDir = if (findProject(":interfaces:jetbrains") != null) {
            project(":interfaces:jetbrains:commander:kotlin-compiler-wrapper").buildDir
        } else {
            project(":commander:kotlin-compiler-wrapper").buildDir
        }
        from(File(wrapperBuildDir, "install/kotlin-compiler-wrapper/lib"))
        into(File(buildDir, "idea-sandbox/plugins/interface-jetbrains/kotlin-compiler"))
    }

    register("getPluginChangelog") {
        doFirst {
            val pluginChangesHeader = "### [JetBrains Plugin](https://github.com/sourceplusplus/interface-jetbrains)\n"
            val fullChangelog = URL("https://raw.githubusercontent.com/sourceplusplus/documentation/master/docs/changelog/README.md")
                .readText()
            if (fullChangelog.contains(pluginChangesHeader)) {
                val changelog = fullChangelog.substringAfter(pluginChangesHeader)
                    .substringBefore("\n### ").substringBefore("\n## ")
                    .trim()
                println(changelog)
            }
        }
    }

    test {
        useJUnitPlatform()
    }
}
