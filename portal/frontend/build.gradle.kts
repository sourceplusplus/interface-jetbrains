plugins {
    kotlin("js")
}

kotlin {
    js {
        useCommonJs()
        browser()
        binaries.executable()
    }

    sourceSets {
        val main by getting {
            dependencies {
//                implementation(npm("echarts", "4.8.0", generateExternals = true))
//                implementation(npm("fomantic-ui-less", "2.8.6"))
//                implementation(npm("sockjs-client", "1.4.0", generateExternals = true))
//                implementation(npm("vertx3-eventbus-client", "3.9.1", generateExternals = true))
                implementation(npm("jquery", "3.5.1", generateExternals = true))

                implementation(project(":protocol"))
                implementation(kotlin("stdlib-common"))
                implementation(kotlin("stdlib-js"))
                implementation("org.jetbrains.kotlinx:kotlinx-html-js:0.7.2")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.1.0")
                implementation("com.github.hadilq:log4k-js:2.3.1")
                //implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.0-RC2")
            }
        }
    }
}