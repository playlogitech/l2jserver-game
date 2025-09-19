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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

import com.l2jserver.gameserver.model.actor.L2Summon;
import com.l2jserver.gameserver.model.holders.SkillHolder;

/**
 * Class hold information about basic pet stats which are same on each level.
 * @author JIV
 */
public class L2PetData {
	private final Map<Integer, L2PetLevelData> _levelStats = new HashMap<>();
	private final List<L2PetSkillLearn> _skills = new ArrayList<>();
	
	private final int _npcId;
	private final int _itemId;
	private int _hungryLimit = 1;
	private int _minLvl = Byte.MAX_VALUE;
	private boolean _syncLevel = false;
	private final List<Integer> _food = new ArrayList<>();
	
	public L2PetData(int npcId, int itemId) {
		_npcId = npcId;
		_itemId = itemId;
	}
	
	/**
	 * @return the npc id representing this pet.
	 */
	public int getNpcId() {
		return _npcId;
	}
	
	/**
	 * @return the item id that could summon this pet.
	 */
	public int getItemId() {
		return _itemId;
	}
	
	/**
	 * @param level the pet's level.
	 * @param data the pet's data.
	 */
	public void addNewStat(int level, L2PetLevelData data) {
		if (_minLvl > level) {
			_minLvl = level;
		}
		_levelStats.put(level, data);
	}
	
	/**
	 * @param petLevel the pet's level.
	 * @return the pet data associated to that pet level.
	 */
	public L2PetLevelData getPetLevelData(int petLevel) {
		return _levelStats.get(petLevel);
	}
	
	/**
	 * @return the pet's hunger limit.
	 */
	public int getHungryLimit() {
		return _hungryLimit;
	}
	
	/**
	 * @return {@code true} if pet synchronizes it's level with his master's
	 */
	public boolean isSyncLevel() {
		return _syncLevel;
	}
	
	/**
	 * @return the pet's minimum level.
	 */
	public int getMinLevel() {
		return _minLvl;
	}
	
	/**
	 * @return the pet's food list.
	 */
	public List<Integer> getFood() {
		return _food;
	}
	
	/**
	 * @param foodId the pet's food Id to add.
	 */
	public void addFood(Integer foodId) {
		_food.add(foodId);
	}
	
	/**
	 * @param limit the hunger limit to set.
	 */
	public void setHungryLimit(int limit) {
		_hungryLimit = limit;
	}
	
	/**
	 * @param val synchronizes level with master or not.
	 */
	public void setSyncLevel(boolean val) {
		_syncLevel = val;
	}
	
	/**
	 * @param skillId the skill Id to add.
	 * @param skillLvl the skill level.
	 * @param petLvl the pet's level when this skill is available.
	 */
	public void addNewSkill(int skillId, int skillLvl, int petLvl) {
		_skills.add(new L2PetSkillLearn(skillId, skillLvl, petLvl));
	}
	
	public int getAvailableLevel(final L2Summon pet, int skillId) {
		final StatsSet parameters = pet.getTemplate().getParameters();
		final int currentStep = (int) Math.floor((pet.getLevel() / 5.) - 11);
		
		return IntStream.rangeClosed(0, currentStep)
			.map(i -> currentStep - i)
			.mapToObj(step -> IntStream.iterate(1, i -> i + 1)
				.mapToObj(skillNum -> parameters.getObject("step" + step + "_skill0" + skillNum, SkillHolder.class))
				.takeWhile(Objects::nonNull)
				.filter(skill -> skill.getSkillId() == skillId)
				.findFirst())
			.filter(Optional::isPresent)
			.map(Optional::get)
			.mapToInt(SkillHolder::getSkillLvl)
			.findFirst()
			.orElse(0);
	}
	
	/**
	 * @return the list with the pet's skill data.
	 */
	public List<L2PetSkillLearn> getAvailableSkills() {
		return _skills;
	}
	
	public static final class L2PetSkillLearn extends SkillHolder {
		private final int _minLevel;
		
		/**
		 * @param id the skill Id.
		 * @param lvl the skill level.
		 * @param minLvl the minimum level when this skill is available.
		 */
		public L2PetSkillLearn(int id, int lvl, int minLvl) {
			super(id, lvl);
			_minLevel = minLvl;
		}
		
		/**
		 * @return the minimum level for the pet to get the skill.
		 */
		public int getMinLevel() {
			return _minLevel;
		}
	}
}
