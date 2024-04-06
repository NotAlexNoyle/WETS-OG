plugins {
  id("com.github.johnrengelman.shadow")
}

repositories {
  maven("https://repo.purpurmc.org/snapshots")
  maven("https://maven.enginehub.org/repo/")
}

dependencies {
  // this is all set up the wrong way around but shadowJar is special
  compileOnly("org.purpurmc.purpur:purpur-api:1.19.4-R0.1-SNAPSHOT") 
  implementation("com.sk89q.worldedit:worldedit-bukkit:7.2.14") 
  shadow(project(":wets-common")) { isTransitive = false }
}

tasks {
  shadowJar {
    configurations = listOf(project.configurations["shadow"])
    archiveBaseName.set("WETS")
    archiveClassifier.set("bukkit")
  }

    assemble {
        dependsOn(shadowJar)
    }
}

