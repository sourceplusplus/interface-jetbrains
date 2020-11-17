plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "1.4.10"
}

kotlin {
    jvm { }
    js {
        useCommonJs()
        browser()
        binaries.executable()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":protocol"))
            }
        }
        val jvmMain by getting {
            dependencies {
                val vertxVersion = "3.9.4"
                implementation(kotlin("stdlib-jdk8"))
                implementation(project(":protocol"))
                implementation("org.slf4j:slf4j-api:1.7.30")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.1.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.1")
                implementation("io.vertx:vertx-core:$vertxVersion")
                implementation("io.vertx:vertx-web:$vertxVersion")
                implementation("io.vertx:vertx-lang-kotlin:$vertxVersion")
                implementation("io.vertx:vertx-lang-kotlin-coroutines:$vertxVersion")
                implementation("com.google.guava:guava:29.0-jre")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.11.3")
                implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.11.3")
                implementation("com.fasterxml.jackson.datatype:jackson-datatype-guava:2.11.3")
            }
        }

        val jsMain by getting {
            dependencies {
                implementation(npm("echarts", "4.9.0", generateExternals = true))
//                implementation(npm("fomantic-ui-less", "2.8.6"))
                implementation(npm("jquery", "3.5.1", generateExternals = true))
                implementation(npm("moment", "2.29.1", generateExternals = true))

                implementation(project(":protocol"))
                implementation(kotlin("stdlib-common"))
                implementation(kotlin("stdlib-js"))
                implementation("org.jetbrains.kotlinx:kotlinx-html-js:0.7.2")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.1.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.0")
                implementation("com.github.bfergerson:kotlin-vertx3-eventbus-bridge:bacec93ae1")
            }
        }
    }
}
