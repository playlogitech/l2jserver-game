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
package com.l2jserver.gameserver.model.itemcontainer;

import static com.l2jserver.gameserver.config.Configuration.general;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.l2jserver.commons.database.ConnectionFactory;
import com.l2jserver.gameserver.data.xml.impl.ArmorSetsData;
import com.l2jserver.gameserver.datatables.ItemTable;
import com.l2jserver.gameserver.enums.ItemLocation;
import com.l2jserver.gameserver.model.L2ArmorSet;
import com.l2jserver.gameserver.model.TradeItem;
import com.l2jserver.gameserver.model.TradeList;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.events.EventDispatcher;
import com.l2jserver.gameserver.model.events.impl.character.player.inventory.PlayerItemAdd;
import com.l2jserver.gameserver.model.events.impl.character.player.inventory.PlayerItemDestroy;
import com.l2jserver.gameserver.model.events.impl.character.player.inventory.PlayerItemDrop;
import com.l2jserver.gameserver.model.events.impl.character.player.inventory.PlayerItemTransfer;
import com.l2jserver.gameserver.model.holders.SkillHolder;
import com.l2jserver.gameserver.model.items.L2Item;
import com.l2jserver.gameserver.model.items.instance.L2ItemInstance;
import com.l2jserver.gameserver.model.items.type.WeaponType;
import com.l2jserver.gameserver.model.skills.Skill;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.InventoryUpdate;
import com.l2jserver.gameserver.network.serverpackets.ItemList;
import com.l2jserver.gameserver.network.serverpackets.SkillCoolTime;
import com.l2jserver.gameserver.network.serverpackets.StatusUpdate;
import com.l2jserver.gameserver.util.Util;

public class PcInventory extends Inventory {
	private static final Logger LOG = LoggerFactory.getLogger(PcInventory.class);
	
	private final L2PcInstance _owner;
	
	private L2ItemInstance _adena;
	
	private L2ItemInstance _ancientAdena;
	
	private int[] _blockItems = null;
	
	private int _questSlots;
	
	private final Object _lock;
	
	/**
	 * Block modes:
	 * <UL>
	 * <LI>-1 - no block
	 * <LI>0 - block items from _invItems, allow usage of other items
	 * <LI>1 - allow usage of items from _invItems, block other items
	 * </UL>
	 */
	private int _blockMode = -1;
	
	public PcInventory(L2PcInstance owner) {
		addPaperdollListener(ArmorSetListener.getInstance());
		addPaperdollListener(BowCrossRodListener.getInstance());
		addPaperdollListener(ItemSkillsListener.getInstance());
		addPaperdollListener(BraceletListener.getInstance());
		
		_owner = owner;
		_lock = new Object();
	}
	
	@Override
	public L2PcInstance getOwner() {
		return _owner;
	}
	
	@Override
	protected ItemLocation getBaseLocation() {
		return ItemLocation.INVENTORY;
	}
	
	@Override
	protected ItemLocation getEquipLocation() {
		return ItemLocation.PAPERDOLL;
	}
	
	public L2ItemInstance getAdenaInstance() {
		return _adena;
	}
	
	@Override
	public long getAdena() {
		return _adena != null ? _adena.getCount() : 0;
	}
	
	public L2ItemInstance getAncientAdenaInstance() {
		return _ancientAdena;
	}
	
	public long getAncientAdena() {
		return (_ancientAdena != null) ? _ancientAdena.getCount() : 0;
	}
	
	/**
	 * Returns the list of items in inventory available for transaction
	 * @param allowAdena
	 * @param allowAncientAdena
	 * @return L2ItemInstance : items in inventory
	 */
	public L2ItemInstance[] getUniqueItems(boolean allowAdena, boolean allowAncientAdena) {
		return getUniqueItems(allowAdena, allowAncientAdena, true);
	}
	
	public L2ItemInstance[] getUniqueItems(boolean allowAdena, boolean allowAncientAdena, boolean onlyAvailable) {
		List<L2ItemInstance> list = new LinkedList<>();
		for (L2ItemInstance item : _items) {
			if (item == null) {
				continue;
			}
			if ((!allowAdena && (item.getId() == ADENA_ID))) {
				continue;
			}
			if ((!allowAncientAdena && (item.getId() == ANCIENT_ADENA_ID))) {
				continue;
			}
			boolean isDuplicate = false;
			for (L2ItemInstance litem : list) {
				if (litem.getId() == item.getId()) {
					isDuplicate = true;
					break;
				}
			}
			if (!isDuplicate && (!onlyAvailable || (item.isSellable() && item.isAvailable(getOwner(), false, false)))) {
				list.add(item);
			}
		}
		return list.toArray(new L2ItemInstance[0]);
	}
	
	/**
	 * Returns the list of items in inventory available for transaction Allows an item to appear twice if and only if there is a difference in enchantment level.
	 * @param allowAdena
	 * @param allowAncientAdena
	 * @return L2ItemInstance : items in inventory
	 */
	public L2ItemInstance[] getUniqueItemsByEnchantLevel(boolean allowAdena, boolean allowAncientAdena) {
		return getUniqueItemsByEnchantLevel(allowAdena, allowAncientAdena, true);
	}
	
