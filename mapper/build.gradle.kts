plugins {
    id("org.jetbrains.kotlin.jvm")
}

repositories {
    maven(url = "https://jitpack.io") { name = "jitpack" }
}

dependencies {
    implementation(project(":protocol"))
    implementation("com.github.sh5i:git-stein:v0.5.0")
    implementation("org.apache.commons:commons-lang3:3.11")
    implementation("org.eclipse.jgit:org.eclipse.jgit:5.9.0.202009080501-r")
    implementation("com.google.guava:guava:29.0-jre")
    testImplementation("junit:junit:4.13.1")
}
