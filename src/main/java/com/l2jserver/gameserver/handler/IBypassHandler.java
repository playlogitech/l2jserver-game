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
package com.l2jserver.gameserver.handler;

import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;

/**
 * Bypass Handler interface.
 * @author nBd
 */
public interface IBypassHandler {
	/**
	 * This is the worker method that is called when someone uses an bypass command.
	 * @param command
	 * @param activeChar
	 * @param bypassOrigin
	 * @return success
	 */
	boolean useBypass(String command, L2PcInstance player, L2Character bypassOrigin);
	
	/**
	 * This method is called at initialization to register all bypasses automatically.
	 * @return all known bypasses
	 */
	String[] getBypassList();
}