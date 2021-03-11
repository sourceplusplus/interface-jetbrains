plugins {
    id("org.jetbrains.kotlin.jvm")
    id("maven-publish")
}

val pluginGroup: String by project
val markerVersion: String by project
publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = pluginGroup
            artifactId = "marker"
            version = markerVersion

            from(components["java"])
        }
    }
}

repositories {
    maven(url = "https://www.jetbrains.com/intellij-repository/releases") { name = "intellij-releases" }
    maven(url = "https://jetbrains.bintray.com/intellij-third-party-dependencies/") { name = "intellij-dependencies" }
}

dependencies {
    val intellijVersion = "202.7660.26"
    val kotlinVersion = "1.4.31"

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.google.guava:guava:30.1-jre")
    implementation("org.jetbrains:annotations:20.1.0")
    compileOnly("org.slf4j:slf4j-api:1.7.30")
    compileOnly("org.jetbrains.intellij.deps.jcef:jcef:87.1.13-g481a82a-chromium-87.0.4280.141-api-1.2")
    compileOnly("com.jetbrains.intellij.platform:util-ui:$intellijVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.platform:analysis:$intellijVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.java:java-analysis:$intellijVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.platform:ide:$intellijVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.platform:ide-impl:$intellijVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.platform:core:$intellijVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.platform:core-ui:$intellijVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.platform:editor:$intellijVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.platform:lang:$intellijVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.platform:lang-impl:$intellijVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.platform:project-model:$intellijVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.groovy:groovy-psi:$intellijVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.platform:uast:$intellijVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.java:java-indexing:$intellijVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.java:java-indexing-impl:$intellijVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.platform:util:$intellijVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.java:java-psi:$intellijVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.platform:extensions:$intellijVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.platform:util-rt:$intellijVersion") { isTransitive = false }
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion") { isTransitive = false }
    compileOnly("org.jetbrains.kotlin:kotlin-compiler:$kotlinVersion") { isTransitive = false }
    testImplementation("junit:junit:4.13.2")
}