	public L2ItemInstance[] getUniqueItemsByEnchantLevel(boolean allowAdena, boolean allowAncientAdena, boolean onlyAvailable) {
		List<L2ItemInstance> list = new LinkedList<>();
		for (L2ItemInstance item : _items) {
			if (item == null) {
				continue;
			}
			if ((!allowAdena && (item.getId() == ADENA_ID))) {
				continue;
			}
			if ((!allowAncientAdena && (item.getId() == ANCIENT_ADENA_ID))) {
				continue;
			}
			
			boolean isDuplicate = false;
			for (L2ItemInstance litem : list) {
				if ((litem.getId() == item.getId()) && (litem.getEnchantLevel() == item.getEnchantLevel())) {
					isDuplicate = true;
					break;
				}
			}
			
			if (!isDuplicate && (!onlyAvailable || (item.isSellable() && item.isAvailable(getOwner(), false, false)))) {
				list.add(item);
			}
		}
		return list.toArray(new L2ItemInstance[list.size()]);
	}
	
	/**
	 * @param itemId
	 * @return
	 */
	public L2ItemInstance[] getAllItemsByItemId(int itemId) {
		return getAllItemsByItemId(itemId, true);
	}
	
	/**
	 * Returns the list of all items in inventory that have a given item id.
	 * @param itemId : ID of item
	 * @param includeEquipped : include equipped items
	 * @return L2ItemInstance[] : matching items from inventory
	 */
	public L2ItemInstance[] getAllItemsByItemId(int itemId, boolean includeEquipped) {
		List<L2ItemInstance> list = new LinkedList<>();
		for (L2ItemInstance item : _items) {
			if (item == null) {
				continue;
			}
			
			if ((item.getId() == itemId) && (includeEquipped || !item.isEquipped())) {
				list.add(item);
			}
		}
		return list.toArray(new L2ItemInstance[0]);
	}
	
	/**
	 * @param itemId
	 * @param enchantment
	 * @return
	 */
	public L2ItemInstance[] getAllItemsByItemId(int itemId, int enchantment) {
		return getAllItemsByItemId(itemId, enchantment, true);
	}
	
	/**
	 * Returns the list of all items in inventory that have a given item id AND a given enchantment level.
	 * @param itemId : ID of item
	 * @param enchantment : enchant level of item
	 * @param includeEquipped : include equipped items
	 * @return L2ItemInstance[] : matching items from inventory
	 */
	public L2ItemInstance[] getAllItemsByItemId(int itemId, int enchantment, boolean includeEquipped) {
		List<L2ItemInstance> list = new LinkedList<>();
		for (L2ItemInstance item : _items) {
			if (item == null) {
				continue;
			}
			
			if ((item.getId() == itemId) && (item.getEnchantLevel() == enchantment) && (includeEquipped || !item.isEquipped())) {
				list.add(item);
			}
		}
		return list.toArray(new L2ItemInstance[0]);
	}
	
	/**
	 * @param allowAdena
	 * @param allowNonTradeable
	 * @param feightable
	 * @return the list of items in inventory available for transaction
	 */
	public L2ItemInstance[] getAvailableItems(boolean allowAdena, boolean allowNonTradeable, boolean feightable) {
		List<L2ItemInstance> list = new LinkedList<>();
		for (L2ItemInstance item : _items) {
			if ((item == null) || !item.isAvailable(getOwner(), allowAdena, allowNonTradeable) || !canManipulateWithItemId(item.getId())) {
				continue;
			} else if (feightable) {
				if ((item.getItemLocation() == ItemLocation.INVENTORY) && item.isFreightable()) {
					list.add(item);
				}
			} else {
				list.add(item);
			}
		}
		return list.toArray(new L2ItemInstance[0]);
	}
	
	/**
	 * Get all augmented items
	 * @return
	 */
	public L2ItemInstance[] getAugmentedItems() {
		List<L2ItemInstance> list = new LinkedList<>();
		for (L2ItemInstance item : _items) {
			if ((item != null) && item.isAugmented()) {
				list.add(item);
			}
		}
		return list.toArray(new L2ItemInstance[0]);
	}
	
	/**
	 * Get all element items
	 * @return
	 */
	public L2ItemInstance[] getElementItems() {
		List<L2ItemInstance> list = new LinkedList<>();
		for (L2ItemInstance item : _items) {
			if ((item != null) && (item.getElementals() != null)) {
				list.add(item);
			}
		}
		return list.toArray(new L2ItemInstance[0]);
	}
	
	/**
	 * Returns the list of items in inventory available for transaction adjusted by tradeList
	 * @param tradeList
	 * @return L2ItemInstance : items in inventory
	 */
	public TradeItem[] getAvailableItems(TradeList tradeList) {
		List<TradeItem> list = new LinkedList<>();
		for (L2ItemInstance item : _items) {
			if ((item != null) && item.isAvailable(getOwner(), false, false)) {
				TradeItem adjItem = tradeList.adjustAvailableItem(item);
				if (adjItem != null) {
					list.add(adjItem);
				}
			}
		}
		return list.toArray(new TradeItem[0]);
	}
	
	/**
	 * Adjust TradeItem according his status in inventory
	 * @param item : L2ItemInstance to be adjusted
	 */
	public void adjustAvailableItem(TradeItem item) {
		boolean notAllEquipped = false;
		for (L2ItemInstance adjItem : getItemsByItemId(item.getItem().getId())) {
			if (adjItem.isEquipable()) {
				if (!adjItem.isEquipped()) {
					notAllEquipped |= true;
				}
			} else {
				notAllEquipped |= true;
				break;
			}
		}
		if (notAllEquipped) {
			L2ItemInstance adjItem = getItemByItemId(item.getItem().getId());
			item.setObjectId(adjItem.getObjectId());
			item.setEnchant(adjItem.getEnchantLevel());
			
			if (adjItem.getCount() < item.getCount()) {
				item.setCount(adjItem.getCount());
			}
			
			return;
		}
		
		item.setCount(0);
	}
	
