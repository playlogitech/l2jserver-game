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
package com.l2jserver.gameserver;

import java.util.Calendar;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.l2jserver.gameserver.instancemanager.DayNightSpawnManager;
import com.l2jserver.gameserver.model.actor.L2Character;

/**
 * Game Time controller class.
 * @author Forsaiken
 */
public final class GameTimeController extends Thread {
	private static final Logger LOG = LoggerFactory.getLogger(GameTimeController.class);
	
	public static final int TICKS_PER_SECOND = 10; // not able to change this without checking through code
	public static final int MILLIS_IN_TICK = 1000 / TICKS_PER_SECOND;
	public static final int IG_DAYS_PER_DAY = 6;
	public static final int MILLIS_PER_IG_DAY = (3600000 * 24) / IG_DAYS_PER_DAY;
	public static final int SECONDS_PER_IG_DAY = MILLIS_PER_IG_DAY / 1000;
	public static final int MINUTES_PER_IG_DAY = SECONDS_PER_IG_DAY / 60;
	public static final int TICKS_PER_IG_DAY = SECONDS_PER_IG_DAY * TICKS_PER_SECOND;
	public static final int TICKS_PER_IG_HOUR = TICKS_PER_IG_DAY / 24;
	public static final int TICKS_PER_IG_MINUTE = TICKS_PER_IG_HOUR / 60;
	public static final int TICKS_SUN_STATE_CHANGE = TICKS_PER_IG_DAY / 4;
	
	private static GameTimeController _instance;
	
	private final Set<L2Character> _movingObjects = ConcurrentHashMap.newKeySet();
	private final long _referenceTime;
	
	private GameTimeController() {
		super("GameTimeController");
		super.setDaemon(true);
		super.setPriority(MAX_PRIORITY);
		
		final Calendar c = Calendar.getInstance();
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		_referenceTime = c.getTimeInMillis();
		
		super.start();
	}
	
	public static void init() {
		_instance = new GameTimeController();
	}
	
	public int getGameTime() {
		return (getGameTicks() % TICKS_PER_IG_DAY) / TICKS_PER_IG_MINUTE;
	}
	
	public int getGameHour() {
		return getGameTime() / 60;
	}
	
	public int getGameMinute() {
		return getGameTime() % 60;
	}
	
	public boolean isNight() {
		return getGameHour() < 6;
	}
	
	/**
	 * The true GameTime tick. Directly taken from current time. This represents the tick of the time.
	 * @return
	 */
	public int getGameTicks() {
		return (int) ((System.currentTimeMillis() - _referenceTime) / MILLIS_IN_TICK);
	}
	
	/**
	 * Add a L2Character to movingObjects of GameTimeController.
	 * @param cha The L2Character to add to movingObjects of GameTimeController
	 */
	public void registerMovingObject(final L2Character cha) {
		if (cha == null) {
			return;
		}
		
		_movingObjects.add(cha);
	}
	
	/**
	 * Move all L2Characters contained in movingObjects of GameTimeController.<BR>
	 * <B><U> Concept</U> :</B><BR>
	 * All L2Character in movement are identified in <B>movingObjects</B> of GameTimeController.<BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <ul>
	 * <li>Update the position of each L2Character</li>
	 * <li>If movement is finished, the L2Character is removed from movingObjects</li>
	 * <li>Create a task to update the _knownObject and _knowPlayers of each L2Character that finished its movement and of their already known L2Object then notify AI with EVT_ARRIVED</li>
	 * </ul>
	 */
	private void moveObjects() {
		_movingObjects.removeIf(L2Character::updatePosition);
	}
	
	public void stopTimer() {
		super.interrupt();
		LOG.info("Stopping {}", getClass().getSimpleName());
	}
	
	@Override
	public void run() {
		LOG.debug("{}: Started.", getClass().getSimpleName());
		
		long nextTickTime, sleepTime;
		boolean isNight = isNight();
		
		if (isNight) {
			ThreadPoolManager.getInstance().executeAi(() -> DayNightSpawnManager.getInstance().notifyChangeMode());
		}
		
		while (true) {
			nextTickTime = System.currentTimeMillis() + MILLIS_IN_TICK;
			
			try {
				moveObjects();
			} catch (final Throwable e) {
				LOG.warn("Unable to move objects!", e);
			}
			
			sleepTime = nextTickTime - System.currentTimeMillis();
			if (sleepTime > 0) {
				try {
					Thread.sleep(sleepTime);
				} catch (final InterruptedException e) {
					
				}
			}
			
			if (isNight() != isNight) {
				isNight = !isNight;
				
				ThreadPoolManager.getInstance().executeAi(() -> DayNightSpawnManager.getInstance().notifyChangeMode());
			}
		}
	}
	
	public static GameTimeController getInstance() {
		return _instance;
	}
}