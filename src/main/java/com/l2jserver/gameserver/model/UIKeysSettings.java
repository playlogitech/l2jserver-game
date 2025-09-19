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
package com.l2jserver.gameserver.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.l2jserver.commons.database.ConnectionFactory;
import com.l2jserver.gameserver.data.xml.impl.UIData;

/**
 * UI Keys Settings class.
 * @author mrTJO
 * @author Zoey76
 */
public class UIKeysSettings {
	private static final Logger LOG = LoggerFactory.getLogger(UIKeysSettings.class);
	
	private final int _playerObjId;
	private Map<Integer, List<ActionKey>> _storedKeys;
	private Map<Integer, List<Integer>> _storedCategories;
	private boolean _saved = true;
	
	public UIKeysSettings(int playerObjId) {
		_playerObjId = playerObjId;
		loadFromDB();
	}
	
	public void storeAll(Map<Integer, List<Integer>> catMap, Map<Integer, List<ActionKey>> keyMap) {
		_saved = false;
		_storedCategories = catMap;
		_storedKeys = keyMap;
	}
	
	public void storeCategories(Map<Integer, List<Integer>> catMap) {
		_saved = false;
		_storedCategories = catMap;
	}
	
	public Map<Integer, List<Integer>> getCategories() {
		return _storedCategories;
	}
	
	public void storeKeys(Map<Integer, List<ActionKey>> keyMap) {
		_saved = false;
		_storedKeys = keyMap;
	}
	
	public Map<Integer, List<ActionKey>> getKeys() {
		return _storedKeys;
	}
	
	private void loadFromDB() {
		getCatsFromDB();
		getKeysFromDB();
	}
	
	/**
	 * Save Categories and Mapped Keys into GameServer DataBase
	 */
	public void saveInDB() {
		StringBuilder query;
		if (_saved) {
			return;
		}
		
		// TODO(Zoey76): Refactor this to use batch.
		query = new StringBuilder("REPLACE INTO character_ui_categories (`charId`, `catId`, `order`, `cmdId`) VALUES ");
		for (int category : _storedCategories.keySet()) {
			int order = 0;
			for (int key : _storedCategories.get(category)) {
				query.append("(").append(_playerObjId).append(", ").append(category).append(", ").append(order++).append(", ").append(key).append("),");
			}
		}
		query = new StringBuilder(query.substring(0, query.length() - 1) + "; ");
		try (var con = ConnectionFactory.getInstance().getConnection();
			var statement = con.prepareStatement(query.toString())) {
			statement.execute();
		} catch (Exception e) {
			LOG.warn("Exception: saveInDB(): {}", e.getMessage(), e);
		}
		
		query = new StringBuilder("REPLACE INTO character_ui_actions (`charId`, `cat`, `order`, `cmd`, `key`, `tgKey1`, `tgKey2`, `show`) VALUES");
		for (List<ActionKey> keyLst : _storedKeys.values()) {
			int order = 0;
			for (ActionKey key : keyLst) {
				query.append(key.getSqlSaveString(_playerObjId, order++)).append(",");
			}
		}
		query = new StringBuilder(query.substring(0, query.length() - 1) + ";");
		
		try (var con = ConnectionFactory.getInstance().getConnection();
			var statement = con.prepareStatement(query.toString())) {
			statement.execute();
		} catch (Exception e) {
			LOG.warn("Exception: saveInDB(): {}", e.getMessage(), e);
		}
		_saved = true;
	}
	
	private void getCatsFromDB() {
		if (_storedCategories != null) {
			return;
		}
		
		_storedCategories = new HashMap<>();
		
		try (var con = ConnectionFactory.getInstance().getConnection();
			var ps = con.prepareStatement("SELECT * FROM character_ui_categories WHERE `charId` = ? ORDER BY `catId`, `order`")) {
			ps.setInt(1, _playerObjId);
			try (var rs = ps.executeQuery()) {
				while (rs.next()) {
					UIData.addCategory(_storedCategories, rs.getInt("catId"), rs.getInt("cmdId"));
				}
			}
		} catch (Exception e) {
			LOG.warn("Exception: getCatsFromDB(): {}", e.getMessage(), e);
		}
		
		if (_storedCategories.isEmpty()) {
			_storedCategories = UIData.getInstance().getCategories();
		}
	}
	
	private void getKeysFromDB() {
		if (_storedKeys != null) {
			return;
		}
		
		_storedKeys = new HashMap<>();
		
		try (var con = ConnectionFactory.getInstance().getConnection();
			var ps = con.prepareStatement("SELECT * FROM character_ui_actions WHERE `charId` = ? ORDER BY `cat`, `order`")) {
			ps.setInt(1, _playerObjId);
			try (var rs = ps.executeQuery()) {
				while (rs.next()) {
					int cat = rs.getInt("cat");
					int cmd = rs.getInt("cmd");
					int key = rs.getInt("key");
					int tgKey1 = rs.getInt("tgKey1");
					int tgKey2 = rs.getInt("tgKey2");
					int show = rs.getInt("show");
					UIData.addKey(_storedKeys, cat, new ActionKey(cat, cmd, key, tgKey1, tgKey2, show));
				}
			}
		} catch (Exception e) {
			LOG.warn("Exception: getKeysFromDB(): {}", e.getMessage(), e);
		}
		
		if (_storedKeys.isEmpty()) {
			_storedKeys = UIData.getInstance().getKeys();
		}
	}
	
	public boolean isSaved() {
		return _saved;
	}
}
