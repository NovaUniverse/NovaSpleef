package net.novauniverse.games.spleef.modules.snowballvote;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import net.zeeraa.novacore.spigot.abstraction.VersionIndependentUtils;
import net.zeeraa.novacore.spigot.abstraction.enums.VersionIndependentMaterial;
import net.zeeraa.novacore.spigot.module.ModuleManager;
import net.zeeraa.novacore.spigot.module.modules.customitems.CustomItem;
import net.zeeraa.novacore.spigot.utils.ItemBuilder;

public class SnowballVoteSelectorItem extends CustomItem {
	@Override
	protected ItemStack createItemStack(Player player) {
		ItemBuilder builder = new ItemBuilder(VersionIndependentMaterial.SNOWBALL);

		builder.addLore(ChatColor.WHITE + "Right click to vote for enabling snowballs");
		builder.addLore(ChatColor.WHITE + "");
		builder.addLore(ChatColor.WHITE + "if enabled snowballs will be given to");
		builder.addLore(ChatColor.WHITE + "the players to use as a weapon");
		builder.setName(ChatColor.GOLD + "" + ChatColor.BOLD + "Enable Snowballs");

		return builder.build();
	}

	@Override
	public void onPlayerDropItem(PlayerDropItemEvent event) {
		event.setCancelled(true);
	}

	@Override
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			event.setCancelled(true);
			if (VersionIndependentUtils.get().isInteractEventMainHand(event)) {
				SpleefSnowballVoteManager selector = (SpleefSnowballVoteManager) ModuleManager.getModule(SpleefSnowballVoteManager.class);
				if (selector.isEnabled()) {
					selector.show(event.getPlayer());
				}
			}
		}
	}
}