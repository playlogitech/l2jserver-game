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
package com.l2jserver.gameserver.model;

import java.util.Set;
import java.util.TreeMap;

import com.l2jserver.gameserver.data.xml.impl.EnchantSkillGroupsData;
import com.l2jserver.gameserver.model.L2EnchantSkillGroup.EnchantSkillHolder;

public final class L2EnchantSkillLearn {
	private final int _id;
	private final int _baseLvl;
	private final TreeMap<Integer, Integer> _enchantRoutes = new TreeMap<>();
	
	public L2EnchantSkillLearn(int id, int baseLvl) {
		_id = id;
		_baseLvl = baseLvl;
	}
	
	public void addNewEnchantRoute(int route, int group) {
		_enchantRoutes.put(route, group);
	}
	
	public int getId() {
		return _id;
	}
	
	public int getBaseLevel() {
		return _baseLvl;
	}
	
	public static int getEnchantRoute(int level) {
		return (int) Math.floor(level / 100.0);
	}
	
	public static int getEnchantIndex(int level) {
		return (level % 100) - 1;
	}
	
	public L2EnchantSkillGroup getFirstRouteGroup() {
		return EnchantSkillGroupsData.getInstance().getEnchantSkillGroupById(_enchantRoutes.firstEntry().getValue());
	}
	
	public Set<Integer> getAllRoutes() {
		return _enchantRoutes.keySet();
	}
	
	public int getMinSkillLevel(int level) {
		if ((level % 100) == 1) {
			return _baseLvl;
		}
		return level - 1;
	}
	
	public boolean isMaxEnchant(int level) {
		int route = getEnchantRoute(level);
		if ((route < 1) || !_enchantRoutes.containsKey(route)) {
			return false;
		}
		int index = getEnchantIndex(level);
		
		return (index + 1) >= EnchantSkillGroupsData.getInstance().getEnchantSkillGroupById(_enchantRoutes.get(route)).getEnchantGroupDetails().size();
	}
	
	public EnchantSkillHolder getEnchantSkillHolder(int level) {
		int route = getEnchantRoute(level);
		if ((route < 1) || !_enchantRoutes.containsKey(route)) {
			return null;
		}
		int index = getEnchantIndex(level);
		L2EnchantSkillGroup group = EnchantSkillGroupsData.getInstance().getEnchantSkillGroupById(_enchantRoutes.get(route));
		
		if (index < 0) {
			return group.getEnchantGroupDetails().get(0);
		} else if (index >= group.getEnchantGroupDetails().size()) {
			return group.getEnchantGroupDetails().get(EnchantSkillGroupsData.getInstance().getEnchantSkillGroupById(_enchantRoutes.get(route)).getEnchantGroupDetails().size() - 1);
		}
		return group.getEnchantGroupDetails().get(index);
	}
}