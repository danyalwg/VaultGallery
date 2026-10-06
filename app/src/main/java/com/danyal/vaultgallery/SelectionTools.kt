package com.danyal.vaultgallery

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.statusBarsPadding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun <K> invertedSelection(available: Collection<K>, selected: Set<K>): Set<K> =
    available.toSet() - selected

internal fun <K> selectionRange(orderedKeys: List<K>, selected: Set<K>): Set<K> {
    val positions = orderedKeys.mapIndexedNotNull { index, key -> index.takeIf { key in selected } }
    if (positions.size < 2) return selected.intersect(orderedKeys.toSet())
    return orderedKeys.subList(positions.min(), positions.max() + 1).toSet()
}

/** Shared selection header used by public and secure collections. */
@Composable
internal fun <T, K> CollectionSelectionHeader(
    items: List<T>,
    selected: Set<K>,
    keyOf: (T) -> K,
    dateOf: (T) -> Long,
    onSelectionSet: (Set<K>) -> Unit,
    onCancel: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var datePickerOpen by remember { mutableStateOf(false) }
    val orderedKeys = remember(items) { items.map(keyOf) }
    val availableKeys = remember(orderedKeys) { orderedKeys.toSet() }
    val selectedHere = remember(selected, availableKeys) { selected.intersect(availableKeys) }

    Row(
        Modifier.fillMaxWidth().statusBarsPadding().height(92.dp).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "${selectedHere.size} selected",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.weight(1f),
        )
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Outlined.MoreVert, "Selection options")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(if (selectedHere.size == availableKeys.size) "Deselect all" else "Select all") },
                    onClick = {
                        menuOpen = false
                        onSelectionSet(if (selectedHere.size == availableKeys.size) emptySet() else availableKeys)
                    },
                )
                DropdownMenuItem(
                    text = { Text("Invert selection") },
                    onClick = {
                        menuOpen = false
                        onSelectionSet(invertedSelection(availableKeys, selectedHere))
                    },
                )
                DropdownMenuItem(
                    text = { Text("Select by date") },
                    onClick = { menuOpen = false; datePickerOpen = true },
                )
                DropdownMenuItem(
                    text = { Text("Select range between endpoints") },
                    enabled = selectedHere.size >= 2,
                    onClick = {
                        menuOpen = false
                        onSelectionSet(selectionRange(orderedKeys, selectedHere))
                    },
                )
            }
        }
        TextButton(onClick = onCancel) { Text("Cancel") }
    }

    if (datePickerOpen) {
        val dateFormatter = remember { SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()) }
        val groups = remember(items) {
            items.groupBy { item -> dateFormatter.format(Date(dateOf(item).coerceAtLeast(0L))) }
                .entries
                .sortedByDescending { entry -> entry.value.maxOfOrNull(dateOf) ?: 0L }
        }
        AlertDialog(
            onDismissRequest = { datePickerOpen = false },
            title = { Text("Select by date") },
            text = {
                androidx.compose.foundation.lazy.LazyColumn {
                    items(groups.size) { index ->
                        val group = groups[index]
                        val keys = group.value.mapTo(LinkedHashSet(), keyOf)
                        val fullySelected = keys.all { it in selectedHere }
                        DropdownMenuItem(
                            text = {
                                Text("${group.key}  ·  ${keys.size} item${if (keys.size == 1) "" else "s"}")
                            },
                            trailingIcon = { Text(if (fullySelected) "✓" else "") },
                            onClick = {
                                onSelectionSet(
                                    selectedHere.toMutableSet().apply {
                                        if (fullySelected) removeAll(keys) else addAll(keys)
                                    },
                                )
                            },
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { datePickerOpen = false }) { Text("Done") } },
        )
    }
}
