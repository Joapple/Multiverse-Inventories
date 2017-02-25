package com.onarandombox.multiverseinventories;

import com.dumptruckman.minecraft.util.Logging;
import com.onarandombox.multiverseinventories.api.Inventories;
import com.onarandombox.multiverseinventories.api.profile.WorldGroupProfile;
import com.onarandombox.multiverseinventories.api.profile.WorldProfile;
import com.onarandombox.multiverseinventories.api.share.Sharables;
import com.onarandombox.multiverseinventories.api.share.Shares;
import com.onarandombox.multiverseinventories.event.MVInventoryHandlingEvent.Cause;
import com.onarandombox.multiverseinventories.util.Perm;

import java.util.List;

import org.bukkit.entity.Player;

/** WorldChange implementation of ShareHandler. */
final class WorldChangeShareHandler extends ShareHandler {
	
	public WorldChangeShareHandler(Inventories inventories, Player player, String fromWorld, String toWorld) {
		super(inventories, player, Cause.WORLD_CHANGE, fromWorld, toWorld, player.getGameMode(), player.getGameMode());
	}
	
	@Override
	public void handle() {
		Logging.finer("=== " + this.event.getPlayer().getName() + " traveling from world: " + this.event.getFromWorld() + " to " + "world: " + this.event.getToWorld() + " ===");
		// Grab the player from the world they're coming from to save their stuff to every time.
		WorldProfile fromWorldProfile = this.inventories.getWorldManager().getWorldProfile(this.event.getFromWorld());
		this.addFromProfile(fromWorldProfile, Sharables.allOf(), fromWorldProfile.getPlayerData(this.event.getPlayer()));
		
		if(Perm.BYPASS_WORLD.hasBypass(this.event.getPlayer(), this.event.getToWorld())) {
			this.hasBypass = true;
			return;
		}
		
		// Get any groups we need to save stuff to.
		List<WorldGroupProfile> fromWorldGroups = this.inventories.getGroupManager().getGroupsForWorld(this.event.getFromWorld());
		for(WorldGroupProfile fromWorldGroup : fromWorldGroups) {
			if(!fromWorldGroup.containsWorld(this.event.getToWorld())) {
				this.addFromProfile(fromWorldGroup, Sharables.fromShares(fromWorldGroup.getShares()), fromWorldGroup.getPlayerData(this.event.getPlayer()));
			} else {
				if(!fromWorldGroup.getShares().isSharing(Sharables.all())) {
					this.addFromProfile(fromWorldGroup, Sharables.fromShares(fromWorldGroup.getShares()), fromWorldGroup.getPlayerData(this.event.getPlayer()));
				}
			}
		}
		if(fromWorldGroups.isEmpty()) {
			Logging.finer("No groups for fromWorld.");
		}
		Shares sharesToUpdate = Sharables.noneOf();
		//Shares optionalSharesToUpdate = Sharables.noneOptional();
		List<WorldGroupProfile> toWorldGroups = this.inventories.getGroupManager().getGroupsForWorld(this.event.getToWorld());
		if(!toWorldGroups.isEmpty()) {
			// Get groups we need to load from
			for(WorldGroupProfile toWorldGroup : toWorldGroups) {
				if(Perm.BYPASS_GROUP.hasBypass(this.event.getPlayer(), toWorldGroup.getName())) {
					this.hasBypass = true;
				} else {
					if(!toWorldGroup.containsWorld(this.event.getFromWorld())) {
						Shares sharesToAdd = Sharables.fromShares(toWorldGroup.getShares());
						this.addToProfile(toWorldGroup, sharesToAdd, toWorldGroup.getPlayerData(this.event.getPlayer()));
						sharesToUpdate.addAll(sharesToAdd);
					} else {
						if(!toWorldGroup.getShares().isSharing(Sharables.all())) {
							Shares sharesToAdd = Sharables.fromShares(toWorldGroup.getShares());
							this.addToProfile(toWorldGroup, sharesToAdd, toWorldGroup.getPlayerData(this.event.getPlayer()));
							sharesToUpdate.addAll(sharesToAdd);
						} else {
							sharesToUpdate = Sharables.allOf();
						}
					}
				}
			}
		} else {
			// Get world we need to load from.
			Logging.finer("No groups for toWorld.");
			WorldProfile toWorldProfile = this.inventories.getWorldManager().getWorldProfile(this.event.getToWorld());
			this.addToProfile(toWorldProfile, Sharables.allOf(), toWorldProfile.getPlayerData(this.event.getPlayer()));
			sharesToUpdate = Sharables.allOf();
		}
		
		// We need to fill in any sharables that are not going to be transferred with what's saved in the world file.
		if(!sharesToUpdate.isSharing(Sharables.all())) {
			sharesToUpdate = Sharables.complimentOf(sharesToUpdate);
			
			// Get world we need to load from.
			Logging.finer(sharesToUpdate.toString() + " are left unhandled, defaulting to toWorld");
			WorldProfile toWorldProfile = this.inventories.getWorldManager().getWorldProfile(this.event.getToWorld());
			this.addToProfile(toWorldProfile, sharesToUpdate, toWorldProfile.getPlayerData(this.event.getPlayer()));
		}
	}
}
