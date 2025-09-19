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
package com.l2jserver.gameserver.config;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.aeonbits.owner.Config.HotReloadType.ASYNC;
import static org.aeonbits.owner.Config.LoadType.MERGE;

import org.aeonbits.owner.Config.HotReload;
import org.aeonbits.owner.Config.LoadPolicy;
import org.aeonbits.owner.Config.Sources;
import org.aeonbits.owner.Reloadable;

import com.l2jserver.gameserver.config.converter.ControlTowerConverter;
import com.l2jserver.gameserver.config.converter.FlameTowerConverter;
import com.l2jserver.gameserver.model.TowerSpawn;

/**
 * Siege Configuration.
 * @author Zoey76
 * @version 2.6.1.0
 */
@Sources({
	"file:${L2J_HOME}/custom/game/config/siege.properties",
	"file:./config/siege.properties",
	"classpath:config/siege.properties"
})
@LoadPolicy(MERGE)
@HotReload(value = 20, unit = MINUTES, type = ASYNC)
public interface SiegeConfiguration extends Reloadable {
	
	@Key("SiegeLength")
	int getSiegeLength();
	
	@Key("MaxFlags")
	int getMaxFlags();
	
	@Key("ClanMinLevel")
	int getClanMinLevel();
	
	@Key("AttackerMaxClans")
	int getAttackerMaxClans();
	
	@Key("DefenderMaxClans")
	int getDefenderMaxClans();
	
	@Key("AttackerRespawn")
	int getAttackerRespawn();
	
	@Key("BloodAllianceReward")
	int getBloodAllianceReward();
	
	@Key("GludioFlameTower1")
	@ConverterClass(FlameTowerConverter.class)
	TowerSpawn getGludioFlameTower1();
	
	@Key("GludioFlameTower2")
	@ConverterClass(FlameTowerConverter.class)
	TowerSpawn getGludioFlameTower2();
	
	@Key("GludioControlTower1")
	@ConverterClass(ControlTowerConverter.class)
	TowerSpawn getGludioControlTower1();
	
	@Key("GludioControlTower2")
	@ConverterClass(ControlTowerConverter.class)
	TowerSpawn getGludioControlTower2();
	
	@Key("GludioControlTower3")
	@ConverterClass(ControlTowerConverter.class)
	TowerSpawn getGludioControlTower3();
	
	@Key("GludioMaxMercenaries")
	int getGludioMaxMercenaries();
	
	@Key("GiranFlameTower1")
	@ConverterClass(FlameTowerConverter.class)
	TowerSpawn getGiranFlameTower1();
	
	@Key("GiranFlameTower2")
	@ConverterClass(FlameTowerConverter.class)
	TowerSpawn getGiranFlameTower2();
	
	@Key("GiranControlTower1")
	@ConverterClass(ControlTowerConverter.class)
	TowerSpawn getGiranControlTower1();
	
	@Key("GiranControlTower2")
	@ConverterClass(ControlTowerConverter.class)
	TowerSpawn getGiranControlTower2();
	
	@Key("GiranControlTower3")
	@ConverterClass(ControlTowerConverter.class)
	TowerSpawn getGiranControlTower3();
	
	@Key("GiranMaxMercenaries")
	int getGiranMaxMercenaries();
	
	@Key("DionFlameTower1")
	@ConverterClass(FlameTowerConverter.class)
	TowerSpawn getDionFlameTower1();
	
	@Key("DionFlameTower2")
	@ConverterClass(FlameTowerConverter.class)
	TowerSpawn getDionFlameTower2();
	
	@Key("DionControlTower1")
	@ConverterClass(ControlTowerConverter.class)
	TowerSpawn getDionControlTower1();
	
	@Key("DionControlTower2")
	@ConverterClass(ControlTowerConverter.class)
	TowerSpawn getDionControlTower2();
	
	@Key("DionControlTower3")
	@ConverterClass(ControlTowerConverter.class)
	TowerSpawn getDionControlTower3();
	
	@Key("DionMaxMercenaries")
	int getDionMaxMercenaries();
	
	@Key("OrenFlameTower1")
	@ConverterClass(FlameTowerConverter.class)
	TowerSpawn getOrenFlameTower1();
	
	@Key("OrenFlameTower2")
	@ConverterClass(FlameTowerConverter.class)
	TowerSpawn getOrenFlameTower2();
	
	@Key("OrenControlTower1")
	@ConverterClass(ControlTowerConverter.class)
	TowerSpawn getOrenControlTower1();
	
	@Key("OrenControlTower2")
	@ConverterClass(ControlTowerConverter.class)
	TowerSpawn getOrenControlTower2();
	
