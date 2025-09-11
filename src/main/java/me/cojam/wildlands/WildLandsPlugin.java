package me.cojam.wildlands;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.OfflinePlayer;


import java.io.File;

public class WildLandsPlugin extends JavaPlugin {
	private File regionsFile;
    private FileConfiguration regionsConfig;
	private WildLandsRespawnManager respawnManager;
	private PortalManager portalManager;
	
    @Override
    public void onEnable() {
        getLogger().info("Загружаем мир wild_lands...");
		
		// Сначала инициализируем поле
		this.portalManager = new PortalManager(this);
		this.respawnManager = new WildLandsRespawnManager(this);
		
		// Регистрируем слушателей
		Bukkit.getPluginManager().registerEvents(new WildLandsSpawnListener(portalManager, respawnManager), this);
		Bukkit.getPluginManager().registerEvents(new WildLandsDeathListener(this, respawnManager), this);


		// Загружаем мир wild_lands
		WorldCreator wcWild = new WorldCreator("wild_lands");
		wcWild.environment(World.Environment.NORMAL);
		World wild = Bukkit.createWorld(wcWild);
		if (wild != null) {
			getLogger().info("Мир wild_lands успешно загружен!");
		} else {
			getLogger().warning("Не удалось загрузить мир wild_lands!");
		}
		
		// Загружаем regions.yml
        loadRegionsConfig();
		
		// Регистрируем защитный обработчик
		Bukkit.getPluginManager().registerEvents(new WildLandsProtectionListener(regionsConfig), this);
		PortalManager portalManager = new PortalManager(this);
		// Если нужно позже перезагрузить portals.yml без рестарта:
		// portalManager.reloadPortals();
		
		WildLandsRespawnManager respawnManager = new WildLandsRespawnManager(this);
		getServer().getPluginManager().registerEvents(
			new WildLandsSpawnListener(portalManager, respawnManager), this
		);

		getCommand("wildlandsreload").setExecutor((sender, command, label, args) -> {
			if (!sender.isOp()) {
				sender.sendMessage("§cУ тебя нет прав на эту команду.");
				return true;
			}

			this.portalManager.reloadPortals();
			this.respawnManager.loadRespawnTimes();

			sender.sendMessage("§aКонфиги WildLands перезагружены!");
			return true;
		});

		// ОТОБРАЗИТЬ ВСЕХ ИГРОКОВ:
		/*getLogger().info("WildLandsPlugin включён!");
		for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
			getLogger().info("Игрок: " + p.getName() + " | UUID: " + p.getUniqueId());
		}*/
		
		// Инициализация менеджера респауна
		respawnManager = new WildLandsRespawnManager(this);

		// Регистрируем слушатель смерти
		getServer().getPluginManager().registerEvents(
			new WildLandsDeathListener(this, respawnManager), this
		);

		
    }
	
	private void loadRegionsConfig() {
        regionsFile = new File(getDataFolder(), "regions.yml");
        if (!regionsFile.exists()) {
            // если файла нет — создаём пустой по умолчанию
            regionsFile.getParentFile().mkdirs();
            saveResource("regions.yml", false);
        }
        regionsConfig = YamlConfiguration.loadConfiguration(regionsFile);
    }

    public FileConfiguration getRegionsConfig() {
        return regionsConfig;
    }
}