	/**
	 * Adds adena to PCInventory
	 * @param process : String Identifier of process triggering this action
	 * @param count : int Quantity of adena to be added
	 * @param actor : L2PcInstance Player requesting the item add
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 */
	public void addAdena(String process, long count, L2PcInstance actor, Object reference) {
		if (count > 0) {
			addItem(process, ADENA_ID, count, actor, reference);
		}
	}
	
	/**
	 * Removes adena to PCInventory
	 * @param process : String Identifier of process triggering this action
	 * @param count : int Quantity of adena to be removed
	 * @param actor : L2PcInstance Player requesting the item add
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return boolean : true if adena was reduced
	 */
	public boolean reduceAdena(String process, long count, L2PcInstance actor, Object reference) {
		if (count > 0) {
			return destroyItemByItemId(process, ADENA_ID, count, actor, reference) != null;
		}
		return false;
	}
	
	/**
	 * Adds specified amount of ancient adena to player inventory.
	 * @param process : String Identifier of process triggering this action
	 * @param count : int Quantity of adena to be added
	 * @param actor : L2PcInstance Player requesting the item add
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 */
	public void addAncientAdena(String process, long count, L2PcInstance actor, Object reference) {
		if (count > 0) {
			addItem(process, ANCIENT_ADENA_ID, count, actor, reference);
		}
	}
	
	/**
	 * Removes specified amount of ancient adena from player inventory.
	 * @param process : String Identifier of process triggering this action
	 * @param count : int Quantity of adena to be removed
	 * @param actor : L2PcInstance Player requesting the item add
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return boolean : true if adena was reduced
	 */
	public boolean reduceAncientAdena(String process, long count, L2PcInstance actor, Object reference) {
		if (count > 0) {
			return destroyItemByItemId(process, ANCIENT_ADENA_ID, count, actor, reference) != null;
		}
		return false;
	}
	
	/**
	 * Adds item in inventory and checks _adena and _ancientAdena
	 * @param process : String Identifier of process triggering this action
	 * @param item : L2ItemInstance to be added
	 * @param actor : L2PcInstance Player requesting the item add
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the new item or the updated item in inventory
	 */
	@Override
	public L2ItemInstance addItem(String process, L2ItemInstance item, L2PcInstance actor, Object reference) {
		item = super.addItem(process, item, actor, reference);
		
		if ((item != null) && (item.getId() == ADENA_ID) && !item.equals(_adena)) {
			_adena = item;
		}
		
		if ((item != null) && (item.getId() == ANCIENT_ADENA_ID) && !item.equals(_ancientAdena)) {
			_ancientAdena = item;
		}
		
		if (item != null) {
			// Notify to scripts
			EventDispatcher.getInstance().notifyEventAsync(new PlayerItemAdd(actor, item), actor, item.getItem());
		}
		return item;
	}
	
	/**
	 * Adds item in inventory and checks _adena and _ancientAdena
	 * @param process : String Identifier of process triggering this action
	 * @param itemId : int Item Identifier of the item to be added
	 * @param count : int Quantity of items to be added
	 * @param enchantLevel : int Enchant of the item; -1 to not modify on existing items, for new items use the default enchantLevel when -1
	 * @param actor : L2PcInstance Player requesting the item creation
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the new item or the updated item in inventory
	 */
	@Override
	public L2ItemInstance addItem(String process, int itemId, long count, int enchantLevel, L2PcInstance actor, Object reference) {
		final L2ItemInstance item = super.addItem(process, itemId, count, enchantLevel, actor, reference);
		if (item != null) {
			if ((item.getId() == ADENA_ID) && !item.equals(_adena)) {
				_adena = item;
			}
			
			if ((item.getId() == ANCIENT_ADENA_ID) && !item.equals(_ancientAdena)) {
				_ancientAdena = item;
			}
			if (actor != null) {
				// Send inventory update packet
				if (!general().forceInventoryUpdate()) {
					InventoryUpdate playerIU = new InventoryUpdate();
					playerIU.addItem(item);
					actor.sendPacket(playerIU);
				} else {
					actor.sendPacket(new ItemList(actor, false));
				}
				
				// Update current load as well
				StatusUpdate su = new StatusUpdate(actor);
				su.addAttribute(StatusUpdate.CUR_LOAD, actor.getCurrentLoad());
				actor.sendPacket(su);
				
				// Notify to scripts
				EventDispatcher.getInstance().notifyEventAsync(new PlayerItemAdd(actor, item), actor, item.getItem());
			}
		}
		return item;
	}
	
	/**
	 * Transfers item to another inventory and checks _adena and _ancientAdena
	 * @param process string Identifier of process triggering this action
	 * @param objectId Item Identifier of the item to be transfered
	 * @param count Quantity of items to be transfered
	 * @param target the item container for the item to be transfered.
	 * @param actor the player requesting the item transfer
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the new item or the updated item in inventory
	 */
	@Override
	public L2ItemInstance transferItem(String process, int objectId, long count, ItemContainer target, L2PcInstance actor, Object reference) {
		L2ItemInstance item = super.transferItem(process, objectId, count, target, actor, reference);
		
		if ((_adena != null) && ((_adena.getCount() <= 0) || (_adena.getOwnerId() != getOwnerId()))) {
			_adena = null;
		}
		
		if ((_ancientAdena != null) && ((_ancientAdena.getCount() <= 0) || (_ancientAdena.getOwnerId() != getOwnerId()))) {
			_ancientAdena = null;
		}
		
		// Notify to scripts
		EventDispatcher.getInstance().notifyEventAsync(new PlayerItemTransfer(actor, item, target), item.getItem());
		return item;
	}
	
