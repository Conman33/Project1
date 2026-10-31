Meteor addon "cluster-esp" for Minecraft 26.3 (Mojang mappings, Java 25).

Build with GitHub (no local setup):
 1. github.com/MeteorDevelopment/meteor-addon-template -> "Use this template" -> create your repo.
 2. In your repo's gradle.properties set (use the template's existing property names):
      minecraft_version = 26.3
      loader_version    = 0.19.5
      fabric_version    = 0.164.0+26.3
      meteor version    = 26.3-SNAPSHOT
 3. Delete the template's example Java package, upload the "dev" folder from this zip into src/main/java,
    and replace src/main/resources/fabric.mod.json with the one in this zip.
 4. Commit. Actions tab -> finished run -> Artifacts -> download the jar.
 5. Put the jar in .minecraft/mods next to the Meteor Client jar for 26.3 and Fabric API for 26.3 (Java 25 required).
