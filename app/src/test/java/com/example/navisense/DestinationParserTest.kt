package com.example.navisense

import com.example.navisense.voice.DestinationParser
import org.junit.Assert.assertEquals
import org.junit.Test

class DestinationParserTest {

    @Test
    fun testParseDestinationWithPrefixes() {
        assertEquals("Railway Station", DestinationParser.parse("Take me to the railway station"))
        assertEquals("Central College", DestinationParser.parse("Can you please take me to the central college"))
        assertEquals("City Hospital", DestinationParser.parse("i want to go to city hospital please"))
        assertEquals("Airport", DestinationParser.parse("find a route to the airport"))
        assertEquals("Shivaji Nagar", DestinationParser.parse("guide me to Shivaji Nagar as soon as possible"))
    }

    @Test
    fun testParseDirectDestination() {
        assertEquals("Bus Stand", DestinationParser.parse("bus stand"))
        assertEquals("Pune", DestinationParser.parse("Pune"))
    }

    @Test
    fun testWhitespaceAndPunctuation() {
        assertEquals("Supermarket", DestinationParser.parse("   take me to the supermarket   "))
        assertEquals("Library", DestinationParser.parse("go to library "))
    }

    @Test
    fun testEmptyInput() {
        assertEquals("Destination", DestinationParser.parse(""))
        assertEquals("Destination", DestinationParser.parse("   "))
    }

    @Test
    fun testShortInput() {
        assertEquals("X", DestinationParser.parse("x"))
    }
}
