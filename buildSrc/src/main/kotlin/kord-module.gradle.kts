import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.jetbrains.dokka")
    id("kotlinx-atomicfu")
    `maven-publish`
}

repositories {
    mavenCentral()
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
}

dependencies {
    testImplementation(kotlin("test"))
    testRuntimeOnly(kotlin("test-junit5"))
}

kotlin {
    explicitApi()

    // allow ExperimentalCoroutinesApi for `runTest {}`
    sourceSets["test"].languageSettings.optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
}

tasks {
    tasks.getByName("apiCheck") {
        onlyIf { Library.isRelease }
    }

    withType<JavaCompile> {
        sourceCompatibility = Jvm.target
        targetCompatibility = Jvm.target
    }

    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = Jvm.target
            allWarningsAsErrors = true
            freeCompilerArgs = listOf(
                CompilerArguments.time,
                CompilerArguments.contracts,

                CompilerArguments.kordPreview,
                CompilerArguments.kordExperimental,
                CompilerArguments.kordVoice,

                CompilerArguments.progressive,
            )
        }
    }

    withType<Test> {
        useJUnitPlatform()
    }

    dokkaHtml.configure {
        this.outputDirectory.set(project.projectDir.resolve("dokka").resolve("kord"))

        dokkaSourceSets {
            configureEach {
                platform.set(org.jetbrains.dokka.Platform.jvm)

                sourceLink {
                    localDirectory.set(file("src/main/kotlin"))
                    remoteUrl.set(uri("https://github.com/kordlib/kord/tree/master/${project.name}/src/main/kotlin/").toURL())

                    remoteLineSuffix.set("#L")
                }

                jdkVersion.set(8)
            }
        }
    }

    val sourcesJar by registering(Jar::class) {
        archiveClassifier.set("sources")
        from(sourceSets.main.get().allSource)
    }

    val dokkaHtml by getting

    val dokkaJar by registering(Jar::class) {
        group = JavaBasePlugin.DOCUMENTATION_GROUP
        description = "Assembles Kotlin docs with Dokka"
        archiveClassifier.set("javadoc")
        from(dokkaHtml)
        dependsOn(dokkaHtml)
    }

    withType<PublishToMavenRepository>().configureEach {
        doFirst { require(!Library.isUndefined) { "No release/snapshot version found." } }
    }

    publishing {
        publications.withType<MavenPublication> {
            from(components["java"])
            artifact(sourcesJar.get())
            artifact(dokkaJar.get())
        }
    }
}