	/**
	 * Destroy item from inventory and checks _adena and _ancientAdena
	 * @param process : String Identifier of process triggering this action
	 * @param item : L2ItemInstance to be destroyed
	 * @param actor : L2PcInstance Player requesting the item destroy
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	@Override
	public L2ItemInstance destroyItem(String process, L2ItemInstance item, L2PcInstance actor, Object reference) {
		return destroyItem(process, item, item.getCount(), actor, reference);
	}
	
	/**
	 * Destroy item from inventory and checks _adena and _ancientAdena
	 * @param process : String Identifier of process triggering this action
	 * @param item : L2ItemInstance to be destroyed
	 * @param actor : L2PcInstance Player requesting the item destroy
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	@Override
	public L2ItemInstance destroyItem(String process, L2ItemInstance item, long count, L2PcInstance actor, Object reference) {
		item = super.destroyItem(process, item, count, actor, reference);
		
		if ((_adena != null) && (_adena.getCount() <= 0)) {
			_adena = null;
		}
		
		if ((_ancientAdena != null) && (_ancientAdena.getCount() <= 0)) {
			_ancientAdena = null;
		}
		
		// Notify to scripts
		if (item != null) {
			EventDispatcher.getInstance().notifyEventAsync(new PlayerItemDestroy(actor, item), item.getItem());
		}
		return item;
	}
	
	/**
	 * Destroys item from inventory and checks _adena and _ancientAdena
	 * @param process : String Identifier of process triggering this action
	 * @param objectId : int Item Instance identifier of the item to be destroyed
	 * @param count : int Quantity of items to be destroyed
	 * @param actor : L2PcInstance Player requesting the item destroy
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	@Override
	public L2ItemInstance destroyItem(String process, int objectId, long count, L2PcInstance actor, Object reference) {
		L2ItemInstance item = getItemByObjectId(objectId);
		if (item == null) {
			return null;
		}
		return this.destroyItem(process, item, count, actor, reference);
	}
	
	/**
	 * Destroy item from inventory by using its <B>itemId</B> and checks _adena and _ancientAdena
	 * @param process : String Identifier of process triggering this action
	 * @param itemId : int Item identifier of the item to be destroyed
	 * @param count : int Quantity of items to be destroyed
	 * @param actor : L2PcInstance Player requesting the item destroy
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	@Override
	public L2ItemInstance destroyItemByItemId(String process, int itemId, long count, L2PcInstance actor, Object reference) {
		L2ItemInstance item = getItemByItemId(itemId);
		if (item == null) {
			return null;
		}
		return this.destroyItem(process, item, count, actor, reference);
	}
	
	/**
	 * Drop item from inventory and checks _adena and _ancientAdena
	 * @param process : String Identifier of process triggering this action
	 * @param item : L2ItemInstance to be dropped
	 * @param actor : L2PcInstance Player requesting the item drop
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	@Override
	public L2ItemInstance dropItem(String process, L2ItemInstance item, L2PcInstance actor, Object reference) {
		item = super.dropItem(process, item, actor, reference);
		
		if ((_adena != null) && ((_adena.getCount() <= 0) || (_adena.getOwnerId() != getOwnerId()))) {
			_adena = null;
		}
		
		if ((_ancientAdena != null) && ((_ancientAdena.getCount() <= 0) || (_ancientAdena.getOwnerId() != getOwnerId()))) {
			_ancientAdena = null;
		}
		
		// Notify to scripts
		if (item != null) {
			EventDispatcher.getInstance().notifyEventAsync(new PlayerItemDrop(actor, item, item.getLocation()), item.getItem());
		}
		return item;
	}
	
	/**
	 * Drop item from inventory by using its <B>objectID</B> and checks _adena and _ancientAdena
	 * @param process : String Identifier of process triggering this action
	 * @param objectId : int Item Instance identifier of the item to be dropped
	 * @param count : int Quantity of items to be dropped
	 * @param actor : L2PcInstance Player requesting the item drop
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	@Override
	public L2ItemInstance dropItem(String process, int objectId, long count, L2PcInstance actor, Object reference) {
		L2ItemInstance item = super.dropItem(process, objectId, count, actor, reference);
		
		if ((_adena != null) && ((_adena.getCount() <= 0) || (_adena.getOwnerId() != getOwnerId()))) {
			_adena = null;
		}
		
		if ((_ancientAdena != null) && ((_ancientAdena.getCount() <= 0) || (_ancientAdena.getOwnerId() != getOwnerId()))) {
			_ancientAdena = null;
		}
		
		// Notify to scripts
		if (item != null) {
			EventDispatcher.getInstance().notifyEventAsync(new PlayerItemDrop(actor, item, item.getLocation()), item.getItem());
		}
		return item;
	}
	
	/**
	 * <b>Overloaded</b>, when removes item from inventory, remove also owner shortcuts.
	 * @param item : L2ItemInstance to be removed from inventory
	 */
	@Override
	protected boolean removeItem(L2ItemInstance item) {
		// Removes any reference to the item from Shortcut bar
		getOwner().removeItemFromShortCut(item.getObjectId());
		
		// Removes active Enchant Scroll
		if (item.getObjectId() == getOwner().getActiveEnchantItemId()) {
			getOwner().setActiveEnchantItemId(L2PcInstance.ID_NONE);
		}
		
		if (item.getId() == ADENA_ID) {
			_adena = null;
		} else if (item.getId() == ANCIENT_ADENA_ID) {
			_ancientAdena = null;
		}
		
		if (item.isQuestItem()) {
			synchronized (_lock) {
				_questSlots--;
				if (_questSlots < 0) {
					_questSlots = 0;
					LOG.warn("QuestInventory size < 0!");
				}
			}
		}
		return super.removeItem(item);
	}
	
