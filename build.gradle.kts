plugins {
    id("com.avast.gradle.docker-compose") version "0.12.1"

    val kotlinVersion = "1.4.10"
    kotlin("jvm") version kotlinVersion apply false
    kotlin("multiplatform") version kotlinVersion apply false
    kotlin("js") version kotlinVersion apply false

    id("io.gitlab.arturbosch.detekt") version "1.13.1"
}

val pluginGroup: String by project
val pluginName: String by project
val pluginVersion: String by project
val pluginSinceBuild: String by project
val pluginUntilBuild: String by project

val platformType: String by project
val platformVersion: String by project
val platformDownloadSources: String by project

group = pluginGroup
version = pluginVersion

repositories {
    mavenCentral()
    jcenter()
    maven(url = "https://kotlin.bintray.com/kotlinx/")
}

subprojects {
    repositories {
        mavenCentral()
        jcenter()
        maven(url = "https://kotlin.bintray.com/kotlinx/")
    }

    apply<io.gitlab.arturbosch.detekt.DetektPlugin>()
    tasks {
        withType<io.gitlab.arturbosch.detekt.Detekt> {
            parallel = true
            buildUponDefaultConfig = true
        }
    }
}

gradle.buildFinished {
    project.buildDir.deleteRecursively()
}

tasks {
    register("downloadSkywalking") {
        doLast {
            println("Downloading Apache SkyWalking")
            val f = File(projectDir, "test/e2e/apache-skywalking-apm-es7-8.1.0.tar.gz")
            if (!f.exists()) {
                java.net.URL("https://downloads.apache.org/skywalking/8.1.0/apache-skywalking-apm-es7-8.1.0.tar.gz")
                    .openStream().use { input ->
                        java.io.FileOutputStream(f).use { output ->
                            input.copyTo(output)
                        }
                    }
            }
            println("Downloaded Apache SkyWalking")
        }
    }
}

dockerCompose {
    dockerComposeWorkingDirectory = "./test/e2e"
    useComposeFiles = listOf("./docker-compose.yml")
    captureContainersOutput = true
}
