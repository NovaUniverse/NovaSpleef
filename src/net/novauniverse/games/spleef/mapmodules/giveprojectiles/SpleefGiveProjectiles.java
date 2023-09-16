package net.novauniverse.games.spleef.mapmodules.giveprojectiles;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.json.JSONObject;

import net.novauniverse.games.spleef.NovaSpleef;
import net.novauniverse.games.spleef.modules.snowballvote.SpleefSnowballVoteManager;
import net.zeeraa.novacore.commons.log.Log;
import net.zeeraa.novacore.commons.tasks.Task;
import net.zeeraa.novacore.spigot.abstraction.enums.VersionIndependentMaterial;
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.Game;
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.GameManager;
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.map.mapmodule.MapModule;
import net.zeeraa.novacore.spigot.module.ModuleManager;
import net.zeeraa.novacore.spigot.tasks.SimpleTask;
import net.zeeraa.novacore.spigot.utils.ItemBuilder;
import net.zeeraa.novacore.spigot.utils.PlayerUtils;

public class SpleefGiveProjectiles extends MapModule {
	private Material material;
	private int maxItems;
	private int delay;
	private Task task;

	public Material getMaterial() {
		return material;
	}

	public int getMaxItems() {
		return maxItems;
	}

	public int getDelay() {
		return delay;
	}

	public Task getTask() {
		return task;
	}

	public SpleefGiveProjectiles(JSONObject json) {
		super(json);

		if (json.has("material")) {
			material = Material.valueOf(json.getString("material"));
		} else {
			material = VersionIndependentMaterial.SNOWBALL.toBukkitVersion();
		}

		if (json.has("max_items")) {
			maxItems = json.getInt("max_items");
		} else {
			maxItems = 16;
		}

		if (json.has("delay")) {
			delay = json.getInt("delay");
		} else {
			delay = 5;
		}

		this.task = new SimpleTask(new Runnable() {
			@Override
			public void run() {
				if (NovaSpleef.getInstance().isSnowballVotingEnabled()) {
					if (ModuleManager.isEnabled(SpleefSnowballVoteManager.class)) {
						if (!ModuleManager.getModule(SpleefSnowballVoteManager.class).isShouldGiveSnowballs()) {
							return;
						}
					}
				} else {
					if(!NovaSpleef.getInstance().isShouldGiveSnowballs()) {
						return;
					}
				}

				GameManager.getInstance().getActiveGame().getPlayers().forEach(uuid -> PlayerUtils.ifOnline(uuid, (player) -> {
					Inventory inventory = player.getInventory();

					int totalItems = 0;

					for (ItemStack item : inventory.getContents()) {
						if (item != null) {
							if (item.getType() == material) {
								totalItems += item.getAmount();
							}
						}
					}

					if (totalItems >= maxItems) {
						return;
					}

					inventory.addItem(ItemBuilder.materialToItemStack(material));
				}));
			}
		}, delay * 20);
	}

	@Override
	public void onGameStart(Game game) {
		Log.trace("SpleefGiveProjectiles", "Starting task");
		task.start();
	}

	@Override
	public void onGameEnd(Game game) {
		Task.tryStopTask(task);
	}
}