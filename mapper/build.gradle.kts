plugins {
    id("org.jetbrains.kotlin.jvm")
}

//todo: remove dependency on intellij
repositories {
    maven(url = "https://jitpack.io") { name = "jitpack" }
    maven(url = "https://www.jetbrains.com/intellij-repository/releases") { name = "intellij-releases" }
}

dependencies {
    val intellijVersion = "202.6948.69"

    implementation(project(":protocol"))
    compileOnly(project(":marker")) //todo: only need MarkerUtils
    implementation("com.github.sh5i:git-stein:v0.5.0")
    implementation("org.apache.commons:commons-lang3:3.11")
    implementation("org.eclipse.jgit:org.eclipse.jgit:5.8.0.202006091008-r")
    implementation("com.google.guava:guava:29.0-jre")

    compileOnly("com.jetbrains.intellij.platform:core:$intellijVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.platform:core-impl:$intellijVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.platform:uast:$intellijVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.platform:util:$intellijVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.java:java-psi:$intellijVersion") { isTransitive = false }

    testImplementation("junit:junit:4.13")
}

//todo: should be able to move to root project
tasks {
    withType<JavaCompile> {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
    }
    listOf("compileKotlin", "compileTestKotlin").forEach {
        getByName<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>(it) {
            kotlinOptions.jvmTarget = "1.8"
        }
    }
}
