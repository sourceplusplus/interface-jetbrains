plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "1.4.10"
}

kotlin {
    jvm { }
    js {
        browser { }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.1.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.0")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmTest by getting {
            dependencies {
                val vertxVersion = "3.9.4"
                implementation("io.vertx:vertx-core:$vertxVersion")
                implementation("com.google.guava:guava:29.0-jre")
                implementation("junit:junit:4.13.1")
                implementation(project(":protocol"))
                implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.11.3")
                implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.11.3")
                implementation("com.fasterxml.jackson.datatype:jackson-datatype-guava:2.11.3")
                implementation("com.fasterxml.jackson.datatype:jackson-datatype-guava:2.11.3")
                implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.3")
            }
        }
    }
}
