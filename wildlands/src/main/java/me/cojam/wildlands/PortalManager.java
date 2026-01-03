package me.cojam.wildlands;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;


import java.io.File;
import java.util.*;

public class PortalManager {

	private final JavaPlugin plugin;
	private File portalsFile;
	private FileConfiguration portalsConfig;

	private final Map<String, Portal> portals = new LinkedHashMap<>();
	private final Set<String> approvedPlayers = new HashSet<>();
	
	private final Map<UUID, Long> nextSoundTime = new HashMap<>();
	private final Random random = new Random();

	public PortalManager(JavaPlugin plugin) {
		this.plugin = plugin;
		loadPortals();
		startParticleAndSoundTask();
	}
	
	public JavaPlugin getPlugin() {
		return plugin;
	}


	public void loadPortals() {
		portalsFile = new File(plugin.getDataFolder(), "portals.yml");
		if (!portalsFile.exists()) {
			plugin.saveResource("portals.yml", false);
		}
		portalsConfig = YamlConfiguration.loadConfiguration(portalsFile);

		portals.clear();
		approvedPlayers.clear();

		for (Map<?, ?> map : portalsConfig.getMapList("portals")) {
			String id = (String) map.get("id");
			String worldName = (String) map.get("world");
			World world = Bukkit.getWorld(worldName);
			if (world == null) continue;

			double x = ((Number) map.get("x")).doubleValue();
			double y = ((Number) map.get("y")).doubleValue();
			double z = ((Number) map.get("z")).doubleValue();
			String linkedId = (String) map.get("linkedId");

			portals.put(id, new Portal(id, world, x, y, z, linkedId));
		}

		approvedPlayers.addAll(portalsConfig.getStringList("approved-players"));
	}
	
	public Set<String> getApprovedPlayers() {
		return approvedPlayers;
	}

	public boolean isApproved(Player player) {
		return approvedPlayers.contains(player.getName());
	}


	public void reloadPortals() {
		loadPortals();
	}

	private void startParticleAndSoundTask() {
		new BukkitRunnable() {
			int tickCounter = 0; // Счётчик для оптимизации
			@Override
			public void run() {
				tickCounter++;
				if (tickCounter % 4 != 0) return; // выполняем проверку раз в 4 тика (0.2 сек)

				for (Portal portal : portals.values()) {
					Location portalLoc = portal.getLocation();

					for (Player player : portal.getWorld().getPlayers()) {
						Location playerLoc = player.getLocation();

						// Проверяем, находится ли игрок в радиусе платформы 3x3 (по горизонтали) и ±1 блок по вертикали
						if (Math.abs(playerLoc.getX() - portalLoc.getX()) <= 1 &&
							Math.abs(playerLoc.getY() - portalLoc.getY()) <= 1 &&
							Math.abs(playerLoc.getZ() - portalLoc.getZ()) <= 1) {

							// Отладка: выводим в чат
							//player.sendMessage("§aПортал засёк игрока!");

							// Если игрок одобрен, запускаем таймер телепорта
							if (approvedPlayers.contains(player.getName())) {
								portal.startTeleportCountdown(player, PortalManager.this);
							}
						} else {
							// Если игрок вышел с платформы — сброс таймера
							portal.cancelTeleport(player);
						}

						// В радиусе 8 чанков (128 блоков) от портала запускаем частицы
						if (playerLoc.distanceSquared(portalLoc) < 128*128) {
							portal.spawnParticles();
							long now = System.currentTimeMillis();
							long next = nextSoundTime.getOrDefault(player.getUniqueId(), 0L);
							if (now >= next) {
								// проигрываем звук
								player.playSound(portalLoc, "minecraft:ambience/magic", 1f, 1f);

								// планируем следующий раз (между 1000 и 1500 мс)
								long delay = 1000 + random.nextInt(500);
								nextSoundTime.put(player.getUniqueId(), now + delay);
							}
						}
					}
				}
			}
		}.runTaskTimer(plugin, 0L, 1L); // тик каждый 0.05 сек
	}


