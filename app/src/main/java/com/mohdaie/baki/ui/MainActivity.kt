package com.mohdaie.baki.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mohdaie.baki.BakiApp
import com.mohdaie.baki.data.CommitmentEntity
import com.mohdaie.baki.data.TransactionEntity
import com.mohdaie.baki.model.TxType
import com.mohdaie.baki.ui.components.*
import com.mohdaie.baki.ui.screens.*
import com.mohdaie.baki.ui.sheets.BakiSheet
import com.mohdaie.baki.ui.sheets.CommitmentForm
import com.mohdaie.baki.ui.sheets.FormInit
import com.mohdaie.baki.ui.sheets.PendingReview
import com.mohdaie.baki.ui.sheets.TransactionForm
import com.mohdaie.baki.ui.theme.*
import com.mohdaie.baki.util.Fmt

enum class BakiTab(val label: String, val title: String, val icon: ImageVector) {
    HOME("Home", "Home", Icons.Filled.Home),
    ACTIVITY("Activity", "Activity", Icons.Filled.Receipt),
    COMMITMENTS("Commitments", "Commitments", Icons.Filled.CalendarMonth),
    PLAN("Plan", "Plan", Icons.Filled.PieChart),
    STATS("Stats", "Statistics", Icons.Filled.BarChart),
}

class MainActivity : ComponentActivity() {

    private val askNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(AndroidColor.WHITE, AndroidColor.WHITE),
        )
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent { BakiTheme { BakiRoot() } }
    }

    override fun onStart() {
        super.onStart()
        (application as BakiApp).mainVisible = true
    }

    override fun onStop() {
        (application as BakiApp).mainVisible = false
        super.onStop()
    }
}

