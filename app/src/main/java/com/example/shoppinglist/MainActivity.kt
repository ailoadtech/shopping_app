@file:OptIn(ExperimentalFoundationApi::class)

package com.example.shoppinglist

import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import java.io.File
import java.io.IOException
import kotlinx.parcelize.Parcelize

enum class Supermarket(val number: Int, val logoRes: Int) {
    REWE(1, R.drawable.ic_rewe),
    HIT(2, R.drawable.ic_hit),
    DM(3, R.drawable.ic_dm),
    MUELLER(4, R.drawable.ic_mueller),
    EDEKA(5, R.drawable.ic_edeka),
    KAUFLAND(6, R.drawable.ic_kaufland);

    companion object {
        fun fromNumber(num: Int): Supermarket? = entries.find { it.number == num }
    }
}

@Parcelize
data class ShoppingItem(
    val storeNumber: Int?,
    val name: String
) : Parcelable

fun parseLine(line: String): ShoppingItem? {
    val parts = line.split(",", limit = 2)
    if (parts.size != 2) return null
    val store = parts[0].toIntOrNull()
    val name = parts[1].trim()
    return if (name.isNotEmpty()) ShoppingItem(store, name) else null
}

fun itemToString(item: ShoppingItem): String {
    return "${item.storeNumber ?: ""},${item.name}"
}

class FileManager(private val context: android.content.Context) {
    private val fileName = "shoppinglist.txt"
    
    private fun getFile(): File? {
        return try {
            val file = File(context.filesDir, fileName)
            if (!file.exists()) {
                file.createNewFile()
            }
            file
        } catch (e: Exception) {
            Log.e("FileManager", "Error: ${e.message}")
            null
        }
    }

    fun getFilePath(): String {
        return File(context.filesDir, fileName).absolutePath
    }

    fun loadAllItems(): List<ShoppingItem> {
        val file = getFile() ?: return emptyList()
        return try {
            file.readLines()
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    try {
                        parseLine(line)
                    } catch (e: Exception) {
                        null
                    }
                }
        } catch (e: IOException) {
            Log.e("FileManager", "Failed to load: ${e.message}")
            emptyList()
        }
    }

    fun saveItem(item: ShoppingItem) {
        val file = getFile() ?: return
        try {
            file.appendText(itemToString(item) + "\n")
        } catch (e: IOException) {
            Log.e("FileManager", "Failed to save: ${e.message}")
        }
    }

    fun deleteItem(item: ShoppingItem) {
        val file = getFile() ?: return
        try {
            val items = loadAllItems()
            val updated = items.filter { it != item }
            file.writeText(updated.joinToString("\n") { itemToString(it) } + "\n")
        } catch (e: IOException) {
            Log.e("FileManager", "Failed to delete: ${e.message}")
        }
    }

    fun updateItem(oldItem: ShoppingItem, newItem: ShoppingItem) {
        val file = getFile() ?: return
        try {
            val items = loadAllItems()
            val updated = items.map { if (it == oldItem) newItem else it }
            file.writeText(updated.joinToString("\n") { itemToString(it) } + "\n")
        } catch (e: IOException) {
            Log.e("FileManager", "Failed to update item: ${e.message}")
        }
    }

    fun saveAllItems(items: List<ShoppingItem>) {
        val file = getFile() ?: return
        try {
            file.writeText(items.joinToString("\n") { itemToString(it) } + "\n")
        } catch (e: IOException) {
            Log.e("FileManager", "Failed to save all items: ${e.message}")
        }
    }
}

@Composable
fun FilterBar(
    selectedFilter: Int?,
    onFilterSelected: (Int?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // All button
        FilterButton(
            text = "Alle",
            contentDescription = "All items",
            isSelected = selectedFilter == null,
            onClick = { onFilterSelected(null) }
        )

        // Individual supermarket buttons
        Supermarket.entries.forEach { supermarket ->
            FilterButton(
                text = null,
                iconRes = supermarket.logoRes,
                contentDescription = supermarket.name,
                isSelected = selectedFilter == supermarket.number,
                onClick = { onFilterSelected(supermarket.number) }
            )
        }
    }
}

@Composable
fun FilterButton(
    text: String? = null,
    iconRes: Int? = null,
    contentDescription: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = if (isSelected) 8.dp else 0.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier.padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                text != null -> {
                    Text(
                        text = text,
                        color = contentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                iconRes != null -> {
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = contentDescription,
                        modifier = Modifier.size(32.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

@Composable
fun DraggableItem(
    item: ShoppingItem,
    isSelected: Boolean,
    onDelete: () -> Unit,
    onToggle: () -> Unit,
    onDragStart: (ShoppingItem, Int) -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableStateOf(0f) }
    var itemHeight by remember { mutableStateOf(0) }

    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    ListItem(
        colors = ListItemDefaults.colors(containerColor = backgroundColor),
        headlineContent = {
            Text(
                text = item.name,
                color = textColor
            )
        },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .onGloballyPositioned { coordinates ->
                itemHeight = coordinates.size.height
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        if (itemHeight > 0) {
                            dragOffset = 0f
                            isDragging = true
                            onDragStart(item, itemHeight)
                        }
                    }
                )
            }
            .pointerInput(isDragging) {
                if (isDragging) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            dragOffset += dragAmount.y
                            onDrag(dragAmount.y)
                        },
                        onDragEnd = {
                            isDragging = false
                            dragOffset = 0f
                            onDragEnd()
                        }
                    )
                }
            }
            .graphicsLayer {
                translationY = dragOffset
                alpha = if (isDragging) 0.7f else 1f
                shadowElevation = if (isDragging) 16.dp.toPx() else 2.dp.toPx()
            }
    )
}

