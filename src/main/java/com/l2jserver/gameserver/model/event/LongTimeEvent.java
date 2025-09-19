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
package com.l2jserver.gameserver.model.event;

import static com.l2jserver.gameserver.config.Configuration.server;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.l2jserver.gameserver.ThreadPoolManager;
import com.l2jserver.gameserver.data.sql.impl.AnnouncementsTable;
import com.l2jserver.gameserver.data.xml.impl.NpcData;
import com.l2jserver.gameserver.datatables.EventDroplist;
import com.l2jserver.gameserver.datatables.ItemTable;
import com.l2jserver.gameserver.model.Location;
import com.l2jserver.gameserver.model.announce.EventAnnouncement;
import com.l2jserver.gameserver.model.drops.DropListScope;
import com.l2jserver.gameserver.model.drops.GeneralDropItem;
import com.l2jserver.gameserver.model.quest.Quest;
import com.l2jserver.gameserver.script.DateRange;
import com.l2jserver.gameserver.util.Broadcast;

/**
 * Parent class for long time events.<br>
 * Maintains config reading, spawn of NPC's, adding of event's drop.
 * @author GKR
 */
public class LongTimeEvent extends Quest {
	
	private static final Logger LOG = LoggerFactory.getLogger(LongTimeEvent.class);
	
	private String _eventName;
	
	// Messages
	private String _onEnterMsg = "Event is in process";
	protected String _endMsg = "Event ends!";
	
	private DateRange _eventPeriod = null;
	private DateRange _dropPeriod;
	
	// NPC's to spawm and their spawn points
	private final List<NpcSpawn> _spawnList = new ArrayList<>();
	
	// Drop data for event
	private final List<GeneralDropItem> _dropList = new ArrayList<>();
	
	private static class NpcSpawn {
		protected final Location loc;
		protected final int npcId;
		
		protected NpcSpawn(int pNpcId, Location spawnLoc) {
			loc = spawnLoc;
			npcId = pNpcId;
		}
	}
	
