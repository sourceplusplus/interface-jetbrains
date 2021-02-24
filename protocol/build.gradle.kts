plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "1.4.30"
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
        val jvmMain by getting {
            dependencies {
                val vertxVersion = "4.0.2"
                implementation("io.vertx:vertx-core:$vertxVersion")
                implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.12.1")
                implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.12.1")
                implementation("com.fasterxml.jackson.datatype:jackson-datatype-guava:2.12.1")
                implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.1")
            }
        }
        val jvmTest by getting {
            dependencies {
                val vertxVersion = "4.0.2"
                implementation("io.vertx:vertx-core:$vertxVersion")
                implementation("com.google.guava:guava:30.1-jre")
                implementation("junit:junit:4.13.2")
                implementation(project(":protocol"))
                //todo: shouldn't be 2.10.3
                implementation("com.fasterxml.jackson.core:jackson-core:2.10.3")
                implementation("com.fasterxml.jackson.core:jackson-databind:2.10.3")
                implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.10.3")
                implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.10.3")
                implementation("com.fasterxml.jackson.datatype:jackson-datatype-guava:2.10.3")
                implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.10.3")
                implementation("org.jetbrains.kotlin:kotlin-reflect:1.4.30")
            }
        }
    }
}
