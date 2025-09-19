/*
 * Copyright © 2004-2025 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2jserver.gameserver.instancemanager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.l2jserver.commons.database.ConnectionFactory;
import com.l2jserver.gameserver.model.L2Spawn;
import com.l2jserver.gameserver.model.entity.Fort;

public final class FortSiegeGuardManager {
	
	private static final Logger LOG = LoggerFactory.getLogger(FortSiegeGuardManager.class);
	
	private final Fort _fort;
	
	private final Map<Integer, List<L2Spawn>> _siegeGuards = new HashMap<>();
	
	public FortSiegeGuardManager(Fort fort) {
		_fort = fort;
	}
	
	public void spawnSiegeGuard() {
		try {
			final List<L2Spawn> monsterList = _siegeGuards.get(getFort().getResidenceId());
			if (monsterList != null) {
				for (L2Spawn spawnDat : monsterList) {
					spawnDat.doSpawn();
					if (spawnDat.getRespawnDelay() == 0) {
						spawnDat.stopRespawn();
					} else {
						spawnDat.startRespawn();
					}
				}
			}
		} catch (Exception e) {
			LOG.warn("Error spawning siege guards for fort {}: {}", getFort().getName(), e.getMessage(), e);
		}
	}
	
	/**
	 * Unspawn guards.
	 */
	public void unspawnSiegeGuard() {
		try {
			final List<L2Spawn> monsterList = _siegeGuards.get(getFort().getResidenceId());
			if (monsterList != null) {
				for (L2Spawn spawnDat : monsterList) {
					spawnDat.stopRespawn();
					if (spawnDat.getLastSpawn() != null) {
						spawnDat.getLastSpawn().doDie(spawnDat.getLastSpawn());
					}
				}
			}
		} catch (Exception e) {
			LOG.warn("Error despawning siege guards for fort {}: {}", getFort().getName(), e.getMessage(), e);
		}
	}
	
	void loadSiegeGuard() {
		_siegeGuards.clear();
		try (var con = ConnectionFactory.getInstance().getConnection();
			var ps = con.prepareStatement("SELECT npcId, x, y, z, heading, respawnDelay FROM fort_siege_guards WHERE fortId = ?")) {
			final int fortId = getFort().getResidenceId();
			ps.setInt(1, fortId);
			try (var rs = ps.executeQuery()) {
				final List<L2Spawn> siegeGuardSpawns = new ArrayList<>();
				while (rs.next()) {
					final L2Spawn spawn = new L2Spawn(rs.getInt("npcId"));
					spawn.setAmount(1);
					spawn.setX(rs.getInt("x"));
					spawn.setY(rs.getInt("y"));
					spawn.setZ(rs.getInt("z"));
					spawn.setHeading(rs.getInt("heading"));
					spawn.setRespawnDelay(rs.getInt("respawnDelay"));
					spawn.setLocationId(0);
					
					siegeGuardSpawns.add(spawn);
				}
				_siegeGuards.put(fortId, siegeGuardSpawns);
			}
		} catch (Exception e) {
			LOG.warn("Error loading siege guard for fort {}: {}", getFort().getName(), e.getMessage(), e);
		}
	}
	
	public Fort getFort() {
		return _fort;
	}
	
	public Map<Integer, List<L2Spawn>> getSiegeGuardSpawn() {
		return _siegeGuards;
	}
}
