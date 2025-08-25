package me.cojam.wildlands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class WildLandsRespawnManager {

	private final JavaPlugin plugin;
	private File respawnTimesFile;
	private FileConfiguration respawnTimesConfig;

	private File respawnDataFile;
	private FileConfiguration respawnDataConfig;

	private int defaultRespawnTime;
	private final Map<UUID, Integer> customTimes = new HashMap<>();

	public WildLandsRespawnManager(JavaPlugin plugin) {
		this.plugin = plugin;
		loadRespawnTimes();
		loadRespawnData();
		startAutoReload();
	}

	/* ===== respawn-times.yml ===== */

	public void loadRespawnTimes() {
		respawnTimesFile = new File(plugin.getDataFolder(), "respawn-times.yml");
		if (!respawnTimesFile.exists()) {
			plugin.saveResource("respawn-times.yml", false);
		}
		respawnTimesConfig = YamlConfiguration.loadConfiguration(respawnTimesFile);

		defaultRespawnTime = respawnTimesConfig.getInt("default", 300);
		customTimes.clear();

		if (respawnTimesConfig.contains("overrides")) {
			for (String key : respawnTimesConfig.getConfigurationSection("overrides").getKeys(false)) {
				int time = respawnTimesConfig.getInt("overrides." + key, defaultRespawnTime);
				try {
					customTimes.put(UUID.fromString(key), time);
				} catch (IllegalArgumentException ignored) {
					plugin.getLogger().warning("Invalid UUID in overrides: " + key);
				}
			}
		}
	}

	public int getRespawnTime(Player player) {
		return customTimes.getOrDefault(player.getUniqueId(), defaultRespawnTime);
	}

	private void startAutoReload() {
		Bukkit.getScheduler().runTaskTimer(plugin, this::loadRespawnTimes, 20L * 60, 20L * 60);
	}

	/* ===== respawn-data.yml ===== */

	public void loadRespawnData() {
		respawnDataFile = new File(plugin.getDataFolder(), "respawn-data.yml");
		if (!respawnDataFile.exists()) {
			try {
				respawnDataFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		respawnDataConfig = YamlConfiguration.loadConfiguration(respawnDataFile);
	}

	public void saveRespawnData() {
		try {
			respawnDataConfig.save(respawnDataFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setNextRespawnTime(Player player, Location lockLocation) {
		long unlockTime = System.currentTimeMillis() + (getRespawnTime(player) * 1000L);

		String path = "players." + player.getUniqueId();
		respawnDataConfig.set(path + ".unlockTime", unlockTime);
		respawnDataConfig.set(path + ".spectatorLocked", true);
		respawnDataConfig.set(path + ".world", lockLocation.getWorld().getName());
		respawnDataConfig.set(path + ".x", lockLocation.getX());
		respawnDataConfig.set(path + ".y", lockLocation.getY());
		respawnDataConfig.set(path + ".z", lockLocation.getZ());
		saveRespawnData();
	}

	public long getUnlockTime(Player player) {
		return respawnDataConfig.getLong("players." + player.getUniqueId() + ".unlockTime", 0L);
	}

	public boolean isSpectatorLocked(Player player) {
		return respawnDataConfig.getBoolean("players." + player.getUniqueId() + ".spectatorLocked", false);
	}

	public Location getLockLocation(Player player) {
		String path = "players." + player.getUniqueId();
		String worldName = respawnDataConfig.getString(path + ".world");
		if (worldName == null) return null;
		World world = Bukkit.getWorld(worldName);
		if (world == null) return null;

		double x = respawnDataConfig.getDouble(path + ".x");
		double y = respawnDataConfig.getDouble(path + ".y");
		double z = respawnDataConfig.getDouble(path + ".z");
		return new Location(world, x, y, z);
	}

	public void clearPlayerData(Player player) {
		respawnDataConfig.set("players." + player.getUniqueId(), null);
		saveRespawnData();
	}
}
