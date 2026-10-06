@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.danyal.vaultgallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Samsung-style creation surface shared by the public and secure galleries. */
@Composable
internal fun CreateMediaSheet(
    onDismiss: () -> Unit,
    onCreate: (CreationType) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null,
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(top = 26.dp, bottom = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Create", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 30.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                CreateAction(Icons.Outlined.AutoAwesome, "GIF") { onCreate(CreationType.GIF) }
                CreateAction(Icons.Outlined.GridView, "Collage") { onCreate(CreationType.COLLAGE) }
                CreateAction(Icons.Outlined.Movie, "Movie") { onCreate(CreationType.MOVIE) }
            }
        }
    }
}

@Composable
private fun CreateAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(68.dp),
        ) {
            Icon(icon, label, Modifier.padding(18.dp), tint = MaterialTheme.colorScheme.onSurface)
        }
        Text(label, Modifier.padding(top = 10.dp), style = MaterialTheme.typography.bodyLarge)
    }
}
