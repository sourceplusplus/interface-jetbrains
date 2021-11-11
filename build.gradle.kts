plugins {
    id("com.avast.gradle.docker-compose")

    id("org.jetbrains.kotlin.jvm") apply false

    id("io.gitlab.arturbosch.detekt")
    id("maven-publish")
}

val pluginGroup: String by project
val pluginName: String by project
val pluginVersion: String by project
val pluginSinceBuild: String by project

val platformType: String by project
val platformDownloadSources: String by project

group = pluginGroup
version = pluginVersion

repositories {
    mavenCentral()
    jcenter()
}

subprojects {
    ext {
        set("vertxVersion", "4.0.3")
        set("kotlinVersion", "1.5.0")
    }

    repositories {
        mavenCentral()
        jcenter()
        maven(url = "https://jitpack.io")
    }

    apply<MavenPublishPlugin>()
    apply<io.gitlab.arturbosch.detekt.DetektPlugin>()
    tasks {
        withType<io.gitlab.arturbosch.detekt.Detekt> {
            parallel = true
            buildUponDefaultConfig = true
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
}

tasks {
    register("downloadSkywalking") {
        doLast {
            println("Downloading Apache SkyWalking")
            val f = File(projectDir, "test/e2e/apache-skywalking-apm-8.8.1.tar.gz")
            if (!f.exists()) {
                java.net.URL("https://downloads.apache.org/skywalking/8.8.1/apache-skywalking-apm-8.8.1.tar.gz")
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
    dockerComposeWorkingDirectory.set(File("./test/e2e"))
    useComposeFiles.add("./docker-compose.yml")
    //captureContainersOutput = true
}
