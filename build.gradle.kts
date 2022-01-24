import org.apache.tools.ant.taskdefs.condition.Os

plugins {
    id("com.avast.gradle.docker-compose")
    id("org.jetbrains.kotlin.jvm") apply false
    id("io.gitlab.arturbosch.detekt")
    id("maven-publish")
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

repositories {
    mavenCentral()
}

subprojects {
    ext {
        set("kotlinVersion", "1.5.0")
    }

    repositories {
        mavenCentral()
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
    register<Exec>("buildExampleWebApp") {
        workingDir = File("./test/e2e/example-web-app")

        if (Os.isFamily(Os.FAMILY_UNIX)) {
            commandLine("./gradlew", "build")
        } else {
            commandLine("cmd", "/c", "gradlew.bat", "build")
        }
    }

    register("assembleUp") {
        dependsOn("buildExampleWebApp", "composeUp")
    }
    getByName("composeUp").shouldRunAfter("buildExampleWebApp")
}

dockerCompose {
    dockerComposeWorkingDirectory.set(File("./test/e2e"))
    useComposeFiles.add("./docker-compose.yml")
    //captureContainersOutput = true
}
