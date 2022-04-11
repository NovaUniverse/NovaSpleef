package net.novauniverse.games.spleef.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import net.novauniverse.games.spleef.NovaSpleef;
import net.novauniverse.games.spleef.mapmodules.config.SpleefConfigMapModule;
import net.novauniverse.games.spleef.mapmodules.mapdecay.SpleefMapDecay;
import net.zeeraa.novacore.commons.log.Log;
import net.zeeraa.novacore.commons.tasks.Task;
import net.zeeraa.novacore.commons.timers.TickCallback;
import net.zeeraa.novacore.commons.utils.Callback;
import net.zeeraa.novacore.spigot.NovaCore;
import net.zeeraa.novacore.spigot.abstraction.VersionIndependantUtils;
import net.zeeraa.novacore.spigot.abstraction.enums.VersionIndependantSound;
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.GameEndReason;
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.MapGame;
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.elimination.PlayerQuitEliminationAction;
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.map.mapmodule.MapModule;
import net.zeeraa.novacore.spigot.language.LanguageManager;
import net.zeeraa.novacore.spigot.tasks.SimpleTask;
import net.zeeraa.novacore.spigot.timers.BasicTimer;
import net.zeeraa.novacore.spigot.utils.ItemBuilder;
import net.zeeraa.novacore.spigot.utils.LocationUtils;
import net.zeeraa.novacore.spigot.utils.PlayerUtils;
import net.zeeraa.novacore.spigot.utils.RandomFireworkEffect;

public class Spleef extends MapGame implements Listener {
	public static final String[] TOOL_LORE_EASER_EGGS = { "IKEA\nSANDIG 20:-\nSpade, blå\n16:- exkl. moms\nUtgår inom kort\n\n\nHey how do i remove\nthe price tag from this?", "Zeeraa Approved!", "Try not to fall into the lava" };

	private boolean started;
	private boolean ended;

	private final int countdownTime = 10;
	private boolean countdownOver;

	private SpleefMapDecay decayModule;

	private SpleefConfigMapModule config;

	private Task gameLoop;

	public Spleef() {
		super(NovaSpleef.getInstance());
		this.started = false;
		this.ended = false;
		this.config = null;
		this.countdownOver = false;
		this.decayModule = null;
	}

	public SpleefConfigMapModule getConfig() {
		return config;
	}

	@Override
	public String getName() {
		return "spleef";
	}

	@Override
	public String getDisplayName() {
		return "Spleef";
	}

	@Override
	public PlayerQuitEliminationAction getPlayerQuitEliminationAction() {
		return NovaSpleef.getInstance().isAllowReconnect() ? PlayerQuitEliminationAction.DELAYED : PlayerQuitEliminationAction.INSTANT;
	}

	@Override
	public boolean eliminatePlayerOnDeath(Player player) {
		return true;
	}

	@Override
	public boolean isPVPEnabled() {
		return true;
	}

	@Override
	public boolean autoEndGame() {
		return true;
	}

	@Override
	public boolean hasStarted() {
		return started;
	}

	@Override
	public boolean hasEnded() {
		return ended;
	}

	@Override
	public boolean isFriendlyFireAllowed() {
		return false;
	}

	@Override
	public boolean canAttack(LivingEntity attacker, LivingEntity target) {
		return true;
	}

	public SpleefMapDecay getDecayModule() {
		return decayModule;
	}

	public void tpToSpectator(Player player) {
		NovaCore.getInstance().getVersionIndependentUtils().resetEntityMaxHealth(player);
		player.setHealth(20);
		player.setGameMode(GameMode.SPECTATOR);
		if (hasActiveMap()) {
			player.teleport(getActiveMap().getSpectatorLocation());
		}
	}

