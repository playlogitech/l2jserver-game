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

import com.l2jserver.gameserver.model.L2Object;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.network.serverpackets.ExBrExtraUserInfo;
import com.l2jserver.gameserver.network.serverpackets.SpawnItem;
import com.l2jserver.gameserver.network.serverpackets.UserInfo;

public class RequestRecordInfo extends L2GameClientPacket {
	private static final String _C__6E_REQUEST_RECORD_INFO = "[C] 6E RequestRecordInfo";
	
	@Override
	protected void readImpl() {
		// trigger
	}
	
	@Override
	protected void runImpl() {
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null) {
			return;
		}
		
		activeChar.sendPacket(new UserInfo(activeChar));
		activeChar.sendPacket(new ExBrExtraUserInfo(activeChar));
		
		for (L2Object object : activeChar.getKnownList().getKnownObjects().values()) {
			if (object.getPoly().isMorphed() && object.getPoly().getPolyType().equals("item")) {
				activeChar.sendPacket(new SpawnItem(object));
			} else {
				if (!object.isVisibleFor(activeChar)) {
					object.sendInfo(activeChar);
					
					if (object instanceof L2Character creature) {
						// Update the state of the L2Character object client side by sending Server->Client packet
						// MoveToPawn/CharMoveToLocation and AutoAttackStart to the L2PcInstance
						creature.getAI().describeStateToPlayer(activeChar);
					}
				}
			}
		}
	}
	
	@Override
	public String getType() {
		return _C__6E_REQUEST_RECORD_INFO;
	}
}
