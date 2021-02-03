plugins {
    id("org.jetbrains.kotlin.jvm")
    id("maven-publish")
}

val pluginGroup: String by project
val mentorVersion: String by project
publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = pluginGroup
            artifactId = "mentor"
            version = mentorVersion

            from(components["java"])
        }
    }
}

dependencies {
    implementation(project(":protocol"))
    implementation(project(":monitor:skywalking")) //todo: impl monitor common lib
    implementation("org.slf4j:slf4j-api:1.7.30")
    implementation("org.slf4j:slf4j-log4j12:1.7.30")
    implementation("org.apache.commons:commons-math3:3.6.1")
    implementation("org.jooq:jooq:3.14.6")

    val vertxVersion = "4.0.0"
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
    implementation("io.vertx:vertx-core:$vertxVersion")
    implementation("io.vertx:vertx-lang-kotlin:$vertxVersion")
    implementation("io.vertx:vertx-lang-kotlin-coroutines:$vertxVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.1.1")
    testImplementation("junit:junit:4.13.1")
    testImplementation(project(":monitor:skywalking"))
}

tasks {
    test {
        dependsOn(":downloadSkywalking", ":composeUp")
        rootProject.tasks.findByName("composeUp")!!.mustRunAfter("downloadSkywalking")
        finalizedBy(":composeDown")
    }
}
