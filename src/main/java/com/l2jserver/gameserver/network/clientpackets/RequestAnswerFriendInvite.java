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

import static com.l2jserver.gameserver.config.Configuration.character;
import static com.l2jserver.gameserver.network.SystemMessageId.FAILED_TO_INVITE_A_FRIEND;
import static com.l2jserver.gameserver.network.SystemMessageId.S1_ADDED_TO_FRIENDS;
import static com.l2jserver.gameserver.network.SystemMessageId.S1_ALREADY_IN_FRIENDS_LIST;
import static com.l2jserver.gameserver.network.SystemMessageId.S1_JOINED_AS_FRIEND;
import static com.l2jserver.gameserver.network.SystemMessageId.THE_FRIENDS_LIST_OF_THE_PERSON_YOU_ARE_TRYING_TO_ADD_IS_FULL_SO_REGISTRATION_IS_NOT_POSSIBLE;
import static com.l2jserver.gameserver.network.SystemMessageId.YOU_CAN_ONLY_ENTER_UP_128_NAMES_IN_YOUR_FRIENDS_LIST;
import static com.l2jserver.gameserver.network.SystemMessageId.YOU_HAVE_SUCCEEDED_INVITING_FRIEND;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.l2jserver.commons.database.ConnectionFactory;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.network.serverpackets.FriendPacket;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;

public final class RequestAnswerFriendInvite extends L2GameClientPacket {
	private static final Logger LOG = LoggerFactory.getLogger(RequestAnswerFriendInvite.class);
	
	private static final String _C__78_REQUESTANSWERFRIENDINVITE = "[C] 78 RequestAnswerFriendInvite";
	
	private int _response;
	
	@Override
	protected void readImpl() {
		_response = readD();
	}
	
	@Override
	protected void runImpl() {
		final L2PcInstance player = getActiveChar();
		if (player == null) {
			return;
		}
		
		final L2PcInstance requestor = player.getActiveRequester();
		if (requestor == null) {
			return;
		}
		
		if (player.isFriend(requestor.getObjectId()) || requestor.isFriend(player.getObjectId())) {
			final SystemMessage sm = SystemMessage.getSystemMessage(S1_ALREADY_IN_FRIENDS_LIST);
			sm.addCharName(player);
			requestor.sendPacket(sm);
			return;
		}
		
		if (player.getFriends().size() >= character().getFriendListLimit()) {
			player.sendPacket(YOU_CAN_ONLY_ENTER_UP_128_NAMES_IN_YOUR_FRIENDS_LIST);
			return;
		}
		
		if (requestor.getFriends().size() >= character().getFriendListLimit()) {
			requestor.sendPacket(THE_FRIENDS_LIST_OF_THE_PERSON_YOU_ARE_TRYING_TO_ADD_IS_FULL_SO_REGISTRATION_IS_NOT_POSSIBLE);
			return;
		}
		
		if (_response == 1) {
			try (var con = ConnectionFactory.getInstance().getConnection();
				var statement = con.prepareStatement("INSERT INTO character_friends (charId, friendId) VALUES (?, ?), (?, ?)")) {
				statement.setInt(1, requestor.getObjectId());
				statement.setInt(2, player.getObjectId());
				statement.setInt(3, player.getObjectId());
				statement.setInt(4, requestor.getObjectId());
				statement.execute();
				requestor.sendPacket(YOU_HAVE_SUCCEEDED_INVITING_FRIEND);
				
				// Player added to your friend list
				var msg = SystemMessage.getSystemMessage(S1_ADDED_TO_FRIENDS);
				msg.addString(player.getName());
				requestor.sendPacket(msg);
				requestor.addFriend(player.getObjectId());
				
				// has joined as friend.
				msg = SystemMessage.getSystemMessage(S1_JOINED_AS_FRIEND);
				msg.addString(requestor.getName());
				player.sendPacket(msg);
				player.addFriend(requestor.getObjectId());
				
				// Send notifications for both player in order to show them online
				player.sendPacket(new FriendPacket(true, requestor.getObjectId()));
				requestor.sendPacket(new FriendPacket(true, player.getObjectId()));
			} catch (Exception e) {
				LOG.warn("Could not add friend objectid: {}", e.getMessage(), e);
			}
		} else {
			requestor.sendPacket(FAILED_TO_INVITE_A_FRIEND);
		}
		
		player.setActiveRequester(null);
		requestor.onTransactionResponse();
	}
	
	@Override
	public String getType() {
		return _C__78_REQUESTANSWERFRIENDINVITE;
	}
}
