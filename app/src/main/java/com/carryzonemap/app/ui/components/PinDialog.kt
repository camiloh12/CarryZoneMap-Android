package com.carryzonemap.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.carryzonemap.app.domain.model.PinStatus
import com.carryzonemap.app.domain.model.RestrictionTag
import com.carryzonemap.app.ui.state.PinDialogState

// UI Constants
private const val DROPDOWN_MENU_WIDTH_FRACTION = 0.9f

/**
 * Callbacks for PinDialog interactions.
 */
data class PinDialogCallbacks(
    val onStatusSelected: (PinStatus) -> Unit,
    val onRestrictionTagSelected: (RestrictionTag?) -> Unit,
    val onSecurityScreeningChanged: (Boolean) -> Unit,
    val onPostedSignageChanged: (Boolean) -> Unit,
    val onConfirm: () -> Unit,
    val onDelete: () -> Unit,
    val onDismiss: () -> Unit,
)

/**
 * Dialog for creating or editing a pin.
 *
 * @param dialogState The current state of the dialog
 * @param callbacks Callbacks for dialog interactions
 */
@Composable
fun PinDialog(
    dialogState: PinDialogState,
    callbacks: PinDialogCallbacks,
) {
    when (dialogState) {
        is PinDialogState.Hidden -> {
            // No dialog to show
        }
        is PinDialogState.Creating -> {
            PinDialogContent(
                config =
                    PinDialogContentConfig(
                        title = "Create Pin",
                        poiName = dialogState.name,
                        selectedStatus = dialogState.selectedStatus,
                        selectedRestrictionTag = dialogState.selectedRestrictionTag,
                        hasSecurityScreening = dialogState.hasSecurityScreening,
                        hasPostedSignage = dialogState.hasPostedSignage,
                        isEditing = false,
                    ),
                callbacks = callbacks,
            )
        }
        is PinDialogState.Editing -> {
            PinDialogContent(
                config =
                    PinDialogContentConfig(
                        title = "Edit Pin",
                        poiName = dialogState.pin.name,
                        selectedStatus = dialogState.selectedStatus,
                        selectedRestrictionTag = dialogState.selectedRestrictionTag,
                        hasSecurityScreening = dialogState.hasSecurityScreening,
                        hasPostedSignage = dialogState.hasPostedSignage,
                        isEditing = true,
                    ),
                callbacks = callbacks,
            )
        }
    }
}

/**
 * Configuration data for the pin dialog content.
 */
private data class PinDialogContentConfig(
    val title: String,
    val poiName: String,
    val selectedStatus: PinStatus,
    val selectedRestrictionTag: RestrictionTag?,
    val hasSecurityScreening: Boolean,
    val hasPostedSignage: Boolean,
    val isEditing: Boolean,
)

@Composable
private fun PinDialogContent(
    config: PinDialogContentConfig,
    callbacks: PinDialogCallbacks,
) {
    AlertDialog(
        onDismissRequest = callbacks.onDismiss,
        title = {
            Text(text = config.title)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Display POI name
                Text(
                    text = config.poiName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )

                Text(
                    text = "Select carry zone status:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Status options
                PinStatus.entries.forEach { status ->
                    StatusOption(
                        status = status,
                        isSelected = status == config.selectedStatus,
                        onClick = { callbacks.onStatusSelected(status) },
                    )
                }

                // Restriction tag dropdown (only for NO_GUN status)
                if (config.selectedStatus == PinStatus.NO_GUN) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Why is carry restricted?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    RestrictionTagDropdown(
                        selectedTag = config.selectedRestrictionTag,
                        onTagSelected = callbacks.onRestrictionTagSelected,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Optional details:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = config.hasSecurityScreening,
                            onCheckedChange = callbacks.onSecurityScreeningChanged,
                        )
                        Text(
                            text = "Active security screening",
                            modifier = Modifier.clickable {
                                callbacks.onSecurityScreeningChanged(!config.hasSecurityScreening)
                            },
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = config.hasPostedSignage,
                            onCheckedChange = callbacks.onPostedSignageChanged,
                        )
                        Text(
                            text = "Posted signage visible",
                            modifier = Modifier.clickable {
                                callbacks.onPostedSignageChanged(!config.hasPostedSignage)
                            },
                        )
                    }
                }

                // Delete button for editing
                if (config.isEditing) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = callbacks.onDelete,
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete pin",
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Delete Pin")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = callbacks.onConfirm) {
                Text(if (config.isEditing) "Save" else "Create")
            }
        },
        dismissButton = {
            TextButton(onClick = callbacks.onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun StatusOption(
    status: PinStatus,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val backgroundColor =
        when (status) {
            PinStatus.ALLOWED -> Color(0xFF4CAF50) // Green
            PinStatus.UNCERTAIN -> Color(0xFFFFC107) // Yellow/Amber
            PinStatus.NO_GUN -> Color(0xFFF44336) // Red
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
                .background(
                    if (isSelected) {
                        backgroundColor.copy(alpha = 0.1f)
                    } else {
                        Color.Transparent
                    },
                )
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) backgroundColor else MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(8.dp),
                )
                .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Color indicator circle
        Spacer(
            modifier =
                Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(backgroundColor),
        )

        // Status name
        Text(
            text = status.displayName,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isSelected) backgroundColor else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun RestrictionTagDropdown(
    selectedTag: RestrictionTag?,
    onTagSelected: (RestrictionTag?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { expanded = true },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = selectedTag?.displayName ?: "Select reason",
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = "Dropdown",
        )
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        modifier = Modifier.fillMaxWidth(DROPDOWN_MENU_WIDTH_FRACTION),
    ) {
        RestrictionTag.entries.forEach { tag ->
            DropdownMenuItem(
                text = {
                    Column {
                        Text(
                            text = tag.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = tag.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                onClick = {
                    onTagSelected(tag)
                    expanded = false
                },
            )
        }
    }
}
