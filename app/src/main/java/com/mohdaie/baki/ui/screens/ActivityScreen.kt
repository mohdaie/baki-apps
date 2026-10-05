@file:OptIn(ExperimentalMaterial3Api::class)

package com.mohdaie.baki.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mohdaie.baki.data.TransactionEntity
import com.mohdaie.baki.model.Category
import com.mohdaie.baki.model.TxType
import com.mohdaie.baki.ui.AppViewModel
import com.mohdaie.baki.ui.components.*
import com.mohdaie.baki.ui.sheets.BakiSheet
import com.mohdaie.baki.ui.sheets.OptionList
import com.mohdaie.baki.ui.theme.*
import com.mohdaie.baki.util.Fmt
import com.mohdaie.baki.util.toLocalDate
import java.time.LocalDate

private class FilterDef(val title: String, val default: String, val options: List<Pair<String, String>>)

private data class DayGroup(val label: String, val total: Double, val items: List<TransactionEntity>)

@Composable
fun ActivityScreen(
    vm: AppViewModel,
    onOpenTx: (TransactionEntity) -> Unit,
    onReview: () -> Unit,
) {
    val month by vm.activityMonth.collectAsStateWithLifecycle()
    val txs by vm.activityTx.collectAsStateWithLifecycle()
    val pendingOrNull by vm.pending.collectAsStateWithLifecycle()
    val pendingCount = pendingOrNull?.size ?: 0

    var search by rememberSaveable { mutableStateOf("") }
    var fCat by rememberSaveable { mutableStateOf("all") }
    var fAcct by rememberSaveable { mutableStateOf("all") }
    var fAmt by rememberSaveable { mutableStateOf("any") }
    var sort by rememberSaveable { mutableStateOf("new") }
    var openFilter by rememberSaveable { mutableStateOf<String?>(null) }

    val accounts = remember(txs) { txs.map { it.account }.distinct().sorted() }
    val filters = remember(accounts) {
        mapOf(
            "cat" to FilterDef(
                "Category",
                "all",
                listOf("all" to "All categories") +
                    Category.entries.map { it.id to it.label } +
                    listOf("uncat" to "Uncategorized", TxType.INCOME to "Income", TxType.TRANSFER to "Transfers"),
            ),
            "acct" to FilterDef("Account", "all", listOf("all" to "All accounts") + accounts.map { it to it }),
            "amt" to FilterDef(
                "Amount",
                "any",
                listOf("any" to "Any amount", "lt10" to "Under RM 10", "mid" to "RM 10 – 50", "gt50" to "Over RM 50"),
            ),
            "sort" to FilterDef(
                "Sort",
                "new",
                listOf("new" to "Newest first", "old" to "Oldest first", "high" to "Highest amount", "low" to "Lowest amount"),
            ),
        )
    }
    fun currentValue(key: String) = when (key) {
        "cat" -> fCat
        "acct" -> fAcct
        "amt" -> fAmt
        else -> sort
    }
    fun applyFilter(key: String, v: String) {
        when (key) {
            "cat" -> fCat = v
            "acct" -> fAcct = v
            "amt" -> fAmt = v
            else -> sort = v
        }
    }

    val groups = remember(txs, search, fCat, fAcct, fAmt, sort) {
        val q = search.trim().lowercase()
        var list = txs.filter { t ->
            (q.isEmpty() || "${t.merchant} ${t.account}".lowercase().contains(q)) &&
                when (fCat) {
                    "all" -> true
                    "uncat" -> t.type == TxType.EXPENSE && t.category == null
                    TxType.INCOME, TxType.TRANSFER -> t.type == fCat
                    else -> t.type == TxType.EXPENSE && t.category == fCat
                } &&
                (fAcct == "all" || t.account == fAcct) &&
                when (fAmt) {
                    "lt10" -> t.amount < 10
                    "mid" -> t.amount in 10.0..50.0
                    "gt50" -> t.amount > 50
                    else -> true
                }
        }
        list = when (sort) {
            "old" -> list.sortedBy { it.timestamp }
            "high" -> list.sortedByDescending { it.amount }
            "low" -> list.sortedBy { it.amount }
            else -> list.sortedByDescending { it.timestamp }
        }
        if (sort == "new" || sort == "old") {
            list.groupBy { it.timestamp.toLocalDate() }.map { (date, items) ->
                DayGroup(dayLabel(date), items.filter { it.type == TxType.EXPENSE }.sumOf { it.amount }, items)
            }
        } else if (list.isNotEmpty()) {
            listOf(
                DayGroup(
                    if (sort == "high") "Highest first" else "Lowest first",
                    list.filter { it.type == TxType.EXPENSE }.sumOf { it.amount },
                    list,
                ),
            )
        } else {
            emptyList()
        }
    }
    val filtering = search.isNotBlank() || fCat != "all" || fAcct != "all" || fAmt != "any"

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 14.dp, top = 4.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Month
        item {
            BrutalCard(radius = 16.dp, contentPadding = PaddingValues(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MonthArrow(Icons.Filled.ChevronLeft, "Previous month", enabled = true) { vm.shiftActivityMonth(-1) }
                    Text(
                        month.format(Fmt.monthYear),
                        color = Ink,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                    MonthArrow(Icons.Filled.ChevronRight, "Next month", enabled = month < vm.currentMonth) {
                        vm.shiftActivityMonth(1)
                    }
                }
            }
        }

        // Search
        item {
            BrutalCard(radius = 16.dp, contentPadding = PaddingValues(start = 14.dp, end = 4.dp, top = 2.dp, bottom = 2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = Ink, modifier = Modifier.size(22.dp))
                    BrutalField(
                        value = search,
                        onValueChange = { search = it },
                        placeholder = "Search transactions",
                        modifier = Modifier.weight(1f),
                        height = 48.dp,
                        textStyle = TextStyle(fontSize = 16.sp, color = Ink),
                        background = Color.Transparent,
                        borderless = true,
                    )
                }
            }
        }

        // Filters
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(end = 4.dp)) {
                listOf("cat", "acct", "amt", "sort").forEach { key ->
                    val def = filters.getValue(key)
                    val value = currentValue(key)
                    val active = value != def.default
                    val label = if (active) def.options.firstOrNull { it.first == value }?.second ?: def.title else def.title
                    FilterChipBox(label, active, Modifier.weight(1f)) { openFilter = key }
                }
            }
        }

        if (pendingCount > 0) {
            item {
                val shape = RoundedCornerShape(16.dp)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(end = 4.dp)
                        .clip(shape)
                        .background(Amber)
                        .border(2.5.dp, Ink, shape)
                        .clickable(onClick = onReview)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconTile(Icons.Filled.Notifications, Color.White, size = 36.dp, iconSize = 20.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "$pendingCount new transaction${if (pendingCount == 1) "" else "s"} from notifications",
                        color = Ink,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(Ink)
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    ) {
                        Text("Review", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        items(groups, key = { it.label }) { g ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.padding(start = 4.dp, end = 8.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(g.label, color = Ink, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                    Text(Fmt.money(g.total), color = Ink, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                }
                BrutalCard(contentPadding = PaddingValues(0.dp)) {
                    g.items.forEachIndexed { i, tx ->
                        TxRow(tx, showDivider = i < g.items.lastIndex) { onOpenTx(tx) }
                    }
                }
            }
        }

        if (groups.isEmpty()) {
            item {
                BrutalCard(contentPadding = PaddingValues(24.dp)) {
                    Text(
                        if (filtering) "No matching transactions" else "Nothing captured in ${month.format(Fmt.monthYear)}",
                        color = Ink,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Transactions show up here automatically when a bank or e-wallet notification arrives. " +
                            "You can also add one with +.",
                        color = Muted,
                        fontSize = 13.5.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    openFilter?.let { key ->
        val def = filters.getValue(key)
        BakiSheet(onDismiss = { openFilter = null }) {
            OptionList(
                title = def.title,
                options = def.options,
                selected = currentValue(key),
                onPick = { applyFilter(key, it); openFilter = null },
                onReset = {
                    search = ""; fCat = "all"; fAcct = "all"; fAmt = "any"; sort = "new"
                    openFilter = null
                },
            )
        }
    }
}

private fun dayLabel(date: LocalDate): String {
    val today = LocalDate.now()
    val base = date.format(Fmt.dayHeader)
    return when (date) {
        today -> "Today · $base"
        today.minusDays(1) -> "Yesterday · $base"
        else -> base
    }
}

@Composable
private fun MonthArrow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = if (enabled) Ink else Disabled, modifier = Modifier.size(26.dp))
    }
}

@Composable
private fun FilterChipBox(label: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier
            .height(42.dp)
            .clip(shape)
            .background(if (active) AccentSoft else Color.White)
            .border(2.dp, Ink, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            label,
            color = Ink,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Icon(Icons.Filled.ExpandMore, contentDescription = null, tint = Ink, modifier = Modifier.size(16.dp))
    }
}
