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
package com.l2jserver.gameserver.model.fishing;

import com.l2jserver.gameserver.model.StatsSet;

/**
 * Class for the Fishing Rod object.
 * @author nonom
 */
public class L2FishingRod {
	private final int _fishingRodId;
	private final int _fishingRodItemId;
	private final int _fishingRodLevel;
	private final String _fishingRodName;
	private final double _fishingRodDamage;
	
	public L2FishingRod(StatsSet set) {
		_fishingRodId = set.getInt("fishingRodId");
		_fishingRodItemId = set.getInt("fishingRodItemId");
		_fishingRodLevel = set.getInt("fishingRodLevel");
		_fishingRodName = set.getString("fishingRodName");
		_fishingRodDamage = set.getDouble("fishingRodDamage");
	}
	
	public int getFishingRodId() {
		return _fishingRodId;
	}
	
	public int getFishingRodItemId() {
		return _fishingRodItemId;
	}
	
	public int getFishingRodLevel() {
		return _fishingRodLevel;
	}
	
	public String getFishingRodItemName() {
		return _fishingRodName;
	}
	
	public double getFishingRodDamage() {
		return _fishingRodDamage;
	}
}