	public Portal getPortal(String id) {
		return portals.get(id);
	}
	
	public Portal getFirstPortal() {
		//return portals.values().stream().findFirst().orElse(null);
		return getPortalById("portal1");
	}
	
	public Portal getPortalById(String id) {
		return portals.get(id); // тут всё просто, потому что ключ = id
	}


	public class Portal {
		private final String id;
		private final World world;
		private final Location loc;
		private final String linkedId;
		private final Map<UUID, Integer> teleportTasks = new HashMap<>();

		public Portal(String id, World world, double x, double y, double z, String linkedId) {
			this.id = id;
			this.world = world;
			this.loc = new Location(world, x, y, z);
			this.linkedId = linkedId;
		}

		public World getWorld() { return world; }
		public Location getLocation() { return loc; }

		// ------------------- Партиклы -------------------
		public void spawnParticles() {
			// Первая спираль — яркая и тонкая
			spawnSpiralParticles(Particle.END_ROD, 1.0, 0.5, -0.5, 0.5, Math.PI/12, 0.15, 40, 0.05);

			// Вторая спираль — красная, шире, быстрее вращается
			spawnSpiralParticles(Particle.FLAME, 1.5, 0.5, -0.5, 0.5, Math.PI/10, 0.12, 50, 0.08);
		}
		
		public void spawnSpiralParticles(
			Particle particleType,
			double radius,
			double offsetX, double offsetY, double offsetZ,
			double angleStep, double heightStep, int steps,
			double rotationSpeed
		) {
			new BukkitRunnable() {
				int step = 0;
				double rotation = 0;
				@Override
				public void run() {
					if (step >= steps) {
						cancel();
						return;
					}
					
					double angle = step * angleStep;
					double x = loc.getX() + offsetX + Math.cos(angle + rotation) * radius;
					double y = loc.getY() + offsetY + 0.2 + step * heightStep;
					double z = loc.getZ() + offsetZ + Math.sin(angle + rotation) * radius;

					loc.getWorld().spawnParticle(particleType, x, y, z, 1, 0, 0, 0, 0);

					step++;
					rotation += rotationSpeed;
					rotation %= 2 * Math.PI;
				}
			}.runTaskTimer(plugin, 0L, 4L);
		}

		
		// ------------------- Подсветка игрока -------------------
		public void showTeleportEffect(Player player) {
			// Используем эффект светящегося облака для визуализации
			player.spawnParticle(org.bukkit.Particle.WITCH,
				player.getLocation().add(0,1,0), 5, 0.3, 0.5, 0.3, 0.05);
		}


		public void startTeleportCountdown(Player player, PortalManager manager) {
			// если уже есть активный таск, не создаём новый
			if (teleportTasks.containsKey(player.getUniqueId())) return;

			int taskId = new BukkitRunnable() {
				int ticks = 60; // 3 секунды

				@Override
				public void run() {
					// проверка по X и Z, игнорируем Y
					double dx = player.getLocation().getX() - loc.getX();
					double dz = player.getLocation().getZ() - loc.getZ();
					if (Math.abs(dx) > 1 || Math.abs(dz) > 1) {
						cancel();
						teleportTasks.remove(player.getUniqueId());
						return;
					}
					
					// визуальный эффект «подсветки» игрока
					showTeleportEffect(player);

					ticks--;
					if (ticks <= 0) {
						Portal linked = manager.getPortal(linkedId);
						if (linked != null) {
							player.teleport(linked.getLocation());
							//player.sendMessage("§aТелепорт выполнен!");
						} else {
							//player.sendMessage("§cСвязанный портал не найден!");
						}
						teleportTasks.remove(player.getUniqueId());
						cancel();
					}
				}
			}.runTaskTimer(plugin, 0L, 1L).getTaskId();

			teleportTasks.put(player.getUniqueId(), taskId);
		}



		public void cancelTeleport(Player player) {
			Integer taskId = teleportTasks.remove(player.getUniqueId());
			if (taskId != null) {
				Bukkit.getScheduler().cancelTask(taskId);
			}
		}
	}
}
