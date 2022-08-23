plugins {
    id("org.jetbrains.kotlin.jvm")
}

val kotlinVersion: String by project
val vertxVersion: String by project
val projectVersion: String by project
val slf4jVersion: String by project
val joorVersion: String by project
val intellijVersion: String by project

repositories {
    maven(url = "https://www.jetbrains.com/intellij-repository/releases") { name = "intellij-releases" }
    maven(url = "https://cache-redirector.jetbrains.com/intellij-dependencies/") { name = "intellij-dependencies" }
    maven(url = "https://maven.google.com/") { name = "Google Repository" }
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
    compileOnly("io.vertx:vertx-core:$vertxVersion")
    compileOnly("io.vertx:vertx-lang-kotlin:$vertxVersion")
    compileOnly("io.vertx:vertx-lang-kotlin-coroutines:$vertxVersion")
    compileOnly("org.jooq:joor:$joorVersion")
    compileOnly("org.apache.commons:commons-lang3:3.12.0")
    compileOnly("com.android.tools.external.org-jetbrains:uast:30.2.2")
    compileOnly("com.jetbrains.intellij.platform:ide:$intellijVersion")
    compileOnly("com.jetbrains.intellij.platform:ide-impl:$intellijVersion")
    compileOnly("com.jetbrains.intellij.platform:core:$intellijVersion")
    compileOnly("com.jetbrains.intellij.platform:core-impl:$intellijVersion")
    compileOnly("com.jetbrains.intellij.platform:core-ui:$intellijVersion")
    compileOnly("com.jetbrains.intellij.platform:code-style:$intellijVersion")
    compileOnly("com.jetbrains.intellij.platform:lang:$intellijVersion")
    compileOnly("com.jetbrains.intellij.platform:lang-impl:$intellijVersion")
    compileOnly("com.jetbrains.intellij.groovy:groovy-psi:$intellijVersion")
    compileOnly("com.jetbrains.intellij.platform:util:$intellijVersion")
    compileOnly("com.jetbrains.intellij.java:java-analysis:$intellijVersion")
    compileOnly("com.jetbrains.intellij.java:java-analysis-impl:$intellijVersion")
    compileOnly("com.jetbrains.intellij.java:java-impl:$intellijVersion")
    compileOnly("com.jetbrains.intellij.java:java-psi:$intellijVersion")
    compileOnly("com.jetbrains.intellij.java:java-debugger:$intellijVersion")
    compileOnly("com.jetbrains.intellij.java:java-debugger-impl:$intellijVersion")
    compileOnly("com.jetbrains.intellij.platform:debugger-impl:$intellijVersion")
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    compileOnly("org.jetbrains.kotlin:kotlin-compiler:$kotlinVersion")
}
