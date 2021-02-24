plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "1.4.30"
    kotlin("kapt")
    id("java")
}

kotlin {
    jvm { }
    js {
        browser { }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.1.1")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.1")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmMain by getting {
            dependencies {
                val vertxVersion = "4.0.2"
                implementation("io.vertx:vertx-core:$vertxVersion")
                implementation("io.vertx:vertx-codegen:$vertxVersion")
            }
            kotlin.srcDirs(kotlin.srcDirs, "$buildDir/generated/source/kapt/main")
        }
        val jvmTest by getting {
            dependencies {
                val vertxVersion = "4.0.2"
                implementation("io.vertx:vertx-core:$vertxVersion")
                implementation("com.google.guava:guava:30.1-jre")
                implementation("junit:junit:4.13.2")
                implementation(project(":protocol"))
                //todo: shouldn't be 2.10.3
                implementation("com.fasterxml.jackson.core:jackson-core:2.10.3")
                implementation("com.fasterxml.jackson.core:jackson-databind:2.10.3")
                implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.10.3")
                implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.10.3")
                implementation("com.fasterxml.jackson.datatype:jackson-datatype-guava:2.10.3")
                implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.10.3")
                implementation("org.jetbrains.kotlin:kotlin-reflect:1.4.30")
            }
        }
    }
}

dependencies {
    "kapt"("io.vertx:vertx-codegen:4.0.2:processor")
}

tasks {
    configure<SourceSetContainer> {
        named("main") {
            java.srcDir("$buildDir/generated/source/kapt/main")

            dependencies {
                val vertxVersion = "4.0.2"
                implementation("io.vertx:vertx-core:$vertxVersion")
                implementation("io.vertx:vertx-codegen:$vertxVersion")
                compileOnly(project(":protocol"))
            }
        }
    }

    register("makeExternalJar") {
        dependsOn("mergeJars")
        doFirst {
            file("$buildDir/libs/protocol-jvm.jar").delete()
            file("$buildDir/libs/protocol-jvm-final.jar")
                .renameTo(file("$buildDir/libs/protocol-jvm.jar"))
        }
    }
    register<Jar>("mergeJars") {
        dependsOn(":protocol:doRename", ":protocol:doUpdate", ":protocol:jar")
        from(zipTree("$buildDir/libs/protocol.jar"))
        from(zipTree("$buildDir/libs/protocol-jvm.jar"))
        archiveBaseName.set("protocol-jvm-final")
    }
    register("doRename") {
        doFirst {
            file("$projectDir/src/commonMain")
                .renameTo(file("$projectDir/src/jvmMain"))
        }
    }
    register("doUpdate") {
        mustRunAfter(":protocol:doRename")
        doLast {
            File("${projectDir}/src/jvmMain/kotlin/com/sourceplusplus/protocol").walkTopDown()
                .forEach {
                    if (it.isDirectory) return@forEach
                    if (it.readText().contains("enum class")) return@forEach
                    if (!it.readText().contains("@Serializable\n")) return@forEach

                    val filename = it.nameWithoutExtension
                    var source = it.readText()
                        .replace(
                            "val sourceAsFilename: String?\n",
                            "var sourceAsFilename: String? = null\n        set\n"
                        )
                        .replace(
                            "val sourceAsLineNumber: Int?\n",
                            "var sourceAsLineNumber: Int? = null\n        set\n"
                        )
                        .replace(
                            "@Serializable\n",
                            "@io.vertx.codegen.annotations.DataObject(generateConverter = true) @Serializable\n"
                        ).replace("    val ", "    var ")
                    if (filename == "ArtifactQualifiedName") {
                        source = source
                            .replace("var identifier: String", "var identifier: String? = null")
                            .replace("var commitId: String", "var commitId: String? = null")
                            .replace("var type: ArtifactType", "var type: ArtifactType? = null")
                        source = source.substring(0, source.length - 2) +
                                "\n    constructor(jsonObject: io.vertx.core.json.JsonObject) : this() {\n" +
                                "        ${filename}Converter.fromJson(jsonObject, this)\n" +
                                "    }\n\n" +
                                "    fun toJson(): io.vertx.core.json.JsonObject {\n" +
                                "        val json = io.vertx.core.json.JsonObject()\n" +
                                "        ${filename}Converter.toJson(this, json)\n" +
                                "        return json\n" +
                                "    }\n" +
                                "}"
                    }
//                    else {
//                        source += "{\n    constructor(jsonObject: io.vertx.core.json.JsonObject) : this() {\n" +
//                                "        ${filename}Converter.fromJson(jsonObject, this)\n" +
//                                "    }\n\n" +
//                                "    fun toJson(): io.vertx.core.json.JsonObject {\n" +
//                                "        val json = io.vertx.core.json.JsonObject()\n" +
//                                "        ${filename}Converter.toJson(this, json)\n" +
//                                "        return json\n" +
//                                "    }\n" +
//                                "}"
//                    }
                    it.writeText(source)
                }
        }
    }
    getByName("jar").mustRunAfter("doUpdate")
}
//./gradlew :protocol:build && ./gradlew :protocol:makeExternalJar && rm -rf protocol/src/jvmMain && git checkout -- protocol/src
