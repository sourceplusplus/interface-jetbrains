plugins {
    id("org.jetbrains.kotlin.jvm")
    id("maven-publish")
}

val pluginGroup: String by project
val pluginVersion: String by project
publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = pluginGroup
            artifactId = "mapper"
            version = pluginVersion

            from(components["java"])
        }
    }
}

repositories {
    maven(url = "https://jitpack.io") { name = "jitpack" }
}

dependencies {
    implementation("com.github.sourceplusplus.protocol:protocol-jvm:0.1.21")
    implementation("com.github.sh5i:git-stein:v0.5.0")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("org.eclipse.jgit:org.eclipse.jgit:5.13.0.202109080827-r")
    implementation("com.google.guava:guava:31.0.1-jre")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.slf4j:slf4j-log4j12:1.7.32")
}

tasks {
    test {
        maxParallelForks = Runtime.getRuntime().availableProcessors() / 2
    }
}
