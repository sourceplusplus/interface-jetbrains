plugins {
    id("org.jetbrains.kotlin.jvm")
}

val vertxVersion: String by project
val apolloVersion: String by project
val projectVersion: String by project

dependencies {
    compileOnly(projectDependency(":common"))
    compileOnly(projectDependency(":marker"))
    compileOnly("plus.sourceplus:protocol:$projectVersion")
    compileOnly("io.vertx:vertx-core:$vertxVersion")
    compileOnly("io.vertx:vertx-lang-kotlin-coroutines:$vertxVersion")
    compileOnly("com.apollographql.apollo3:apollo-api:$apolloVersion")
}

fun projectDependency(name: String): ProjectDependency {
    throw IllegalStateException("root project: " + rootProject.name)
    return if (rootProject.name != "jetbrains") {
        DependencyHandlerScope.of(rootProject.dependencies).project(":interfaces:jetbrains$name")
    } else {
        DependencyHandlerScope.of(rootProject.dependencies).project(name)
    }
}
