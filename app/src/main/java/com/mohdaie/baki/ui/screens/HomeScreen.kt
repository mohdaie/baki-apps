package com.mohdaie.baki.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mohdaie.baki.data.Repository
import com.mohdaie.baki.data.TransactionEntity
import com.mohdaie.baki.model.Category
import com.mohdaie.baki.ui.AppViewModel
import com.mohdaie.baki.ui.BakiTab
import com.mohdaie.baki.ui.color
import com.mohdaie.baki.ui.components.*
import com.mohdaie.baki.ui.summarize
import com.mohdaie.baki.ui.theme.*
import com.mohdaie.baki.util.Fmt
import com.mohdaie.baki.util.Permissions
import java.time.LocalDate

@Composable
fun HomeScreen(
    vm: AppViewModel,
    onGoTab: (BakiTab) -> Unit,
    onOpenTx: (TransactionEntity) -> Unit,
) {
    val context = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val txs by vm.monthTx.collectAsStateWithLifecycle()
    val commitments by vm.commitments.collectAsStateWithLifecycle()
    val payments by vm.payments.collectAsStateWithLifecycle()
    val budgets by vm.budgets.collectAsStateWithLifecycle()

    var resumeTick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { resumeTick++ }
    val listenerOn = remember(resumeTick) { Permissions.listenerEnabled(context) }

    val salary = settings?.get(Repository.SALARY)?.toDoubleOrNull() ?: 0.0
    val s = summarize(salary, commitments, payments, txs, budgets.orEmpty(), vm.currentMonth, LocalDate.now())

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 14.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!listenerOn) {
            item {
                BrutalCard(background = Amber) {
                    Text("Turn on notification access", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Baki reads bank and e-wallet notifications on this phone to add your spending automatically. " +
                            "If the switch is greyed out: App info → ⋮ → Allow restricted settings, then try again.",
                        color = Ink,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallButton("Turn on", onClick = { Permissions.openListenerSettings(context) })
                        SmallButton("App info", onClick = { Permissions.openAppInfo(context) })
                    }
                }
            }
        }
        if (settings != null && salary <= 0.0) {
            item {
                BrutalCard(onClick = { onGoTab(BakiTab.PLAN) }) {
                    Text("Set your net salary", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Left to spend is worked out from it. Tap to open Plan.", color = Muted, fontSize = 13.sp)
                }
            }
        }

        item { LeftToSpendCard(s.salary, s.paidTotal, s.spent, s.left) }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BrutalCard(Modifier.weight(1f), contentPadding = PaddingValues(14.dp)) {
                    CapsLabel("Available today")
                    Spacer(Modifier.height(6.dp))
                    Text("RM ${Fmt.money(s.dailyAvailable)}", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                    Text("${s.daysLeft} days left", color = Muted, fontSize = 12.5.sp)
                }
                BrutalCard(Modifier.weight(1f), contentPadding = PaddingValues(14.dp), onClick = { onGoTab(BakiTab.COMMITMENTS) }) {
                    CapsLabel("Commitments due")
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "RM ${Fmt.money(s.commitTotal - s.paidTotal)}",
                        color = Ink,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                    )
                    val sub = when {
                        s.overdue.isNotEmpty() -> "${s.overdue.size} overdue"
                        s.unpaid.isEmpty() -> "All paid"
                        else -> "${s.unpaid.size} unpaid"
                    }
                    Text(sub, color = if (s.overdue.isNotEmpty()) RedText else Muted, fontSize = 12.5.sp, maxLines = 1)
                }
            }
        }

        item {
            BrutalCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("Budget balance", Modifier.weight(1f))
                    SmallButton("Edit plan", onClick = { onGoTab(BakiTab.PLAN) })
                }
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Category.entries.forEach { c ->
                        val alloc = s.allocationByCategory[c.id] ?: 0.0
                        val spent = s.spentByCategory[c.id] ?: 0.0
                        BudgetBalanceRow(c, alloc, spent)
                    }
                    val uncategorized = s.spentByCategory[null] ?: 0.0
                    if (uncategorized > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CategoryTile(null, size = 32.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Uncategorized", color = Ink, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
                                Text("Counted in spending, not in any budget", color = Muted, fontSize = 12.sp)
                            }
                            Text("RM ${Fmt.money(uncategorized)}", color = Ink, fontSize = 14.5.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }

        item {
            BrutalCard(contentPadding = PaddingValues(0.dp)) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("Recent activity", Modifier.weight(1f))
                    SmallButton("See all", onClick = { onGoTab(BakiTab.ACTIVITY) })
                }
                HairlineDivider()
                val recent = txs.take(4)
                if (recent.isEmpty()) {
                    Text(
                        "Nothing yet this month. Spending appears here as notifications come in.",
                        color = Muted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(16.dp),
                    )
                } else {
                    recent.forEachIndexed { i, tx -> TxRow(tx, showDivider = i < recent.lastIndex) { onOpenTx(tx) } }
                }
            }
        }
    }
}

@Composable
private fun LeftToSpendCard(salary: Double, paid: Double, spent: Double, left: Double) {
    BrutalCard(background = AccentSoft, radius = 22.dp, contentPadding = PaddingValues(18.dp)) {
        CapsLabel("Left to spend this month", color = Deep)
        Spacer(Modifier.height(8.dp))
        val parts = Fmt.money(kotlin.math.abs(left)).split(".")
        Row(verticalAlignment = Alignment.Bottom) {
            Text("RM ", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 7.dp))
            Text(
                (if (left < 0) "−" else "") + parts[0],
                color = if (left < 0) Red else Ink,
                fontSize = 46.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Visible,
            )
            Text(".${parts.getOrElse(1) { "00" }}", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
        }
        val pct = if (salary > 0) kotlin.math.max(0, (left / salary * 100).toInt()) else 0
        Text("$pct% of net salary still free", color = Deep, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        DashedRule()
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            KeyValueRow("Net salary", "RM ${Fmt.money(salary)}")
            KeyValueRow("Paid commitments", "− ${Fmt.money(paid)}")
            KeyValueRow("Spent this month", "− ${Fmt.money(spent)}")
        }
    }
}

@Composable
private fun BudgetBalanceRow(c: Category, alloc: Double, spent: Double) {
    val left = alloc - spent
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryTile(c.id, size = 32.dp)
            Spacer(Modifier.width(10.dp))
            Text(c.label, color = Ink, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(
                (if (left < 0) "Over RM " else "RM ") + Fmt.money(kotlin.math.abs(left)),
                color = if (left < 0) Red else Ink,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            val fraction = when {
                alloc > 0 -> (spent / alloc).toFloat()
                spent > 0 -> 1f
                else -> 0f
            }
            Bar(fraction, c.color, Modifier.weight(1f))
            Text(
                "of ${Fmt.money0(alloc)}",
                color = Muted,
                fontSize = 12.sp,
                modifier = Modifier.width(76.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
            )
        }
    }
}
