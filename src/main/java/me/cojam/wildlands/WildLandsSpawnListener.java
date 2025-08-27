package me.cojam.wildlands;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class WildLandsSpawnListener implements Listener {

	private final PortalManager portalManager;
	private final WildLandsRespawnManager respawnManager;
	private final Set<UUID> frozenSpectators = new HashSet<>();

	public WildLandsSpawnListener(PortalManager portalManager, WildLandsRespawnManager respawnManager) {
		this.portalManager = portalManager;
		this.respawnManager = respawnManager;
		startRespawnChecker();
		startSpectatorFreeze();
	}

	// Первое появление игрока
	@EventHandler
	public void onFirstJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		if (!player.hasPlayedBefore()) {
			PortalManager.Portal portal = portalManager.getFirstPortal();
			if (portal != null) {
				Bukkit.getScheduler().runTaskLater(portalManager.getPlugin(), () -> {
					player.teleport(portal.getLocation());
					player.sendMessage("§aДобро пожаловать! Ты появился у портала в Дикие земли.");
				}, 1L);
			}
		}
	}

	private boolean isApproved(Player player) {
		return portalManager.getApprovedPlayers().contains(player.getName().toLowerCase());
	}

	// Смерть игрока
	@EventHandler
	public void onDeath(PlayerDeathEvent event) {
		Player player = event.getEntity();
		if (!isApproved(player) && player.getWorld().getName().equalsIgnoreCase("wild_lands")) {
			PortalManager.Portal portal = portalManager.getFirstPortal();
			Location lockLoc = (portal != null) ? portal.getLocation() : player.getWorld().getSpawnLocation();

			respawnManager.setNextRespawnTime(player, lockLoc);

			player.sendMessage("§cТы умер в Диких землях! Респаун будет доступен через " +
					respawnManager.getRespawnTime(player) + " секунд.");
		}
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		
		// approved — вообще не трогаем
		if (portalManager.isApproved(player)) return;

		if (respawnManager.isSpectatorLocked(player)) {
			PortalManager.Portal portal = portalManager.getFirstPortal();
			Location lockLoc = (portal != null) ? portal.getLocation() : player.getWorld().getSpawnLocation();
			handleRespawnLock(player, lockLoc);
		}
	}

	@EventHandler
	public void onRespawn(PlayerRespawnEvent event) {
		Player player = event.getPlayer();

		// approved — обычный респаун
		if (portalManager.isApproved(player)) return;

		PortalManager.Portal portal = portalManager.getFirstPortal();
		if (portal == null) return; // на всякий случай, если нет портала

		Location respawnLoc = portal.getLocation();
		event.setRespawnLocation(respawnLoc);

		// через тик блокируем/возрождаем
		Bukkit.getScheduler().runTaskLater(portalManager.getPlugin(), () -> {
			handleRespawnLock(player, respawnLoc);
		}, 1L);
	}

	
	private void handleRespawnLock(Player player, Location portalLoc) {
		long unlock = respawnManager.getUnlockTime(player);
		long now = System.currentTimeMillis();

		if (now < unlock) {
			// Игрок должен ждать
			long left = (unlock - now) / 1000;
			player.sendMessage("§cТы ещё не можешь возродиться! Подожди " + left + " секунд.");

			Bukkit.getScheduler().runTaskLater(portalManager.getPlugin(), () -> {
				player.setGameMode(GameMode.SPECTATOR);
				player.teleport(portalLoc);
				player.setHealth(20.0);
				frozenSpectators.add(player.getUniqueId());
			}, 1L);

		} else {
			// Можно возрождать
			Bukkit.getScheduler().runTaskLater(portalManager.getPlugin(), () -> {
				player.setGameMode(GameMode.SURVIVAL);
				player.teleport(portalLoc);
				player.setHealth(20.0);
				player.sendMessage("§aТы снова готов отправиться в Дикие земли!");
				respawnManager.clearPlayerData(player);
			}, 1L);
		}
	}





	// Проверка респауна каждую секунду
	private void startRespawnChecker() {
		new BukkitRunnable() {
			@Override
			public void run() {
				for (Player player : Bukkit.getOnlinePlayers()) {
					if (player.getGameMode() == GameMode.SPECTATOR && frozenSpectators.contains(player.getUniqueId())) {
						long unlock = respawnManager.getUnlockTime(player);
						if (System.currentTimeMillis() >= unlock) {

							// Определяем реальную точку респауна
							Location target;
							if (player.getBedSpawnLocation() != null && player.getBedSpawnLocation().getWorld() != null) {
								target = player.getBedSpawnLocation();
							} else {
								PortalManager.Portal portal = portalManager.getFirstPortal();
								target = (portal != null) ? portal.getLocation() : player.getWorld().getSpawnLocation();
							}

							player.setGameMode(GameMode.SURVIVAL);
							player.teleport(target);
							player.setHealth(20.0);
							player.sendMessage("§aТы снова готов отправиться в Дикие земли!");
							frozenSpectators.remove(player.getUniqueId());
						}
					}
				}
			}
		}.runTaskTimer(portalManager.getPlugin(), 20L, 20L); // каждую секунду
	}

	// Фиксируем спектаторов на месте (чтобы не летали)
	private void startSpectatorFreeze() {
		new BukkitRunnable() {
			@Override
			public void run() {
				for (UUID uuid : frozenSpectators) {
					Player player = Bukkit.getPlayer(uuid);
					if (player != null && player.getGameMode() == GameMode.SPECTATOR) {
						PortalManager.Portal portal = portalManager.getFirstPortal();
						Location lockLoc = (portal != null) ? portal.getLocation() : player.getWorld().getSpawnLocation();

						// сохраняем yaw/pitch игрока
						Location current = player.getLocation();
						Location fixed = new Location(
								lockLoc.getWorld(),
								lockLoc.getX(),
								lockLoc.getY(),
								lockLoc.getZ(),
								current.getYaw(),
								current.getPitch()
						);

						player.teleport(fixed);
					}
				}
			}
		}.runTaskTimer(portalManager.getPlugin(), 5L, 5L);
	}
}
