package me.cojam.wildlands;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.java.JavaPlugin;

public class WildLandsDeathListener implements Listener {

	private final WildLandsRespawnManager respawnManager;
	private final JavaPlugin plugin;

	public WildLandsDeathListener(JavaPlugin plugin, WildLandsRespawnManager manager) {
		this.plugin = plugin;
		this.respawnManager = manager;
	}

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
		Player player = event.getEntity();
		if (!player.getWorld().getName().equals("wild_lands")) return;

		//event.setKeepInventory(true); // опционально
		event.setDeathMessage(null);  // опционально

		int respawnTime = respawnManager.getRespawnTime(player);

		new BukkitRunnable() {
			int ticksLeft = respawnTime;
			@Override
			public void run() {
				if (ticksLeft <= 0) {
					player.spigot().respawn(); // или teleport на портал
					cancel();
					return;
				}
				// ActionBar для таймера
				player.sendActionBar("§cВы мертвы! Респаун через " + (ticksLeft/20) + " секунд");
				ticksLeft -= 20; // 1 секунда
			}
		}.runTaskTimer(plugin, 0L, 20L); // тик 1 секунда
	}
}
