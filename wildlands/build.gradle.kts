plugins {
    id("java-library")
    id("de.eldoria.plugin-yml.bukkit")
}

group = rootProject.group.toString()
version = "1.2"

dependencies {
    compileOnly("io.papermc.paper:paper-api:${property("paperVersion")}")
}

bukkit {
    name = "WildLandsPlugin"
    main = "${(rootProject.group.toString())}.wildlands.WildLandsPlugin"
    generateLibrariesJson = true
    apiVersion = "${property("paperApiVersion")}"
    commands {
        register("wildlandsreload") {
            description = "Перезагрузка approved players и respawn таймингов"
            usage = "/wildlandsreload"
            permission = "wildlands.reload"
            permissionMessage = "У тебя нет прав на это"
        }
    }
}

tasks.jar {
    archiveBaseName.set("WildLandsPlugin")
}
