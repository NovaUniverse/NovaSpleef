package net.novauniverse.games.spleef.modules.snowballvote;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;

import net.md_5.bungee.api.ChatColor;
import net.zeeraa.novacore.spigot.abstraction.enums.ColoredBlockType;
import net.zeeraa.novacore.spigot.abstraction.enums.VersionIndependentMaterial;
import net.zeeraa.novacore.spigot.abstraction.enums.VersionIndependentSound;
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.GameManager;
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.events.GameStartEvent;
import net.zeeraa.novacore.spigot.gameengine.module.modules.gamelobby.events.PlayerJoinGameLobbyEvent;
import net.zeeraa.novacore.spigot.module.NovaModule;
import net.zeeraa.novacore.spigot.module.modules.customitems.CustomItemManager;
import net.zeeraa.novacore.spigot.module.modules.gui.GUIAction;
import net.zeeraa.novacore.spigot.module.modules.gui.callbacks.GUIClickCallback;
import net.zeeraa.novacore.spigot.module.modules.gui.holders.GUIReadOnlyHolder;
import net.zeeraa.novacore.spigot.utils.ItemBuilder;

public class SpleefSnowballVoteManager extends NovaModule implements Listener {
	private boolean shouldGiveSnowballs;
	private List<VoteData> votes;

	public SpleefSnowballVoteManager() {
		super("Spleef.SnowballVoteManager");
		votes = new ArrayList<>();
		shouldGiveSnowballs = false;
	}

	public boolean isShouldGiveSnowballs() {
		return shouldGiveSnowballs;
	}

	@Override
	public void onDisable() throws Exception {
		votes.clear();
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent e) {
		votes.removeIf(v -> v.getUuid().equals(e.getPlayer().getUniqueId()));
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerJoinGameLobby(PlayerJoinGameLobbyEvent e) {
		if (GameManager.getInstance().hasGame()) {
			if (!GameManager.getInstance().getActiveGame().hasStarted()) {
				e.getPlayer().getInventory().setItem(1, CustomItemManager.getInstance().getCustomItemStack(SnowballVoteSelectorItem.class, e.getPlayer()));
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onGameStart(GameStartEvent e) {
		int yesVotes = (int) votes.stream().filter(v -> v.isWantSnowballs()).count();
		int noVotes = (int) votes.stream().filter(v -> !v.isWantSnowballs()).count();

		if(noVotes == 0 && yesVotes == 0) {
			Bukkit.getServer().broadcastMessage(ChatColor.GRAY + "Snowballs will be disabled since no one voted for enabling or disabling them");
			return;
		}
		
		if (yesVotes > noVotes) {
			Bukkit.getServer().broadcastMessage(ChatColor.GREEN + "Snowballs will be enabled since " + yesVotes + " voted for snowballs and " + noVotes + " voted against snowballs");
			shouldGiveSnowballs = true;
		} else if (yesVotes == noVotes) {
			Bukkit.getServer().broadcastMessage(ChatColor.YELLOW + "Snowballs will be disabled since there was a tie in voting");
		} else {
			Bukkit.getServer().broadcastMessage(ChatColor.RED + "Snowballs will be disabled since " + noVotes + " voted against snowballs and " + yesVotes + " voted for snowballs");
		}
	}

	private void vote(Player player, boolean vote) {
		VoteData existingVote = votes.stream().filter(v -> v.getUuid().equals(player.getUniqueId())).findFirst().orElse(null);
		if (existingVote == null) {
			votes.add(new VoteData(player.getUniqueId(), vote));
		} else {
			existingVote.setWantSnowballs(vote);
		}
		player.sendMessage(ChatColor.GOLD + "Voted " + (vote ? ChatColor.GREEN + "yes" : ChatColor.RED + "no") + ChatColor.GOLD + " for snowballs");
		VersionIndependentSound.ORB_PICKUP.play(player);
	}

	public void show(Player player) {
		GUIReadOnlyHolder holder = new GUIReadOnlyHolder();
		Inventory inventory = Bukkit.getServer().createInventory(holder, 9, ChatColor.GOLD + "Enable snowballs?");

		ItemBuilder yesBuilder = new ItemBuilder(ColoredBlockType.GLASS_PANE, DyeColor.LIME);
		ItemBuilder noBuilder = new ItemBuilder(ColoredBlockType.GLASS_PANE, DyeColor.RED);
		ItemBuilder centerItem = new ItemBuilder(VersionIndependentMaterial.SNOWBALL);

		yesBuilder.setName(ChatColor.GREEN + "Yes");
		yesBuilder.addLore("Vote for enabling snowballs");
		yesBuilder.setAmount(1);

		noBuilder.setName(ChatColor.GREEN + "No");
		noBuilder.addLore("Vote for disabling snowballs");
		noBuilder.setAmount(1);

		centerItem.setName(ChatColor.AQUA + "Enable snowbals?");
		centerItem.addLore(ChatColor.WHITE + "Vote for if you want snowballs to be enabled.");
		centerItem.addLore(ChatColor.WHITE + "");
		centerItem.addLore(ChatColor.WHITE + "if enabled snowballs will be given to");
		centerItem.addLore(ChatColor.WHITE + "the players to use as a weapon");

		GUIClickCallback yesCallback = new GUIClickCallback() {
			@Override
			public GUIAction onClick(Inventory clickedInventory, Inventory inventory, HumanEntity entity, int clickedSlot, SlotType slotType, InventoryAction clickType) {
				vote((Player) entity, true);
				return GUIAction.CANCEL_INTERACTION;
			}
		};

		GUIClickCallback noCallback = new GUIClickCallback() {
			@Override
			public GUIAction onClick(Inventory clickedInventory, Inventory inventory, HumanEntity entity, int clickedSlot, SlotType slotType, InventoryAction clickType) {
				vote((Player) entity, false);
				return GUIAction.CANCEL_INTERACTION;
			}
		};

		for (int i = 0; i < 4; i++) {
			inventory.setItem(i, yesBuilder.build());
			holder.addClickCallback(i, yesCallback);
		}

		for (int i = 5; i < 9; i++) {
			inventory.setItem(i, noBuilder.build());
			holder.addClickCallback(i, noCallback);
		}

		inventory.setItem(4, centerItem.build());

		player.openInventory(inventory);
	}
}