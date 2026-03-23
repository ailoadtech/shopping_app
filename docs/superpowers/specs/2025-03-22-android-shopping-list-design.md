# Android Shopping List App - Design Document

## Project Overview

Build a simple Android shopping list app using Jetpack Compose that manages a list of shopping items saved to a local text file in the Downloads folder.

## Requirements Summary

### Core Features
- Display shopping items in a scrollable list
- Add new items via input field at bottom with OK button
- Swipe to delete items
- Filter by supermarket using logo buttons (single-select)
- File stored at: `Downloads/shoppinglist.txt`
- Format: `[storeNumber],ItemName` (e.g., `3,Milk` or `,Milk` for no store)

### Technical Requirements
- Platform: Android with Jetpack Compose
- Storage: Android Downloads folder with permissions
- Supermarket mapping (1-6): Rewe, Hit, DM, Mueller, Edeka, Kaufland
- Create file silently on first start if missing
- Input validation: reject empty item names

### UI Components

#### 1. Filter Bar (Top)
- 7 buttons total: **"All"** + 6 supermarket logos
- **Order**: All | Rewe | Hit | DM | Mueller | Edeka | Kaufland
- **All button**: Shows text "All" or an icon, clears filter when clicked
- **Logo buttons**: Display corresponding supermarket logo from drawable resources
- **Selection state**: Highlight selected button (e.g., tinted color, border, or background change)
- **Single-select**: Only one filter active at a time
- **Default state**: "All" selected (filter = null)

#### Supermarket Mapping
| Number | Supermarket | Logo Resource Name |
|--------|-------------|-------------------|
| 1 | Rewe | `ic_rewe.png` |
| 2 | Hit | `ic_hit.png` |
| 3 | DM | `ic_dm.png` |
| 4 | Mueller | `ic_mueller.png` |
| 5 | Edeka | `ic_edeka.png` |
| 6 | Kaufland | `ic_kaufland.png` |

**Logo Asset Handling:**
- Place logos in `app/src/main/res/drawable/` as PNG files
- Recommended size: 48dp x 48dp (or 24dp for smaller buttons)
- Use provided `.png` files with transparent backgrounds for optimal Compose rendering
- Load with: `painterResource(id = R.drawable.ic_rewe)`

#### 2. Item List (Center)
- Use `LazyColumn` for efficient scrolling
- Each row displays: item name only (e.g., "Milk")
- No store number shown in list
- Swipe-to-delete: swipe item left or right to trigger immediate deletion
- **Empty state**: Show centered text "No items in list" (or "No items") with optional icon
- Scrollbar: `LazyColumn` includes scrollbar automatically on right side

#### 3. Input Row (Bottom)
- Horizontal row with `TextField` and `Button("OK")`
- TextField accepts **item name only** (no store number parsing)
  - Users can type any text (including numbers or commas), but entire content is treated as item name
  - Example: typing "3,Milk" creates item named "3,Milk" (not store 3)
- OK button or IME "Done" action adds item
- **New item store assignment**: Always unassigned (storeNumber = null) → saved as `,ItemName`
- **Clear input** after successful add
- **Close keyboard** after add (using `LocalFocusManager`)
- **Validation**: Reject empty or whitespace-only input
- **Input error feedback**: Show brief error message or shake animation for invalid input

**Filter Behavior on New Items**: New items are **always unassigned** regardless of active filter. A filter only controls what's displayed, not what gets created.

## Architecture: Simple Monolithic Approach

### Structure
```
MainActivity.kt
├── onCreate()
│   └── setContent { ShoppingApp() }
├── ShoppingApp() - Root composable
├── FilterBar() - Logo filter buttons
├── ShoppingList() - LazyColumn with swipe
├── InputRow() - TextField + Button
└── File operations & state management inline
```

### State Management
- `MutableState<List<ShoppingItem>>` for the shopping list
- `MutableState<Int?>` for current filter (null = all)
- All state in MainActivity for simplicity

### Data Model
```kotlin
data class ShoppingItem(
    val storeNumber: Int?, // null = unassigned
    val name: String
)
```

### File Format & Parsing
**File Format:**
- One item per line
- `number,itemName` or `,itemName` (store number can be empty for unassigned)
- The `itemName` may contain commas (e.g., `3,Cheese,Cracker`) because parsing uses `split(",", limit=2)` which only splits on the first comma
- Examples:
  ```
  3,Milk
  1,Bread
  ,Eggs
  5,Cheese
  2,Red,Apple
  ```

**Parsing Logic:**
```kotlin
fun parseLine(line: String): ShoppingItem? {
    val parts = line.split(",", limit = 2)
    if (parts.size != 2) return null
    val store = parts[0].toIntOrNull()
    val name = parts[1].trim()
    return if (name.isNotEmpty()) ShoppingItem(store, name) else null
}
```

