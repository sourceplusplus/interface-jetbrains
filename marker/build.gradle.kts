plugins {
    id("org.jetbrains.kotlin.jvm")
}

val kotlinVersion = ext.get("kotlinVersion")
val pluginGroup: String by project
val projectVersion: String by project
val slf4jVersion: String by project
val joorVersion: String by project
val vertxVersion: String by project

repositories {
    maven(url = "https://www.jetbrains.com/intellij-repository/releases") { name = "intellij-releases" }
    maven(url = "https://cache-redirector.jetbrains.com/intellij-dependencies/") { name = "intellij-dependencies" }
}

dependencies {
    val intellijVersion = "213.7172.25"

    compileOnly("io.vertx:vertx-core:$vertxVersion")
    compileOnly("org.jooq:joor:$joorVersion")
    compileOnly("com.github.sourceplusplus.protocol:protocol:$projectVersion")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinVersion")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    compileOnly("com.google.guava:guava:31.1-jre")
    compileOnly("org.jetbrains:annotations:23.0.0")
    compileOnly("org.slf4j:slf4j-api:$slf4jVersion")
    compileOnly("org.jetbrains.intellij.deps.jcef:jcef:97.2.22-g6779618-chromium-97.0.4692.45-api-1.6")
    compileOnly("com.jetbrains.intellij.platform:ide:$intellijVersion")
    compileOnly("com.jetbrains.intellij.platform:ide-impl:$intellijVersion")
    compileOnly("com.jetbrains.intellij.platform:core:$intellijVersion")
    compileOnly("com.jetbrains.intellij.platform:core-impl:$intellijVersion")
    compileOnly("com.jetbrains.intellij.platform:core-ui:$intellijVersion")
    compileOnly("com.jetbrains.intellij.platform:code-style:$intellijVersion")
    compileOnly("com.jetbrains.intellij.platform:lang:$intellijVersion")
    compileOnly("com.jetbrains.intellij.platform:lang-impl:$intellijVersion")
    testImplementation("junit:junit:4.13.2")
}