	public LongTimeEvent() {
		loadConfig();
		
		if (_eventPeriod != null) {
			if (_eventPeriod.isWithinRange(new Date())) {
				startEvent();
				LOG.info("Event {} active till {}.", _eventName, _eventPeriod.getEndDate());
			} else if (_eventPeriod.getStartDate().after(new Date())) {
				long delay = _eventPeriod.getStartDate().getTime() - System.currentTimeMillis();
				ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleStart(), delay);
				LOG.info("Event {} will be started at {}.", _eventName, _eventPeriod.getStartDate());
			} else {
				LOG.info("Event {} has passed... Ignored.", _eventName);
			}
		}
	}
	
	/**
	 * Load event configuration file
	 */
	private void loadConfig() {
		File configFile = new File(server().getDatapackRoot(), "data/events/" + getName() + ".xml");
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(configFile);
			if (!doc.getDocumentElement().getNodeName().equalsIgnoreCase("event")) {
				throw new NullPointerException("WARNING!!! " + getName() + " event: bad config file!");
			}
			_eventName = doc.getDocumentElement().getAttributes().getNamedItem("name").getNodeValue();
			String period = doc.getDocumentElement().getAttributes().getNamedItem("active").getNodeValue();
			_eventPeriod = DateRange.parse(period, new SimpleDateFormat("dd MM yyyy", Locale.US));
			
			if (doc.getDocumentElement().getAttributes().getNamedItem("dropPeriod") != null) {
				String dropPeriod = doc.getDocumentElement().getAttributes().getNamedItem("dropPeriod").getNodeValue();
				_dropPeriod = DateRange.parse(dropPeriod, new SimpleDateFormat("dd MM yyyy", Locale.US));
				// Check if drop period is within range of event period
				if (!_eventPeriod.isWithinRange(_dropPeriod.getStartDate()) || !_eventPeriod.isWithinRange(_dropPeriod.getEndDate())) {
					_dropPeriod = _eventPeriod;
				}
			} else {
				_dropPeriod = _eventPeriod; // Drop period, if not specified, assumes all event period.
			}
			
			if (_eventPeriod == null) {
				throw new NullPointerException("WARNING!!! " + getName() + " event: illegal event period");
			}
			
			Date today = new Date();
			
			if (_eventPeriod.getStartDate().after(today) || _eventPeriod.isWithinRange(today)) {
				Node first = doc.getDocumentElement().getFirstChild();
				for (Node n = first; n != null; n = n.getNextSibling()) {
					// Loading droplist
					if (n.getNodeName().equalsIgnoreCase("droplist")) {
						for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
							if (d.getNodeName().equalsIgnoreCase("add")) {
								try {
									int itemId = Integer.parseInt(d.getAttributes().getNamedItem("item").getNodeValue());
									int minCount = Integer.parseInt(d.getAttributes().getNamedItem("min").getNodeValue());
									int maxCount = Integer.parseInt(d.getAttributes().getNamedItem("max").getNodeValue());
									String chance = d.getAttributes().getNamedItem("chance").getNodeValue();
									int finalChance = 0;
									
									if (!chance.isEmpty() && chance.endsWith("%")) {
										finalChance = Integer.parseInt(chance.substring(0, chance.length() - 1)) * 10000;
									}
									
									if (ItemTable.getInstance().getTemplate(itemId) == null) {
										LOG.warn("{} event: {} is wrong item id, item was not added in droplist", getName(), itemId);
										continue;
									}
									
									if (minCount > maxCount) {
										LOG.warn("{} event: item {} - min greater than max, item was not added in droplist", getName(), itemId);
										continue;
									}
									
									if ((finalChance < 10000) || (finalChance > 1000000)) {
										LOG.warn("{} event: item {} - incorrect drop chance, item was not added in droplist", getName(), itemId);
										continue;
									}
									
									_dropList.add((GeneralDropItem) DropListScope.STATIC.newDropItem(itemId, minCount, maxCount, finalChance));
								} catch (NumberFormatException nfe) {
									LOG.warn("Wrong number format in config.xml droplist block for {} event", getName());
								}
							}
						}
					} else if (n.getNodeName().equalsIgnoreCase("spawnlist")) {
						// Loading spawnlist
						for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
							if (d.getNodeName().equalsIgnoreCase("add")) {
								try {
									int npcId = Integer.parseInt(d.getAttributes().getNamedItem("npc").getNodeValue());
									int xPos = Integer.parseInt(d.getAttributes().getNamedItem("x").getNodeValue());
									int yPos = Integer.parseInt(d.getAttributes().getNamedItem("y").getNodeValue());
									int zPos = Integer.parseInt(d.getAttributes().getNamedItem("z").getNodeValue());
									int heading = d.getAttributes().getNamedItem("heading").getNodeValue() != null ? Integer.parseInt(d.getAttributes().getNamedItem("heading").getNodeValue()) : 0;
									
									if (NpcData.getInstance().getTemplate(npcId) == null) {
										LOG.warn("{} event: {} is wrong NPC id, NPC was not added in spawnlist.", getName(), npcId);
										continue;
									}
									
									_spawnList.add(new NpcSpawn(npcId, new Location(xPos, yPos, zPos, heading)));
								} catch (NumberFormatException nfe) {
									LOG.warn("Wrong number format in config.xml spawnlist block for {} event.", getName());
								}
							}
						}
					} else if (n.getNodeName().equalsIgnoreCase("messages")) {
						// Loading Messages
						for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
							if (d.getNodeName().equalsIgnoreCase("add")) {
								String msgType = d.getAttributes().getNamedItem("type").getNodeValue();
								String msgText = d.getAttributes().getNamedItem("text").getNodeValue();
								if ((msgType != null) && (msgText != null)) {
									if (msgType.equalsIgnoreCase("onEnd")) {
										_endMsg = msgText;
									} else if (msgType.equalsIgnoreCase("onEnter")) {
										_onEnterMsg = msgText;
									}
								}
							}
						}
					}
				}
			}
		} catch (Exception ex) {
			LOG.warn("{} event: error reading {}!", getName(), configFile.getAbsolutePath(), ex);
		}
	}
	
	/**
	 * Maintenance event start - adds global drop, spawns event NPC's, shows start announcement.
	 */
	protected void startEvent() {
		long currentTime = System.currentTimeMillis();
		// Add drop
		if (_dropList != null) {
			if (currentTime < _dropPeriod.getEndDate().getTime()) {
				for (GeneralDropItem drop : _dropList) {
					EventDroplist.getInstance().addGlobalDrop(drop.getItemId(), drop.getMin(), drop.getMax(), (int) drop.getChance(), _dropPeriod);
				}
			}
		}
		
		// Add spawns
		long millisToEventEnd = _eventPeriod.getEndDate().getTime() - currentTime;
		if (_spawnList != null) {
			for (NpcSpawn spawn : _spawnList) {
				addSpawn(spawn.npcId, spawn.loc.getX(), spawn.loc.getY(), spawn.loc.getZ(), spawn.loc.getHeading(), false, millisToEventEnd, false);
			}
		}
		
		// Send message on begin
		Broadcast.toAllOnlinePlayers(_onEnterMsg);
		
		// Add announce for entering players
		AnnouncementsTable.getInstance().addAnnouncement(new EventAnnouncement(_eventPeriod, _onEnterMsg));
		
		// Schedule event end (now only for message sending)
		ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleEnd(), millisToEventEnd);
	}
	
	/**
	 * @return event period
	 */
	public DateRange getEventPeriod() {
		return _eventPeriod;
	}
	
	/**
	 * @return {@code true} if now is event period
	 */
	public boolean isEventPeriod() {
		return _eventPeriod.isWithinRange(new Date());
	}
	
	/**
	 * @return {@code true} if now is drop period
	 */
	public boolean isDropPeriod() {
		return _dropPeriod.isWithinRange(new Date());
	}
	
	protected class ScheduleStart implements Runnable {
		@Override
		public void run() {
			startEvent();
		}
	}
	
	protected class ScheduleEnd implements Runnable {
		@Override
		public void run() {
			// Send message on end
			Broadcast.toAllOnlinePlayers(_endMsg);
		}
	}
}
