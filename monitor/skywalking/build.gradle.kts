plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.apollographql.apollo").version("2.5.6")
}

val vertxVersion = ext.get("vertxVersion")

dependencies {
    implementation(project(":protocol"))
    implementation("org.slf4j:slf4j-api:1.7.30")
    implementation("com.apollographql.apollo:apollo-runtime:2.5.6")
    implementation("com.apollographql.apollo:apollo-coroutines-support:2.5.6")
    api("com.apollographql.apollo:apollo-api:2.5.6")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.3-native-mt")
    implementation("io.vertx:vertx-core:$vertxVersion")
    implementation("io.vertx:vertx-lang-kotlin:$vertxVersion")
    implementation("io.vertx:vertx-lang-kotlin-coroutines:$vertxVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.1.1")
    implementation("io.dropwizard.metrics:metrics-core:4.1.19")
    implementation("eu.geekplace.javapinning:java-pinning-core:1.2.0")
}

apollo {
    generateKotlinModels.set(true)
    rootPackageName.set("monitor.skywalking.protocol")
}
