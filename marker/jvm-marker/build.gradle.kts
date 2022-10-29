plugins {
    kotlin("jvm")
}

val vertxVersion: String by project
val projectVersion: String by project
val joorVersion: String by project
val intellijVersion: String by project
val jupiterVersion: String by project
val protocolVersion = project.properties["protocolVersion"] as String? ?: projectVersion

intellij {
    plugins.set(listOf("java", "Groovy", "Kotlin", "org.intellij.scala:2022.2.13"))
}

dependencies {
    implementation(projectDependency(":common"))
    implementation(projectDependency(":marker"))
    implementation("plus.sourceplus:protocol:$protocolVersion")

    implementation("org.jooq:joor:$joorVersion")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    compileOnly("com.google.guava:guava:31.1-jre")
    compileOnly("org.jetbrains:annotations:23.0.0")
    compileOnly("io.vertx:vertx-core:$vertxVersion")
    compileOnly("io.vertx:vertx-lang-kotlin:$vertxVersion")
    compileOnly("io.vertx:vertx-lang-kotlin-coroutines:$vertxVersion")
    compileOnly("org.apache.commons:commons-lang3:3.12.0")

    testRuntimeOnly(projectDependency(":marker:ult-marker"))
    testImplementation("io.vertx:vertx-lang-kotlin-coroutines:$vertxVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:$jupiterVersion")
}

fun projectDependency(name: String): ProjectDependency {
    return if (rootProject.name.contains("jetbrains")) {
        DependencyHandlerScope.of(rootProject.dependencies).project(name)
    } else {
        DependencyHandlerScope.of(rootProject.dependencies).project(":interfaces:jetbrains$name")
    }
}
