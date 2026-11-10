package com.litelauncher.tv.ui

import android.app.AlertDialog
import android.content.Context
import android.text.InputType
import android.widget.EditText
import android.widget.FrameLayout
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.litelauncher.tv.data.AppEntry
import kotlinx.coroutines.delay

private val TileShape = RoundedCornerShape(12.dp)
private val RowShape = RoundedCornerShape(10.dp)

/** Focus can only be requested once the target is on screen, so retry briefly. */
suspend fun FocusRequester.requestFocusWhenReady() {
    repeat(20) {
        if (runCatching { requestFocus() }.isSuccess) return
        delay(50)
    }
}

@Composable
fun TileSurface(
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(TileShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = TileColor,
            contentColor = Color.White,
            focusedContainerColor = TileColor,
            focusedContentColor = Color.White,
            pressedContainerColor = TileColor,
            pressedContentColor = Color.White,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(3.dp, accent), shape = TileShape),
        ),
        content = content,
    )
}

@Composable
fun AppTile(
    app: AppEntry,
    width: Dp,
    showLabel: Boolean,
    accent: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    marked: Boolean = false,
) {
    Column(modifier = Modifier.width(width)) {
        TileSurface(
            accent = accent,
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
        ) {
            if (app.isBanner) {
                Image(
                    bitmap = app.image,
                    contentDescription = app.label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        bitmap = app.image,
                        contentDescription = null,
                        modifier = Modifier.size(width * 0.24f),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = app.label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (marked) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(10.dp)
                        .background(accent, CircleShape),
                )
            }
        }
        if (showLabel && app.isBanner) {
            Text(
                text = app.label,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 10.dp, start = 2.dp),
            )
        }
    }
}

@Composable
fun AllAppsTile(width: Dp, accent: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TileSurface(
        accent = accent,
        onClick = onClick,
        modifier = modifier
            .width(width)
            .aspectRatio(16f / 9f),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(3) {
                            Box(
                                Modifier
                                    .size(6.dp)
                                    .background(LocalContentColor.current, CircleShape),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(text = "All apps", fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun OptionRow(
    title: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    value: String? = null,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = ClickableSurfaceDefaults.shape(RowShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            focusedContainerColor = accent,
            focusedContentColor = Color.Black,
            pressedContainerColor = accent,
            pressedContentColor = Color.Black,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (value != null) {
                Spacer(Modifier.width(16.dp))
                Text(
                    text = value,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = LocalContentColor.current.copy(alpha = 0.7f),
                )
            }
        }
    }
}

data class MenuOption(val title: String, val value: String? = null, val onClick: () -> Unit)

@Composable
fun MenuDialog(
    title: String,
    options: List<MenuOption>,
    accent: Color,
    onDismiss: () -> Unit,
    emptyText: String = "",
    initialIndex: Int = 0,
) {
    val firstFocus = remember { FocusRequester() }
    val focusIndex = initialIndex.coerceIn(0, (options.size - 1).coerceAtLeast(0))
    LaunchedEffect(options.isEmpty()) {
        if (options.isNotEmpty()) firstFocus.requestFocusWhenReady()
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(400.dp)
                .background(PanelColor, RoundedCornerShape(18.dp))
                .padding(18.dp),
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 16.dp, bottom = 12.dp, top = 4.dp),
            )
            if (options.isEmpty()) {
                Text(
                    text = emptyText,
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(16.dp),
                )
            }
            LazyColumn(
                modifier = Modifier.heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                itemsIndexed(options) { index, option ->
                    OptionRow(
                        title = option.title,
                        value = option.value,
                        accent = accent,
                        onClick = option.onClick,
                        modifier = if (index == focusIndex) Modifier.focusRequester(firstFocus) else Modifier,
                    )
                }
            }
        }
    }
}

/** A plain Android text dialog: the TV on-screen keyboard works reliably with it. */
fun promptText(
    context: Context,
    title: String,
    initial: String,
    hint: String,
    isUrl: Boolean = false,
    onResult: (String) -> Unit,
) {
    val input = EditText(context).apply {
        setText(initial)
        this.hint = hint
        isSingleLine = true
        inputType = InputType.TYPE_CLASS_TEXT or
            if (isUrl) InputType.TYPE_TEXT_VARIATION_URI else InputType.TYPE_TEXT_FLAG_CAP_WORDS
        setSelection(text.length)
    }
    val padding = (24 * context.resources.displayMetrics.density).toInt()
    val container = FrameLayout(context).apply {
        setPadding(padding, padding / 2, padding, 0)
        addView(input)
    }
    AlertDialog.Builder(context)
        .setTitle(title)
        .setView(container)
        .setPositiveButton("OK") { _, _ -> onResult(input.text.toString()) }
        .setNegativeButton("Cancel", null)
        .show()
    input.requestFocus()
}
