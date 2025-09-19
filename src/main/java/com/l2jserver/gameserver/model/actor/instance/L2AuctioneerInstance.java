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
package com.l2jserver.gameserver.model.actor.instance;

import static com.l2jserver.gameserver.config.Configuration.character;
import static com.l2jserver.gameserver.config.Configuration.general;
import static java.util.concurrent.TimeUnit.DAYS;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.l2jserver.gameserver.enums.InstanceType;
import com.l2jserver.gameserver.instancemanager.AuctionManager;
import com.l2jserver.gameserver.instancemanager.ClanHallManager;
import com.l2jserver.gameserver.instancemanager.MapRegionManager;
import com.l2jserver.gameserver.model.ClanPrivilege;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.templates.L2NpcTemplate;
import com.l2jserver.gameserver.model.entity.Auction;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.NpcHtmlMessage;

public final class L2AuctioneerInstance extends L2Npc {
	private static final Logger LOG = LoggerFactory.getLogger(L2AuctioneerInstance.class);
	
	private static final int COND_ALL_FALSE = 0;
	private static final int COND_BUSY_BECAUSE_OF_SIEGE = 1;
	private static final int COND_REGULAR = 3;
	
	private int _currentPage = 1;
	
	private final Map<Integer, Auction> _pendingAuctions = new ConcurrentHashMap<>();
	
	public L2AuctioneerInstance(int objectId, L2NpcTemplate template) {
		super(objectId, template);
		setInstanceType(InstanceType.L2AuctioneerInstance);
	}
	