	/**
	 * Teleport a player to a provided start location
	 * 
	 * @param player   {@link Player} to teleport
	 * @param location {@link Location} to teleport the player to
	 */
	protected void tpToArena(Player player, Location location) {
		player.teleport(location.getWorld().getSpawnLocation());
		PlayerUtils.clearPlayerInventory(player);
		PlayerUtils.clearPotionEffects(player);
		PlayerUtils.resetPlayerXP(player);
		player.setHealth(player.getMaxHealth());
		player.setSaturation(20);
		player.setFoodLevel(20);
		player.setGameMode(GameMode.SURVIVAL);
		player.teleport(location);

		ItemStack tool = config.getToolItemStack();

		if (NovaSpleef.getInstance().isEnableEasterEggLores()) {
			Random random = new Random();
			if (random.nextInt(4) == 1) {
				String selected = TOOL_LORE_EASER_EGGS[random.nextInt(TOOL_LORE_EASER_EGGS.length)];

				ItemMeta toolMeta = tool.getItemMeta();
				List<String> lore;

				if (toolMeta.hasLore()) {
					lore = toolMeta.getLore();
				} else {
					lore = new ArrayList<>();
				}
				for (String s : selected.split("\n")) {
					lore.add(s);
				}
				toolMeta.setLore(lore);
				tool.setItemMeta(toolMeta);

			}
		}

		player.getInventory().setItem(8, Spleef.getTrackingCompassItem());
		player.getInventory().setItem(0, tool);

		new BukkitRunnable() {
			@Override
			public void run() {
				player.teleport(location);
				new BukkitRunnable() {
					@Override
					public void run() {
						player.teleport(location);
						new BukkitRunnable() {
							@Override
							public void run() {
								player.teleport(location);
							}
						}.runTaskLater(NovaSpleef.getInstance(), 10L);
					}
				}.runTaskLater(NovaSpleef.getInstance(), 10L);
			}
		}.runTaskLater(NovaSpleef.getInstance(), 10L);
	}

	public static final ItemStack getTrackingCompassItem() {
		return new ItemBuilder(Material.COMPASS).setName(ChatColor.GOLD + "" + ChatColor.BOLD + "Tracking compass").build();
	}

	@Override
	public void onStart() {
		if (started) {
			return;
		}
		started = true;

		world.setDifficulty(Difficulty.PEACEFUL);

		SpleefConfigMapModule cfg = (SpleefConfigMapModule) this.getActiveMap().getMapData().getMapModule(SpleefConfigMapModule.class);
		if (cfg == null) {
			Log.fatal("Spleef", "The map " + this.getActiveMap().getMapData().getMapName() + " has no spleef config map module");
			Bukkit.getServer().broadcastMessage(ChatColor.RED + "Spleef has run into an uncorrectable error and has to be ended");
			this.endGame(GameEndReason.ERROR);
			return;
		}
		this.config = cfg;

		List<Player> toTeleport = new ArrayList<Player>();

		Bukkit.getServer().getOnlinePlayers().forEach(player -> {
			if (players.contains(player.getUniqueId())) {
				toTeleport.add(player);
			} else {
				tpToSpectator(player);
			}
		});

		Collections.shuffle(toTeleport);

		List<Location> toUse = new ArrayList<Location>();
		while (toTeleport.size() > 0) {
			if (toUse.size() == 0) {
				for (Location location : getActiveMap().getStarterLocations()) {
					toUse.add(location);
				}

				Collections.shuffle(toUse);
			}

			if (toUse.size() == 0) {
				// Could not load spawn locations. break out to prevent server from crashing
				Log.fatal("Spleef", "The map " + this.getActiveMap().getMapData().getMapName() + " has no spawn locations. Ending game to prevent crash");
				Bukkit.getServer().broadcastMessage(ChatColor.RED + "Spleef has run into an uncorrectable error and has to be ended");
				this.endGame(GameEndReason.ERROR);
				return;
			}

			tpToArena(toTeleport.remove(0), toUse.remove(0));
		}

		BasicTimer startTimer = new BasicTimer(countdownTime, 20L);
		startTimer.addFinishCallback(new Callback() {
			@Override
			public void execute() {
				countdownOver = true;

				Bukkit.getServer().getOnlinePlayers().forEach(player -> VersionIndependantUtils.get().playSound(player, player.getLocation(), VersionIndependantSound.NOTE_PLING, 1F, 1F));

				sendBeginEvent();
			}
		});

		startTimer.addTickCallback(new TickCallback() {
			@Override
			public void execute(long timeLeft) {
				Bukkit.getServer().getOnlinePlayers().forEach(player -> {
					VersionIndependantUtils.get().playSound(player, player.getLocation(), VersionIndependantSound.NOTE_PLING, 1F, 1.3F);
					VersionIndependantUtils.get().sendActionBarMessage(player, LanguageManager.getString(player, "novacore.game.starting_in", timeLeft));
				});
				LanguageManager.broadcast("novacore.game.starting_in", timeLeft);

			}
		});

		startTimer.start();

		// Disable drops
		getActiveMap().getWorld().setGameRuleValue("doTileDrops", "false");

		MapModule decayMapModule = getActiveMap().getMapData().getMapModule(SpleefMapDecay.class);
		if (decayMapModule != null) {
			decayModule = (SpleefMapDecay) decayMapModule;
		}

		gameLoop = new SimpleTask(new Runnable() {
			@Override
			public void run() {
				Bukkit.getServer().getOnlinePlayers().forEach(player -> {
					player.setFoodLevel(20);
					player.setSaturation(20);
				});
			}
		}, 20L);
		gameLoop.start();
	}

