package com.winthier.cape;

import com.winthier.custom.CustomPlugin;
import com.winthier.custom.event.CustomRegisterEvent;
import com.winthier.custom.item.CustomItem;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import lombok.Getter;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class CapePlugin extends JavaPlugin implements Listener {
    private final Set<GameMode> gameModes = EnumSet.of(GameMode.SURVIVAL, GameMode.ADVENTURE);
    private final List<Player> flyingPlayers = new ArrayList<>();
    private final Random random = new Random(System.currentTimeMillis());
    private int ticks = 0;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().runTaskTimer(this, () -> onTick(), 1, 1);
    }

    public void onDisable() {
        for (Player player: flyingPlayers) {
            if (!gameModes.contains(player.getGameMode())) return;
            player.setFlying(false);
            player.setAllowFlight(false);
            player.setFlySpeed(0.1f);
        }
        flyingPlayers.clear();
    }

    @EventHandler
    public void onCustomRegister(CustomRegisterEvent event) {
        reloadConfig();
        event.addItem(new CapeItem(this));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        setFlying(event.getPlayer(), false);
    }

    boolean isFlying(Player player) {
        return flyingPlayers.contains(player);
    }

    void setFlying(Player player, boolean flying) {
        if (flying) {
            if (flyingPlayers.contains(player)) return;
            flyingPlayers.add(player);
            if (!gameModes.contains(player.getGameMode())) return;
            player.setAllowFlight(true);
            player.setFlying(true);
            player.setFlySpeed(0.05f);
            Location loc = player.getEyeLocation();
            loc.getWorld().playSound(loc, Sound.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.PLAYERS, 0.3f, 0.75f);
            // loc.getWorld().playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 0.5f, 2.0f);
            loc.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, loc, 16, 1, 1, 1, 0.0);
        } else {
            if (!flyingPlayers.contains(player)) return;
            flyingPlayers.remove(player);
            if (!gameModes.contains(player.getGameMode())) return;
            player.setFlying(false);
            player.setAllowFlight(false);
            player.setFlySpeed(0.1f);
            Location loc = player.getEyeLocation();
            loc.getWorld().playSound(loc, Sound.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.PLAYERS, 0.3f, 0.5f);
            loc.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, loc, 16, 1, 1, 1, 0.0);
        }
    }

    void onTick() {
        if (flyingPlayers.isEmpty()) return;
        ticks += 1;
        List<Player> noFlightList = new ArrayList<>();
        for (Iterator<Player> iter = flyingPlayers.iterator(); iter.hasNext();) {
            Player player = iter.next();
            if (!player.isValid()) {
                iter.remove();
                continue;
            }
            if (!player.isFlying()) {
                noFlightList.add(player);
                continue;
            }
            ItemStack item = player.getEquipment().getChestplate();
            if (item == null || item.getType() != Material.ELYTRA || item.getDurability() >= Material.ELYTRA.getMaxDurability()) {
                noFlightList.add(player);
                continue;
            }
            CustomItem customItem = CustomPlugin.getInstance().getItemManager().getCustomItem(item);
            if (customItem == null || !(customItem instanceof CapeItem)) {
                noFlightList.add(player);
                continue;
            }
            int level = item.getEnchantmentLevel(Enchantment.DURABILITY);
            if (level == 0 || random.nextInt(level + 1) == 0) {
                item.setDurability((short)(item.getDurability() + 1));
            }
            if (ticks % 5 == 0) {
                player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, player.getLocation(), 1, 0.5, 0.5, 0.5, 0.0);
            }
        }
        for (Player player: noFlightList) setFlying(player, false);
    }
}
