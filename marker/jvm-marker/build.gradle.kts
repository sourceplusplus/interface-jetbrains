plugins {
    id("org.jetbrains.kotlin.jvm")
}

val kotlinVersion: String by project
val vertxVersion: String by project
val projectVersion: String by project
val joorVersion: String by project

repositories {
    maven(url = "https://maven.google.com/") { name = "Google Repository" }
}

dependencies {
    compileOnly(projectDependency(":marker"))
    compileOnly("plus.sourceplus:protocol:$projectVersion")

    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    compileOnly("com.google.guava:guava:31.1-jre")
    compileOnly("org.jetbrains:annotations:23.0.0")
    compileOnly("io.vertx:vertx-core:$vertxVersion")
    compileOnly("io.vertx:vertx-lang-kotlin:$vertxVersion")
    compileOnly("io.vertx:vertx-lang-kotlin-coroutines:$vertxVersion")
    compileOnly("org.jooq:joor:$joorVersion")
    compileOnly("org.apache.commons:commons-lang3:3.12.0")
    compileOnly("com.android.tools.external.org-jetbrains:uast:30.2.2")
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    compileOnly("org.jetbrains.kotlin:kotlin-compiler:$kotlinVersion")
}

fun projectDependency(name: String): ProjectDependency {
    return if (rootProject.name != "jetbrains") {
        DependencyHandlerScope.of(rootProject.dependencies).project(":interfaces:jetbrains$name")
    } else {
        DependencyHandlerScope.of(rootProject.dependencies).project(name)
    }
}
