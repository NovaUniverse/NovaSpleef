package net.novauniverse.games.spleef.mapmodules.mapdecay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.json.JSONArray;
import org.json.JSONObject;

import net.novauniverse.games.spleef.mapmodules.mapdecay.material.ColoredSpleefMaterial;
import net.novauniverse.games.spleef.mapmodules.mapdecay.material.ISpleefMaterial;
import net.novauniverse.games.spleef.mapmodules.mapdecay.material.NormalSpleefMaterial;
import net.zeeraa.novacore.commons.log.Log;
import net.zeeraa.novacore.commons.utils.RandomGenerator;
import net.zeeraa.novacore.spigot.abstraction.enums.ColoredBlockType;
import net.zeeraa.novacore.spigot.abstraction.enums.VersionIndependentSound;
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.Game;
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.GameManager;
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.map.mapmodule.MapModule;
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.triggers.DelayedGameTrigger;
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.triggers.GameTrigger;
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.triggers.RepeatingGameTrigger;
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.triggers.TriggerCallback;
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.triggers.TriggerFlag;
import net.zeeraa.novacore.spigot.language.LanguageManager;
import net.zeeraa.novacore.spigot.utils.VectorArea;

public class SpleefMapDecay extends MapModule implements Listener {
	private Random random;

	private List<VectorArea> floors;
	private List<Material> floorBlockMaterials;

	private List<ISpleefMaterial> decaySteps;

	private List<List<Location>> floorBlocks;

	public List<List<Location>> getFloorBlocks() {
		return floorBlocks;
	}

	private RepeatingGameTrigger trigger;
	private DelayedGameTrigger startTrigger;

	private int beginAfter;
	private int attemptsPerFloor;

	private boolean playerDecay;

	private Map<Location, MapDecayData> decayingBlocks;

	private DecayMode mode;

	public SpleefMapDecay(JSONObject json) {
		super(json);

		floors = new ArrayList<>();
		floorBlockMaterials = new ArrayList<>();
		decaySteps = new ArrayList<>();
		decayingBlocks = new HashMap<>();
		floorBlocks = new ArrayList<>();

		this.mode = DecayMode.RANDOM;

		random = new Random();

		JSONArray floorsJson = json.getJSONArray("floors");
		for (int i = 0; i < floorsJson.length(); i++) {
			JSONObject floor = floorsJson.getJSONObject(i);

			VectorArea area = new VectorArea(floor.getInt("x1"), floor.getInt("y1"), floor.getInt("z1"), floor.getInt("x2"), floor.getInt("y2"), floor.getInt("z2"));

			floors.add(area);
		}

		if (json.has("player_decay")) {
			playerDecay = json.getBoolean("player_decay");
		}

		if (json.has("mode")) {
			mode = DecayMode.valueOf(json.getString("mode"));
		}
		Log.info("SpleefMapDecay", "Mode: " + mode.name());

		JSONArray floorBlocksJson = json.getJSONArray("floor_blocks");
		for (int i = 0; i < floorBlocksJson.length(); i++) {
			floorBlockMaterials.add(Material.valueOf(floorBlocksJson.getString(i)));
		}

		JSONArray decayStepsJson = json.getJSONArray("dacay_steps");
		for (int i = 0; i < decayStepsJson.length(); i++) {
			String material = decayStepsJson.getString(i);
			if (material.startsWith("COLOREDBLOCK:")) {
				String[] data = material.split(":");
				ColoredBlockType type = ColoredBlockType.valueOf(data[1]);
				DyeColor color = DyeColor.valueOf(data[2]);

				decaySteps.add(new ColoredSpleefMaterial(color, type));
			} else {
				decaySteps.add(new NormalSpleefMaterial(Material.valueOf(material)));
			}
		}

		beginAfter = json.getInt("begin_after");
		attemptsPerFloor = json.getInt("attempts_per_floor");

		trigger = new RepeatingGameTrigger("novauniverse.spleef.map_decay", 20L, 20L, new TriggerCallback() {
			@Override
			public void run(GameTrigger trigger2, TriggerFlag reason) {
				decay();
			}
		});

		trigger.addFlag(TriggerFlag.STOP_ON_GAME_END);
		trigger.addFlag(TriggerFlag.DISABLE_LOGGING);

		startTrigger = new DelayedGameTrigger("novauniverse.spleef.begin_map_decay", beginAfter * 20L, new TriggerCallback() {
			@Override
			public void run(GameTrigger trigger2, TriggerFlag reason) {
				Bukkit.getServer().getOnlinePlayers().forEach(player -> VersionIndependentSound.NOTE_PLING.play(player, player.getLocation(), 1F, 1F));

				LanguageManager.broadcast("spleef.begin_decay");

				trigger.start();
			}
		});

		startTrigger.addFlag(TriggerFlag.STOP_ON_GAME_END);
		startTrigger.addFlag(TriggerFlag.RUN_ONLY_ONCE);
	}

	public boolean isPlayerDecay() {
		return playerDecay;
	}

	public Random getRandom() {
		return random;
	}

	public List<Material> getFloorBlockMaterials() {
		return floorBlockMaterials;
	}

	public List<VectorArea> getFloors() {
		return floors;
	}

	public RepeatingGameTrigger getTrigger() {
		return trigger;
	}

	public int getAttemptsPerFloor() {
		return attemptsPerFloor;
	}

	public Map<Location, MapDecayData> getDecayingBlocks() {
		return decayingBlocks;
	}

