plugins {
    kotlin("jvm")
}

val vertxVersion: String by project
val projectVersion: String by project
val protocolVersion = project.properties["protocolVersion"] as String? ?: projectVersion

intellij {
    plugins.set(listOf("JavaScript"))
}

dependencies {
    implementation(projectDependency(":common"))
    implementation(projectDependency(":marker"))
    implementation("plus.sourceplus:protocol:$protocolVersion")

    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    compileOnly("com.google.guava:guava:31.1-jre")
    compileOnly("io.vertx:vertx-core:$vertxVersion")

    testRuntimeOnly(projectDependency(":marker:ult-marker"))
    testImplementation("io.vertx:vertx-lang-kotlin-coroutines:$vertxVersion")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

fun projectDependency(name: String): ProjectDependency {
    return if (rootProject.name.contains("jetbrains")) {
        DependencyHandlerScope.of(rootProject.dependencies).project(name)
    } else {
        DependencyHandlerScope.of(rootProject.dependencies).project(":interfaces:jetbrains$name")
    }
}
