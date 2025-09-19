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
package com.l2jserver.gameserver.config.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Item Holder Converter test.
 * @author Zoey76
 * @version 2.6.3.0
 */
class ItemHolderConverterTest {
	
	private static final ItemHolderConverter CONVERTER = new ItemHolderConverter();
	
	@ParameterizedTest
	@MethodSource("provideItems")
	void convertTest(String input, int id, long count) {
		final var result = CONVERTER.convert(null, input);
		assertEquals(result.getId(), id);
		assertEquals(result.getCount(), count);
	}
	
	private static Object[][] provideItems() {
		return new Object[][] {
			{
				"57,100000",
				57,
				100000
			},
			{
				"57,12345678910",
				57,
				12345678910L
			}
		};
	}
}
