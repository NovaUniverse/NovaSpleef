package net.novauniverse.games.spleef.mapmodules.mapdecay.material;

import org.bukkit.DyeColor;
import org.bukkit.block.Block;

import net.zeeraa.novacore.spigot.NovaCore;
import net.zeeraa.novacore.spigot.abstraction.enums.ColoredBlockType;

public class ColoredSpleefMaterial implements ISpleefMaterial {
	private DyeColor color;
	private ColoredBlockType type;

	public ColoredSpleefMaterial(DyeColor color, ColoredBlockType type) {
		this.color = color;
		this.type = type;
	}

	@Override
	public void setBlock(Block block) {
		NovaCore.getInstance().getVersionIndependentUtils().setColoredBlock(block, color, type);
	}
}