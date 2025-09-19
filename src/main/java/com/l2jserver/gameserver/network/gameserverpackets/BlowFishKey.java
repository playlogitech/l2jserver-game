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
package com.l2jserver.gameserver.network.gameserverpackets;

import java.security.interfaces.RSAPublicKey;

import javax.crypto.Cipher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.l2jserver.commons.network.BaseSendablePacket;

/**
 * @author -Wooden-
 */
public class BlowFishKey extends BaseSendablePacket {
	private static final Logger LOG = LoggerFactory.getLogger(BlowFishKey.class);
	
	public BlowFishKey(byte[] blowfishKey, RSAPublicKey publicKey) {
		writeC(0x00);
		try {
			final Cipher rsaCipher = Cipher.getInstance("RSA/ECB/nopadding");
			rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey);
			byte[] encrypted = rsaCipher.doFinal(blowfishKey);
			writeD(encrypted.length);
			writeB(encrypted);
		} catch (Exception e) {
			LOG.error("Error While encrypting blowfish key for transmision (Crypt error): {}", e.getMessage(), e);
		}
	}
	
	@Override
	public byte[] getContent() {
		return getBytes();
	}
}
