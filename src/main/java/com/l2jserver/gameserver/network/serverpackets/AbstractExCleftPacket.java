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
package com.l2jserver.gameserver.network.serverpackets;

import java.util.Objects;

import com.l2jserver.gameserver.model.cleft.CleftTeam;

/**
 * @author HorridoJoho
 * @since 2.6.3.0
 */
public abstract class AbstractExCleftPacket extends L2GameServerPacket {
	protected AbstractExCleftPacket() {
	}
	
	protected final void writeTeamId(CleftTeam team) {
		Objects.requireNonNull(team);
		
		writeD(team.getTeam().ordinal() - 1);
	}
}
