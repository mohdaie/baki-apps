@file:OptIn(ExperimentalLayoutApi::class)

package com.mohdaie.baki.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohdaie.baki.data.TransactionEntity
import com.mohdaie.baki.model.Category
import com.mohdaie.baki.model.TxType
import com.mohdaie.baki.ui.color
import com.mohdaie.baki.ui.icon
import com.mohdaie.baki.ui.theme.*
import com.mohdaie.baki.util.Fmt
import com.mohdaie.baki.util.toLocalDateTime

/** The faint graph-paper background. */
fun Modifier.paperGrid(): Modifier = drawBehind {
    val step = 28.dp.toPx()
    var x = 0f
    while (x < size.width) {
        drawLine(GridLine, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
        x += step
    }
    var y = 0f
    while (y < size.height) {
        drawLine(GridLine, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        y += step
    }
}

@Composable
fun CapsLabel(text: String, modifier: Modifier = Modifier, color: Color = Muted) {
    Text(
        text.uppercase(),
        modifier = modifier,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.9.sp,
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
}

/** White card with a thick outline and a hard offset shadow. */
@Composable
fun BrutalCard(
    modifier: Modifier = Modifier,
    background: Color = Color.White,
    radius: Dp = 18.dp,
    shadow: Dp = 4.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    Box(modifier.padding(end = shadow, bottom = shadow)) {
        Box(
            Modifier
                .matchParentSize()
                .offset(shadow, shadow)
                .background(Ink, shape),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(background)
                .border(2.5.dp, Ink, shape)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(contentPadding),
            content = content,
        )
    }
}

@Composable
fun IconTile(icon: ImageVector, background: Color, size: Dp = 34.dp, iconSize: Dp = 18.dp) {
    val shape = RoundedCornerShape(size * 0.3f)
    Box(
        Modifier
            .size(size)
            .clip(shape)
            .background(background)
            .border(2.dp, Ink, shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Ink, modifier = Modifier.size(iconSize))
    }
}

@Composable
fun CategoryTile(categoryId: String?, size: Dp = 34.dp, iconSize: Dp = 18.dp) {
    val c = Category.of(categoryId)
    IconTile(c?.icon ?: Icons.Filled.QuestionMark, c?.color ?: SlateTile, size, iconSize)
}

@Composable
fun TxTile(type: String, categoryId: String?, size: Dp = 40.dp) {
    when (type) {
        TxType.TRANSFER -> IconTile(Icons.Filled.SwapHoriz, SlateTile, size, 20.dp)
        TxType.INCOME -> IconTile(Icons.Filled.ArrowDownward, IncomeTile, size, 20.dp)
        else -> CategoryTile(categoryId, size, 20.dp)
    }
}

@Composable
fun Bar(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 10.dp,
    bordered: Boolean = true,
    track: Color = Track,
) {
    val shape = RoundedCornerShape(height / 2)
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(track)
            .then(if (bordered) Modifier.border(1.5.dp, Ink, shape) else Modifier),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .background(color),
        )
    }
}

@Composable
fun BrutalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    background: Color = Accent,
    foreground: Color = Color.White,
    enabled: Boolean = true,
    shadow: Boolean = true,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(modifier.padding(end = 3.dp, bottom = 3.dp)) {
        if (shadow && enabled) {
            Box(Modifier.matchParentSize().offset(3.dp, 3.dp).background(Ink, shape))
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(shape)
                .background(if (enabled) background else Hairline)
                .border(2.5.dp, Ink, shape)
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text,
                color = if (enabled) foreground else Muted,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun SmallButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, background: Color = Color.White) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier
            .height(36.dp)
            .clip(shape)
            .background(background)
            .border(2.dp, Ink, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
fun Pill(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .height(40.dp)
            .clip(shape)
            .background(if (selected) Ink else Color.White)
            .border(2.dp, Ink, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (selected) Color.White else Ink,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
fun PillRow(options: List<String>, selected: String?, onSelect: (String) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { opt -> Pill(opt, opt == selected, onClick = { onSelect(opt) }) }
    }
}

@Composable
fun StatusPill(text: String, background: Color, foreground: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(text, color = foreground, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
fun BrutalField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    prefix: String? = null,
    textStyle: TextStyle = TextStyle(fontSize = 16.sp, color = Ink),
    height: Dp = 50.dp,
    background: Color = Color.White,
    capitalizeWords: Boolean = false,
    borderless: Boolean = false,
) {
    val shape = RoundedCornerShape(12.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = textStyle,
        cursorBrush = SolidColor(Ink),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            capitalization = if (capitalizeWords) KeyboardCapitalization.Words else KeyboardCapitalization.None,
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        decorationBox = { inner ->
            Row(
                Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(background)
                    .then(if (borderless) Modifier else Modifier.border(2.dp, Ink, shape))
                    .padding(horizontal = if (borderless) 10.dp else 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (prefix != null) {
                    Text(prefix, color = Muted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) Text(placeholder, style = textStyle.copy(color = Faint), maxLines = 1)
                    inner()
                }
            }
        },
    )
}

/** Square icon button with an offset shadow, used in the header. */
@Composable
fun IconSquare(icon: ImageVector, description: String, onClick: () -> Unit, badge: Int = 0) {
    val shape = RoundedCornerShape(14.dp)
    Box {
        Box(Modifier.padding(end = 3.dp, bottom = 3.dp)) {
            Box(Modifier.matchParentSize().offset(3.dp, 3.dp).background(Ink, shape))
            Box(
                Modifier
                    .size(46.dp)
                    .clip(shape)
                    .background(Color.White)
                    .border(2.5.dp, Ink, shape)
                    .clickable(onClickLabel = description, onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = description, tint = Ink, modifier = Modifier.size(22.dp))
            }
        }
        if (badge > 0) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 7.dp, y = (-7).dp)
                    .defaultMinSize(minWidth = 22.dp, minHeight = 22.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(Red)
                    .border(2.dp, Ink, RoundedCornerShape(11.dp))
                    .padding(horizontal = 5.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("$badge", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun Fab(description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(18.dp)
    Box(modifier.padding(end = 4.dp, bottom = 4.dp)) {
        Box(Modifier.matchParentSize().offset(4.dp, 4.dp).background(Ink, shape))
        Box(
            Modifier
                .size(60.dp)
                .clip(shape)
                .background(Accent)
                .border(2.5.dp, Ink, shape)
                .clickable(onClickLabel = description, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Add, contentDescription = description, tint = Color.White, modifier = Modifier.size(30.dp))
        }
    }
}

@Composable
fun HairlineDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.5.dp).background(Hairline))
}

@Composable
fun DashedRule(color: Color = Ink) {
    Canvas(Modifier.fillMaxWidth().height(2.dp)) {
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f),
        )
    }
}

@Composable
fun KeyValueRow(key: String, value: String, bold: Boolean = false, valueSize: Int = 14) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            key,
            modifier = Modifier.weight(1f),
            color = Ink,
            fontSize = 14.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        )
        Text(value, color = Ink, fontSize = valueSize.sp, fontWeight = FontWeight.Bold)
    }
}

fun amountText(type: String, amount: Double): String = when (type) {
    TxType.EXPENSE -> "−" + Fmt.money(amount)
    TxType.INCOME -> "+" + Fmt.money(amount)
    else -> Fmt.money(amount)
}

fun amountColor(type: String): Color = when (type) {
    TxType.EXPENSE -> Red
    TxType.INCOME -> Green
    else -> Muted
}

/** One row in Activity / Recent activity. Emits the row and, optionally, a divider below it. */
@Composable
fun TxRow(tx: TransactionEntity, showDivider: Boolean, onClick: () -> Unit) {
    val time = tx.timestamp.toLocalDateTime().format(Fmt.time)
    val suffix = when {
        tx.type == TxType.TRANSFER -> " · Transfer"
        tx.type == TxType.INCOME -> " · Income"
        tx.category == null -> " · Uncategorized"
        else -> ""
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TxTile(tx.type, tx.category)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                tx.merchant,
                color = Ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${tx.account} · $time$suffix",
                color = Muted,
                fontSize = 12.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(amountText(tx.type, tx.amount), color = amountColor(tx.type), fontSize = 15.5.sp, fontWeight = FontWeight.ExtraBold)
            if (tx.fxText != null) Text(tx.fxText, color = Muted, fontSize = 11.5.sp)
        }
    }
    if (showDivider) HairlineDivider()
}

/**
 * Text state for an editable field backed by the database: filled once when the data first
 * loads, then owned by the field so typing isn't overwritten by the round-trip.
 */
@Composable
fun rememberSyncedText(source: String?, ready: Boolean): MutableState<String> {
    val state = rememberSaveable { mutableStateOf("") }
    var loaded by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(ready) {
        if (ready && !loaded) {
            state.value = source ?: ""
            loaded = true
        }
    }
    return state
}