@Composable
fun BakiRoot(vm: AppViewModel = viewModel()) {
    var tab by rememberSaveable { mutableStateOf(BakiTab.HOME) }
    var showSettings by rememberSaveable { mutableStateOf(false) }

    val pendingOrNull by vm.pending.collectAsStateWithLifecycle()
    val pending = pendingOrNull.orEmpty()
    val accounts by vm.accounts.collectAsStateWithLifecycle()
    val commitments by vm.commitments.collectAsStateWithLifecycle()
    val payments by vm.payments.collectAsStateWithLifecycle()

    var reviewOpen by remember { mutableStateOf(false) }
    var seenIds by remember { mutableStateOf(emptySet<Long>()) }
    var editTx by remember { mutableStateOf<TransactionEntity?>(null) }
    var addTx by remember { mutableStateOf(false) }
    var editCommitment by remember { mutableStateOf<CommitmentEntity?>(null) }
    var addCommitment by remember { mutableStateOf(false) }

    // Pop the review sheet up automatically whenever a new captured transaction arrives.
    val firstPendingId = pending.firstOrNull()?.id
    LaunchedEffect(firstPendingId) {
        if (firstPendingId == null) {
            reviewOpen = false
        } else if (firstPendingId !in seenIds) {
            seenIds = seenIds + firstPendingId
            reviewOpen = true
        }
    }

    BackHandler(enabled = showSettings) { showSettings = false }

    val monthLabel = vm.currentMonth.format(Fmt.monthYear)
    val subtitle = when (tab) {
        BakiTab.HOME -> monthLabel
        BakiTab.ACTIVITY -> "Auto-captured from notifications"
        BakiTab.COMMITMENTS -> "$monthLabel · monthly"
        BakiTab.PLAN -> "$monthLabel budget"
        BakiTab.STATS -> "Your money, over time"
    }

    Box(Modifier.fillMaxSize().background(Paper).paperGrid()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 18.dp, end = 15.dp, top = 14.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(tab.title, color = Ink, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                    Text(subtitle, color = Muted, fontSize = 13.sp)
                }
                IconSquare(Icons.Filled.Settings, "Settings", onClick = { showSettings = true })
                Spacer(Modifier.width(10.dp))
                IconSquare(
                    Icons.Filled.Notifications,
                    if (pending.isEmpty()) "No new transactions" else "${pending.size} new transactions",
                    onClick = { if (pending.isNotEmpty()) reviewOpen = true },
                    badge = pending.size,
                )
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (tab) {
                    BakiTab.HOME -> HomeScreen(vm, onGoTab = { tab = it }, onOpenTx = { editTx = it })
                    BakiTab.ACTIVITY -> ActivityScreen(vm, onOpenTx = { editTx = it }, onReview = { reviewOpen = true })
                    BakiTab.COMMITMENTS -> CommitmentsScreen(vm, onEdit = { editCommitment = it })
                    BakiTab.PLAN -> PlanScreen(vm)
                    BakiTab.STATS -> StatsScreen(vm)
                }
                if (tab == BakiTab.ACTIVITY || tab == BakiTab.COMMITMENTS) {
                    Fab(
                        description = if (tab == BakiTab.ACTIVITY) "Add transaction" else "Add commitment",
                        onClick = { if (tab == BakiTab.ACTIVITY) addTx = true else addCommitment = true },
                        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 16.dp),
                    )
                }
            }

            BottomNav(tab) { tab = it }
        }

        if (showSettings) SettingsScreen(vm, onClose = { showSettings = false })
    }

    // ----- sheets -----
    val current = pending.firstOrNull()
    if (reviewOpen && current != null) {
        BakiSheet(onDismiss = { reviewOpen = false }) {
            PendingReview(
                p = current,
                index = 1,
                total = pending.size,
                accounts = accounts,
                unpaidCommitments = unpaidCommitments(commitments, payments),
                onSave = { vm.confirmPending(current.id, it) },
                onIgnore = { vm.ignorePending(current.id) },
                onLinkCommitment = { vm.markPendingAsCommitment(current.id, it) },
            )
        }
    }

    editTx?.let { tx ->
        BakiSheet(onDismiss = { editTx = null }) {
            TransactionForm(
                title = "Edit transaction",
                init = FormInit("tx-${tx.id}", tx.amount, tx.type, tx.merchant, tx.account, tx.category, null, tx.fxText),
                accounts = accounts,
                primaryLabel = "Save",
                secondaryLabel = "Delete",
                onPrimary = {
                    vm.saveTransaction(tx.id, it, tx.timestamp)
                    editTx = null
                },
                onSecondary = {
                    vm.deleteTransaction(tx.id)
                    editTx = null
                },
            )
        }
    }

    if (addTx) {
        BakiSheet(onDismiss = { addTx = false }) {
            TransactionForm(
                title = "Add transaction",
                init = FormInit("new", null, TxType.EXPENSE, "", accounts.firstOrNull() ?: "Cash", null, null, null),
                accounts = accounts,
                primaryLabel = "Save",
                secondaryLabel = "Cancel",
                onPrimary = {
                    vm.saveTransaction(null, it, System.currentTimeMillis())
                    addTx = false
                },
                onSecondary = { addTx = false },
            )
        }
    }

    if (addCommitment || editCommitment != null) {
        val editing = editCommitment
        BakiSheet(onDismiss = { addCommitment = false; editCommitment = null }) {
            CommitmentForm(
                initial = editing,
                onSave = {
                    vm.saveCommitment(it)
                    addCommitment = false
                    editCommitment = null
                },
                onDelete = editing?.let { c ->
                    {
                        vm.deleteCommitment(c.id)
                        editCommitment = null
                    }
                },
            )
        }
    }
}

@Composable
private fun BottomNav(current: BakiTab, onSelect: (BakiTab) -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding()) {
        Box(Modifier.fillMaxWidth().height(2.5.dp).background(Ink))
        Row(
            Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            BakiTab.entries.forEach { t ->
                val on = t == current
                Column(
                    Modifier
                        .weight(1f)
                        .height(58.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (on) Accent else Color.Transparent)
                        .clickable { onSelect(t) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(t.icon, contentDescription = null, tint = if (on) Color.White else Muted, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        t.label,
                        color = if (on) Color.White else Muted,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