	/**
	 * Refresh the weight of equipment loaded
	 */
	@Override
	public void refreshWeight() {
		super.refreshWeight();
		getOwner().refreshOverloaded();
	}
	
	/**
	 * Get back items in inventory from database
	 */
	@Override
	public void restore() {
		super.restore();
		_adena = getItemByItemId(ADENA_ID);
		_ancientAdena = getItemByItemId(ANCIENT_ADENA_ID);
	}
	
	public static int[][] restoreVisibleInventory(int objectId) {
		int[][] paperdoll = new int[31][3];
		try (var con = ConnectionFactory.getInstance().getConnection();
			var ps = con.prepareStatement("SELECT object_id, item_id, loc_data, enchant_level FROM items WHERE owner_id=? AND loc='PAPERDOLL'")) {
			ps.setInt(1, objectId);
			try (var rs = ps.executeQuery()) {
				while (rs.next()) {
					int slot = rs.getInt("loc_data");
					paperdoll[slot][0] = rs.getInt("object_id");
					paperdoll[slot][1] = rs.getInt("item_id");
					paperdoll[slot][2] = rs.getInt("enchant_level");
					
					// if (slot == Inventory.PAPERDOLL_RHAND) {
					// paperdoll[Inventory.PAPERDOLL_RHAND][0] = rs.getInt("object_id");
					// paperdoll[Inventory.PAPERDOLL_RHAND][1] = rs.getInt("item_id");
					// paperdoll[Inventory.PAPERDOLL_RHAND][2] = rs.getInt("enchant_level");
					// }
				}
			}
		} catch (Exception e) {
			LOG.warn("Could not restore inventory: {}", e.getMessage(), e);
		}
		return paperdoll;
	}
	
	/**
	 * @param itemList the items that needs to be validated.
	 * @param sendMessage if {@code true} will send a message of inventory full.
	 * @param sendSkillMessage if {@code true} will send a message of skill not available.
	 * @return {@code true} if the inventory isn't full after taking new items and items weight add to current load doesn't exceed max weight load.
	 */
	public boolean checkInventorySlotsAndWeight(List<L2Item> itemList, boolean sendMessage, boolean sendSkillMessage) {
		int lootWeight = 0;
		int requiredSlots = 0;
		if (itemList != null) {
			for (L2Item item : itemList) {
				// If the item is not stackable or is stackable and not present in inventory, will need a slot.
				if (!item.isStackable() || (getInventoryItemCount(item.getId(), -1) <= 0)) {
					requiredSlots++;
				}
				lootWeight += item.getWeight();
			}
		}
		
		boolean inventoryStatusOK = validateCapacity(requiredSlots) && validateWeight(lootWeight);
		if (!inventoryStatusOK && sendMessage) {
			_owner.sendPacket(SystemMessageId.SLOTS_FULL);
			if (sendSkillMessage) {
				_owner.sendPacket(SystemMessageId.WEIGHT_EXCEEDED_SKILL_UNAVAILABLE);
			}
		}
		return inventoryStatusOK;
	}
	
	/**
	 * If the item is not stackable or is stackable and not present in inventory, will need a slot.
	 * @param item the item to validate.
	 * @return {@code true} if there is enough room to add the item inventory.
	 */
	public boolean validateCapacity(L2ItemInstance item) {
		if (item.getItem().hasExImmediateEffect() || (item.isStackable() && (getInventoryItemCount(item.getId(), -1) > 0))) {
			return true;
		}
		
		return validateCapacity(1, item.isQuestItem());
	}
	
	/**
	 * If the item is not stackable or is stackable and not present in inventory, will need a slot.
	 * @param itemId the item Id for the item to validate.
	 * @return {@code true} if there is enough room to add the item inventory.
	 */
	public boolean validateCapacityByItemId(int itemId) {
		final L2ItemInstance invItem = getItemByItemId(itemId);
		if ((invItem == null) || !(invItem.isStackable() && (getInventoryItemCount(itemId, -1) > 0))) {
			validateCapacity(1, ItemTable.getInstance().getTemplate(itemId).isQuestItem());
		}
		return true;
	}
	
	@Override
	public boolean validateCapacity(long slots) {
		return validateCapacity(slots, false);
	}
	
	public boolean validateCapacity(long slots, boolean questItem) {
		if (!questItem) {
			return (((_items.size() - _questSlots) + slots) <= _owner.getInventoryLimit());
		}
		return (_questSlots + slots) <= _owner.getQuestInventoryLimit();
	}
	
	@Override
	public boolean validateWeight(long weight) {
		// Disable weight check for GMs.
		if (_owner.isGM() && _owner.getDietMode() && _owner.getAccessLevel().allowTransaction()) {
			return true;
		}
		return ((_totalWeight + weight) <= _owner.getMaxLoad());
	}
	