	@Override
	public void onBypassFeedback(L2PcInstance player, String command) {
		int condition = validateCondition(player);
		if (condition <= COND_ALL_FALSE) {
			// TODO: html
			player.sendMessage("Wrong conditions.");
			return;
		} else if (condition == COND_BUSY_BECAUSE_OF_SIEGE) {
			String filename = "data/html/auction/auction-busy.htm";
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile(player.getHtmlPrefix(), filename);
			html.replace("%objectId%", String.valueOf(getObjectId()));
			player.sendPacket(html);
			return;
		} else if (condition == COND_REGULAR) {
			StringTokenizer st = new StringTokenizer(command, " ");
			String actualCommand = st.nextToken(); // Get actual command
			
			String val = "";
			if (st.hasMoreTokens()) {
				val = st.nextToken();
			}
			
			if (actualCommand.equalsIgnoreCase("auction")) {
				if (val.isEmpty()) {
					return;
				}
				
				try {
					int days = Integer.parseInt(val);
					try {
						SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
						long bid = 0;
						if (st.hasMoreTokens()) {
							bid = Math.min(Long.parseLong(st.nextToken()), character().getMaxAdena());
						}
						
						Auction a = new Auction(player.getClan().getHideoutId(), player.getClan(), DAYS.toMillis(days), bid, ClanHallManager.getInstance().getClanHallByOwner(player.getClan()).getName());
						if (_pendingAuctions.get(a.getId()) != null) {
							_pendingAuctions.remove(a.getId());
						}
						
						_pendingAuctions.put(a.getId(), a);
						
						String filename = "data/html/auction/AgitSale3.htm";
						final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
						html.setFile(player.getHtmlPrefix(), filename);
						html.replace("%x%", val);
						html.replace("%AGIT_AUCTION_END%", format.format(a.getEndDate()));
						html.replace("%AGIT_AUCTION_MINBID%", String.valueOf(a.getStartingBid()));
						html.replace("%AGIT_AUCTION_MIN%", String.valueOf(a.getStartingBid()));
						html.replace("%AGIT_AUCTION_DESC%", ClanHallManager.getInstance().getClanHallByOwner(player.getClan()).getDesc());
						html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_sale2");
						html.replace("%objectId%", String.valueOf((getObjectId())));
						player.sendPacket(html);
					} catch (Exception e) {
						player.sendMessage("Invalid bid!");
					}
				} catch (Exception e) {
					player.sendMessage("Invalid auction duration!");
				}
				return;
			} else if (actualCommand.equalsIgnoreCase("confirmAuction")) {
				try {
					Auction a = _pendingAuctions.get(player.getClan().getHideoutId());
					a.confirmAuction();
					_pendingAuctions.remove(player.getClan().getHideoutId());
				} catch (Exception e) {
					player.sendMessage("Invalid auction");
				}
				return;
			} else if (actualCommand.equalsIgnoreCase("bidding")) {
				if (val.isEmpty()) {
					return;
				}
				
				if (general().debug()) {
					LOG.debug("bidding show successful");
				}
				
				try {
					SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
					int auctionId = Integer.parseInt(val);
					
					if (general().debug()) {
						LOG.debug("auction test started");
					}
					
					String filename = "data/html/auction/AgitAuctionInfo.htm";
					Auction a = AuctionManager.getInstance().getAuction(auctionId);
					
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(player.getHtmlPrefix(), filename);
					if (a != null) {
						html.replace("%AGIT_NAME%", a.getItemName());
						html.replace("%OWNER_PLEDGE_NAME%", a.getSellerClanName());
						html.replace("%OWNER_PLEDGE_MASTER%", a.getSellerName());
						html.replace("%AGIT_SIZE%", String.valueOf(ClanHallManager.getInstance().getAuctionableHallById(a.getItemId()).getGrade() * 10));
						html.replace("%AGIT_LEASE%", String.valueOf(ClanHallManager.getInstance().getAuctionableHallById(a.getItemId()).getLease()));
						html.replace("%AGIT_LOCATION%", ClanHallManager.getInstance().getAuctionableHallById(a.getItemId()).getLocation());
						html.replace("%AGIT_AUCTION_END%", format.format(a.getEndDate()));
						html.replace("%AGIT_AUCTION_REMAIN%", ((a.getEndDate() - System.currentTimeMillis()) / 3600000) + " hours " + (((a.getEndDate() - System.currentTimeMillis()) / 60000) % 60) + " minutes");
						html.replace("%AGIT_AUCTION_MINBID%", String.valueOf(a.getStartingBid()));
						html.replace("%AGIT_AUCTION_COUNT%", String.valueOf(a.getBidders().size()));
						html.replace("%AGIT_AUCTION_DESC%", ClanHallManager.getInstance().getAuctionableHallById(a.getItemId()).getDesc());
						html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_list");
						html.replace("%AGIT_LINK_BIDLIST%", "bypass -h npc_" + getObjectId() + "_bidlist " + a.getId());
						html.replace("%AGIT_LINK_RE%", "bypass -h npc_" + getObjectId() + "_bid1 " + a.getId());
					} else {
						LOG.debug("Auctioneer Auction null for AuctionId : {}", auctionId);
					}
					
					player.sendPacket(html);
				} catch (Exception e) {
					player.sendMessage("Invalid auction!");
				}
				return;
			} else if (actualCommand.equalsIgnoreCase("bid")) {
				if (val.isEmpty()) {
					return;
				}
				
				try {
					int auctionId = Integer.parseInt(val);
					try {
						long bid = 0;
						if (st.hasMoreTokens()) {
							bid = Math.min(Long.parseLong(st.nextToken()), character().getMaxAdena());
						}
						
						AuctionManager.getInstance().getAuction(auctionId).setBid(player, bid);
					} catch (Exception e) {
						player.sendMessage("Invalid bid!");
					}
				} catch (Exception e) {
					player.sendMessage("Invalid auction!");
				}
				return;
			} else if (actualCommand.equalsIgnoreCase("bid1")) {
				if ((player.getClan() == null) || (player.getClan().getLevel() < 2)) {
					player.sendPacket(SystemMessageId.AUCTION_ONLY_CLAN_LEVEL_2_HIGHER);
					return;
				}
				
				if (val.isEmpty()) {
					return;
				}
				
				if (((player.getClan().getAuctionBidAt() > 0) && (player.getClan().getAuctionBidAt() != Integer.parseInt(val))) || (player.getClan().getHideoutId() > 0)) {
					player.sendPacket(SystemMessageId.ALREADY_SUBMITTED_BID);
					return;
				}
				
				try {
					String filename = "data/html/auction/AgitBid1.htm";
					
					long minimumBid = AuctionManager.getInstance().getAuction(Integer.parseInt(val)).getHighestBidderMaxBid();
					if (minimumBid == 0) {
						minimumBid = AuctionManager.getInstance().getAuction(Integer.parseInt(val)).getStartingBid();
					}
					
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(player.getHtmlPrefix(), filename);
					html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_bidding " + val);
					html.replace("%PLEDGE_ADENA%", String.valueOf(player.getClan().getWarehouse().getAdena()));
					html.replace("%AGIT_AUCTION_MINBID%", String.valueOf(minimumBid));
					html.replace("npc_%objectId%_bid", "npc_" + getObjectId() + "_bid " + val);
					player.sendPacket(html);
					return;
				} catch (Exception e) {
					player.sendMessage("Invalid auction!");
				}
				return;
			} else if (actualCommand.equalsIgnoreCase("list")) {
				List<Auction> auctions = AuctionManager.getInstance().getAuctions();
				SimpleDateFormat format = new SimpleDateFormat("yy/MM/dd");
				// Limit for make new page, prevent client crash.
				
				int limit = 10;
				int start;
				int i = 1;
				
				if (val.isEmpty()) {
					start = 1;
					_currentPage = 1;
				} else {
					start = (limit * (Integer.parseInt(val) - 1)) + 1;
					limit *= Integer.parseInt(val);
					_currentPage = Integer.parseInt(val);
				}
				
				if (general().debug()) {
					LOG.debug("cmd list: auction test started");
				}
				
				StringBuilder items = new StringBuilder();
				items.append("<table width=280 border=0>");
				
				for (Auction a : auctions) {
					if (a == null) {
						continue;
					}
					
					if (i > limit) {
						break;
					} else if (i < start) {
						i++;
						continue;
					} else {
						i++;
					}
					
					items.append("<tr>");
					items.append("<td width=70 align=left><font color=\"99B3FF\">");
					items.append(ClanHallManager.getInstance().getAuctionableHallById(a.getItemId()).getLocation());
					items.append("</font></td>");
					items.append("<td width=70 align=left><font color=\"FFFF99\"><a action=\"bypass -h npc_");
					items.append(getObjectId());
					items.append("_bidding ");
					items.append(a.getId());
					items.append("\">");
					items.append(a.getItemName());
					items.append("[");
					items.append(AuctionManager.getInstance().getAuction(a.getId()).getBidders().size());
					items.append("]</a></font></td>");
					items.append("<td width=70 align=left>");
					items.append(format.format(a.getEndDate()));
					items.append("</td>");
					items.append("<td width=70 align=left><font color=\"99FFFF\">");
					items.append(a.getStartingBid());
					items.append("</font></td>");
					items.append("</tr>");
					items.append("<tr><td height=5></td></tr>");
				}
				items.append("</table>");
				
				items.append("<table width=280 border=0>");
				items.append("<tr>");
				if (_currentPage > 1) {
					items.append("<td width=80 align=left>");
					items.append("<button action=\"bypass -h npc_");
					items.append(getObjectId());
					items.append("_list ");
					items.append(_currentPage - 1);
					items.append("\"");
					items.append(" value=\"Previous\" width=80 height=27 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
					items.append("</td>");
				}
				items.append("<td width=80 align=left>");
				items.append("<button action=\"bypass -h npc_");
				items.append(getObjectId());
				items.append("_list ");
				items.append(_currentPage + 1);
				items.append("\"");
				items.append(" value=\"Next\" width=80 height=27 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
				items.append("</td>");
				items.append("<td width=120></td>");
				items.append("</tr>");
				items.append("</table>");
				
				String filename = "data/html/auction/AgitAuctionList.htm";
				
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player.getHtmlPrefix(), filename);
				html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_start");
				html.replace("%itemsField%", items.toString());
				player.sendPacket(html);
				return;
			} else if (actualCommand.equalsIgnoreCase("bidlist")) {
				int auctionId;
				if (val.isEmpty()) {
					if (player.getClan().getAuctionBidAt() <= 0) {
						return;
					}
					auctionId = player.getClan().getAuctionBidAt();
				} else {
					auctionId = Integer.parseInt(val);
				}
				
				if (general().debug()) {
					LOG.debug("cmd bidlist: auction test started");
				}
				
				final var bidders = new StringBuilder();
				for (var b : AuctionManager.getInstance().getAuction(auctionId).getBidders().values()) {
					bidders.append("<tr><td>")
						.append(b.getClanName())
						.append("</td><td>")
						.append(b.getName())
						.append("</td><td>")
						.append(b.getTimeBid().get(Calendar.YEAR))
						.append("/")
						.append(b.getTimeBid().get(Calendar.MONTH) + 1)
						.append("/")
						.append(b.getTimeBid().get(Calendar.DATE))
						.append("</td><td>")
						.append(b.getBid())
						.append("</td>")
						.append("</tr>");
				}
				
				final var fileName = "data/html/auction/AgitBidderList.htm";
				final var html = new NpcHtmlMessage(getObjectId());
				html.setFile(player.getHtmlPrefix(), fileName);
				html.replace("%AGIT_LIST%", bidders.toString());
				html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_bidding " + auctionId);
				player.sendPacket(html);
				return;
			} else if (actualCommand.equalsIgnoreCase("selectedItems")) {
				if ((player.getClan() != null) && (player.getClan().getHideoutId() == 0) && (player.getClan().getAuctionBidAt() > 0)) {
					SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
					String filename = "data/html/auction/AgitBidInfo.htm";
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(player.getHtmlPrefix(), filename);
					Auction a = AuctionManager.getInstance().getAuction(player.getClan().getAuctionBidAt());
					if (a != null) {
						html.replace("%AGIT_NAME%", a.getItemName());
						html.replace("%OWNER_PLEDGE_NAME%", a.getSellerClanName());
						html.replace("%OWNER_PLEDGE_MASTER%", a.getSellerName());
						html.replace("%AGIT_SIZE%", String.valueOf(ClanHallManager.getInstance().getAuctionableHallById(a.getItemId()).getGrade() * 10));
						html.replace("%AGIT_LEASE%", String.valueOf(ClanHallManager.getInstance().getAuctionableHallById(a.getItemId()).getLease()));
						html.replace("%AGIT_LOCATION%", ClanHallManager.getInstance().getAuctionableHallById(a.getItemId()).getLocation());
						html.replace("%AGIT_AUCTION_END%", format.format(a.getEndDate()));
						html.replace("%AGIT_AUCTION_REMAIN%", ((a.getEndDate() - System.currentTimeMillis()) / 3600000) + " hours " + (((a.getEndDate() - System.currentTimeMillis()) / 60000) % 60) + " minutes");
						html.replace("%AGIT_AUCTION_MINBID%", String.valueOf(a.getStartingBid()));
						html.replace("%AGIT_AUCTION_MYBID%", String.valueOf(a.getBidders().get(player.getClanId()).getBid()));
						html.replace("%AGIT_AUCTION_DESC%", ClanHallManager.getInstance().getAuctionableHallById(a.getItemId()).getDesc());
						html.replace("%objectId%", String.valueOf(getObjectId()));
						html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_start");
					} else {
						LOG.warn("Auctioneer Auction null for AuctionBiddedAt : {}", player.getClan().getAuctionBidAt());
					}
					
					player.sendPacket(html);
					return;
				} else if ((player.getClan() != null) && (AuctionManager.getInstance().getAuction(player.getClan().getHideoutId()) != null)) {
					SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
					String filename = "data/html/auction/AgitSaleInfo.htm";
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(player.getHtmlPrefix(), filename);
					Auction a = AuctionManager.getInstance().getAuction(player.getClan().getHideoutId());
					if (a != null) {
						html.replace("%AGIT_NAME%", a.getItemName());
						html.replace("%AGIT_OWNER_PLEDGE_NAME%", a.getSellerClanName());
						html.replace("%OWNER_PLEDGE_MASTER%", a.getSellerName());
						html.replace("%AGIT_SIZE%", String.valueOf(ClanHallManager.getInstance().getAuctionableHallById(a.getItemId()).getGrade() * 10));
						html.replace("%AGIT_LEASE%", String.valueOf(ClanHallManager.getInstance().getAuctionableHallById(a.getItemId()).getLease()));
						html.replace("%AGIT_LOCATION%", ClanHallManager.getInstance().getAuctionableHallById(a.getItemId()).getLocation());
						html.replace("%AGIT_AUCTION_END%", format.format(a.getEndDate()));
						html.replace("%AGIT_AUCTION_REMAIN%", ((a.getEndDate() - System.currentTimeMillis()) / 3600000) + " hours " + (((a.getEndDate() - System.currentTimeMillis()) / 60000) % 60) + " minutes");
						html.replace("%AGIT_AUCTION_MINBID%", String.valueOf(a.getStartingBid()));
						html.replace("%AGIT_AUCTION_BIDCOUNT%", String.valueOf(a.getBidders().size()));
						html.replace("%AGIT_AUCTION_DESC%", ClanHallManager.getInstance().getAuctionableHallById(a.getItemId()).getDesc());
						html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_start");
						html.replace("%id%", String.valueOf(a.getId()));
						html.replace("%objectId%", String.valueOf(getObjectId()));
					} else {
						LOG.warn("Auctioneer Auction null for getHasHideout : {}", player.getClan().getHideoutId());
					}
					
					player.sendPacket(html);
					return;
				} else if ((player.getClan() != null) && (player.getClan().getHideoutId() != 0)) {
					int ItemId = player.getClan().getHideoutId();
					String filename = "data/html/auction/AgitInfo.htm";
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(player.getHtmlPrefix(), filename);
					if (ClanHallManager.getInstance().getAuctionableHallById(ItemId) != null) {
						html.replace("%AGIT_NAME%", ClanHallManager.getInstance().getAuctionableHallById(ItemId).getName());
						html.replace("%AGIT_OWNER_PLEDGE_NAME%", player.getClan().getName());
						html.replace("%OWNER_PLEDGE_MASTER%", player.getClan().getLeaderName());
						html.replace("%AGIT_SIZE%", String.valueOf(ClanHallManager.getInstance().getAuctionableHallById(ItemId).getGrade() * 10));
						html.replace("%AGIT_LEASE%", String.valueOf(ClanHallManager.getInstance().getAuctionableHallById(ItemId).getLease()));
						html.replace("%AGIT_LOCATION%", ClanHallManager.getInstance().getAuctionableHallById(ItemId).getLocation());
						html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_start");
						html.replace("%objectId%", String.valueOf(getObjectId()));
					} else {
						LOG.warn("Clan Hall ID NULL : {} Can be caused by concurrent write in ClanHallManager", ItemId);
					}
					
					player.sendPacket(html);
					return;
				} else if ((player.getClan() != null) && (player.getClan().getHideoutId() == 0)) {
					player.sendPacket(SystemMessageId.NO_OFFERINGS_OWN_OR_MADE_BID_FOR);
					return;
				} else if (player.getClan() == null) {
					player.sendPacket(SystemMessageId.CANNOT_PARTICIPATE_IN_AN_AUCTION);
					return;
				}
			} else if (actualCommand.equalsIgnoreCase("cancelBid")) {
				long bid = AuctionManager.getInstance().getAuction(player.getClan().getAuctionBidAt()).getBidders().get(player.getClanId()).getBid();
				String filename = "data/html/auction/AgitBidCancel.htm";
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player.getHtmlPrefix(), filename);
				html.replace("%AGIT_BID%", String.valueOf(bid));
				html.replace("%AGIT_BID_REMAIN%", String.valueOf((long) (bid * 0.9)));
				html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_selectedItems");
				html.replace("%objectId%", String.valueOf(getObjectId()));
				player.sendPacket(html);
				return;
			} else if (actualCommand.equalsIgnoreCase("doCancelBid")) {
				if (AuctionManager.getInstance().getAuction(player.getClan().getAuctionBidAt()) != null) {
					AuctionManager.getInstance().getAuction(player.getClan().getAuctionBidAt()).cancelBid(player.getClanId());
					player.sendPacket(SystemMessageId.CANCELED_BID);
				}
				return;
			} else if (actualCommand.equalsIgnoreCase("cancelAuction")) {
				if (!player.hasClanPrivilege(ClanPrivilege.CH_AUCTION)) {
					String filename = "data/html/auction/not_authorized.htm";
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(player.getHtmlPrefix(), filename);
					html.replace("%objectId%", String.valueOf(getObjectId()));
					player.sendPacket(html);
					return;
				}
				String filename = "data/html/auction/AgitSaleCancel.htm";
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player.getHtmlPrefix(), filename);
				html.replace("%AGIT_DEPOSIT%", String.valueOf(ClanHallManager.getInstance().getClanHallByOwner(player.getClan()).getLease()));
				html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_selectedItems");
				html.replace("%objectId%", String.valueOf(getObjectId()));
				player.sendPacket(html);
				return;
			} else if (actualCommand.equalsIgnoreCase("doCancelAuction")) {
				if (AuctionManager.getInstance().getAuction(player.getClan().getHideoutId()) != null) {
					AuctionManager.getInstance().getAuction(player.getClan().getHideoutId()).cancelAuction();
					player.sendMessage("Your auction has been canceled");
				}
				return;
			} else if (actualCommand.equalsIgnoreCase("sale2")) {
				String filename = "data/html/auction/AgitSale2.htm";
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player.getHtmlPrefix(), filename);
				html.replace("%AGIT_LAST_PRICE%", String.valueOf(ClanHallManager.getInstance().getClanHallByOwner(player.getClan()).getLease()));
				html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_sale");
				html.replace("%objectId%", String.valueOf(getObjectId()));
				player.sendPacket(html);
				return;
			} else if (actualCommand.equalsIgnoreCase("sale")) {
				if (!player.hasClanPrivilege(ClanPrivilege.CH_AUCTION)) {
					String filename = "data/html/auction/not_authorized.htm";
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(player.getHtmlPrefix(), filename);
					html.replace("%objectId%", String.valueOf(getObjectId()));
					player.sendPacket(html);
					return;
				}
				String filename = "data/html/auction/AgitSale1.htm";
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player.getHtmlPrefix(), filename);
				html.replace("%AGIT_DEPOSIT%", String.valueOf(ClanHallManager.getInstance().getClanHallByOwner(player.getClan()).getLease()));
				html.replace("%AGIT_PLEDGE_ADENA%", String.valueOf(player.getClan().getWarehouse().getAdena()));
				html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_selectedItems");
				html.replace("%objectId%", String.valueOf(getObjectId()));
				player.sendPacket(html);
				return;
			} else if (actualCommand.equalsIgnoreCase("rebid")) {
				SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
				if (!player.hasClanPrivilege(ClanPrivilege.CH_AUCTION)) {
					String filename = "data/html/auction/not_authorized.htm";
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(player.getHtmlPrefix(), filename);
					html.replace("%objectId%", String.valueOf(getObjectId()));
					player.sendPacket(html);
					return;
				}
				try {
					String filename = "data/html/auction/AgitBid2.htm";
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(player.getHtmlPrefix(), filename);
					Auction a = AuctionManager.getInstance().getAuction(player.getClan().getAuctionBidAt());
					if (a != null) {
						html.replace("%AGIT_AUCTION_BID%", String.valueOf(a.getBidders().get(player.getClanId()).getBid()));
						html.replace("%AGIT_AUCTION_MINBID%", String.valueOf(a.getStartingBid()));
						html.replace("%AGIT_AUCTION_END%", format.format(a.getEndDate()));
						html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_selectedItems");
						html.replace("npc_%objectId%_bid1", "npc_" + getObjectId() + "_bid1 " + a.getId());
					} else {
						LOG.warn("Auctioneer Auction null for AuctionBiddedAt : {}", player.getClan().getAuctionBidAt());
					}
					
					player.sendPacket(html);
				} catch (Exception e) {
					player.sendMessage("Invalid auction!");
				}
				return;
			} else if (actualCommand.equalsIgnoreCase("location")) {
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player.getHtmlPrefix(), "data/html/auction/location.htm");
				html.replace("%location%", MapRegionManager.getInstance().getClosestTownName(player));
				html.replace("%LOCATION%", getPictureName(player));
				html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_start");
				player.sendPacket(html);
				return;
			} else if (actualCommand.equalsIgnoreCase("start")) {
				showChatWindow(player);
				return;
			}
		}
		
		super.onBypassFeedback(player, command);
	}
	
	@Override
	public void showChatWindow(L2PcInstance player) {
		String filename;
		int condition = validateCondition(player);
		if (condition == COND_BUSY_BECAUSE_OF_SIEGE) {
			filename = "data/html/auction/auction-busy.htm"; // Busy because of siege
		} else {
			filename = "data/html/auction/auction.htm";
		}
		
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player.getHtmlPrefix(), filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcId%", String.valueOf(getId()));
		html.replace("%npcname%", getName());
		player.sendPacket(html);
	}
	
	private int validateCondition(L2PcInstance player) {
		if ((getCastle() != null) && (getCastle().getResidenceId() > 0)) {
			if (getCastle().getSiege().isInProgress()) {
				return COND_BUSY_BECAUSE_OF_SIEGE; // Busy because of siege
			}
			return COND_REGULAR;
		}
		
		return COND_ALL_FALSE;
	}
	
	private String getPictureName(L2PcInstance plyr) {
		int nearestTownId = MapRegionManager.getInstance().getMapRegionLocId(plyr);
		return switch (nearestTownId) {
			case 911 -> "GLUDIN";
			case 912 -> "GLUDIO";
			case 916 -> "DION";
			case 918 -> "GIRAN";
			case 1537 -> "RUNE";
			case 1538 -> "GODARD";
			case 1714 -> "SCHUTTGART";
			default -> "ADEN";
		};
	}
}