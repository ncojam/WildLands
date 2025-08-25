package me.cojam.wildlands;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Map;

public class WildLandsProtectionListener implements Listener {

    private final FileConfiguration regionsConfig;

    public WildLandsProtectionListener(FileConfiguration regionsConfig) {
        this.regionsConfig = regionsConfig;
    }

    // Запрет порталов в Nether
    @EventHandler
    public void onPortalCreate(PlayerPortalEvent event) {
        World world = event.getFrom().getWorld();
        if (world.getName().equalsIgnoreCase("wild_lands")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cВ Диких землях порталы в Nether запрещены!");
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        handleProtection(event.getPlayer(), event.getBlock().getWorld(),
                event.getBlock().getX(), event.getBlock().getY(), event.getBlock().getZ(), event);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        handleProtection(event.getPlayer(), event.getBlock().getWorld(),
                event.getBlock().getX(), event.getBlock().getY(), event.getBlock().getZ(), event);
    }
	
	// Взрывы (криперы, ТНТ и прочее)
    @EventHandler
    public void onExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> isProtected(block.getWorld(),
                block.getX(), block.getY(), block.getZ()));
    }

    // Огненное зажигание (лавой, игроком, молнией и т.д.)
    @EventHandler
    public void onIgnite(BlockIgniteEvent event) {
        if (isProtected(event.getBlock().getWorld(),
                event.getBlock().getX(), event.getBlock().getY(), event.getBlock().getZ())) {
            event.setCancelled(true);
        }
    }

    // Горение блока
    @EventHandler
    public void onBurn(BlockBurnEvent event) {
        if (isProtected(event.getBlock().getWorld(),
                event.getBlock().getX(), event.getBlock().getY(), event.getBlock().getZ())) {
            event.setCancelled(true);
        }
    }

    // Лава/вода не должны заливаться в регионы
    @EventHandler
    public void onLiquidFlow(BlockFromToEvent event) {
        if (isProtected(event.getToBlock().getWorld(),
                event.getToBlock().getX(), event.getToBlock().getY(), event.getToBlock().getZ())) {
            event.setCancelled(true);
        }
    }

    // Распространение (например, огонь на соседние блоки)
    @EventHandler
    public void onSpread(BlockSpreadEvent event) {
        if (isProtected(event.getBlock().getWorld(),
                event.getBlock().getX(), event.getBlock().getY(), event.getBlock().getZ())) {
            event.setCancelled(true);
        }
    }

    private void handleProtection(Player player, World world, int x, int y, int z, org.bukkit.event.Cancellable event) {
        if (player.isOp()) return;

        List<Map<?, ?>> regions = regionsConfig.getMapList("protected-regions");
        for (Map<?, ?> region : regions) {
            String wName = (String) region.get("world");
            if (!world.getName().equalsIgnoreCase(wName)) continue;

            int x1 = (int) region.get("x1");
            int y1 = (int) region.get("y1");
            int z1 = (int) region.get("z1");
            int x2 = (int) region.get("x2");
            int y2 = (int) region.get("y2");
            int z2 = (int) region.get("z2");

            if (isInside(x, y, z, x1, y1, z1, x2, y2, z2)) {
                event.setCancelled(true);
                player.sendMessage("§cЭта территория защищена!");
                return;
            }
        }
    }

    private boolean isInside(int x, int y, int z, int x1, int y1, int z1, int x2, int y2, int z2) {
        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);

        return x >= minX && x <= maxX &&
                y >= minY && y <= maxY &&
                z >= minZ && z <= maxZ;
    }
	
	// ---------- Вспомогательная проверка ----------
    private boolean isProtected(World world, int x, int y, int z) {
        List<Map<?, ?>> regions = regionsConfig.getMapList("protected-regions");
        for (Map<?, ?> region : regions) {
            String wName = (String) region.get("world");
            if (!world.getName().equalsIgnoreCase(wName)) continue;

            int x1 = (int) region.get("x1");
            int y1 = (int) region.get("y1");
            int z1 = (int) region.get("z1");
            int x2 = (int) region.get("x2");
            int y2 = (int) region.get("y2");
            int z2 = (int) region.get("z2");

            if (isInside(x, y, z, x1, y1, z1, x2, y2, z2)) {
                return true;
            }
        }
        return false;
    }
}
