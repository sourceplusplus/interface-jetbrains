plugins {
    id("org.jetbrains.kotlin.jvm")
}

val vertxVersion: String by project
val projectVersion: String by project
val protocolVersion = project.properties["protocolVersion"] as String? ?: projectVersion

intellij {
    type.set("IU")
}

dependencies {
    implementation(projectDependency(":common"))
    implementation(projectDependency(":marker"))
    implementation("plus.sourceplus:protocol:$protocolVersion")

    compileOnly("io.vertx:vertx-core:$vertxVersion")
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
