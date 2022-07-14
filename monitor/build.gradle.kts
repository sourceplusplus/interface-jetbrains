plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.apollographql.apollo3")
}

val vertxVersion: String by project
val kotlinVersion: String by project
val apolloVersion: String by project
val projectVersion: String by project
val slf4jVersion: String by project

repositories {
    maven(url = "https://www.jetbrains.com/intellij-repository/releases") { name = "intellij-releases" }
    maven(url = "https://cache-redirector.jetbrains.com/intellij-dependencies/") { name = "intellij-dependencies" }
}

dependencies {
    implementation("plus.sourceplus:protocol:$projectVersion")
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    implementation("com.apollographql.apollo3:apollo-runtime:$apolloVersion")
    api("com.apollographql.apollo3:apollo-api:$apolloVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("io.vertx:vertx-core:$vertxVersion")
    implementation("io.vertx:vertx-lang-kotlin:$vertxVersion")
    implementation("io.vertx:vertx-lang-kotlin-coroutines:$vertxVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
    implementation("io.dropwizard.metrics:metrics-core:4.2.10")
    implementation("eu.geekplace.javapinning:java-pinning-core:1.2.0")

    val intellijVersion = "221.5787.30"
    compileOnly("com.jetbrains.intellij.platform:ide:$intellijVersion")
}

apollo {
    packageNamesFromFilePaths("monitor.skywalking.protocol")
}
