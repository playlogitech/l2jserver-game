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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Minutes to Milliseconds converter test.
 * @author Zoey76
 * @version 2.6.3.0
 */
class Minutes2MillisecondsConverterTest {
	
	private static final Minutes2MillisecondsConverter CONVERTER = new Minutes2MillisecondsConverter();
	
	@ParameterizedTest
	@MethodSource("provideValidInputs")
	void testConvert(String input, Long expectedResult) {
		final var result = CONVERTER.convert(null, input);
		assertEquals(expectedResult, result);
	}
	
	@ParameterizedTest
	@MethodSource("provideInvalidInputs")
	void testConvertThrowsNumberFormatException(String input) {
		assertThrows(NumberFormatException.class, () -> CONVERTER.convert(null, input));
	}
	
	private static Stream<Object[]> provideValidInputs() {
		// @formatter:off
        return Stream.of(
            new Object[]{"1", 60000L},        // 1 minute to milliseconds
            new Object[]{"0", 0L},            // 0 minutes
            new Object[]{"-3", -180000L},     // Negative minutes
            new Object[]{"", 0L},             // Blank input
            new Object[]{null, 0L}            // Null input
        );
        // @formatter:on
	}
	
	private static Stream<String> provideInvalidInputs() {
		// @formatter:off
        return Stream.of(
            "abc",  // Non-numeric input
            "2.5"   // Invalid format for long
        );
        // @formatter:on
	}
}
