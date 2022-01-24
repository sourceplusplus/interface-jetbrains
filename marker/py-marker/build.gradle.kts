plugins {
    id("org.jetbrains.kotlin.jvm")
}

val kotlinVersion = ext.get("kotlinVersion")
val vertxVersion: String by project
val projectVersion: String by project

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
    compileOnly("com.github.sourceplusplus.protocol:protocol:$projectVersion")
    val intellijVersion = "213.6461.79"

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.google.guava:guava:31.0.1-jre")
    implementation("org.jetbrains:annotations:23.0.0")
    compileOnly("org.slf4j:slf4j-api:1.7.33")
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