**Serialization:**
```kotlin
fun itemToString(item: ShoppingItem): String {
    return "${item.storeNumber ?: ""},${item.name}"
}
```

### Permissions Approach

**IMPORTANT**: The original specification requested the file be saved to the **Android device's Downloads folder** (`Downloads/shoppinglist.txt`). However, modern Android versions (API 29+, Android 10+) severely restrict direct file system access to protect user privacy. The originally proposed `Environment.getExternalStoragePublicDirectory()` is deprecated and no longer works reliably.

**Storage Strategy Decision:**

We have **two options**:

#### Option 1: App-Specific Storage (RECOMMENDED ✓)
- Use `context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)`
- File path: `/Android/data/<package>/files/Download/shoppinglist.txt`
- **Advantages**:
  - No runtime permissions needed
  - Works on all Android versions
  - Simple file I/O operations
  - No deprecation warnings
- **Disadvantages**:
  - File is removed when app is uninstalled
  - Not visible in user's main Downloads folder (in a separate app-specific area)

This is the best choice for a simple shopping list app that prioritizes reliability over file visibility.

#### Option 2: Public Downloads via MediaStore (Advanced)
- Use `MediaStore.Downloads` ContentResolver API
- Requires permissions on Android 9 and below; on Android 10+ uses scoped storage
- File visible in user's standard Downloads folder
- **Disadvantages**:
  - Complex implementation (ContentResolver URIs, not direct File paths)
  - Requires handling multiple URIs and query operations
  - Overkill for this use case

**Decision**: We're using **Option 1 (App-Specific Storage)** for simplicity and reliability. The file will be called `shoppinglist.txt` and will be stored in the app's external downloads directory. This meets the intent of maintaining a persistent local file while ensuring the app works on all Android versions.

---

### File Operations (Revised for Option 1)
```kotlin
class FileManager(private val context: Context) {
    // App-specific Downloads directory (no permissions needed)
    private val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        ?: throw IOException("External storage not available")
    private val file = File(downloadsDir, "shoppinglist.txt")

    /**
     * Load all items from file. Creates file if it doesn't exist.
     */
    fun loadAllItems(): List<ShoppingItem> {
        return try {
            if (!file.exists()) {
                // Create empty file silently on first start
                file.createNewFile()
                return emptyList()
            }

            file.readLines()
                .mapNotNull { line ->
                    try {
                        parseLine(line)
                    } catch (e: Exception) {
                        // Skip malformed lines silently
                        null
                    }
                }
        } catch (e: IOException) {
            // Log error in real app, return empty list gracefully
            emptyList()
        }
    }

    /**
     * Append a new item to the file.
     */
    fun saveItem(item: ShoppingItem) {
        try {
            if (!file.exists()) {
                file.createNewFile()
            }
            file.appendText(itemToString(item) + "\n")
        } catch (e: IOException) {
            // Log error - in production should notify user
        }
    }

    /**
     * Delete an item by rewriting entire file.
     * Note: For small lists (<100 items), this is acceptable.
     */
    fun deleteItem(item: ShoppingItem) {
        try {
            val items = loadAllItems()
            val updated = items.filter { it != item }
            file.writeText(updated.joinToString("\n") { itemToString(it) } + "\n")
        } catch (e: IOException) {
            // Log error
        }
    }
}
```

**Note:** The delete operation rewrites the entire file. For a small shopping list (< 100 items), this is acceptable.

### Composable Structure

```kotlin
@Composable
fun ShoppingApp() {
    val context = LocalContext.current
    val fileManager = remember { FileManager(context) }

    // State
    val items = remember { mutableStateListOf<ShoppingItem>() }
    val filter = remember { mutableStateOf<Int?>(null) }

    // Load on startup
    LaunchedEffect(Unit) {
        items.addAll(fileManager.loadAllItems())
    }

    // Filter logic
    val filteredItems = remember(items, filter) {
        if (filter.value == null) items
        else items.filter { it.storeNumber == filter.value }
    }

    Scaffold { padding ->
        Column(modifier = Modifier.padding(padding)) {
            FilterBar(
                selectedFilter = filter.value,
                onFilterSelected = { filter.value = it }
            )
            ShoppingList(
                items = filteredItems,
                onDelete = { item ->
                    items.remove(item)
                    fileManager.deleteItem(item)
                }
            )
            InputRow(
                onAdd = { text ->
                    if (text.trim().isNotEmpty()) {
                        val item = ShoppingItem(null, text.trim())
                        items.add(item)
                        fileManager.saveItem(item)
                    }
                }
            )
        }
    }
}
```

### Swipe-to-Delete Implementation