	public List<ISpleefMaterial> getDecaySteps() {
		return decaySteps;
	}

	public DelayedGameTrigger getStartTrigger() {
		return startTrigger;
	}

	public int getBeginAfter() {
		return beginAfter;
	}

	@Override
	public void onGameStart(Game game) {
		game.addTrigger(trigger);
		game.addTrigger(startTrigger);

		startTrigger.start();

		Log.debug("SpleefMapDecay", "Scanning for floor blocks");
		floors.forEach(floor -> {
			List<Location> locations = new ArrayList<Location>();
			for (int x = floor.getPosition1().getBlockX(); x <= floor.getPosition2().getBlockX(); x++) {
				for (int y = floor.getPosition1().getBlockY(); y <= floor.getPosition2().getBlockY(); y++) {
					for (int z = floor.getPosition1().getBlockZ(); z <= floor.getPosition2().getBlockZ(); z++) {
						Block block = game.getWorld().getBlockAt(x, y, z);
						if (floorBlockMaterials.contains(block.getType())) {
							locations.add(block.getLocation());
						}
					}
				}
			}
			Log.debug("SpleefMapDecay", "Floor " + (floorBlocks.size() + 1) + " has " + locations.size() + " blocks");
			floorBlocks.add(locations);
		});
		Log.debug("SpleefMapDecay", floorBlocks.size() + " floors scanned");

		Bukkit.getServer().getPluginManager().registerEvents(this, game.getPlugin());
	}

	@Override
	public void onGameEnd(Game game) {
		HandlerList.unregisterAll(this);
	}
	
	public void handleBlockBreak(Block block) {
		//Log.trace("Block break on " + block.getLocation());
		for (List<Location> singleFloor : floorBlocks) {
			if (singleFloor.contains(block.getLocation())) {
				//Log.trace("Removing block " + block.getLocation() + " from floor list");
				singleFloor.remove(block.getLocation());
				break;
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent e) {
		handleBlockBreak(e.getBlock());
	}

	private Location getGroundLocation(Location location) {
		Location testLocation = location.clone();
		while (testLocation.getBlock().getType() == Material.AIR && testLocation.getBlockY() > 1) {
			testLocation.add(0, -1, 0);
		}

		return testLocation;
	}

	public void decay() {
		World world = GameManager.getInstance().getActiveGame().getWorld();

		if (mode == DecayMode.RANDOM) {
			floorBlocks.forEach(floor -> {
				for (int i = 0; i < attemptsPerFloor; i++) {
					if (floor.size() > 0) {
						Location location = floor.get(random.nextInt(floor.size()));
						floor.remove(location);
						tryDecay(location);
					}
				}
			});
		} else {
			for (List<Location> floor : floorBlocks) {
				if (floor.size() == 0) {
					continue;
				}

				for (int i = 0; i < attemptsPerFloor; i++) {
					if (floor.size() > 0) {
						Location location = floor.get(random.nextInt(floor.size()));
						floor.remove(location);
						tryDecay(location);
					}
				}
				break;
			}
		}

		if (playerDecay) {
			Bukkit.getServer().getOnlinePlayers().forEach(player -> {
				if (GameManager.getInstance().getActiveGame().getPlayers().contains(player.getUniqueId())) {
					/*
					 * Decay rate under players can be changed here by modifying the random numbers
					 * and the comparing number
					 */
					if (RandomGenerator.generate(0, 10) < 7) {
						if (player.getWorld().getUID().toString().equals(world.getUID().toString())) {
							for (int i = 0; i < 3; i++) {
								Location groundLocation = getGroundLocation(player.getLocation().clone().add(RandomGenerator.generateDouble(-1, 1), 0, RandomGenerator.generateDouble(-1, 1)));

								floors.forEach(floor -> {
									if (floor.isInsideBlock(groundLocation.toVector())) {
										this.tryDecay(groundLocation);
									}
								});
							}
						}
					}
				}
			});
		}

		List<Location> toRemove = new ArrayList<Location>();

		final int stageSize = decaySteps.size();

		decayingBlocks.keySet().forEach(location -> {
			MapDecayData data = decayingBlocks.get(location);

			if (location.getBlock().getType() != data.getLastMaterial()) {
				// Block has been broken
				toRemove.add(location);
				return;
			}

			if (data.getStage() >= stageSize) {
				toRemove.add(location);
				location.getBlock().breakNaturally();
				return;
			}

			// Material newMaterial = decaySteps.get(data.getStage());
			// location.getBlock().setType(newMaterial);
			decaySteps.get(data.getStage()).setBlock(location.getBlock());

			data.setLastMaterial(location.getBlock().getType());
			data.setStage(data.getStage() + 1);
		});

		toRemove.forEach(location -> decayingBlocks.remove(location));
	}

	private boolean tryDecay(Location location) {
		if (floorBlockMaterials.contains(location.getBlock().getType())) {
			for (Location alreadyDecaying : decayingBlocks.keySet()) {
				if (compareLocation(alreadyDecaying, location)) {
					return false;
				}
			}

			decayingBlocks.put(location, new MapDecayData(0, location.getBlock().getType()));
			return true;
		}
		return false;
	}

	private boolean compareLocation(Location location1, Location location2) {
		return location1.getBlockX() == location2.getBlockX() && location1.getBlockY() == location2.getBlockY() && location1.getBlockZ() == location2.getBlockZ();
	}
}