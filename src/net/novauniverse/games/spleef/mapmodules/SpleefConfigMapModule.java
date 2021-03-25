package net.novauniverse.games.spleef.mapmodules;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.json.JSONArray;
import org.json.JSONObject;

import net.zeeraa.novacore.commons.log.Log;
import net.zeeraa.novacore.spigot.module.modules.game.map.mapmodule.MapModule;

public class SpleefConfigMapModule extends MapModule {
	private List<Material> breakableBlocks;
	
	public SpleefConfigMapModule(JSONObject json) {
		super(json);

		breakableBlocks = new ArrayList<>();
		if (json.has("breakable_blocks")) {
			JSONArray breakableBlocksJson = json.getJSONArray("breakable_blocks");
			for (int i = 0; i < breakableBlocksJson.length(); i++) {
				String materialName = breakableBlocksJson.getString(i);
				try {
					Material material = Material.valueOf(materialName);

					breakableBlocks.add(material);
				} catch (Exception e) {
					Log.error("SpleefMapModule", "Invalid material: " + materialName + ". " + e.getClass().getName());
				}
			}

		} else {
			Log.warn("SpleefMapModule", "Missing array: breakable_blocks in map data. Using default of SNOW_BLOCK");
			breakableBlocks.add(Material.SNOW_BLOCK);
		}

	}

	public List<Material> getBreakableBlocks() {
		return breakableBlocks;
	}
}