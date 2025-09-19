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

import static com.l2jserver.gameserver.network.SystemMessageId.FAILED_TO_REGISTER_TO_IGNORE_LIST;
import static com.l2jserver.gameserver.network.SystemMessageId.MESSAGE_ACCEPTANCE_MODE;
import static com.l2jserver.gameserver.network.SystemMessageId.MESSAGE_REFUSAL_MODE;
import static com.l2jserver.gameserver.network.SystemMessageId.YOU_CANNOT_EXCLUDE_YOURSELF;
import static com.l2jserver.gameserver.network.SystemMessageId.YOU_MAY_NOT_IMPOSE_A_BLOCK_ON_GM;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.l2jserver.gameserver.data.sql.impl.CharNameTable;
import com.l2jserver.gameserver.model.BlockList;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;

public final class RequestBlock extends L2GameClientPacket {
	private static final Logger LOG = LoggerFactory.getLogger(RequestBlock.class);
	
	private static final String _C__A9_REQUESTBLOCK = "[C] A9 RequestBlock";
	
	private static final int BLOCK = 0;
	private static final int UNBLOCK = 1;
	private static final int BLOCKLIST = 2;
	private static final int ALLBLOCK = 3;
	private static final int ALLUNBLOCK = 4;
	
	private String _name;
	private Integer _type;
	
	@Override
	protected void readImpl() {
		_type = readD(); // 0x00 - block, 0x01 - unblock, 0x03 - allblock, 0x04 - allunblock
		
		if ((_type == BLOCK) || (_type == UNBLOCK)) {
			_name = readS();
		}
	}
	
	@Override
	protected void runImpl() {
		final L2PcInstance activeChar = getClient().getActiveChar();
		final int targetId = CharNameTable.getInstance().getIdByName(_name);
		final int targetAL = CharNameTable.getInstance().getAccessLevelById(targetId);
		
		if (activeChar == null) {
			return;
		}
		
		// can't use block/unblock for locating invisible characters
		switch (_type) {
			case BLOCK, UNBLOCK -> {
				if (targetId <= 0) {
					// Incorrect player name.
					activeChar.sendPacket(FAILED_TO_REGISTER_TO_IGNORE_LIST);
					return;
				}
				if (targetAL > 0) {
					// Cannot block a GM character.
					activeChar.sendPacket(YOU_MAY_NOT_IMPOSE_A_BLOCK_ON_GM);
					return;
				}
				if (activeChar.getObjectId() == targetId) {
					activeChar.sendPacket(YOU_CANNOT_EXCLUDE_YOURSELF);
					return;
				}
				if (_type == BLOCK) {
					BlockList.addToBlockList(activeChar, targetId);
				} else {
					BlockList.removeFromBlockList(activeChar, targetId);
				}
			}
			case BLOCKLIST -> BlockList.sendListToOwner(activeChar);
			case ALLBLOCK -> {
				activeChar.sendPacket(MESSAGE_REFUSAL_MODE);
				BlockList.setBlockAll(activeChar, true);
			}
			case ALLUNBLOCK -> {
				activeChar.sendPacket(MESSAGE_ACCEPTANCE_MODE);
				BlockList.setBlockAll(activeChar, false);
			}
			default -> LOG.info("Unknown 0xA9 block type: {}", _type);
		}
	}
	
	@Override
	public String getType() {
		return _C__A9_REQUESTBLOCK;
	}
}
