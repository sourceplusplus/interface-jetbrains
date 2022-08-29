plugins {
    id("org.jetbrains.kotlin.jvm")
}

val vertxVersion: String by project
val apolloVersion: String by project
val projectVersion: String by project
val intellijVersion: String by project

repositories {
    maven(url = "https://www.jetbrains.com/intellij-repository/releases") { name = "intellij-releases" }
    maven(url = "https://cache-redirector.jetbrains.com/intellij-dependencies/") { name = "intellij-dependencies" }
}

dependencies {
    compileOnly(projectDependency(":common"))
    compileOnly(projectDependency(":marker"))
    compileOnly("plus.sourceplus:protocol:$projectVersion")
    compileOnly("io.vertx:vertx-core:$vertxVersion")
    compileOnly("io.vertx:vertx-lang-kotlin-coroutines:$vertxVersion")
    compileOnly("com.apollographql.apollo3:apollo-api:$apolloVersion")

    compileOnly("com.jetbrains.intellij.platform:ide:$intellijVersion")
}

fun projectDependency(name: String): ProjectDependency {
    return if (rootProject.name != "jetbrains") {
        DependencyHandlerScope.of(rootProject.dependencies).project(":interfaces:jetbrains$name")
    } else {
        DependencyHandlerScope.of(rootProject.dependencies).project(name)
    }
}
