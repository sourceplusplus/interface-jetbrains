plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.apollographql.apollo").version("2.5.9")
}

val vertxVersion = ext.get("vertxVersion")
val kotlinVersion = ext.get("kotlinVersion")

dependencies {
    implementation(project(":protocol"))
    implementation("org.slf4j:slf4j-api:1.7.32")
    implementation("com.apollographql.apollo:apollo-runtime:2.5.9")
    implementation("com.apollographql.apollo:apollo-coroutines-support:2.5.9")
    api("com.apollographql.apollo:apollo-api:2.5.9")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinVersion")
    implementation("io.vertx:vertx-core:$vertxVersion")
    implementation("io.vertx:vertx-lang-kotlin:$vertxVersion")
    implementation("io.vertx:vertx-lang-kotlin-coroutines:$vertxVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.3.0")
    implementation("io.dropwizard.metrics:metrics-core:4.2.3")
    implementation("eu.geekplace.javapinning:java-pinning-core:1.2.0")
}

apollo {
    generateKotlinModels.set(true)
    rootPackageName.set("monitor.skywalking.protocol")
}
