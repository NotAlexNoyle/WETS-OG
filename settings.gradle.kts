rootProject.name = "worldedit-spread-placing"

pluginManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}

listOf("common", "bukkit").forEach {
  include(it)
  project(":$it").name = "wets-$it"
}
