buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("app.cash.licensee:licensee-gradle-plugin:1.5.0")
    }
}

plugins {
    id("com.diffplug.spotless") apply false
    id("org.jetbrains.kotlin.jvm") apply false
    id("io.gitlab.arturbosch.detekt") apply false
}

val pluginGroup: String by project
val pluginName: String by project
val projectVersion: String by project
val pluginSinceBuild: String by project
val vertxVersion: String by project

val platformType: String by project
val platformDownloadSources: String by project

group = pluginGroup
version = projectVersion

subprojects {
    repositories {
        mavenCentral()
        maven(url = "https://jitpack.io")
        maven(url = "https://pkg.sourceplus.plus/sourceplusplus/protocol")
        maven(url = "https://pkg.sourceplus.plus/sourceplusplus/interface-booster-ui")
    }

    apply(plugin = "app.cash.licensee")
    configure<app.cash.licensee.LicenseeExtension> {
        ignoreDependencies("plus.sourceplus", "protocol")
        ignoreDependencies("plus.sourceplus", "protocol-jvm")
        allow("Apache-2.0")
        allow("MIT")
        allow("EPL-1.0")
        allow("LGPL-2.1-only")
        allowUrl("https://raw.githubusercontent.com/apollographql/apollo-kotlin/main/LICENSE") //MIT
        allowUrl("https://www.gnu.org/licenses/old-licenses/lgpl-2.1.en.html") //LGPL 2.1
        allowUrl("https://www.bouncycastle.org/licence.html") //MIT
        allowUrl("https://api.github.com/licenses/apache-2.0") //Apache 2.0
        allowUrl("http://www.jcraft.com/jsch/LICENSE.txt") //BSD-style
        allowUrl("http://www.jcraft.com/jzlib/LICENSE.txt") //BSD-style
        allowUrl("http://jgrapht.org/LGPL.html") //LGPL 2.1
        allowUrl("http://www.eclipse.org/legal/epl-v20.html") //EPL 2.0
        allowDependency("net.jcip", "jcip-annotations", "1.0") {
            because("Creative Commons")
        }
    }

    apply<io.gitlab.arturbosch.detekt.DetektPlugin>()
    val detektPlugins by configurations
    dependencies {
        detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.21.0")
    }

    tasks {
        withType<io.gitlab.arturbosch.detekt.Detekt> {
            parallel = true
            buildUponDefaultConfig = true
            config.setFrom(arrayOf(File(project.rootDir, "detekt.yml")))
        }

        withType<JavaCompile> {
            sourceCompatibility = "1.8"
            targetCompatibility = "1.8"
        }
        withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            kotlinOptions.apiVersion = "1.4"
            kotlinOptions.jvmTarget = "1.8"
            kotlinOptions.freeCompilerArgs +=
                listOf(
                    "-Xno-optimized-callable-references",
                    "-Xjvm-default=compatibility"
                )
        }

        withType<Test> {
            testLogging {
                events("passed", "skipped", "failed")
                setExceptionFormat("full")

                outputs.upToDateWhen { false }
                showStandardStreams = true
            }
        }
    }

    apply(plugin = "com.diffplug.spotless")
    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        kotlin {
            targetExclude("**/generated/**", "**/liveplugin/**")
            if (file("../LICENSE-HEADER.txt").exists()) {
                licenseHeaderFile(file("../LICENSE-HEADER.txt"))
            } else {
                licenseHeaderFile(file("../../LICENSE-HEADER.txt"))
            }
        }
    }
}