**Approach**: Use `androidx.compose.material3:swipe-to-dismiss` component from Material 3 library. This provides a complete, accessible, and animated swipe-to-dismiss behavior.

**Gradle Dependency:**
```
implementation "androidx.compose.material3:material3:1.2.1"
```

**Implementation:**
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableItem(
    item: ShoppingItem,
    onDelete: () -> Unit
) {
    val dismissState = remember DismissState(
        confirmStateChange = { dismissValue ->
            if (dismissValue == DismissValue.DismissedToStart || dismissValue == DismissValue.DismissedToEnd) {
                onDelete()
            }
            true
        }
    )

    SwipeToDismiss(
        state = dismissState,
        background = {
            // Red background with delete icon
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Red),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.White,
                    modifier = Modifier.padding(16.dp)
                )
            }
        },
        dismissContent = {
            ListItem(
                headlineText = { Text(item.name) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}
```

**Alternative (if Material 3 not used)**: Implement with `Modifier.animateItemPlacement()` and custom gesture detection with `AnimatedVisibility` or `Offset` animation. But Material 3's `SwipeToDismiss` is recommended.

**Behavior**: Swipe left or right to the threshold triggers immediate deletion. Partial swipe does not delete.

---

### Accessibility
- Logo buttons must have `contentDescription` (e.g., `stringResource(R.string.filter_rewe)` or "Filter by Rewe")
- Delete swipe should have appropriate `semantics { contentDescription = "Delete ${item.name}" }`
- Consider adding accessibility action for delete on list items (for users who can't swipe)

## Technical Specifications

### Material Design
- Use **Material 3** theme: `MaterialTheme` from `androidx.compose.material3`
- Color scheme: Default Material blue accent or custom primary color
- Components: Use `TextField`, `Button`, `LazyColumn`, `ListItem` from Material 3

### API Level Targeting
- `minSdkVersion`: 24 (Android 7.0 Nougat)
- `targetSdkVersion`: 34 (Android 14)
- `compileSdkVersion`: 34

### State Persistence
- Use `rememberSaveable` instead of `remember` for list and filter state to survive process death and configuration changes:
```kotlin
val items = rememberSaveable { mutableStateListOf<ShoppingItem>() }
val filter = rememberSaveable { mutableStateOf<Int?>(null) }
```
- `ShoppingItem` must implement `Parcelable` or use `@Parcelize` for `rememberSaveable` with custom classes

### Concurrency
- File operations are quick for small lists; no explicit locking needed
- If rapid add/delete operations occur, they will be serialized by Kotlin's single-threaded coroutine context (if using Main dispatcher)
- For added safety, FileManager methods could use `synchronized` or `Mutex`

### Duplicate Items
- **Allowed**: Multiple identical item names (e.g., two "Milk" entries) are permitted
- Each entry is independent and can be removed individually

### File Path Clarification
- Actual path (with app-specific storage): `/Android/data/<package>/files/Download/shoppinglist.txt`
- This is **not** the user's main `Downloads/` folder but an app-specific downloads directory
- The file persists across app restarts and is removed only when app is uninstalled

### AndroidManifest.xml

**Permissions**: None required for app-specific storage.

**Activities**:
```xml
<activity
    android:name=".MainActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

**Note**: Because we use `getExternalFilesDir()`, no `READ_EXTERNAL_STORAGE` or `WRITE_EXTERNAL_STORAGE` permissions are needed, even on older Android versions. This simplifies the app and avoids permission dialogs.

---

## Testing & Verification

### Manual Testing Checklist
- [ ] App starts without crashing
- [ ] File is created on first launch
- [ ] Can add item "Milk" → appears in list
- [ ] Saved as ",Milk" in file
- [ ] Can add "3,Bread" → appears as "Bread"
- [ ] Filter by DM shows only items with store 3
- [ ] Filter by All shows all items
- [ ] Swipe item → disappears immediately
- [ ] Item removed from file on next app restart
- [ ] Empty input ignored (no item added)
- [ ] "No items" shown when list empty
- [ ] Logos display correctly in filter bar

## Implementation Plan (Next Step)

Once design is approved, the implementation plan will detail:
- Exact file structure and Kotlin code
- Gradle dependencies required
- AndroidManifest.xml setup (permissions)
- Logo asset handling (Bitmap loading)
- Error handling and edge cases
- Complete composable implementations

## Assumptions & Decisions

- **Single Activity**: All UI in MainActivity
- **No database**: File-based only (sufficient for small lists)
- **No sorting**: Items appear in order added
- **No edit functionality**: Only add and delete
- **No cloud sync**: Local only
- **No dark mode**: Light theme acceptable (can add later)

---

**Design Status:** Ready for implementation
**Architecture:** Simple Monolithic (Approach A)
**Next:** Invoke writing-plans skill for detailed implementation steps
