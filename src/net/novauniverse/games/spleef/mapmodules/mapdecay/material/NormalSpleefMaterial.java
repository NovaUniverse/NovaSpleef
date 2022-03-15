package net.novauniverse.games.spleef.mapmodules.mapdecay.material;

import org.bukkit.Material;
import org.bukkit.block.Block;

public class NormalSpleefMaterial implements ISpleefMaterial{
	private Material material;
	
	public NormalSpleefMaterial(Material material) {
		this.material = material;
	}

	@Override
	public void setBlock(Block block) {
		block.setType(material);
	}
}