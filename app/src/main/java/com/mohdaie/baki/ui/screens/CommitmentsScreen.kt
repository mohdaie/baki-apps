package com.mohdaie.baki.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mohdaie.baki.data.CommitmentEntity
import com.mohdaie.baki.ui.AppViewModel
import com.mohdaie.baki.ui.DueStatus
import com.mohdaie.baki.ui.components.*
import com.mohdaie.baki.ui.dueDayIn
import com.mohdaie.baki.ui.dueStatus
import com.mohdaie.baki.ui.theme.*
import com.mohdaie.baki.util.Fmt
import java.time.LocalDate

@Composable
fun CommitmentsScreen(vm: AppViewModel, onEdit: (CommitmentEntity) -> Unit) {
    val commitments by vm.commitments.collectAsStateWithLifecycle()
    val payments by vm.payments.collectAsStateWithLifecycle()
    val month = vm.currentMonth
    val today = LocalDate.now()
    val paidIds = payments.map { it.commitmentId }.toSet()
    val total = commitments.sumOf { it.amount }
    val paidTotal = commitments.filter { it.id in paidIds }.sumOf { it.amount }
    val paidCount = commitments.count { it.id in paidIds }
    val unpaid = commitments.filter { it.id !in paidIds }
    val overdue = unpaid.filter { dueDayIn(it, month) < today.dayOfMonth }
    val next = unpaid.filter { dueDayIn(it, month) >= today.dayOfMonth }.minByOrNull { it.dueDay }
    val sorted = commitments.sortedBy { it.dueDay }
    val monShort = month.format(Fmt.monShort)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 14.dp, top = 4.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            BrutalCard {
                CapsLabel("Paid this month")
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("RM ${Fmt.money(paidTotal)}", color = Ink, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.width(6.dp))
                    Text("of RM ${Fmt.money(total)}", color = Muted, fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
                }
                Spacer(Modifier.height(10.dp))
                Bar(if (total > 0) (paidTotal / total).toFloat() else 0f, Accent, height = 14.dp)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Ticking Paid deducts the amount from this month's salary balance on Home.",
                    color = Muted,
                    fontSize = 12.5.sp,
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BrutalCard(Modifier.weight(1f), contentPadding = PaddingValues(14.dp)) {
                    CapsLabel("Progress")
                    Spacer(Modifier.height(6.dp))
                    Text("$paidCount / ${commitments.size}", color = Ink, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                    Text("commitments paid", color = Muted, fontSize = 12.5.sp)
                }
                BrutalCard(Modifier.weight(1f), contentPadding = PaddingValues(14.dp)) {
                    CapsLabel("Attention")
                    Spacer(Modifier.height(6.dp))
                    val (headline, sub, color) = when {
                        overdue.isNotEmpty() -> Triple("${overdue.size} overdue", overdue.joinToString { it.name }, RedText)
                        next != null -> Triple("Next: ${next.name}", "Due ${dueDayIn(next, month)} $monShort · RM ${Fmt.money(next.amount)}", Green)
                        commitments.isEmpty() -> Triple("Nothing yet", "Add your loans, rent and bills", Muted)
                        else -> Triple("All clear", "Everything is paid", Green)
                    }
                    Text(headline, color = color, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(sub, color = Muted, fontSize = 12.5.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        item {
            if (sorted.isEmpty()) {
                BrutalCard(contentPadding = PaddingValues(24.dp)) {
                    Text("No commitments yet", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Add loans, rent, insurance and subscriptions with +. Each one repeats every month.",
                        color = Muted,
                        fontSize = 13.5.sp,
                    )
                }
            } else {
                BrutalCard(contentPadding = PaddingValues(0.dp)) {
                    sorted.forEachIndexed { i, c ->
                        val paid = c.id in paidIds
                        CommitmentRow(
                            c = c,
                            paid = paid,
                            sub = "Due ${dueDayIn(c, month)} $monShort · ${c.kind}",
                            status = dueStatus(c, paid, month, today),
                            onToggle = { vm.setPaid(c, !paid) },
                            onClick = { onEdit(c) },
                        )
                        if (i < sorted.lastIndex) HairlineDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun CommitmentRow(
    c: CommitmentEntity,
    paid: Boolean,
    sub: String,
    status: DueStatus,
    onToggle: () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 6.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The paid tick box (44dp touch target).
        Box(
            Modifier
                .size(44.dp)
                .toggleable(value = paid, role = Role.Checkbox, onValueChange = { onToggle() }),
            contentAlignment = Alignment.Center,
        ) {
            val shape = RoundedCornerShape(9.dp)
            Box(
                Modifier
                    .size(30.dp)
                    .clip(shape)
                    .background(if (paid) Accent else Color.White)
                    .border(2.5.dp, Ink, shape),
                contentAlignment = Alignment.Center,
            ) {
                if (paid) {
                    Icon(Icons.Filled.Check, contentDescription = "Paid", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(c.name, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sub, color = Muted, fontSize = 12.5.sp, maxLines = 1)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("RM ${Fmt.money(c.amount)}", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            StatusPill(status.text, status.background, status.foreground)
        }
    }
}
