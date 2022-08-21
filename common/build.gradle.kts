plugins {
    id("org.jetbrains.kotlin.jvm")
}

val vertxVersion: String by project
val kotlinVersion: String by project
val apolloVersion: String by project
val projectVersion: String by project
val slf4jVersion: String by project
val intellijVersion: String by project
val joorVersion: String by project

repositories {
    maven(url = "https://www.jetbrains.com/intellij-repository/releases") { name = "intellij-releases" }
    maven(url = "https://cache-redirector.jetbrains.com/intellij-dependencies/") { name = "intellij-dependencies" }
}

dependencies {
    compileOnly("plus.sourceplus:protocol:$projectVersion")
    compileOnly("io.vertx:vertx-core:$vertxVersion")
    compileOnly("io.vertx:vertx-lang-kotlin-coroutines:$vertxVersion")
    compileOnly("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")

    compileOnly("com.jetbrains.intellij.platform:ide:$intellijVersion")
    compileOnly("com.jetbrains.intellij.platform:ide-impl:$intellijVersion")
}
