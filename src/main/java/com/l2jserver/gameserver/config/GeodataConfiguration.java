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

import java.io.File;
import java.util.List;

import org.aeonbits.owner.Config.HotReload;
import org.aeonbits.owner.Config.LoadPolicy;
import org.aeonbits.owner.Config.Sources;
import org.aeonbits.owner.Mutable;
import org.aeonbits.owner.Reloadable;

/**
 * Geodata Configuration.
 * @author Zoey76
 * @version 2.6.1.0
 */
@Sources({
	"file:${L2J_HOME}/custom/game/config/geodata.properties",
	"file:./config/geodata.properties",
	"classpath:config/geodata.properties"
})
@LoadPolicy(MERGE)
@HotReload(value = 20, unit = MINUTES, type = ASYNC)
public interface GeodataConfiguration extends Mutable, Reloadable {
	
	/**
	 * Pathfinding options:
	 * <ul>
	 * <li>0 = Disabled</li>
	 * <li>1 = Enabled using path node files</li>
	 * <li>2 = Enabled using geodata cells at runtime</li>
	 * </ul>
	 * @return
	 */
	@Key("PathFinding")
	int getPathFinding();
	
	@Key("PathnodePath")
	File getPathnodePath();
	
	@Key("PathFindBuffers")
	String getPathFindBuffers();
	
	@Key("LowWeight")
	float getLowWeight();
	
	@Key("MediumWeight")
	float getMediumWeight();
	
	@Key("HighWeight")
	float getHighWeight();
	
	@Key("AdvancedDiagonalStrategy")
	boolean advancedDiagonalStrategy();
	
	@Key("DiagonalWeight")
	float getDiagonalWeight();
	
	@Key("MaxPostfilterPasses")
	int getMaxPostfilterPasses();
	
	@Key("DebugPath")
	boolean debugPath();
	
	@Key("ForceGeoData")
	boolean forceGeoData();
	
	@Key("CoordSynchronize")
	int getCoordSynchronize();
	
	@Key("GeoDataPath")
	File getGeoDataPath();
	
	@Key("TryLoadUnspecifiedRegions")
	boolean tryLoadUnspecifiedRegions();
	
	@Separator(";")
	@Key("IncludedRegions")
	List<String> getIncludedRegions();
	
	@Separator(";")
	@Key("ExcludedRegions")
	List<String> getExcludedRegions();
}