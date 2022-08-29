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
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    compileOnly("com.google.guava:guava:31.1-jre")
    testImplementation("junit:junit:4.13.2")
}

fun projectDependency(name: String): ProjectDependency {
    return if (rootProject.name.contains("jetbrains")) {
        DependencyHandlerScope.of(rootProject.dependencies).project(name)
    } else {
        DependencyHandlerScope.of(rootProject.dependencies).project(":interfaces:jetbrains$name")
    }
}
