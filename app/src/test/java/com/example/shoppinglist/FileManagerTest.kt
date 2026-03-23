package com.example.shoppinglist

import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FileManagerTest {

    @JvmField
    @Rule
    val tempFolder = TemporaryFolder()

    private lateinit var fileManager: FileManager

    @Before
    fun setup() {
        val downloadsDir = tempFolder.newFolder("downloads")
        fileManager = FileManager(downloadsDir)
    }

    @Test
    fun `loadAllItems on empty file returns empty list`() {
        val file = File(fileManager.downloadsDir, "shoppinglist.txt")
        file.createNewFile()
        val items = fileManager.loadAllItems()
        assertTrue(items.isEmpty())
    }

    @Test
    fun `loadAllItems returns all valid items`() {
        val file = File(fileManager.downloadsDir, "shoppinglist.txt")
        file.writeText("1,Milk\n2,Bread\n,Eggs\n")
        val items = fileManager.loadAllItems()
        assertEquals(3, items.size)
        assertEquals("Milk", items[0].name)
        assertEquals(1, items[0].storeNumber)
        assertEquals("Bread", items[1].name)
        assertEquals(2, items[1].storeNumber)
        assertEquals("Eggs", items[2].name)
        assertNull(items[2].storeNumber)
    }

    @Test
    fun `loadAllItems skips malformed lines`() {
        val file = File(fileManager.downloadsDir, "shoppinglist.txt")
        file.writeText("1,Milk\ninvalid\n2,\n")
        val items = fileManager.loadAllItems()
        assertEquals(1, items.size)
        assertEquals("Milk", items[0].name)
    }

    @Test
    fun `saveItem appends to file`() {
        val file = File(fileManager.downloadsDir, "shoppinglist.txt")
        file.createNewFile()
        fileManager.saveItem(ShoppingItem(3, "Cheese"))
        val content = file.readText()
        assertTrue(content.contains("3,Cheese"))
    }

    @Test
    fun `deleteItem removes item from file`() {
        val file = File(fileManager.downloadsDir, "shoppinglist.txt")
        file.writeText("1,Milk\n2,Bread\n")
        val milk = ShoppingItem(1, "Milk")
        fileManager.deleteItem(milk)
        val items = fileManager.loadAllItems()
        assertEquals(1, items.size)
        assertEquals("Bread", items[0].name)
    }
}

// Test version of FileManager that exposes directory
class FileManager(testDownloadsDir: File) {
    internal val downloadsDir: File = testDownloadsDir
    private val file = File(downloadsDir, "shoppinglist.txt")

    fun loadAllItems(): List<ShoppingItem> {
        return try {
            if (!file.exists()) {
                file.createNewFile()
                return emptyList()
            }
            file.readLines()
                .mapNotNull { line ->
                    try {
                        parseLine(line)
                    } catch (e: Exception) {
                        null
                    }
                }
        } catch (e: IOException) {
            emptyList()
        }
    }

    fun saveItem(item: ShoppingItem) {
        try {
            if (!file.exists()) {
                file.createNewFile()
            }
            file.appendText(itemToString(item) + "\n")
        } catch (e: IOException) {
            // ignore
        }
    }

    fun deleteItem(item: ShoppingItem) {
        try {
            val items = loadAllItems()
            val updated = items.filter { it != item }
            file.writeText(updated.joinToString("\n") { itemToString(it) } + "\n")
        } catch (e: IOException) {
            // ignore
        }
    }
}
