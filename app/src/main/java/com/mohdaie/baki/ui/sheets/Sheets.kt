@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.mohdaie.baki.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohdaie.baki.data.CommitmentEntity
import com.mohdaie.baki.data.PendingEntity
import com.mohdaie.baki.data.TxDraft
import com.mohdaie.baki.model.COMMITMENT_KINDS
import com.mohdaie.baki.model.Category
import com.mohdaie.baki.model.TxType
import com.mohdaie.baki.parser.NotificationParser
import com.mohdaie.baki.ui.components.*
import com.mohdaie.baki.ui.theme.*
import com.mohdaie.baki.util.Fmt
import com.mohdaie.baki.util.toAmount
import com.mohdaie.baki.util.toLocalDateTime

/** Bottom sheet frame used for every form. */
@Composable
fun BakiSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 46.dp, height = 5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Ink),
            )
        },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(start = 18.dp, end = 18.dp, bottom = 24.dp),
            content = content,
        )
    }
}

/** Initial values for the transaction form. [key] resets the form when it changes. */
data class FormInit(
    val key: Any,
    val amount: Double?,
    val type: String,
    val merchant: String,
    val account: String,
    val category: String?,
    val suggested: String?,
    val fxText: String?,
)

/**
 * The popup form: amount, type, merchant, account, category.
 * Used for captured notifications, editing and manual adding.
 */
@Composable
fun TransactionForm(
    title: String,
    init: FormInit,
    accounts: List<String>,
    primaryLabel: String,
    secondaryLabel: String,
    onPrimary: (TxDraft) -> Unit,
    onSecondary: () -> Unit,
    notification: PendingEntity? = null,
    counter: String? = null,
    matching: CommitmentEntity? = null,
    onMatchCommitment: ((CommitmentEntity) -> Unit)? = null,
) {
    var amount by remember(init.key) { mutableStateOf(init.amount?.let { Fmt.fixed2(it) } ?: "") }
    var type by remember(init.key) { mutableStateOf(init.type) }
    var merchant by remember(init.key) {
        mutableStateOf(if (init.merchant == NotificationParser.UNKNOWN_MERCHANT) "" else init.merchant)
    }
    var account by remember(init.key) { mutableStateOf(init.account) }
    var category by remember(init.key) { mutableStateOf(init.category) }
    val accountOptions = remember(accounts, init.account) {
        (listOf(init.account) + accounts + "Cash").filter { it.isNotBlank() }.distinct()
    }
    val amountValue = amount.toAmount()

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (notification != null) NotificationPreview(notification, counter)

        Text(title, color = Ink, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)

        if (matching != null && onMatchCommitment != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Amber)
                    .border(2.dp, Ink, RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Looks like ${matching.name}", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Same amount as this month's commitment. Tick it instead so it isn't counted twice.",
                        color = Muted,
                        fontSize = 12.5.sp,
                    )
                }
                Spacer(Modifier.width(10.dp))
                SmallButton("Mark paid", onClick = { onMatchCommitment(matching) }, background = Color.White)
            }
        }

        // Amount
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Text("RM", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(6.dp))
                BasicTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 44.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Ink,
                        textAlign = TextAlign.Center,
                    ),
                    cursorBrush = SolidColor(Ink),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.width(210.dp),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.Center) {
                            if (amount.isEmpty()) {
                                Text("0.00", fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, color = Faint)
                            }
                            inner()
                        }
                    },
                )
            }
            Box(Modifier.fillMaxWidth().height(3.dp).background(Ink))
            if (init.fxText != null) {
                Spacer(Modifier.height(6.dp))
                Text("Charged as ${init.fxText}", color = Muted, fontSize = 12.5.sp)
            }
        }

        // Type
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Paper)
                .border(2.5.dp, Ink, RoundedCornerShape(14.dp))
                .padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOf(TxType.EXPENSE to "Expense", TxType.INCOME to "Income", TxType.TRANSFER to "Transfer").forEach { (key, label) ->
                val on = type == key
                val bg = when {
                    !on -> Color.Transparent
                    key == TxType.EXPENSE -> Red
                    key == TxType.INCOME -> Green
                    else -> Ink
                }
                Box(
                    Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(bg)
                        .clickable { type = key },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label.uppercase(),
                        color = if (on) Color.White else Ink,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                    )
                }
            }
        }

        // Merchant
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            CapsLabel("Merchant")
            BrutalField(merchant, { merchant = it }, placeholder = "Where did you spend?", capitalizeWords = true)
        }

        // Account
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            CapsLabel("Account")
            PillRow(accountOptions, account) { account = it }
        }

        // Category
        if (type == TxType.EXPENSE) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CapsLabel("Category")
                Category.entries.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { c ->
                            CategoryOption(
                                c = c,
                                selected = category == c.id,
                                suggested = init.suggested == c.id,
                                onClick = { category = c.id },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
            if (notification != null) {
                Text(
                    "Pick another category to correct it. Baki remembers it for this merchant next time.",
                    color = Muted,
                    fontSize = 12.5.sp,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 2.dp)) {
            BrutalButton(
                secondaryLabel,
                onClick = onSecondary,
                modifier = Modifier.weight(1f),
                background = Color.White,
                foreground = if (secondaryLabel == "Delete") Red else Ink,
                shadow = false,
            )
            BrutalButton(
                primaryLabel,
                onClick = {
                    onPrimary(
                        TxDraft(
                            amount = amountValue,
                            type = type,
                            category = if (type == TxType.EXPENSE) category else null,
                            merchant = merchant.trim().ifBlank { NotificationParser.UNKNOWN_MERCHANT },
                            account = account,
                        ),
                    )
                },
                modifier = Modifier.weight(1f),
                enabled = amountValue > 0,
            )
        }
    }
}

