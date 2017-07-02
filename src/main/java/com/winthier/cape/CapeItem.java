package com.winthier.cape;

import com.winthier.custom.item.CustomItem;
import com.winthier.custom.item.ItemContext;
import com.winthier.custom.item.ItemDescription;
import com.winthier.custom.item.TickableItem;
import com.winthier.generic_events.ItemNameEvent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class CapeItem implements CustomItem, TickableItem {
    private final CapePlugin plugin;
    private final ItemDescription itemDescription;
    private final ItemStack itemStack;

    CapeItem(CapePlugin plugin) {
        this.plugin = plugin;
        this.itemDescription = ItemDescription.of(plugin.getConfig().getConfigurationSection("description"));
        this.itemStack = new ItemStack(Material.ELYTRA);
        itemDescription.apply(itemStack);
    }

    @Override
    public String getCustomId() {
        return "cape:cape";
    }

    @Override
    public ItemStack spawnItemStack(int amount) {
        ItemStack result = itemStack.clone();
        result.setAmount(amount);
        return result;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityToggleGlide(EntityToggleGlideEvent event, ItemContext context) {
        if (!event.isGliding()) return;
        event.setCancelled(true);
        if (context.getItemStack().getDurability() >= Material.ELYTRA.getMaxDurability()) return;
        Player player = context.getPlayer();
        plugin.setFlying(context.getPlayer(), true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrepareAnvil(PrepareAnvilEvent event, ItemContext context) {
        ItemStack result = event.getResult();
        if (result == null) return;
        itemDescription.apply(result);
        if (!result.getEnchantments().isEmpty()) {
            ItemMeta meta = result.getItemMeta();
            meta.setDisplayName(ChatColor.AQUA + itemDescription.getDisplayName());
            result.setItemMeta(meta);
        }
    }

    @EventHandler
    public void onItemName(ItemNameEvent event, ItemContext context) {
        event.setItemName(itemDescription.getDisplayName());
    }

    @EventHandler
    public void onPlayerItemDamage(PlayerItemDamageEvent event, ItemContext context) {
        event.setCancelled(true);
        if (context.getPlayer().isGliding()) {
            context.getPlayer().setGliding(false);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event, ItemContext context) {
        if (context.getPosition() != ItemContext.Position.CHESTPLATE) return;
        if (context.getPlayer() == null) return;
        if (!plugin.isFlying(context.getPlayer())) return;
        plugin.setFlying(context.getPlayer(), false);
        context.getItemStack().setDurability(Material.ELYTRA.getMaxDurability());
    }

    @Override
    public void onTick(ItemContext context, int ticks) {
        if (context.getPosition() != ItemContext.Position.CHESTPLATE) return;
        if (ticks % 5 != 0) return;
        if (plugin.isFlying(context.getPlayer())) return;
        ItemStack item = context.getItemStack();
        if (item.getDurability() <= 0) return;
        if (!context.getPlayer().isOnGround()) return;
        item.setDurability((short)(item.getDurability() - 1));
    }
}
