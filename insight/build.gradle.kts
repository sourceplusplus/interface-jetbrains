plugins {
    kotlin("jvm")
    id("maven-publish")
}

val vertxVersion: String by project
val joorVersion: String by project
val jupiterVersion: String by project
val guavaVersion: String by project
val projectVersion: String by project
val protocolVersion = project.properties["protocolVersion"] as String? ?: projectVersion

group = "plus.sourceplus.interface"
version = project.properties["projectVersion"] as String? ?: projectVersion

intellij {
    plugins.set(listOf("java", "Groovy", "Kotlin", "org.intellij.scala:2023.1.7", "JavaScript", "Pythonid:231.6890.12", "intellij.grid.impl"))
}

val sourcesJar = tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(project.the<SourceSetContainer>()["main"].allSource)
}

configure<PublishingExtension> {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/sourceplusplus/interface-jetbrains")
            credentials {
                username = System.getenv("GH_PUBLISH_USERNAME")?.toString()
                password = System.getenv("GH_PUBLISH_TOKEN")?.toString()
            }
        }
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                groupId = project.group.toString()
                artifactId = "jetbrains-insight"
                version = project.version.toString()

                from(components["kotlin"])

                // Ship the sources jar
                artifact(sourcesJar)
            }
        }
    }
}

dependencies {
    implementation(projectDependency(":common"))
    implementation(projectDependency(":marker"))
    implementation("plus.sourceplus:protocol:$protocolVersion")

    compileOnly("org.jooq:joor:$joorVersion")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    compileOnly("com.google.guava:guava:$guavaVersion")
    compileOnly("org.jetbrains:annotations:24.0.1")
    compileOnly("io.vertx:vertx-core:$vertxVersion")
    compileOnly("io.vertx:vertx-lang-kotlin:$vertxVersion")
    compileOnly("io.vertx:vertx-lang-kotlin-coroutines:$vertxVersion")
    compileOnly("org.apache.commons:commons-lang3:3.12.0")

    testImplementation(projectDependency(":marker:js-marker"))
    testImplementation(projectDependency(":marker:jvm-marker"))
    testImplementation(projectDependency(":marker:py-marker"))
    testImplementation(projectDependency(":marker:ult-marker"))
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