	@Key("OrenControlTower3")
	@ConverterClass(ControlTowerConverter.class)
	TowerSpawn getOrenControlTower3();
	
	@Key("OrenMaxMercenaries")
	int getOrenMaxMercenaries();
	
	@Key("AdenFlameTower1")
	@ConverterClass(FlameTowerConverter.class)
	TowerSpawn getAdenFlameTower1();
	
	@Key("AdenFlameTower2")
	@ConverterClass(FlameTowerConverter.class)
	TowerSpawn AdenFlameTower2();
	
	@Key("AdenControlTower1")
	@ConverterClass(ControlTowerConverter.class)
	TowerSpawn getAdenControlTower1();
	
	@Key("AdenControlTower2")
	@ConverterClass(ControlTowerConverter.class)
	TowerSpawn getAdenControlTower2();
	
	@Key("AdenControlTower3")
	@ConverterClass(ControlTowerConverter.class)
	TowerSpawn getAdenControlTower3();
	
	@Key("AdenMaxMercenaries")
	int getAdenMaxMercenaries();
	
	@Key("InnadrilFlameTower1")
	@ConverterClass(FlameTowerConverter.class)
	TowerSpawn getInnadrilFlameTower1();
	
	@Key("InnadrilFlameTower2")
	@ConverterClass(FlameTowerConverter.class)
	TowerSpawn getInnadrilFlameTower2();
	
	@Key("InnadrilControlTower1")
	@ConverterClass(ControlTowerConverter.class)
	TowerSpawn getInnadrilControlTower1();
	
	@Key("InnadrilControlTower2")
	@ConverterClass(ControlTowerConverter.class)
	TowerSpawn getInnadrilControlTower2();
	
	@Key("InnadrilControlTower3")
	@ConverterClass(ControlTowerConverter.class)
	TowerSpawn getInnadrilControlTower3();
	
	@Key("InnadrilMaxMercenaries")
	int getInnadrilMaxMercenaries();
	
	@Key("GoddardFlameTower1")
	@ConverterClass(FlameTowerConverter.class)
	TowerSpawn getGoddardFlameTower1();
	
	@Key("GoddardFlameTower2")
	@ConverterClass(FlameTowerConverter.class)
	TowerSpawn getGoddardFlameTower2();
	
	@Key("GoddardControlTower1")
	@ConverterClass(ControlTowerConverter.class)
	TowerSpawn getGoddardControlTower1();
	
	@Key("GoddardControlTower2")
	@ConverterClass(ControlTowerConverter.class)
	TowerSpawn getGoddardControlTower2();
	
	@Key("GoddardControlTower3")
	@ConverterClass(ControlTowerConverter.class)
	TowerSpawn getGoddardControlTower3();
	
	@Key("GoddardMaxMercenaries")
	int getGoddardMaxMercenaries();
	
	@Key("RuneFlameTower1")
	@ConverterClass(FlameTowerConverter.class)
	TowerSpawn getRuneFlameTower1();
	
	@Key("RuneFlameTower2")
	@ConverterClass(FlameTowerConverter.class)
	TowerSpawn getRuneFlameTower2();
	
	@Key("RuneControlTower1")
	@ConverterClass(ControlTowerConverter.class)
	TowerSpawn getRuneControlTower1();
	
	@Key("RuneControlTower2")
	@ConverterClass(ControlTowerConverter.class)
	TowerSpawn getRuneControlTower2();
	
	@Key("RuneControlTower3")
	@ConverterClass(ControlTowerConverter.class)
	TowerSpawn getRuneControlTower3();
	
	@Key("RuneMaxMercenaries")
	int getRuneMaxMercenaries();
	
	@Key("SchuttgartFlameTower1")
	@ConverterClass(FlameTowerConverter.class)
	TowerSpawn getSchuttgartFlameTower1();
	
	@Key("SchuttgartFlameTower2")
	@ConverterClass(FlameTowerConverter.class)
	TowerSpawn getSchuttgartFlameTower2();
	
	@Key("SchuttgartControlTower1")
	@ConverterClass(ControlTowerConverter.class)
	TowerSpawn getSchuttgartControlTower1();
	
	@Key("SchuttgartControlTower2")
	@ConverterClass(ControlTowerConverter.class)
	TowerSpawn getSchuttgartControlTower2();
	
	@Key("SchuttgartControlTower3")
	@ConverterClass(ControlTowerConverter.class)
	TowerSpawn getSchuttgartControlTower3();
	
	@Key("SchuttgartMaxMercenaries")
	int getSchuttgartMaxMercenaries();
}