package net.novauniverse.games.spleef.mapmodules.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.json.JSONArray;
import org.json.JSONObject;

import net.zeeraa.novacore.commons.log.Log;
import net.zeeraa.novacore.spigot.module.modules.game.map.mapmodule.MapModule;
import net.zeeraa.novacore.spigot.utils.ItemBuilder;

public class SpleefConfigMapModule extends MapModule {
	private List<Material> breakableBlocks;

	private Material toolMaterial;
	private Map<Enchantment, Integer> toolEnchants;

	public SpleefConfigMapModule(JSONObject json) {
		super(json);

		toolEnchants = new HashMap<>();

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

		if (json.has("tool")) {
			JSONObject tool = json.getJSONObject("tool");

			String materialName = tool.getString("material");
			try {
				toolMaterial = Material.valueOf(materialName);
			} catch (Exception e) {
				Log.error("SpleefMapModule", "Invalid tool material: " + materialName + ". " + e.getClass().getName());
			}

			if (json.has("enchants")) {
				JSONObject enchants = new JSONObject();

				for (String key : enchants.keySet()) {
					try {
						int level = enchants.getInt(key);

						toolEnchants.put(Enchantment.getByName(key), level);
					} catch (Exception e) {
						Log.error("SpleefMapModule", "Invalid enchantment: " + key + ". " + e.getClass().getName());
					}
				}
			} else {
				toolEnchants.put(Enchantment.DIG_SPEED, 1000);
			}
		} else {
			toolMaterial = Material.DIAMOND_SPADE;
			toolEnchants.put(Enchantment.DIG_SPEED, 1000);
		}
	}

	public Map<Enchantment, Integer> getToolEnchants() {
		return toolEnchants;
	}

	public Material getToolMaterial() {
		return toolMaterial;
	}

	public List<Material> getBreakableBlocks() {
		return breakableBlocks;
	}

	public ItemStack getToolItemStack() {
		ItemBuilder builder = new ItemBuilder(toolMaterial);

		for (Enchantment enchantment : toolEnchants.keySet()) {
			builder.addEnchant(enchantment, toolEnchants.get(enchantment), true);
		}

		builder.setUnbreakable(true);

		return builder.build();
	}
}