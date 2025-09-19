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
package com.l2jserver.gameserver.model.zone.type;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.l2jserver.commons.util.Rnd;
import com.l2jserver.gameserver.ThreadPoolManager;
import com.l2jserver.gameserver.datatables.SkillData;
import com.l2jserver.gameserver.enums.InstanceType;
import com.l2jserver.gameserver.instancemanager.ZoneManager;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.skills.Skill;
import com.l2jserver.gameserver.model.zone.AbstractZoneSettings;
import com.l2jserver.gameserver.model.zone.L2ZoneType;
import com.l2jserver.gameserver.model.zone.TaskZoneSettings;
import com.l2jserver.gameserver.model.zone.ZoneId;
import com.l2jserver.gameserver.network.serverpackets.EtcStatusUpdate;

/**
 * another type of damage zone with skills
 * @author kerberos
 */
public class L2EffectZone extends L2ZoneType {
	private static final Logger LOG = LoggerFactory.getLogger(L2EffectZone.class);
	
	private int _chance;
	private int _initialDelay;
	private int _reuse;
	private boolean _bypassConditions;
	private boolean _isShowDangerIcon;
	protected volatile Map<Integer, Integer> _skills;
	
	public L2EffectZone(int id) {
		super(id);
		_chance = 100;
		_initialDelay = 0;
		_reuse = 30000;
		setTargetType(InstanceType.L2Playable); // default only playable
		_bypassConditions = false;
		_isShowDangerIcon = true;
		AbstractZoneSettings settings = ZoneManager.getSettings(getName());
		if (settings == null) {
			settings = new TaskZoneSettings();
		}
		setSettings(settings);
	}
	
	@Override
	public TaskZoneSettings getSettings() {
		return (TaskZoneSettings) super.getSettings();
	}
	
	@Override
	public void setParameter(String name, String value) {
		switch (name) {
			case "chance" -> _chance = Integer.parseInt(value);
			case "initialDelay" -> _initialDelay = Integer.parseInt(value);
			case "reuse" -> _reuse = Integer.parseInt(value);
			case "bypassSkillConditions" -> _bypassConditions = Boolean.parseBoolean(value);
			case "maxDynamicSkillCount" -> _skills = new ConcurrentHashMap<>(Integer.parseInt(value));
			case "skillIdLvl" -> {
				String[] propertySplit = value.split(";");
				_skills = new ConcurrentHashMap<>(propertySplit.length);
				for (String skill : propertySplit) {
					String[] skillSplit = skill.split("-");
					if (skillSplit.length != 2) {
						LOG.warn("Invalid config property -> skillsIdLvl \"{}\"", skill);
					} else {
						try {
							_skills.put(Integer.parseInt(skillSplit[0]), Integer.parseInt(skillSplit[1]));
						} catch (NumberFormatException nfe) {
							if (!skill.isEmpty()) {
								LOG.warn("Invalid config property -> skillsIdLvl \"{}\"{}", skillSplit[0], skillSplit[1]);
							}
						}
					}
				}
			}
			case "showDangerIcon" -> _isShowDangerIcon = Boolean.parseBoolean(value);
			default -> super.setParameter(name, value);
		}
	}
	
	@Override
	protected void onEnter(L2Character character) {
		if (_skills != null) {
			if (getSettings().getTask() == null) {
				synchronized (this) {
					if (getSettings().getTask() == null) {
						getSettings().setTask(ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new ApplySkill(), _initialDelay, _reuse));
					}
				}
			}
		}
		if (character.isPlayer()) {
			character.setInsideZone(ZoneId.ALTERED, true);
			if (_isShowDangerIcon) {
				character.setInsideZone(ZoneId.DANGER_AREA, true);
				character.sendPacket(new EtcStatusUpdate(character.getActingPlayer()));
			}
		}
	}
	
	@Override
	protected void onExit(L2Character character) {
		if (character.isPlayer()) {
			character.setInsideZone(ZoneId.ALTERED, false);
			if (_isShowDangerIcon) {
				character.setInsideZone(ZoneId.DANGER_AREA, false);
				if (!character.isInsideZone(ZoneId.DANGER_AREA)) {
					character.sendPacket(new EtcStatusUpdate(character.getActingPlayer()));
				}
			}
		}
		if (_characterList.isEmpty() && (getSettings().getTask() != null)) {
			getSettings().clear();
		}
	}
	
	protected Skill getSkill(int skillId, int skillLvl) {
		return SkillData.getInstance().getSkill(skillId, skillLvl);
	}
	
	public int getChance() {
		return _chance;
	}
	
	public void addSkill(int skillId, int skillLvL) {
		if (skillLvL < 1) // remove skill
		{
			removeSkill(skillId);
			return;
		}
		
		if (_skills == null) {
			synchronized (this) {
				if (_skills == null) {
					_skills = new ConcurrentHashMap<>(3);
				}
			}
		}
		_skills.put(skillId, skillLvL);
	}
	
	public void removeSkill(int skillId) {
		if (_skills != null) {
			_skills.remove(skillId);
		}
	}
	
	public void clearSkills() {
		if (_skills != null) {
			_skills.clear();
		}
	}
	
	public int getSkillLevel(int skillId) {
		final Map<Integer, Integer> skills = _skills;
		return skills != null ? skills.getOrDefault(skillId, 0) : 0;
	}
	
	private final class ApplySkill implements Runnable {
		private ApplySkill() {
			if (_skills == null) {
				throw new IllegalStateException("No skills defined.");
			}
		}
		
		@Override
		public void run() {
			if (isEnabled()) {
				for (L2Character temp : getCharactersInside()) {
					if ((temp != null) && !temp.isDead()) {
						if (Rnd.get(100) < getChance()) {
							for (Entry<Integer, Integer> e : _skills.entrySet()) {
								Skill skill = getSkill(e.getKey(), e.getValue());
								if ((skill != null) && (_bypassConditions || skill.checkCondition(temp, temp, false))) {
									if (!temp.isAffectedBySkill(e.getKey())) {
										skill.applyEffects(temp, temp);
									}
								}
							}
						}
					}
				}
			}
		}
	}
}