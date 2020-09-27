plugins {
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    val vertxVersion = "3.9.2"
    implementation(kotlin("stdlib-jdk8"))
    implementation(project(":protocol"))
    implementation("org.slf4j:slf4j-api:1.7.30")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.6.10")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")
    implementation("com.apollographql.apollo:apollo-runtime:2.3.0")
    implementation("com.apollographql.apollo:apollo-coroutines-support:2.3.0")
    implementation("io.vertx:vertx-core:$vertxVersion")
    implementation("io.vertx:vertx-web:$vertxVersion")
    implementation("io.vertx:vertx-lang-kotlin:$vertxVersion")
    implementation("io.vertx:vertx-lang-kotlin-coroutines:$vertxVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.11.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.11.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-guava:2.11.1")
    implementation("io.dropwizard.metrics:metrics-core:4.1.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.1.0")
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
    }
    listOf("compileKotlin", "compileTestKotlin").forEach {
        getByName<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>(it) {
            kotlinOptions.jvmTarget = "1.8"
        }
    }

    //todo: should be a way to just add implementation() to dependencies
    getByName("processResources") {
        dependsOn(":portal:frontend:build")
        doLast {
            copy {
                from(file("$rootDir/portal/frontend/build/distributions/frontend.js"))
                into(file("$rootDir/portal/backend/build/resources/main/webroot"))
            }
            copy {
                from(file("$rootDir/portal/frontend/build/distributions/frontend.js.map"))
                into(file("$rootDir/portal/backend/build/resources/main/webroot"))
            }
        }
    }
}
