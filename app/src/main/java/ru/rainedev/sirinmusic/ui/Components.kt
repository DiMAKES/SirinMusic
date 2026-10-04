package ru.rainedev.sirinmusic.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage

/** Обложка трека. Размер просим у сервера параметром w, а не масштабируем 640 в списке. */
@Composable
fun Artwork(
    url: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    corner: Dp = 12.dp,
) {
    Surface(
        modifier = modifier.size(size),
        shape = RoundedCornerShape(corner),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        if (url == null) {
            Placeholder()
        } else {
            SubcomposeAsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { Placeholder() },
                error = { Placeholder() },
            )
        }
    }
}

@Composable
private fun Placeholder() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Icon(
            Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun TwoLine(
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
fun RowCard(
    artworkUrl: String?,
    title: String,
    subtitle: String?,
    trailing: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Artwork(artworkUrl, 52.dp)
            TwoLine(
                title = title,
                subtitle = subtitle,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            )
            if (trailing != null) {
                Box(contentAlignment = Alignment.CenterEnd) { trailing() }
            }
        }
    }
}

fun formatTime(sec: Double): String {
    if (!sec.isFinite() || sec <= 0) return "0:00"
    val total = sec.toInt()
    return "%d:%02d".format(total / 60, total % 60)
}

@Composable
fun Spacer48() = Box(Modifier.height(48.dp))
