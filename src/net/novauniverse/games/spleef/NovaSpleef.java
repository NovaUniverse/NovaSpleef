package net.novauniverse.games.spleef;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONException;
import org.json.JSONObject;

import net.novauniverse.games.spleef.game.Spleef;
import net.novauniverse.games.spleef.mapmodules.config.SpleefConfigMapModule;
import net.novauniverse.games.spleef.mapmodules.giveprojectiles.SpleefGiveProjectiles;
import net.novauniverse.games.spleef.mapmodules.mapdecay.SpleefMapDecay;
import net.zeeraa.novacore.commons.log.Log;
import net.zeeraa.novacore.commons.utils.JSONFileUtils;
import net.zeeraa.novacore.spigot.abstraction.events.VersionIndependantPlayerAchievementAwardedEvent;
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.GameManager;
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.map.mapmodule.MapModuleManager;
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.mapselector.selectors.guivoteselector.GUIMapVote;
import net.zeeraa.novacore.spigot.gameengine.module.modules.gamelobby.GameLobby;
import net.zeeraa.novacore.spigot.language.LanguageReader;
import net.zeeraa.novacore.spigot.module.ModuleManager;
import net.zeeraa.novacore.spigot.module.modules.compass.CompassTracker;
import net.zeeraa.novacore.spigot.module.modules.compass.event.CompassTrackingEvent;

public class NovaSpleef extends JavaPlugin implements Listener {
	private static NovaSpleef instance;

	public static NovaSpleef getInstance() {
		return instance;
	}

	private boolean allowReconnect;
	private int reconnectTime;
	private boolean enableEasterEggLores;

	private Spleef game;

	public boolean isAllowReconnect() {
		return allowReconnect;
	}

	public int getReconnectTime() {
		return reconnectTime;
	}

	public Spleef getGame() {
		return game;
	}

	public boolean isEnableEasterEggLores() {
		return enableEasterEggLores;
	}

	@Override
	public void onEnable() {
		NovaSpleef.instance = this;

		saveDefaultConfig();

		Log.info(getName(), "Loading language files...");
		try {
			LanguageReader.readFromJar(this.getClass(), "/lang/en-us.json");
		} catch (Exception e) {
			e.printStackTrace();
		}

		allowReconnect = getConfig().getBoolean("allow_reconnect");
		reconnectTime = getConfig().getInt("player_elimination_delay");
		enableEasterEggLores = getConfig().getBoolean("enable_easter_egg_lores");

		// Create files and folders
		File mapFolder = new File(this.getDataFolder().getPath() + File.separator + "Maps");
		File worldFolder = new File(this.getDataFolder().getPath() + File.separator + "Worlds");

		File mapOverrides = new File(this.getDataFolder().getPath() + File.separator + "map_overrides.json");
		if (mapOverrides.exists()) {
			Log.info(getName(), "Trying to read map overrides file");
			try {
				JSONObject mapFiles = JSONFileUtils.readJSONObjectFromFile(mapOverrides);

				boolean relative = mapFiles.getBoolean("relative");

				mapFolder = new File((relative ? this.getDataFolder().getPath() + File.separator : "") + mapFiles.getString("maps_folder"));
				worldFolder = new File((relative ? this.getDataFolder().getPath() + File.separator : "") + mapFiles.getString("worlds_folder"));

				Log.info(getName(), "New paths:");
				Log.info(getName(), "Map folder: " + mapFolder.getAbsolutePath());
				Log.info(getName(), "World folder: " + worldFolder.getAbsolutePath());
			} catch (JSONException | IOException e) {
				e.printStackTrace();
				Log.error(getName(), "Failed to read map overrides from file " + mapOverrides.getAbsolutePath());
			}
		}

		try {
			FileUtils.forceMkdir(getDataFolder());
			FileUtils.forceMkdir(mapFolder);
			FileUtils.forceMkdir(worldFolder);
		} catch (IOException e1) {
			e1.printStackTrace();
			Log.fatal(getName(), "Failed to setup data directory");
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}

		// Register map modules
		MapModuleManager.addMapModule("spleef.config", SpleefConfigMapModule.class);
		MapModuleManager.addMapModule("spleef.add_projectiles", SpleefGiveProjectiles.class);
		MapModuleManager.addMapModule("spleef.map_dacay", SpleefMapDecay.class);
		// MapModuleManager.addMapModule("spleef.projectile_break_blocks",
		// ProjectilesBreakBlocks.class);

		// Enable required modules
		ModuleManager.enable(GameManager.class);
		ModuleManager.enable(GameLobby.class);
		ModuleManager.enable(CompassTracker.class);

		// All players have compasses so strict mode is not needed
		CompassTracker.getInstance().setStrictMode(false);

		// Init game and maps
		this.game = new Spleef();

		GameManager.getInstance().loadGame(game);

		GUIMapVote mapSelector = new GUIMapVote();

		GameManager.getInstance().setMapSelector(mapSelector);

		// Register events
		Bukkit.getServer().getPluginManager().registerEvents(this, this);
		Bukkit.getServer().getPluginManager().registerEvents(mapSelector, this);

		// Read maps
		Log.info(getName(), "Loading maps from " + mapFolder.getPath());
		GameManager.getInstance().readMapsFromFolder(mapFolder, worldFolder);
	}

	@Override
	public void onDisable() {
		Bukkit.getScheduler().cancelTasks(this);
		HandlerList.unregisterAll((Plugin) this);
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onCompassTracking(CompassTrackingEvent e) {
		boolean enabled = false;
		if (GameManager.getInstance().isEnabled()) {
			if (GameManager.getInstance().hasGame()) {
				if (GameManager.getInstance().getActiveGame().hasStarted()) {
					enabled = true;
				}
			}
		}
		e.setCancelled(!enabled);
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onVersionIndependantPlayerAchievementAwarded(VersionIndependantPlayerAchievementAwardedEvent e) {
		e.setCancelled(true);
	}
}