plugins {
    id("org.jetbrains.kotlin.jvm")
}

val vertxVersion: String by project
val projectVersion: String by project
val protocolVersion = project.properties["protocolVersion"] as String? ?: projectVersion

dependencies {
    compileOnly("plus.sourceplus:protocol:$protocolVersion")
    compileOnly("io.vertx:vertx-core:$vertxVersion")
    compileOnly("io.vertx:vertx-lang-kotlin-coroutines:$vertxVersion")
}
