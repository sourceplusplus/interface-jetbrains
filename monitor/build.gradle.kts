plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.apollographql.apollo3")
}

val vertxVersion: String by project
val apolloVersion: String by project
val projectVersion: String by project

dependencies {
    compileOnly(projectDependency(":common"))

    implementation("plus.sourceplus:protocol:$projectVersion")
    implementation("com.apollographql.apollo3:apollo-runtime:$apolloVersion")
    api("com.apollographql.apollo3:apollo-api:$apolloVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("io.vertx:vertx-core:$vertxVersion")
    implementation("io.vertx:vertx-lang-kotlin:$vertxVersion")
    implementation("io.vertx:vertx-lang-kotlin-coroutines:$vertxVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
    implementation("io.dropwizard.metrics:metrics-core:4.2.11")
    implementation("eu.geekplace.javapinning:java-pinning-core:1.2.0")

    compileOnly("com.google.guava:guava:31.1-jre")
}

apollo {
    packageNamesFromFilePaths("monitor.skywalking.protocol")
}

fun projectDependency(name: String): ProjectDependency {
    return if (rootProject.name.contains("jetbrains")) {
        DependencyHandlerScope.of(rootProject.dependencies).project(name)
    } else {
        DependencyHandlerScope.of(rootProject.dependencies).project(":interfaces:jetbrains$name")
    }
}
