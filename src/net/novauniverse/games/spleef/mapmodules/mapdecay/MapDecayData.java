package net.novauniverse.games.spleef.mapmodules.mapdecay;

import org.bukkit.Material;

public class MapDecayData {
	private int stage;
	private Material lastMaterial;
	
	public MapDecayData(int stage, Material lastMaterial) {
		this.stage = stage;
		this.lastMaterial = lastMaterial;
	}
	
	public int getStage() {
		return stage;
	}
	
	public Material getLastMaterial() {
		return lastMaterial;
	}
	
	public void setStage(int stage) {
		this.stage = stage;
	}
	
	public void setLastMaterial(Material lastMaterial) {
		this.lastMaterial = lastMaterial;
	}
}
