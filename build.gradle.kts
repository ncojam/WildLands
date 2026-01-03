plugins {
    `java-library`
    id("de.eldoria.plugin-yml.bukkit") version "0.8.0"
}

group = "me.cojam"
version = "1.2"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

bukkit {
    main = "me.cojam.wildlands.WildLandsPlugin"
    generateLibrariesJson = true
    apiVersion = "1.21.11"
    commands {
        register("wildlandsreload") {
            description = "Перезагрузка approved players и respawn таймингов"
            usage = "/wildlandsreload"
            permission = "wildlands.reload"
            permissionMessage = "У тебя нет прав на это"
        }
    }
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"

    filesMatching("plugin.yml") {
        expand("project" to mapOf("version" to version))
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:deprecation")
}
