plugins {
    id("org.jetbrains.kotlin.jvm")
    id("maven-publish")
    kotlin("kapt")
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
    implementation("org.slf4j:slf4j-api:1.7.30")
    implementation("org.slf4j:slf4j-log4j12:1.7.30")
    implementation("org.apache.commons:commons-math3:3.6.1")
    implementation("org.jooq:jooq:3.14.7")

    val vertxVersion = "4.0.2"
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
    implementation("io.vertx:vertx-core:$vertxVersion")
    implementation("io.vertx:vertx-lang-kotlin:$vertxVersion")
    implementation("io.vertx:vertx-lang-kotlin-coroutines:$vertxVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.1.1")
    testImplementation("junit:junit:4.13.2")

    implementation("io.vertx:vertx-codegen:$vertxVersion")
    kapt("io.vertx:vertx-codegen:$vertxVersion:processor")
    annotationProcessor("io.vertx:vertx-service-proxy:$vertxVersion")
}

tasks.register<Copy>("setupJsonMappers") {
    from(file("$projectDir/src/main/resources/META-INF/vertx/json-mappers.properties"))
    into(file("$buildDir/tmp/kapt3/src/main/resources/META-INF/vertx"))
}
tasks.getByName("compileKotlin").dependsOn("setupJsonMappers")