plugins {
    id("org.jetbrains.kotlin.jvm")
}

val kotlinVersion: String by project
val vertxVersion: String by project
val projectVersion: String by project
val slf4jVersion: String by project
val intellijVersion: String by project

repositories {
    maven(url = "https://www.jetbrains.com/intellij-repository/releases") { name = "intellij-releases" }
    maven(url = "https://cache-redirector.jetbrains.com/intellij-dependencies/") { name = "intellij-dependencies" }
}

dependencies {
    if (findProject(":interfaces:jetbrains") != null) {
        compileOnly(project(":interfaces:jetbrains:marker"))
    } else {
        compileOnly(project(":marker"))
    }
    compileOnly("plus.sourceplus:protocol:$projectVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.google.guava:guava:31.1-jre")
    implementation("org.jetbrains:annotations:23.0.0")
    compileOnly("com.jetbrains.intellij.platform:ide:$intellijVersion")
    compileOnly("com.jetbrains.intellij.platform:ide-impl:$intellijVersion")
    compileOnly("com.jetbrains.intellij.platform:core:$intellijVersion")
    compileOnly("com.jetbrains.intellij.platform:core-impl:$intellijVersion")
    compileOnly("com.jetbrains.intellij.platform:core-ui:$intellijVersion")
    compileOnly("com.jetbrains.intellij.platform:code-style:$intellijVersion")
    compileOnly("com.jetbrains.intellij.platform:lang:$intellijVersion")
    compileOnly("com.jetbrains.intellij.platform:lang-impl:$intellijVersion")
    compileOnly("com.jetbrains.intellij.python:python-psi:211.7628.21") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.python:python-community:211.7628.21") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.platform:debugger:$intellijVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.platform:debugger-impl:$intellijVersion") { isTransitive = false }

    compileOnly("io.vertx:vertx-core:$vertxVersion")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}
