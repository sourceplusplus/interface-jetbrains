plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "1.4.10"
}

kotlin {
    jvm("jvm8") {
        compilations["main"].kotlinOptions.jvmTarget = "1.8"
    }
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

//        val jsMain by getting {
//            dependencies {
//                implementation(kotlin("stdlib-js"))
//            }
//        }
//
//        val jsTest by getting {
//            dependencies {
//                implementation(kotlin("test-js"))
//            }
//        }
    }
}
