plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "1.4.21"
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
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.1.1")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.1")
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
                val vertxVersion = "4.0.0"
                implementation("io.vertx:vertx-core:$vertxVersion")
                implementation("com.google.guava:guava:30.1-jre")
                implementation("junit:junit:4.13.1")
                implementation(project(":protocol"))
                implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.12.1")
                implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.12.1")
                implementation("com.fasterxml.jackson.datatype:jackson-datatype-guava:2.12.1")
                implementation("com.fasterxml.jackson.datatype:jackson-datatype-guava:2.12.1")
                implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.1")
            }
        }
    }
}
