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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.l2jserver.commons.database.ConnectionFactory;
import com.l2jserver.gameserver.model.L2World;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.entity.Couple;

/**
 * Couple Manager.
 * @author evill33t
 */
public final class CoupleManager {
	
	private static final Logger LOG = LoggerFactory.getLogger(CoupleManager.class);
	
	private final List<Couple> _couples = new CopyOnWriteArrayList<>();
	
	private CoupleManager() {
		load();
	}
	
	public void reload() {
		_couples.clear();
		load();
	}
	
	private void load() {
		try (var con = ConnectionFactory.getInstance().getConnection();
			var ps = con.createStatement();
			var rs = ps.executeQuery("SELECT id FROM mods_wedding ORDER BY id")) {
			while (rs.next()) {
				getCouples().add(new Couple(rs.getInt("id")));
			}
			LOG.info("Loaded: {} couples(s)", getCouples().size());
		} catch (Exception e) {
			LOG.error("Exception: CoupleManager.load(): {}", e.getMessage(), e);
		}
	}
	
	public Couple getCouple(int coupleId) {
		int index = getCoupleIndex(coupleId);
		if (index >= 0) {
			return getCouples().get(index);
		}
		return null;
	}
	
	public void createCouple(L2PcInstance player1, L2PcInstance player2) {
		if ((player1 != null) && (player2 != null)) {
			if ((player1.getPartnerId() == 0) && (player2.getPartnerId() == 0)) {
				int player1id = player1.getObjectId();
				int player2id = player2.getObjectId();
				
				Couple couple = new Couple(player1, player2);
				getCouples().add(couple);
				player1.setPartnerId(player2id);
				player2.setPartnerId(player1id);
				player1.setCoupleId(couple.getId());
				player2.setCoupleId(couple.getId());
			}
		}
	}
	
	public void deleteCouple(int coupleId) {
		int index = getCoupleIndex(coupleId);
		Couple couple = getCouples().get(index);
		if (couple != null) {
			L2PcInstance player1 = L2World.getInstance().getPlayer(couple.getPlayer1Id());
			L2PcInstance player2 = L2World.getInstance().getPlayer(couple.getPlayer2Id());
			if (player1 != null) {
				player1.setPartnerId(0);
				player1.setMarried(false);
				player1.setCoupleId(0);
				
			}
			if (player2 != null) {
				player2.setPartnerId(0);
				player2.setMarried(false);
				player2.setCoupleId(0);
				
			}
			couple.divorce();
			getCouples().remove(index);
		}
	}
	
	public int getCoupleIndex(int coupleId) {
		int i = 0;
		for (Couple temp : getCouples()) {
			if ((temp != null) && (temp.getId() == coupleId)) {
				return i;
			}
			i++;
		}
		return -1;
	}
	
	public List<Couple> getCouples() {
		return _couples;
	}
	
	public static CoupleManager getInstance() {
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder {
		protected static final CoupleManager _instance = new CoupleManager();
	}
}
