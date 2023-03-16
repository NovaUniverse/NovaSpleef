package net.novauniverse.games.spleef.modules.snowballvote;

import java.util.UUID;

public class VoteData {
	private final UUID uuid;
	private boolean wantSnowballs;
	
	public VoteData(UUID uuid, boolean wantsSnowballs) {
		this.uuid = uuid;
		this.wantSnowballs = wantsSnowballs;
	}
	
	public UUID getUuid() {
		return uuid;
	}
	
	public void setWantSnowballs(boolean wantSnowballs) {
		this.wantSnowballs = wantSnowballs;
	}
	
	public boolean isWantSnowballs() {
		return wantSnowballs;
	}
}