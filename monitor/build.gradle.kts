plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.apollographql.apollo3")
}

val vertxVersion: String by project
val kotlinVersion = ext.get("kotlinVersion")
val apolloVersion: String by project
val projectVersion: String by project
val slf4jVersion: String by project

dependencies {
    implementation("com.github.sourceplusplus.protocol:protocol:a97325afb6")
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    implementation("com.apollographql.apollo3:apollo-runtime:$apolloVersion")
    api("com.apollographql.apollo3:apollo-api:$apolloVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinVersion")
    implementation("io.vertx:vertx-core:$vertxVersion")
    implementation("io.vertx:vertx-lang-kotlin:$vertxVersion")
    implementation("io.vertx:vertx-lang-kotlin-coroutines:$vertxVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.3.2")
    implementation("io.dropwizard.metrics:metrics-core:4.2.7")
    implementation("eu.geekplace.javapinning:java-pinning-core:1.2.0")
}

apollo {
    packageNamesFromFilePaths("monitor.skywalking.protocol")
}
