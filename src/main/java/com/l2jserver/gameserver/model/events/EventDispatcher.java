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
package com.l2jserver.gameserver.model.events;

import java.util.Queue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.l2jserver.gameserver.ThreadPoolManager;
import com.l2jserver.gameserver.model.events.impl.BaseEvent;
import com.l2jserver.gameserver.model.events.listeners.AbstractEventListener;
import com.l2jserver.gameserver.model.events.returns.AbstractEventReturn;

/**
 * @author UnAfraid
 */
public final class EventDispatcher {
	private static final Logger LOG = LoggerFactory.getLogger(EventDispatcher.class);
	
	private EventDispatcher() {
	}
	
	public <T extends AbstractEventReturn> T notifyEvent(BaseEvent event) {
		return notifyEvent(event, null, null);
	}
	
	public <T extends AbstractEventReturn> T notifyEvent(BaseEvent event, Class<T> callbackClass) {
		return notifyEvent(event, null, callbackClass);
	}
	
	public <T extends AbstractEventReturn> T notifyEvent(BaseEvent event, ListenersContainer container) {
		return notifyEvent(event, container, null);
	}
	
	public <T extends AbstractEventReturn> T notifyEvent(BaseEvent event, ListenersContainer container, Class<T> callbackClass) {
		try {
			return Containers.Global().hasListener(event.getType()) || ((container != null) && container.hasListener(event.getType())) ? notifyEventImpl(event, container, callbackClass) : null;
		} catch (Exception e) {
			LOG.warn("Couldn't notify event {}", event.getClass().getSimpleName(), e);
		}
		return null;
	}
	
	/**
	 * Executing current listener notification asynchronously
	 * @param event
	 * @param containers
	 */
	public void notifyEventAsync(BaseEvent event, ListenersContainer... containers) {
		if (event == null) {
			throw new NullPointerException("Event cannot be null!");
		}
		
		boolean hasListeners = Containers.Global().hasListener(event.getType());
		if (!hasListeners) {
			for (ListenersContainer container : containers) {
				if (container.hasListener(event.getType())) {
					hasListeners = true;
					break;
				}
			}
		}
		
		if (hasListeners) {
			ThreadPoolManager.getInstance().executeEvent(() -> notifyEventToMultipleContainers(event, containers, null));
		}
	}
	
	/**
	 * Scheduling current listener notification asynchronously after specified delay.
	 * @param event
	 * @param container
	 * @param delay
	 */
	public void notifyEventAsyncDelayed(BaseEvent event, ListenersContainer container, long delay) {
		if (Containers.Global().hasListener(event.getType()) || container.hasListener(event.getType())) {
			ThreadPoolManager.getInstance().scheduleEvent(() -> notifyEvent(event, container, null), delay);
		}
	}
	
	/**
	 * Scheduling current listener notification asynchronously after specified delay.
	 * @param event
	 * @param container
	 * @param delay
	 * @param unit
	 */
	public void notifyEventAsyncDelayed(BaseEvent event, ListenersContainer container, long delay, TimeUnit unit) {
		if (Containers.Global().hasListener(event.getType()) || container.hasListener(event.getType())) {
			ThreadPoolManager.getInstance().scheduleEvent(() -> notifyEvent(event, container, null), delay, unit);
		}
	}
	
	/**
	 * @param <T>
	 * @param event
	 * @param containers
	 * @param callbackClass
	 * @return
	 */
	private <T extends AbstractEventReturn> T notifyEventToMultipleContainers(BaseEvent event, ListenersContainer[] containers, Class<T> callbackClass) {
		if (event == null) {
			throw new NullPointerException("Event cannot be null!");
		}
		
		try {
			T callback = null;
			if (containers != null) {
				// Local listeners container first.
				for (ListenersContainer container : containers) {
					if ((callback == null) || !callback.abort()) {
						callback = notifyToListeners(container.getListeners(event.getType()), event, callbackClass, callback);
					}
				}
			}
			
			// Global listener container.
			if ((callback == null) || !callback.abort()) {
				callback = notifyToListeners(Containers.Global().getListeners(event.getType()), event, callbackClass, callback);
			}
			
			return callback;
		} catch (Exception e) {
			LOG.warn("Couldn't notify event {}", event.getClass().getSimpleName(), e);
		}
		return null;
	}
	
	/**
	 * @param <T>
	 * @param event
	 * @param container
	 * @param callbackClass
	 * @return {@link AbstractEventReturn} object that may keep data from the first listener, or last that breaks notification.
	 */
	private <T extends AbstractEventReturn> T notifyEventImpl(BaseEvent event, ListenersContainer container, Class<T> callbackClass) {
		if (event == null) {
			throw new NullPointerException("Event cannot be null!");
		}
		
		T callback = null;
		// Local listener container first.
		if (container != null) {
			callback = notifyToListeners(container.getListeners(event.getType()), event, callbackClass, callback);
		}
		
		// Global listener container.
		if ((callback == null) || !callback.abort()) {
			callback = notifyToListeners(Containers.Global().getListeners(event.getType()), event, callbackClass, callback);
		}
		
		return callback;
	}
	
	/**
	 * @param <T>
	 * @param listeners
	 * @param event
	 * @param returnBackClass
	 * @param callback
	 * @return
	 */
	private <T extends AbstractEventReturn> T notifyToListeners(Queue<AbstractEventListener> listeners, BaseEvent event, Class<T> returnBackClass, T callback) {
		for (AbstractEventListener listener : listeners) {
			try {
				final T rb = listener.executeEvent(event, returnBackClass);
				if (rb == null) {
					continue;
				}
				
				if ((callback == null) || rb.override()) {
					// Let's check if this listener wants to override previous return object or we simply don't have one
					callback = rb;
				} else if (rb.abort()) {
					// This listener wants to abort the notification to others.
					break;
				}
			} catch (Exception e) {
				LOG.warn("Exception during notification of event: {} listener: {}", event.getClass().getSimpleName(), listener.getClass().getSimpleName(), e);
			}
		}
		
		return callback;
	}
	
	public static EventDispatcher getInstance() {
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder {
		protected static final EventDispatcher _instance = new EventDispatcher();
	}
}
