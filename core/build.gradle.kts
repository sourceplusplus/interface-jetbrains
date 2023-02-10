plugins {
    id("org.jetbrains.kotlin.jvm")
    id("maven-publish")
}

val vertxVersion: String by project
val apolloVersion: String by project
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
                artifactId = "jetbrains-core"
                version = project.version.toString()

                from(components["kotlin"])

                // Ship the sources jar
                artifact(sourcesJar)
            }
        }
    }
}

dependencies {
    compileOnly(projectDependency(":common"))
    compileOnly(projectDependency(":marker"))
    compileOnly("plus.sourceplus:protocol:$protocolVersion") {
        isTransitive = false
    }
    compileOnly("io.vertx:vertx-core:$vertxVersion")
    compileOnly("io.vertx:vertx-lang-kotlin-coroutines:$vertxVersion") {
        isTransitive = false
    }
    compileOnly("com.apollographql.apollo3:apollo-api:$apolloVersion")
}

fun projectDependency(name: String): ProjectDependency {
    return if (rootProject.name.contains("jetbrains")) {
        DependencyHandlerScope.of(rootProject.dependencies).project(name)
    } else {
        DependencyHandlerScope.of(rootProject.dependencies).project(":interfaces:jetbrains$name")
    }
}