@Composable
private fun NotificationPreview(p: PendingEntity, counter: String?) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Paper)
            .border(2.dp, Ink, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Notifications, contentDescription = null, tint = Ink, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(p.appLabel, color = Ink, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            Text(" · " + p.postedAt.toLocalDateTime().format(Fmt.time), color = Muted, fontSize = 12.5.sp)
            Spacer(Modifier.weight(1f))
            if (counter != null) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Ink)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(counter, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Text(p.rawText, color = Ink, fontSize = 13.5.sp, lineHeight = 19.sp, maxLines = 6)
    }
}

@Composable
private fun CategoryOption(
    c: Category,
    selected: Boolean,
    suggested: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier
            .heightIn(min = 54.dp)
            .clip(shape)
            .background(if (selected) Paper else Color.White)
            .border(if (selected) 2.5.dp else 1.5.dp, if (selected) Ink else Color(0xFFCFCDC4), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryTile(c.id, size = 28.dp, iconSize = 16.dp)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(c.label, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold, lineHeight = 16.sp)
            if (suggested) {
                Text("SUGGESTED", color = Green, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
            }
        }
        if (selected) Icon(Icons.Filled.Check, contentDescription = "Selected", tint = Ink, modifier = Modifier.size(18.dp))
    }
}

/** Review of one captured notification (used by the in-app sheet and the over-other-apps popup). */
@Composable
fun PendingReview(
    p: PendingEntity,
    index: Int,
    total: Int,
    accounts: List<String>,
    matching: CommitmentEntity?,
    onSave: (TxDraft) -> Unit,
    onIgnore: () -> Unit,
    onMarkCommitment: (CommitmentEntity) -> Unit,
) {
    TransactionForm(
        title = "New transaction detected",
        init = FormInit(
            key = p.id,
            amount = p.amount,
            type = p.type,
            merchant = p.merchant,
            account = p.account,
            category = p.category,
            suggested = p.category,
            fxText = p.fxText,
        ),
        accounts = accounts,
        primaryLabel = "Save",
        secondaryLabel = "Ignore",
        onPrimary = onSave,
        onSecondary = onIgnore,
        notification = p,
        counter = "$index of $total",
        matching = matching,
        onMatchCommitment = onMarkCommitment,
    )
}

@Composable
fun CommitmentForm(
    initial: CommitmentEntity?,
    onSave: (CommitmentEntity) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var name by remember(initial?.id) { mutableStateOf(initial?.name ?: "") }
    var amount by remember(initial?.id) { mutableStateOf(initial?.amount?.let { Fmt.plain(it) } ?: "") }
    var due by remember(initial?.id) { mutableStateOf(initial?.dueDay?.toString() ?: "") }
    var kind by remember(initial?.id) { mutableStateOf(initial?.kind ?: COMMITMENT_KINDS.first()) }
    val dueDay = due.trim().toIntOrNull() ?: 0
    val valid = name.isNotBlank() && amount.toAmount() > 0 && dueDay in 1..31

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            if (initial == null) "New commitment" else "Edit commitment",
            color = Ink,
            fontSize = 21.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            CapsLabel("Name")
            BrutalField(name, { name = it }, placeholder = "e.g. Car loan", capitalizeWords = true)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CapsLabel("Amount (RM)")
                BrutalField(
                    amount,
                    { amount = it },
                    placeholder = "0.00",
                    keyboardType = KeyboardType.Decimal,
                    textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CapsLabel("Due day (1–31)")
                BrutalField(
                    due,
                    { due = it.filter { ch -> ch.isDigit() }.take(2) },
                    placeholder = "e.g. 7",
                    keyboardType = KeyboardType.Number,
                    textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink),
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            CapsLabel("Type")
            PillRow(COMMITMENT_KINDS, kind) { kind = it }
        }
        Text(
            "Repeats every month. Tick it as paid on the Commitments tab and it comes off this month's salary.",
            color = Muted,
            fontSize = 12.5.sp,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (onDelete != null) {
                BrutalButton(
                    "Delete",
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    background = Color.White,
                    foreground = Red,
                    shadow = false,
                )
            }
            BrutalButton(
                if (initial == null) "Add commitment" else "Save",
                onClick = {
                    onSave(
                        CommitmentEntity(
                            id = initial?.id ?: 0,
                            name = name.trim(),
                            amount = amount.toAmount(),
                            dueDay = dueDay,
                            kind = kind,
                        ),
                    )
                },
                modifier = Modifier.weight(1f),
                enabled = valid,
            )
        }
    }
}

/** A simple pick-one list (used by the Activity filters). */
@Composable
fun OptionList(
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    onPick: (String) -> Unit,
    onReset: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
            Text(title, color = Ink, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            SmallButton("Reset all", onClick = onReset)
        }
        options.forEach { (value, label) ->
            val on = value == selected
            val shape = RoundedCornerShape(12.dp)
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(shape)
                    .background(if (on) Paper else Color.White)
                    .border(if (on) 2.5.dp else 1.5.dp, if (on) Ink else Color(0xFFCFCDC4), shape)
                    .clickable { onPick(value) }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    label,
                    color = Ink,
                    fontSize = 15.sp,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                if (on) Icon(Icons.Filled.Check, contentDescription = "Selected", tint = Ink, modifier = Modifier.size(20.dp))
            }
        }
    }
}
