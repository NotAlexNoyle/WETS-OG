rootProject.name = "WETS-OG"

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