	/**
	 * Set inventory block for specified IDs<br>
	 * array reference is used for {@link PcInventory#_blockItems}
	 * @param items array of Ids to block/allow
	 * @param mode blocking mode {@link PcInventory#_blockMode}
	 */
	public void setInventoryBlock(int[] items, int mode) {
		_blockMode = mode;
		_blockItems = items;
		
		_owner.sendPacket(new ItemList(_owner, false));
	}
	
	/**
	 * Unblock blocked itemIds
	 */
	public void unblock() {
		_blockMode = -1;
		_blockItems = null;
		
		_owner.sendPacket(new ItemList(_owner, false));
	}
	
	/**
	 * Check if player inventory is in block mode.
	 * @return true if some itemIds blocked
	 */
	public boolean hasInventoryBlock() {
		return ((_blockMode > -1) && (_blockItems != null) && (_blockItems.length > 0));
	}
	
	/**
	 * Block all player items
	 */
	public void blockAllItems() {
		// temp fix, some id must be sended
		setInventoryBlock(new int[] {
			(ItemTable.getInstance().getArraySize() + 2)
		}, 1);
	}
	
	/**
	 * Return block mode
	 * @return int {@link PcInventory#_blockMode}
	 */
	public int getBlockMode() {
		return _blockMode;
	}
	
	/**
	 * Return TIntArrayList with blocked item ids
	 * @return TIntArrayList
	 */
	public int[] getBlockItems() {
		return _blockItems;
	}
	
	/**
	 * Check if player can use item by itemid
	 * @param itemId int
	 * @return true if can use
	 */
	public boolean canManipulateWithItemId(int itemId) {
		return ((_blockMode != 0) || !Util.contains(_blockItems, itemId)) && ((_blockMode != 1) || Util.contains(_blockItems, itemId));
	}
	
	@Override
	protected void addItem(L2ItemInstance item) {
		if (item.isQuestItem()) {
			synchronized (_lock) {
				_questSlots++;
			}
		}
		super.addItem(item);
	}
	
	public int getSize(boolean quest) {
		if (quest) {
			return _questSlots;
		}
		return getSize() - _questSlots;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + _owner + "]";
	}
	
	/**
	 * Apply skills of inventory items
	 */
	public void applyItemSkills() {
		for (L2ItemInstance item : _items) {
			item.giveSkillsToOwner();
			item.applyEnchantStats();
		}
	}
	
	private static final class BowCrossRodListener implements PaperdollListener {
		private static final BowCrossRodListener instance = new BowCrossRodListener();
		
		public static BowCrossRodListener getInstance() {
			return instance;
		}
		
		@Override
		public void notifyUnequiped(int slot, L2ItemInstance item, Inventory inventory) {
			if (slot != PAPERDOLL_RHAND) {
				return;
			}
			
			if (item.getItemType() == WeaponType.BOW) {
				L2ItemInstance arrow = inventory.getPaperdollItem(PAPERDOLL_LHAND);
				
				if (arrow != null) {
					inventory.setPaperdollItem(PAPERDOLL_LHAND, null);
				}
			} else if (item.getItemType() == WeaponType.CROSSBOW) {
				L2ItemInstance bolts = inventory.getPaperdollItem(PAPERDOLL_LHAND);
				
				if (bolts != null) {
					inventory.setPaperdollItem(PAPERDOLL_LHAND, null);
				}
			} else if (item.getItemType() == WeaponType.FISHINGROD) {
				L2ItemInstance lure = inventory.getPaperdollItem(PAPERDOLL_LHAND);
				
				if (lure != null) {
					inventory.setPaperdollItem(PAPERDOLL_LHAND, null);
				}
			}
		}
		
		@Override
		public void notifyEquiped(int slot, L2ItemInstance item, Inventory inventory) {
			if (slot != PAPERDOLL_RHAND) {
				return;
			}
			
			if (item.getItemType() == WeaponType.BOW) {
				L2ItemInstance arrow = inventory.findArrowForBow(item.getItem());
				
				if (arrow != null) {
					inventory.setPaperdollItem(PAPERDOLL_LHAND, arrow);
				}
			} else if (item.getItemType() == WeaponType.CROSSBOW) {
				L2ItemInstance bolts = inventory.findBoltForCrossBow(item.getItem());
				
				if (bolts != null) {
					inventory.setPaperdollItem(PAPERDOLL_LHAND, bolts);
				}
			}
		}
	}
	
	private static final class ItemSkillsListener implements PaperdollListener {
		private static final ItemSkillsListener instance = new ItemSkillsListener();
		
		public static ItemSkillsListener getInstance() {
			return instance;
		}
		
