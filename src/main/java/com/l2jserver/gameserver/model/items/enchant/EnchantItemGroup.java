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
package com.l2jserver.gameserver.model.items.enchant;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.l2jserver.gameserver.model.holders.RangeChanceHolder;

/**
 * @author UnAfraid
 */
public final class EnchantItemGroup {
	private static final Logger LOG = LoggerFactory.getLogger(EnchantItemGroup.class);
	
	private final List<RangeChanceHolder> _chances = new ArrayList<>();
	private final String _name;
	
	public EnchantItemGroup(String name) {
		_name = name;
	}
	
	/**
	 * @return name of current enchant item group.
	 */
	public String getName() {
		return _name;
	}
	
	/**
	 * @param holder
	 */
	public void addChance(RangeChanceHolder holder) {
		_chances.add(holder);
	}
	
	/**
	 * @param index
	 * @return chance for success rate for current enchant item group.
	 */
	public double getChance(int index) {
		if (!_chances.isEmpty()) {
			for (RangeChanceHolder holder : _chances) {
				if ((holder.getMin() <= index) && (holder.getMax() >= index)) {
					return holder.getChance();
				}
			}
			LOG.warn("Couldn't match proper chance for item group: {}", _name, new IllegalStateException());
			return _chances.getLast().getChance();
		}
		LOG.warn("Item group: {} doesn't have any chances!", _name);
		return -1;
	}
}
