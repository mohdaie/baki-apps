package com.mohdaie.baki.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mohdaie.baki.model.Category
import com.mohdaie.baki.model.TxType
import com.mohdaie.baki.ui.AppViewModel
import com.mohdaie.baki.ui.color
import com.mohdaie.baki.ui.components.*
import com.mohdaie.baki.ui.theme.*
import com.mohdaie.baki.util.Fmt
import com.mohdaie.baki.util.toLocalDate
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun StatsScreen(vm: AppViewModel) {
    val history by vm.historyTx.collectAsStateWithLifecycle()
    val month = vm.currentMonth
    val today = LocalDate.now()

    val expenses = history.filter { it.type == TxType.EXPENSE }
    val months = (3 downTo 0).map { month.minusMonths(it.toLong()) }
    val monthTotals = months.map { m -> expenses.filter { YearMonth.from(it.timestamp.toLocalDate()) == m }.sumOf { it.amount } }
    val current = expenses.filter { YearMonth.from(it.timestamp.toLocalDate()) == month }
    val spent = monthTotals.last()
    val lastMonth = monthTotals[2]
    val daysSoFar = if (YearMonth.from(today) == month) today.dayOfMonth else month.lengthOfMonth()
    val avgPerDay = spent / max(1, daysSoFar)
    val projected = avgPerDay * month.lengthOfMonth()

    val byDay = (1..month.lengthOfMonth()).map { d -> current.filter { it.timestamp.toLocalDate().dayOfMonth == d }.sumOf { it.amount } }
    val peakDay = byDay.indices.maxByOrNull { byDay[it] }

    val byCategory = current.groupBy { Category.of(it.category) }
        .map { (c, list) -> CatSpend(c?.id, c?.label ?: "Uncategorized", c?.color ?: SlateTile, list.sumOf { it.amount }) }
        .sortedByDescending { it.amount }
    val merchants = current.groupBy { it.merchant }
        .map { (name, list) -> Triple(name, list.size, list.sumOf { it.amount }) }
        .sortedByDescending { it.third }
        .take(5)
    val accounts = current.groupBy { it.account }
        .map { (name, list) -> name to list.sumOf { it.amount } }
        .sortedByDescending { it.second }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 14.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Kpi("Spent in ${month.format(Fmt.monShort)}", "RM ${Fmt.money(spent)}", "Month to date", Muted, Modifier.weight(1f))
                    Kpi("Daily average", "RM ${Fmt.money(avgPerDay)}", "Over $daysSoFar days", Muted, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val vsText: String
                    val vsColor: Color
                    if (lastMonth > 0) {
                        val pct = ((projected - lastMonth) / lastMonth * 100).roundToInt()
                        vsText = (if (pct <= 0) "↓ " else "↑ ") + "${kotlin.math.abs(pct)}% vs last month"
                        vsColor = if (pct <= 0) Green else Red
                    } else {
                        vsText = "No data for last month"
                        vsColor = Muted
                    }
                    Kpi("Projected", "RM ${Fmt.money0(projected)}", vsText, vsColor, Modifier.weight(1f))
                    val top = byCategory.firstOrNull()
                    Kpi(
                        "Top category",
                        top?.label ?: "—",
                        top?.let { "RM ${Fmt.money(it.amount)}" } ?: "No spending yet",
                        Muted,
                        Modifier.weight(1f),
                    )
                }
            }
        }

        item {
            BrutalCard {
                SectionTitle("Monthly spending")
                Text("Last 4 months · this month is to date", color = Muted, fontSize = 12.5.sp)
                Spacer(Modifier.height(12.dp))
                val maxTotal = monthTotals.maxOrNull()?.takeIf { it > 0 } ?: 1.0
                Row(
                    Modifier.fillMaxWidth().height(170.dp).padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    months.forEachIndexed { i, m ->
                        val v = monthTotals[i]
                        val h = max(4.0, v / maxTotal * 112).dp
                        val shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                        Column(
                            Modifier.weight(1f).fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                        ) {
                            Text(Fmt.money0(v), color = Ink, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Spacer(Modifier.height(4.dp))
                            Box(
                                Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(h)
                                    .clip(shape)
                                    .background(if (i == months.lastIndex) Accent else Color(0xFFC9C7BE))
                                    .border(2.dp, Ink, shape),
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(m.format(Fmt.monShort), color = Ink, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            BrutalCard {
                SectionTitle("Daily spending · ${month.format(Fmt.monShort)}")
                Text(
                    if (peakDay != null && byDay[peakDay] > 0) {
                        "Highest day: ${peakDay + 1} ${month.format(Fmt.monShort)} · RM ${Fmt.money(byDay[peakDay])}"
                    } else {
                        "No spending yet"
                    },
                    color = Muted,
                    fontSize = 12.5.sp,
                )
                Spacer(Modifier.height(12.dp))
                val maxDay = byDay.maxOrNull()?.takeIf { it > 0 } ?: 1.0
                Row(
                    Modifier.fillMaxWidth().height(100.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    byDay.forEachIndexed { i, v ->
                        val day = i + 1
                        val future = YearMonth.from(today) == month && day > today.dayOfMonth
                        val h = when {
                            future -> 3.0
                            v > 0 -> max(4.0, v / maxDay * 96)
                            else -> 3.0
                        }
                        Box(
                            Modifier
                                .weight(1f)
                                .height(h.dp)
                                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                .background(
                                    when {
                                        future -> Hairline
                                        day == today.dayOfMonth && YearMonth.from(today) == month -> Accent
                                        else -> Ink
                                    },
                                ),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    byDay.indices.forEach { i ->
                        val day = i + 1
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            if (day == 1 || day % 7 == 1) {
                                Text(
                                    "$day",
                                    color = Muted,
                                    fontSize = 10.5.sp,
                                    softWrap = false,
                                    modifier = Modifier.wrapContentWidth(unbounded = true),
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            BrutalCard {
                SectionTitle("Where it went")
                Spacer(Modifier.height(12.dp))
                if (byCategory.isEmpty()) {
                    Text("No spending yet this month.", color = Muted, fontSize = 13.sp)
                }
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    val top = byCategory.firstOrNull()?.amount ?: 1.0
                    byCategory.forEach { c ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CategoryTile(c.id, size = 32.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Row {
                                    Text(c.label, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                    Text("RM ${Fmt.money(c.amount)}", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Bar((c.amount / top).toFloat(), c.color, Modifier.weight(1f), height = 8.dp, bordered = false)
                                    Text(
                                        "${if (spent > 0) (c.amount / spent * 100).roundToInt() else 0}%",
                                        color = Muted,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.width(40.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            BrutalCard(contentPadding = PaddingValues(0.dp)) {
                SectionTitle("Top merchants", Modifier.padding(horizontal = 16.dp, vertical = 14.dp))
                HairlineDivider()
                if (merchants.isEmpty()) {
                    Text("No spending yet this month.", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(16.dp))
                }
                merchants.forEachIndexed { i, (name, count, amount) ->
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(Ink),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("${i + 1}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(name, color = Ink, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("$count payment${if (count == 1) "" else "s"}", color = Muted, fontSize = 12.sp)
                        }
                        Text("RM ${Fmt.money(amount)}", color = Ink, fontSize = 14.5.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    if (i < merchants.lastIndex) HairlineDivider()
                }
            }
        }

        if (accounts.isNotEmpty()) {
            item {
                BrutalCard {
                    SectionTitle("By account")
                    Spacer(Modifier.height(12.dp))
                    val top = accounts.first().second
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        accounts.forEach { (name, amount) ->
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Row {
                                    Text(name, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                    Text("RM ${Fmt.money(amount)}", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                                }
                                Bar((amount / top).toFloat(), Ink, height = 8.dp, bordered = false)
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class CatSpend(val id: String?, val label: String, val color: Color, val amount: Double)

@Composable
private fun Kpi(label: String, value: String, sub: String, subColor: Color, modifier: Modifier) {
    BrutalCard(modifier, contentPadding = PaddingValues(14.dp)) {
        CapsLabel(label)
        Spacer(Modifier.height(6.dp))
        Text(value, color = Ink, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(sub, color = subColor, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
