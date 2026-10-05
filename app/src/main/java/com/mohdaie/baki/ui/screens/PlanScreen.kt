package com.mohdaie.baki.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mohdaie.baki.data.BudgetEntity
import com.mohdaie.baki.data.Repository
import com.mohdaie.baki.model.BudgetMode
import com.mohdaie.baki.model.Category
import com.mohdaie.baki.model.TxType
import com.mohdaie.baki.ui.AppViewModel
import com.mohdaie.baki.ui.allocationFor
import com.mohdaie.baki.ui.color
import com.mohdaie.baki.ui.components.*
import com.mohdaie.baki.ui.theme.*
import com.mohdaie.baki.util.Fmt
import com.mohdaie.baki.util.toAmount
import kotlin.math.max
import kotlin.math.round

@Composable
fun PlanScreen(vm: AppViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val commitments by vm.commitments.collectAsStateWithLifecycle()
    val budgets by vm.budgets.collectAsStateWithLifecycle()
    val txs by vm.monthTx.collectAsStateWithLifecycle()

    var salaryText by rememberSyncedText(
        settings?.get(Repository.SALARY)?.toDoubleOrNull()?.takeIf { it > 0 }?.let { Fmt.plain(it) },
        ready = settings != null,
    )
    val salary = salaryText.toAmount()
    val commitTotal = commitments.sumOf { it.amount }
    val available = salary - commitTotal
    val budgetMap = budgets.orEmpty()
    val totalAllocated = Category.entries.sumOf { allocationFor(budgetMap[it.id], available) }
    val unallocated = available - totalAllocated
    val spentByCat = txs.filter { it.type == TxType.EXPENSE }.groupBy { it.category }.mapValues { (_, l) -> l.sumOf { it.amount } }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 14.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            BrutalCard {
                CapsLabel("Net salary · monthly")
                Spacer(Modifier.height(8.dp))
                BrutalField(
                    value = salaryText,
                    onValueChange = {
                        salaryText = it
                        vm.setSalary(it.toAmount())
                    },
                    placeholder = "0",
                    prefix = "RM",
                    keyboardType = KeyboardType.Decimal,
                    textStyle = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Ink),
                    height = 58.dp,
                    background = Paper,
                )
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    KeyValueRow("Net salary", "RM ${Fmt.money(salary)}")
                    KeyValueRow("Commitments (${commitments.size})", "− ${Fmt.money(commitTotal)}")
                    DashedRule()
                    KeyValueRow("Available to plan", "RM ${Fmt.money(available)}", bold = true, valueSize = 16)
                }
            }
        }

        item {
            BrutalCard {
                Row(verticalAlignment = Alignment.Bottom) {
                    SectionTitle("Divide by category", Modifier.weight(1f))
                    Text("RM or %", color = Muted, fontSize = 12.5.sp)
                }
                Spacer(Modifier.height(4.dp))
                Text("% is a share of what's available after commitments.", color = Muted, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                Category.entries.forEachIndexed { i, c ->
                    BudgetRow(
                        c = c,
                        budget = budgetMap[c.id],
                        ready = budgets != null,
                        available = available,
                        spent = spentByCat[c.id] ?: 0.0,
                        onChange = { mode, value -> vm.setBudget(c.id, mode, value) },
                    )
                    if (i < Category.entries.lastIndex) {
                        Spacer(Modifier.height(14.dp))
                        HairlineDivider()
                        Spacer(Modifier.height(14.dp))
                    }
                }
            }
        }

        item {
            BrutalCard(background = if (unallocated < 0) RedSoft else AccentSoft) {
                KeyValueRow("Allocated", "RM ${Fmt.money(totalAllocated)}")
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (unallocated < 0) "Over-allocated by" else "Unallocated",
                        color = Ink,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    Text("RM ${Fmt.money(kotlin.math.abs(unallocated))}", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(Modifier.height(10.dp))
                Bar(
                    if (available > 0) (totalAllocated / available).toFloat() else 1f,
                    Ink,
                    track = Color.White,
                    height = 12.dp,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (available > 0) "${(totalAllocated / available * 100).toInt()}% of available money is planned"
                    else "Commitments are more than your salary",
                    color = Deep,
                    fontSize = 12.5.sp,
                )
            }
        }
    }
}

@Composable
private fun BudgetRow(
    c: Category,
    budget: BudgetEntity?,
    ready: Boolean,
    available: Double,
    spent: Double,
    onChange: (String, Double) -> Unit,
) {
    val mode = budget?.mode ?: BudgetMode.AMOUNT
    val isPct = mode == BudgetMode.PERCENT
    var text by rememberSyncedText(budget?.value?.takeIf { it > 0 }?.let { Fmt.plain(it) }, ready)
    val value = text.toAmount()
    val amount = if (isPct) max(0.0, available) * value / 100.0 else value

    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryTile(c.id, size = 32.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(c.label, color = Ink, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                Text("RM ${Fmt.money(spent)} spent of ${Fmt.money(amount)}", color = Muted, fontSize = 12.sp)
            }
            Row(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .border(2.dp, Ink, RoundedCornerShape(10.dp)),
            ) {
                ModeButton("RM", selected = !isPct) {
                    if (isPct) {
                        val v = round(amount * 100) / 100
                        text = Fmt.plain(v)
                        onChange(BudgetMode.AMOUNT, v)
                    }
                }
                Box(Modifier.width(2.dp).height(36.dp).background(Ink))
                ModeButton("%", selected = isPct) {
                    if (!isPct) {
                        val v = if (available > 0) round(amount / available * 1000) / 10 else 0.0
                        text = Fmt.plain(v)
                        onChange(BudgetMode.PERCENT, v)
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrutalField(
                value = text,
                onValueChange = {
                    text = it
                    onChange(mode, it.toAmount())
                },
                placeholder = "0",
                prefix = if (isPct) "%" else "RM",
                keyboardType = KeyboardType.Decimal,
                textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink),
                height = 46.dp,
                modifier = Modifier.weight(1f),
            )
            Text(
                if (isPct) "= RM ${Fmt.money(amount)}"
                else if (available > 0) "${Fmt.plain(round(amount / available * 1000) / 10)}% of available"
                else "",
                color = Muted,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End,
                modifier = Modifier.width(128.dp),
            )
        }
        Bar(
            if (amount > 0) (spent / amount).toFloat() else 0f,
            c.color,
            height = 6.dp,
            bordered = false,
        )
    }
}

@Composable
private fun ModeButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(width = 44.dp, height = 36.dp)
            .background(if (selected) Ink else Color.White)
            .semantics {
                role = Role.Tab
                this.selected = selected
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (selected) Color.White else Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}
