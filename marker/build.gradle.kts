plugins {
    id("org.jetbrains.kotlin.jvm")
}

val projectVersion: String by project
val joorVersion: String by project
val vertxVersion: String by project

dependencies {
    compileOnly(projectDependency(":common"))
    compileOnly("plus.sourceplus:protocol:$projectVersion")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    compileOnly("io.vertx:vertx-core:$vertxVersion")
    compileOnly("io.vertx:vertx-lang-kotlin:$vertxVersion")
    compileOnly("io.vertx:vertx-lang-kotlin-coroutines:$vertxVersion")
    compileOnly("org.jooq:joor:$joorVersion")
    compileOnly("plus.sourceplus:protocol:$projectVersion")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    compileOnly("com.google.guava:guava:31.1-jre")
    compileOnly("org.jetbrains:annotations:23.0.0")
    compileOnly("org.jetbrains.intellij.deps.jcef:jcef:97.2.22-g6779618-chromium-97.0.4692.45-api-1.6")
    testImplementation("junit:junit:4.13.2")
}

fun projectDependency(name: String): ProjectDependency {
    return if (rootProject.name != "jetbrains") {
        DependencyHandlerScope.of(rootProject.dependencies).project(":interfaces:jetbrains$name")
    } else {
        DependencyHandlerScope.of(rootProject.dependencies).project(name)
    }
}
