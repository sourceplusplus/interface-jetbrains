plugins {
    id("org.jetbrains.kotlin.jvm")
    id("maven-publish")
}

val pluginGroup: String by project
val mapperVersion: String by project
publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = pluginGroup
            artifactId = "mapper"
            version = mapperVersion

            from(components["java"])
        }
    }
}

repositories {
    maven(url = "https://jitpack.io") { name = "jitpack" }
}

dependencies {
    implementation(project(":protocol"))
    implementation("com.github.sh5i:git-stein:v0.5.0")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("org.eclipse.jgit:org.eclipse.jgit:5.10.0.202012080955-r")
    implementation("com.google.guava:guava:30.1-jre")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.slf4j:slf4j-log4j12:1.7.30")
}

tasks {
    test {
        maxParallelForks = Runtime.getRuntime().availableProcessors() / 2
    }
}
