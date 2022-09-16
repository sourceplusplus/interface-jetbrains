plugins {
    id("org.jetbrains.kotlin.jvm")
}

val vertxVersion: String by project
val projectVersion: String by project

dependencies {
    compileOnly("plus.sourceplus:protocol:$projectVersion")
    compileOnly("io.vertx:vertx-core:$vertxVersion")
    compileOnly("io.vertx:vertx-lang-kotlin-coroutines:$vertxVersion")
}
