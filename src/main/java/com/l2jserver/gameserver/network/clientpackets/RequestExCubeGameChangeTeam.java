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
package com.l2jserver.gameserver.network.clientpackets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.l2jserver.gameserver.instancemanager.HandysBlockCheckerManager;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;

/**
 * Format: chdd d: Arena d: Team
 * @author mrTJO
 */
public final class RequestExCubeGameChangeTeam extends L2GameClientPacket {
	private static final Logger LOG = LoggerFactory.getLogger(RequestExCubeGameChangeTeam.class);
	
	private static final String _C__D0_5A_REQUESTEXCUBEGAMECHANGETEAM = "[C] D0:5A RequestExCubeGameChangeTeam";
	
	private int _arena;
	private int _team;
	
	@Override
	protected void readImpl() {
		// client sends -1,0,1,2 for arena parameter
		_arena = readD() + 1;
		_team = readD();
	}
	
	@Override
	public void runImpl() {
		// do not remove players after start
		if (HandysBlockCheckerManager.getInstance().arenaIsBeingUsed(_arena)) {
			return;
		}
		L2PcInstance player = getClient().getActiveChar();
		switch (_team) {
			// Change Player Team
			case 0, 1 -> HandysBlockCheckerManager.getInstance().changePlayerToTeam(player, _arena, _team);
			// Remove Player (me)
			case -1 -> {
				int team = HandysBlockCheckerManager.getInstance().getHolder(_arena).getPlayerTeam(player);
				// client sends two times this packet if click on exit
				// client did not send this packet on restart
				if (team > -1) {
					HandysBlockCheckerManager.getInstance().removePlayer(player, _arena, team);
				}
			}
			default -> LOG.warn("Wrong Cube Game Team ID: {}", _team);
		}
	}
	
	@Override
	public String getType() {
		return _C__D0_5A_REQUESTEXCUBEGAMECHANGETEAM;
	}
}