	/*
	 * public boolean isBlockDecaying(Material material) { if (decayModule != null)
	 * {
	 * 
	 * return decayModule.getDecaySteps().contains(material); } return false; }
	 */

	public boolean isBlockDecaying(Block block) {
		if (decayModule != null) {
			for (Location location : decayModule.getDecayingBlocks().keySet()) {
				if (LocationUtils.isSameBlock(location, block.getLocation())) {
					return true;
				}
			}
		}

		return false;
	}

	public boolean canBreakBlock(Block block) {
		// Log.debug("this.isBlockDecaying(block)", "" + this.isBlockDecaying(block));
		if (this.isBlockDecaying(block)) {
			return true;
		}

		return config.getBreakableBlocks().contains(block.getType());
	}

	/*
	 * public boolean canBreakBlock(Material material) { return
	 * config.getBreakableBlocks().contains(material) || isBlockDecaying(material);
	 * }
	 */

	@Override
	public void onEnd(GameEndReason reason) {
		if (ended) {
			return;
		}

		Task.tryStopTask(gameLoop);

		for (Location location : getActiveMap().getStarterLocations()) {
			Firework fw = (Firework) location.getWorld().spawnEntity(location, EntityType.FIREWORK);
			FireworkMeta fwm = fw.getFireworkMeta();

			fwm.setPower(2);
			fwm.addEffect(RandomFireworkEffect.randomFireworkEffect());

			fw.setFireworkMeta(fwm);
		}

		for (Player p : Bukkit.getServer().getOnlinePlayers()) {
			p.setHealth(p.getMaxHealth());
			p.setFoodLevel(20);
			PlayerUtils.clearPlayerInventory(p);
			PlayerUtils.resetPlayerXP(p);
			p.setGameMode(GameMode.SPECTATOR);
			VersionIndependantUtils.get().playSound(p, p.getLocation(), VersionIndependantSound.WITHER_DEATH, 1F, 1F);
		}

		ended = true;
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getEntity() instanceof Player) {
			if (!countdownOver) {
				e.setCancelled(true);
			}

			if (e.getDamager().getType() == EntityType.SNOWBALL) {
				return;
			}
		}

		e.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity() instanceof Player) {

			if (!countdownOver) {
				e.setCancelled(true);
			}

			if (e.getCause() == DamageCause.FALL) {
				e.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent e) {
		if (e.getPlayer().getGameMode() == GameMode.CREATIVE) {
			return;
		}

		e.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onBlockFade(BlockFadeEvent e) {
		if (hasStarted()) {
			e.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onInventoryClick(InventoryClickEvent e) {
		if (hasStarted()) {
			if (e.getWhoClicked().getGameMode() != GameMode.CREATIVE) {
				e.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerJoin(PlayerJoinEvent e) {
		if (hasStarted()) {
			if (!players.contains(e.getPlayer().getUniqueId())) {
				tpToSpectator(e.getPlayer());
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerDeath(PlayerDeathEvent e) {
		if (hasStarted()) {
			e.setKeepInventory(true);
			e.getEntity().getInventory().clear();
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerRespawn(PlayerRespawnEvent e) {
		if (hasStarted()) {
			e.setRespawnLocation(getActiveMap().getSpectatorLocation());

			new BukkitRunnable() {
				@Override
				public void run() {
					if (e.getPlayer().isOnline()) {
						tpToSpectator(e.getPlayer());
					}
				}
			}.runTaskLater(NovaSpleef.getInstance(), 1L);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onPlayerDropItem(PlayerDropItemEvent e) {
		if (hasStarted()) {
			if (e.getPlayer().getGameMode() != GameMode.CREATIVE) {
				e.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent e) {
		if (hasStarted()) {
			if (e.getPlayer().getGameMode() != GameMode.CREATIVE) {
				e.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerInteract(PlayerInteractEvent e) {
		if (hasStarted()) {
			if (countdownOver) {
				if (e.getPlayer().getGameMode() != GameMode.CREATIVE) {
					if (players.contains(e.getPlayer().getUniqueId())) {
						if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
							if (canBreakBlock(e.getClickedBlock())) {
								e.getClickedBlock().breakNaturally(NovaCore.getInstance().getVersionIndependentUtils().getItemInMainHand(e.getPlayer()));
							}
						}
					}
				}
			}
		}
	}
}