@Composable
fun ShoppingList(
    items: List<ShoppingItem>,
    selectedItems: Set<ShoppingItem>,
    onDelete: (ShoppingItem) -> Unit,
    onToggle: (ShoppingItem) -> Unit,
    onDragStart: (ShoppingItem, Int) -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth()
    ) {
        if (items.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillParentMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No items in list",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(items, key = { it }) { item ->
                DraggableItem(
                    item = item,
                    isSelected = item in selectedItems,
                    onDelete = { onDelete(item) },
                    onToggle = { onToggle(item) },
                    onDragStart = onDragStart,
                    onDrag = onDrag,
                    onDragEnd = onDragEnd,
                    modifier = Modifier.animateItemPlacement()
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun ShopSelector(
    selectedShop: Int?,
    onStoreClick: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Supermarket.entries.forEach { supermarket ->
            FilterButton(
                text = null,
                iconRes = supermarket.logoRes,
                contentDescription = supermarket.name,
                isSelected = selectedShop == supermarket.number,
                onClick = {
                    onStoreClick(supermarket.number)
                }
            )
        }
    }
}

@Composable
fun InputRow(
    selectedShop: Int?,
    onShopSelected: (Int?) -> Unit,
    onAdd: (String, Int?) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var text by remember { mutableStateOf("") }

    val onStoreIconClick: (Int) -> Unit = { storeNumber ->
        val trimmed = text.trim()
        if (trimmed.isNotEmpty()) {
            onAdd(trimmed, storeNumber)
            text = ""
            onShopSelected(null)
            focusManager.clearFocus()
        } else {
            onShopSelected(if (selectedShop == storeNumber) null else storeNumber)
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            ShopSelector(
                selectedShop = selectedShop,
                onStoreClick = onStoreIconClick
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = text,
                    onValueChange = { newText ->
                        text = newText
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    placeholder = { Text("") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val trimmed = text.trim()
                        if (trimmed.isNotEmpty()) {
                            onAdd(trimmed, selectedShop)
                            text = ""
                            onShopSelected(null)
                            focusManager.clearFocus()
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text("OK", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingApp() {
    val context = LocalContext.current
    val fileManager = remember { FileManager(context) }

    val items = remember { mutableStateListOf<ShoppingItem>() }
    val filter = remember { mutableStateOf<Int?>(null) }
    val selectedShop = remember { mutableStateOf<Int?>(null) }
    val draggedItem = remember { mutableStateOf<ShoppingItem?>(null) }
    val dragOriginalIndex = remember { mutableStateOf(-1) }
    val draggedOffset = remember { mutableStateOf(0f) }
    val draggedItemHeight = remember { mutableStateOf(0) }
    val selectedItems = remember { mutableStateOf<Set<ShoppingItem>>(emptySet()) }

    LaunchedEffect(Unit) {
        val loaded = fileManager.loadAllItems()
        items.addAll(loaded)
    }

    val filteredItems = if (filter.value == null) items else items.filter { it.storeNumber == filter.value }

    val onDragStart: (ShoppingItem, Int) -> Unit = { item, height ->
        draggedItem.value = item
        dragOriginalIndex.value = items.indexOf(item)
        draggedOffset.value = 0f
        draggedItemHeight.value = height
    }

    val onDrag: (Float) -> Unit = { deltaY ->
        val originalIndex = dragOriginalIndex.value
        val height = draggedItemHeight.value
        if (originalIndex != -1 && height > 0) {
            draggedOffset.value += deltaY
            val offsetSteps = (draggedOffset.value / height).toInt()
            var targetIndex = originalIndex + offsetSteps
            targetIndex = targetIndex.coerceIn(0, items.size - 1)
            val item = draggedItem.value
            if (item != null) {
                val currentIndex = items.indexOf(item)
                if (currentIndex != targetIndex) {
                    items.remove(item)
                    items.add(targetIndex, item)
                }
            }
        }
    }

    val onDragEnd: () -> Unit = {
        fileManager.saveAllItems(items)
        val reloaded = fileManager.loadAllItems()
        items.clear()
        items.addAll(reloaded)
        draggedItem.value = null
        dragOriginalIndex.value = -1
        draggedOffset.value = 0f
        draggedItemHeight.value = 0
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Einkaufsliste") }) },
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            FilterBar(
                selectedFilter = filter.value,
                onFilterSelected = { filter.value = it }
            )
            ShoppingList(
                items = filteredItems,
                selectedItems = selectedItems.value,
                onDelete = { item ->
                    items.remove(item)
                    fileManager.deleteItem(item)
                },
                onToggle = { item ->
                    val currentSet = selectedItems.value
                    selectedItems.value = if (item in currentSet) {
                        currentSet - item
                    } else {
                        currentSet + item
                    }
                },
                onDragStart = onDragStart,
                onDrag = onDrag,
                onDragEnd = onDragEnd,
                modifier = Modifier.weight(1f)
            )
            InputRow(
                selectedShop = selectedShop.value,
                onShopSelected = { selectedShop.value = it },
                onAdd = { text, shop ->
                    if (text.trim().isNotEmpty()) {
                        val item = ShoppingItem(shop, text.trim())
                        items.add(0, item)
                        fileManager.saveItem(item)
                    }
                }
            )
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            val file = File(filesDir, "shoppinglist.txt")
            if (!file.exists()) {
                file.createNewFile()
            }
        } catch (e: Exception) {
            // Silent fail - file will be created when needed
        }
        
        setContent {
            val darkTheme = isSystemInDarkTheme()
            val colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
            
            MaterialTheme(colorScheme = colorScheme) {
                ShoppingApp()
            }
        }
    }
}
