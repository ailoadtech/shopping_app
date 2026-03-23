package com.example.shoppinglist

import org.junit.Assert.*
import org.junit.Test

class ParsingTest {

    @Test
    fun `parseLine with valid store number returns correct item`() {
        val line = "3,Milk"
        val item = parseLine(line)
        assertNotNull(item)
        assertEquals(3, item?.storeNumber)
        assertEquals("Milk", item?.name)
    }

    @Test
    fun `parseLine with empty store returns unassigned item`() {
        val line = ",Eggs"
        val item = parseLine(line)
        assertNotNull(item)
        assertNull(item?.storeNumber)
        assertEquals("Eggs", item?.name)
    }

    @Test
    fun `parseLine with item containing commas returns first part as store`() {
        val line = "2,Red,Apple"
        val item = parseLine(line)
        assertNotNull(item)
        assertEquals(2, item?.storeNumber)
        assertEquals("Red,Apple", item?.name)
    }

    @Test
    fun `parseLine with whitespace trimmed`() {
        val line = "1,  Bread  "
        val item = parseLine(line)
        assertNotNull(item)
        assertEquals(1, item?.storeNumber)
        assertEquals("Bread", item?.name)
    }

    @Test
    fun `parseLine with empty name returns null`() {
        val line = "3,"
        val item = parseLine(line)
        assertNull(item)
    }

    @Test
    fun `parseLine with invalid store number returns null`() {
        val line = "abc,Milk"
        val item = parseLine(line)
        assertNull(item)
    }

    @Test
    fun `parseLine with missing comma returns null`() {
        val line = "Milk"
        val item = parseLine(line)
        assertNull(item)
    }

    @Test
    fun `itemToString with store number returns correct format`() {
        val item = ShoppingItem(3, "Milk")
        val result = itemToString(item)
        assertEquals("3,Milk", result)
    }

    @Test
    fun `itemToString with null store returns comma prefix`() {
        val item = ShoppingItem(null, "Eggs")
        val result = itemToString(item)
        assertEquals(",Eggs", result)
    }
}
