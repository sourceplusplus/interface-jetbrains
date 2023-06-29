plugins {
    id("org.jetbrains.kotlin.jvm")
    id("maven-publish")
}

val joorVersion: String by project
val vertxVersion: String by project
val guavaVersion: String by project
val projectVersion: String by project
val protocolVersion = project.properties["protocolVersion"] as String? ?: projectVersion

group = "plus.sourceplus.interface"
version = project.properties["projectVersion"] as String? ?: projectVersion

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
                artifactId = "jetbrains-marker"
                version = project.version.toString()

                from(components["kotlin"])

                // Ship the sources jar
                artifact(sourcesJar)
            }
        }
    }
}

dependencies {
    compileOnly(projectDependency(":core"))
    compileOnly("plus.sourceplus:protocol:$protocolVersion")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.2")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    compileOnly("io.vertx:vertx-core:$vertxVersion")
    compileOnly("io.vertx:vertx-lang-kotlin:$vertxVersion")
    compileOnly("io.vertx:vertx-lang-kotlin-coroutines:$vertxVersion")
    compileOnly("org.jooq:joor:$joorVersion")
    compileOnly("com.google.guava:guava:$guavaVersion")
    testImplementation("junit:junit:4.13.2")
}

fun projectDependency(name: String): ProjectDependency {
    return if (rootProject.name.contains("jetbrains")) {
        DependencyHandlerScope.of(rootProject.dependencies).project(name)
    } else {
        DependencyHandlerScope.of(rootProject.dependencies).project(":interfaces:jetbrains$name")
    }
}
