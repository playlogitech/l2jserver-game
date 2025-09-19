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

import java.util.Map;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Map Integer-Integer Converter test.
 * @author Zoey76
 * @version 2.6.3.0
 */
class MapIntegerIntegerConverterTest {
	
	private static final MapIntegerIntegerConverter CONVERTER = new MapIntegerIntegerConverter();
	
	@ParameterizedTest
	@MethodSource("provideKeyValues")
	void convertTest(String keyValues, Map<Integer, Integer> expected) {
		assertEquals(CONVERTER.convert(null, keyValues), expected);
	}
	
	private static Object[][] provideKeyValues() {
		return new Object[][] {
			{
				"264,3600;265,3600;266,3600;267,3600",
				Map.of(264, 3600, 265, 3600, 266, 3600, 267, 3600)
			},
			{
				"264, 3600; 265, 3600; 266, 3600; 267, 3600",
				Map.of(264, 3600, 265, 3600, 266, 3600, 267, 3600)
			},
			{
				"",
				Map.of()
			},
			{
				null,
				Map.of()
			}
		};
	}
}
