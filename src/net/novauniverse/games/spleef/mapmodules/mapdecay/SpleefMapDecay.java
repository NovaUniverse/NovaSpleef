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
import org.json.JSONArray;
import org.json.JSONObject;

import net.novauniverse.games.spleef.mapmodules.mapdecay.material.ColoredSpleefMaterial;
import net.novauniverse.games.spleef.mapmodules.mapdecay.material.ISpleefMaterial;
import net.novauniverse.games.spleef.mapmodules.mapdecay.material.NormalSpleefMaterial;
import net.zeeraa.novacore.commons.utils.RandomGenerator;
import net.zeeraa.novacore.spigot.abstraction.enums.ColoredBlockType;
import net.zeeraa.novacore.spigot.abstraction.enums.VersionIndependantSound;
import net.zeeraa.novacore.spigot.language.LanguageManager;
import net.zeeraa.novacore.spigot.module.modules.game.Game;
import net.zeeraa.novacore.spigot.module.modules.game.GameManager;
import net.zeeraa.novacore.spigot.module.modules.game.map.mapmodule.MapModule;
import net.zeeraa.novacore.spigot.module.modules.game.triggers.DelayedGameTrigger;
import net.zeeraa.novacore.spigot.module.modules.game.triggers.GameTrigger;
import net.zeeraa.novacore.spigot.module.modules.game.triggers.RepeatingGameTrigger;
import net.zeeraa.novacore.spigot.module.modules.game.triggers.TriggerCallback;
import net.zeeraa.novacore.spigot.module.modules.game.triggers.TriggerFlag;
import net.zeeraa.novacore.spigot.utils.LocationUtils;
import net.zeeraa.novacore.spigot.utils.VectorArea;

public class SpleefMapDecay extends MapModule {
	private Random random;

	private List<VectorArea> floors;
	private List<Material> floorBlocks;

	private List<ISpleefMaterial> decaySteps;

	private RepeatingGameTrigger trigger;
	private DelayedGameTrigger startTrigger;

	private int beginAfter;
	private int attemptsPerFloor;
	private int extraAttempsOnFail;

	private Map<Location, MapDecayData> decayingBlocks;

	public SpleefMapDecay(JSONObject json) {
		super(json);

		floors = new ArrayList<>();
		floorBlocks = new ArrayList<>();
		decaySteps = new ArrayList<>();
		decayingBlocks = new HashMap<>();

		random = new Random();

		JSONArray floorsJson = json.getJSONArray("floors");
		for (int i = 0; i < floorsJson.length(); i++) {
			JSONObject floor = floorsJson.getJSONObject(i);

			VectorArea area = new VectorArea(floor.getInt("x1"), floor.getInt("y1"), floor.getInt("z1"), floor.getInt("x2"), floor.getInt("y2"), floor.getInt("z2"));

			floors.add(area);
		}

		JSONArray floorBlocksJson = json.getJSONArray("floor_blocks");
		for (int i = 0; i < floorBlocksJson.length(); i++) {
			floorBlocks.add(Material.valueOf(floorBlocksJson.getString(i)));
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
		extraAttempsOnFail = json.getInt("extra_attemps_on_fail");

		trigger = new RepeatingGameTrigger("novauniverse.spleef.map_decay", 20L, 20L, new TriggerCallback() {
			@Override
			public void run(GameTrigger trigger2, TriggerFlag reason) {
				decay();
			}
		});

		trigger.addFlag(TriggerFlag.STOP_ON_GAME_END);

		startTrigger = new DelayedGameTrigger("novauniverse.spleef.begin_map_decay", beginAfter * 20L, new TriggerCallback() {
			@Override
			public void run(GameTrigger trigger2, TriggerFlag reason) {
				Bukkit.getServer().getOnlinePlayers().forEach(player -> VersionIndependantSound.NOTE_PLING.play(player, player.getLocation(), 1F, 1F));

				LanguageManager.broadcast("spleef.begin_decay");

				trigger.start();
			}
		});

		startTrigger.addFlag(TriggerFlag.STOP_ON_GAME_END);
		startTrigger.addFlag(TriggerFlag.RUN_ONLY_ONCE);
	}

	public Random getRandom() {
		return random;
	}

	public List<Material> getFloorBlocks() {
		return floorBlocks;
	}

	public List<VectorArea> getFloors() {
		return floors;
	}

	public RepeatingGameTrigger getTrigger() {
		return trigger;
	}

	public int getExtraAttempsOnFail() {
		return extraAttempsOnFail;
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

		floors.forEach(floor -> {
			for (int i = 0; i < attemptsPerFloor; i++) {
				for (int j = 0; j < extraAttempsOnFail; j++) {
					Location location = LocationUtils.getLocation(world, floor.getRandomVectorWithin(random));

					if (tryDecay(location)) {
						break;
					}
				}
			}
		});

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
		if (floorBlocks.contains(location.getBlock().getType())) {
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