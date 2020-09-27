plugins {
    id("org.jetbrains.kotlin.jvm")
}

repositories {
    maven(url = "https://www.jetbrains.com/intellij-repository/releases") { name = "intellij-releases" }
    maven(url = "https://jetbrains.bintray.com/intellij-third-party-dependencies/") { name = "intellij-dependencies" }
}

dependencies {
    val intellijVersion = "202.6948.69"
    val kotlinVersion = "1.4.0"

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.google.guava:guava:29.0-jre")
    implementation("org.jetbrains:annotations:19.0.0")
    compileOnly("org.slf4j:slf4j-api:1.7.30")
    compileOnly("org.jetbrains.intellij.deps.jcef:jcef:77.1.18-g8e8d602-chromium-77.0.3865.120")
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
    testImplementation("junit:junit:4.12")
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
    }
    listOf("compileKotlin", "compileTestKotlin").forEach {
        getByName<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>(it) {
            kotlinOptions.jvmTarget = "1.8"
            kotlinOptions.freeCompilerArgs = listOf("-Xjvm-default=compatibility")
        }
    }
}