		@Override
		public void notifyUnequiped(int slot, L2ItemInstance item, Inventory inventory) {
			if (!(inventory.getOwner() instanceof L2PcInstance)) {
				return;
			}
			final L2PcInstance player = (L2PcInstance) inventory.getOwner();
			
			Skill enchant4Skill, itemSkill;
			L2Item it = item.getItem();
			boolean update = false;
			boolean updateTimeStamp = false;
			
			// Remove augmentation bonuses on unequip
			if (item.isAugmented()) {
				item.getAugmentation().removeBonus(player);
			}
			
			item.unChargeAllShots();
			item.removeElementAttrBonus(player);
			
			// Remove skills bestowed from +4 armor
			if (item.getEnchantLevel() >= 4) {
				enchant4Skill = it.getEnchant4Skill();
				
				if (enchant4Skill != null) {
					player.removeSkill(enchant4Skill, false, enchant4Skill.isPassive());
					update = true;
				}
			}
			
			item.clearEnchantStats();
			
			final SkillHolder[] skills = it.getSkills();
			
			if (skills != null) {
				for (SkillHolder skillInfo : skills) {
					if (skillInfo == null) {
						continue;
					}
					
					itemSkill = skillInfo.getSkill();
					
					if (itemSkill != null) {
						player.removeSkill(itemSkill, false, itemSkill.isPassive());
						update = true;
					} else {
						LOG.warn("Inventory.ItemSkillsListener.Weapon: Incorrect skill: {}.", skillInfo);
					}
				}
			}
			
			if (item.isArmor()) {
				for (L2ItemInstance itm : inventory.getItems()) {
					if (!itm.isEquipped() || (itm.getItem().getSkills() == null) || itm.equals(item)) {
						continue;
					}
					for (SkillHolder sk : itm.getItem().getSkills()) {
						if (player.getSkillLevel(sk.getSkillId()) != -1) {
							continue;
						}
						
						itemSkill = sk.getSkill();
						
						if (itemSkill != null) {
							player.addSkill(itemSkill, false);
							
							if (itemSkill.isActive()) {
								if (!player.hasSkillReuse(itemSkill.getReuseHashCode())) {
									int equipDelay = item.getEquipReuseDelay();
									if (equipDelay > 0) {
										player.addTimeStamp(itemSkill, equipDelay);
										player.disableSkill(itemSkill, equipDelay);
									}
								}
								updateTimeStamp = true;
							}
							update = true;
						}
					}
				}
			}
			
			// Apply skill, if weapon have "skills on unequip"
			Skill unequipSkill = it.getUnequipSkill();
			if (unequipSkill != null) {
				unequipSkill.activateSkill(player, player);
			}
			
			if (update) {
				player.sendSkillList();
				
				if (updateTimeStamp) {
					player.sendPacket(new SkillCoolTime(player));
				}
			}
		}
		
		@Override
		public void notifyEquiped(int slot, L2ItemInstance item, Inventory inventory) {
			if (!(inventory.getOwner() instanceof L2PcInstance)) {
				return;
			}
			
			final L2PcInstance player = (L2PcInstance) inventory.getOwner();
			
			Skill enchant4Skill, itemSkill;
			L2Item it = item.getItem();
			boolean update = false;
			boolean updateTimeStamp = false;
			
			// Apply augmentation bonuses on equip
			if (item.isAugmented()) {
				item.getAugmentation().applyBonus(player);
			}
			item.rechargeShots(true, true);
			item.updateElementAttrBonus(player);
			
			// Add skills bestowed from +4 armor
			if (item.getEnchantLevel() >= 4) {
				enchant4Skill = it.getEnchant4Skill();
				
				if (enchant4Skill != null) {
					player.addSkill(enchant4Skill, false);
					update = true;
				}
			}
			
			item.applyEnchantStats();
			
			final SkillHolder[] skills = it.getSkills();
			
			if (skills != null) {
				for (SkillHolder skillInfo : skills) {
					if (skillInfo == null) {
						continue;
					}
					
					itemSkill = skillInfo.getSkill();
					
					if (itemSkill != null) {
						itemSkill.setReferenceItemId(item.getId());
						player.addSkill(itemSkill, false);
						
						if (itemSkill.isActive()) {
							if (!player.hasSkillReuse(itemSkill.getReuseHashCode())) {
								int equipDelay = item.getEquipReuseDelay();
								if (equipDelay > 0) {
									player.addTimeStamp(itemSkill, equipDelay);
									player.disableSkill(itemSkill, equipDelay);
								}
							}
							updateTimeStamp = true;
						}
						update = true;
					} else {
						LOG.warn("Inventory.ItemSkillsListener.Weapon: Incorrect skill: {}.", skillInfo);
					}
				}
			}
			
			if (update) {
				player.sendSkillList();
				
				if (updateTimeStamp) {
					player.sendPacket(new SkillCoolTime(player));
				}
			}
		}
	}
	
	private static final class ArmorSetListener implements PaperdollListener {
		private static final ArmorSetListener instance = new ArmorSetListener();
		
		public static ArmorSetListener getInstance() {
			return instance;
		}
		
