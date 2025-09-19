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
package com.l2jserver.gameserver.model.events.listeners;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.l2jserver.gameserver.model.events.EventType;
import com.l2jserver.gameserver.model.events.ListenersContainer;
import com.l2jserver.gameserver.model.events.impl.BaseEvent;
import com.l2jserver.gameserver.model.events.returns.AbstractEventReturn;

/**
 * Function event listener provides callback operation with return object possibility.
 * @author UnAfraid
 */
public class FunctionEventListener extends AbstractEventListener {
	private static final Logger LOG = LoggerFactory.getLogger(FunctionEventListener.class);
	
	private final Function<BaseEvent, ? extends AbstractEventReturn> _callback;
	
	@SuppressWarnings("unchecked")
	public FunctionEventListener(ListenersContainer container, EventType type, Function<? extends BaseEvent, ? extends AbstractEventReturn> callback, Object owner) {
		super(container, type, owner);
		_callback = (Function<BaseEvent, ? extends AbstractEventReturn>) callback;
	}
	
	@Override
	public <R extends AbstractEventReturn> R executeEvent(BaseEvent event, Class<R> returnBackClass) {
		try {
			return returnBackClass.cast(_callback.apply(event));
		} catch (Exception e) {
			LOG.warn("Error while invoking {} on {}", event, getOwner(), e);
		}
		return null;
	}
}
