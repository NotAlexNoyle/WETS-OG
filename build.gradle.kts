import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.23"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("eclipse")
}

group = "net.trueog.wets-og" // Declare bundle identifier.
version = "1.1"

val apiVersion = "1.19"

tasks.named<ProcessResources>("processResources") {
    val props = mapOf(
        "version" to version,
        "apiVersion" to apiVersion,
    )

    filesMatching("plugin.yml") {
        expand(props)
    }
}

repositories {

    mavenCentral()

    maven {
        url = uri("https://repo.purpurmc.org/snapshots")
    }
	maven {
		url = uri("https://maven.enginehub.org/repo") // Get WorldEdit API from EngineHub Repository.
	}

}

dependencies {
    compileOnly("org.purpurmc.purpur:purpur-api:1.19.4-R0.1-SNAPSHOT")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("com.sk89q.worldedit:worldedit-bukkit:7.3.0-SNAPSHOT") 
}

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.shadowJar {
    minimize()
}

tasks.shadowJar.configure {
    archiveClassifier.set("")
    from("LICENSE") { 
        into("/")
    } 
}

tasks.jar {
    dependsOn("shadowJar")
}

tasks.jar.configure {
    archiveClassifier.set("part")
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-parameters")
    options.encoding = "UTF-8" 
    options.forkOptions.executable = File(options.forkOptions.javaHome, "bin/javac").path
}

kotlin {
    jvmToolchain(17)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
        vendor = JvmVendorSpec.GRAAL_VM
    }
}
