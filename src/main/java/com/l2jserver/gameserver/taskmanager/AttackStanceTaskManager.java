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
package com.l2jserver.gameserver.taskmanager;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.l2jserver.gameserver.ThreadPoolManager;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.instance.L2CubicInstance;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.network.serverpackets.AutoAttackStop;

/**
 * Attack stance task manager.
 * @author Luca Baldi
 * @author Zoey76
 */
public class AttackStanceTaskManager {
	private static final Logger LOG = LoggerFactory.getLogger(AttackStanceTaskManager.class);
	
	private static final Map<L2Character, Long> _attackStanceTasks = new ConcurrentHashMap<>();
	
	protected AttackStanceTaskManager() {
		ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new FightModeScheduler(), 0, 1000);
	}
	
	public void addAttackStanceTask(L2Character actor) {
		if (actor != null) {
			if (actor.isPlayable()) {
				final L2PcInstance player = actor.getActingPlayer();
				for (L2CubicInstance cubic : player.getCubics().values()) {
					if (cubic.getId() != L2CubicInstance.LIFE_CUBIC) {
						cubic.doAction();
					}
				}
			}
			_attackStanceTasks.put(actor, System.currentTimeMillis());
		}
	}
	
	public void removeAttackStanceTask(L2Character actor) {
		if (actor != null) {
			if (actor.isSummon()) {
				actor = actor.getActingPlayer();
			}
			_attackStanceTasks.remove(actor);
		}
	}
	
	/**
	 * Checks for attack stance task.<br>
	 * @param actor the actor
	 * @return {@code true} if the character has an attack stance task, {@code false} otherwise
	 */
	public boolean hasAttackStanceTask(L2Character actor) {
		if (actor != null) {
			if (actor.isSummon()) {
				actor = actor.getActingPlayer();
			}
			return _attackStanceTasks.containsKey(actor);
		}
		return false;
	}
	
	protected static class FightModeScheduler implements Runnable {
		@Override
		public void run() {
			long current = System.currentTimeMillis();
			try {
				final Iterator<Entry<L2Character, Long>> iter = _attackStanceTasks.entrySet().iterator();
				Entry<L2Character, Long> e;
				L2Character actor;
				while (iter.hasNext()) {
					e = iter.next();
					if ((current - e.getValue()) > 15000) {
						actor = e.getKey();
						if (actor != null) {
							actor.broadcastPacket(new AutoAttackStop(actor.getObjectId()));
							actor.getAI().setAutoAttacking(false);
							if (actor.isPlayer() && actor.hasSummon()) {
								actor.getSummon().broadcastPacket(new AutoAttackStop(actor.getSummon().getObjectId()));
							}
						}
						iter.remove();
					}
				}
			} catch (Exception e) {
				// Unless caught here, players remain in attack positions.
				LOG.warn("Error in FightModeScheduler: {}", e.getMessage(), e);
			}
		}
	}
	
	public static AttackStanceTaskManager getInstance() {
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder {
		protected static final AttackStanceTaskManager _instance = new AttackStanceTaskManager();
	}
}