		@Override
		public void notifyEquiped(int slot, L2ItemInstance item, Inventory inventory) {
			if (!(inventory.getOwner() instanceof L2PcInstance player)) {
				return;
			}
			
			// Checks if player is wearing a chest item
			final L2ItemInstance chestItem = inventory.getPaperdollItem(PAPERDOLL_CHEST);
			
			if (chestItem == null) {
				return;
			}
			
			// Checks for armor set for the equipped chest.
			if (!ArmorSetsData.getInstance().isArmorSet(chestItem.getId())) {
				return;
			}
			final L2ArmorSet armorSet = ArmorSetsData.getInstance().getSet(chestItem.getId());
			boolean update = false;
			boolean updateTimeStamp = false;
			// Checks if equipped item is part of set
			if (armorSet.containItem(slot, item.getId())) {
				if (armorSet.containAll(player)) {
					Skill itemSkill;
					final List<SkillHolder> skills = armorSet.getSkills();
					
					if (skills != null) {
						for (SkillHolder holder : skills) {
							
							itemSkill = holder.getSkill();
							if (itemSkill != null) {
								player.addSkill(itemSkill, false);
								
								if (itemSkill.isActive()) {
									if (!player.hasSkillReuse(itemSkill.getReuseHashCode())) {
										int equipDelay = item.getEquipReuseDelay();
										if (equipDelay > 0) {
											player.addTimeStamp(itemSkill, equipDelay);
											player.disableSkill(itemSkill, equipDelay);
										}
									}
									updateTimeStamp = true;
								}
								update = true;
							} else {
								LOG.warn("Inventory.ArmorSetListener: Incorrect skill: {}.", holder);
							}
						}
					}
					
					if (armorSet.containShield(player)) // has shield from set
					{
						for (SkillHolder holder : armorSet.getShieldSkillId()) {
							if (holder.getSkill() != null) {
								player.addSkill(holder.getSkill(), false);
								update = true;
							} else {
								LOG.warn("Inventory.ArmorSetListener: Incorrect skill: {}.", holder);
							}
						}
					}
					
					if (armorSet.isEnchanted6(player)) // has all parts of set enchanted to 6 or more
					{
						for (SkillHolder holder : armorSet.getEnchant6skillId()) {
							if (holder.getSkill() != null) {
								player.addSkill(holder.getSkill(), false);
								update = true;
							} else {
								LOG.warn("Inventory.ArmorSetListener: Incorrect skill: {}.", holder);
							}
						}
					}
				}
			} else if (armorSet.containShield(item.getId())) {
				for (SkillHolder holder : armorSet.getShieldSkillId()) {
					if (holder.getSkill() != null) {
						player.addSkill(holder.getSkill(), false);
						update = true;
					} else {
						LOG.warn("Inventory.ArmorSetListener: Incorrect skill: {}.", holder);
					}
				}
			}
			
			if (update) {
				player.sendSkillList();
				
				if (updateTimeStamp) {
					player.sendPacket(new SkillCoolTime(player));
				}
			}
		}
		
		@Override
		public void notifyUnequiped(int slot, L2ItemInstance item, Inventory inventory) {
			if (!(inventory.getOwner() instanceof L2PcInstance player)) {
				return;
			}
			
			boolean remove = false;
			Skill itemSkill;
			List<SkillHolder> skills = null;
			List<SkillHolder> shieldSkill = null; // shield skill
			List<SkillHolder> skillId6 = null; // enchant +6 skill
			
			if (slot == PAPERDOLL_CHEST) {
				if (!ArmorSetsData.getInstance().isArmorSet(item.getId())) {
					return;
				}
				final L2ArmorSet armorSet = ArmorSetsData.getInstance().getSet(item.getId());
				remove = true;
				skills = armorSet.getSkills();
				shieldSkill = armorSet.getShieldSkillId();
				skillId6 = armorSet.getEnchant6skillId();
			} else {
				L2ItemInstance chestItem = inventory.getPaperdollItem(PAPERDOLL_CHEST);
				if (chestItem == null) {
					return;
				}
				
				L2ArmorSet armorSet = ArmorSetsData.getInstance().getSet(chestItem.getId());
				if (armorSet == null) {
					return;
				}
				
				if (armorSet.containItem(slot, item.getId())) // removed part of set
				{
					remove = true;
					skills = armorSet.getSkills();
					shieldSkill = armorSet.getShieldSkillId();
					skillId6 = armorSet.getEnchant6skillId();
				} else if (armorSet.containShield(item.getId())) // removed shield
				{
					remove = true;
					shieldSkill = armorSet.getShieldSkillId();
				}
			}
			
			if (remove) {
				if (skills != null) {
					for (SkillHolder holder : skills) {
						itemSkill = holder.getSkill();
						if (itemSkill != null) {
							player.removeSkill(itemSkill, false, itemSkill.isPassive());
						} else {
							LOG.warn("Inventory.ArmorSetListener: Incorrect skill: {}.", holder);
						}
					}
				}
				
				if (shieldSkill != null) {
					for (SkillHolder holder : shieldSkill) {
						itemSkill = holder.getSkill();
						if (itemSkill != null) {
							player.removeSkill(itemSkill, false, itemSkill.isPassive());
						} else {
							LOG.warn("Inventory.ArmorSetListener: Incorrect skill: {}.", holder);
						}
					}
				}
				
				if (skillId6 != null) {
					for (SkillHolder holder : skillId6) {
						itemSkill = holder.getSkill();
						if (itemSkill != null) {
							player.removeSkill(itemSkill, false, itemSkill.isPassive());
						} else {
							LOG.warn("Inventory.ArmorSetListener: Incorrect skill: {}.", holder);
						}
					}
				}
				
				player.checkItemRestriction();
				player.sendSkillList();
			}
		}
	}
	
	private static final class BraceletListener implements PaperdollListener {
		private static final BraceletListener instance = new BraceletListener();
		
		public static BraceletListener getInstance() {
			return instance;
		}
		
		@Override
		public void notifyUnequiped(int slot, L2ItemInstance item, Inventory inventory) {
			if (item.getItem().getBodyPart() == L2Item.SLOT_R_BRACELET) {
				inventory.unEquipItemInSlot(PAPERDOLL_DECO1);
				inventory.unEquipItemInSlot(PAPERDOLL_DECO2);
				inventory.unEquipItemInSlot(PAPERDOLL_DECO3);
				inventory.unEquipItemInSlot(PAPERDOLL_DECO4);
				inventory.unEquipItemInSlot(PAPERDOLL_DECO5);
				inventory.unEquipItemInSlot(PAPERDOLL_DECO6);
			}
		}
		
		// Note (April 3, 2009): Currently on equip, talismans do not display properly, do we need checks here to fix this?
		@Override
		public void notifyEquiped(int slot, L2ItemInstance item, Inventory inventory) {
		}
	}
}
