package com.mohdaie.baki.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mohdaie.baki.data.AppSourceEntity
import com.mohdaie.baki.data.Repository
import com.mohdaie.baki.ui.AppViewModel
import com.mohdaie.baki.ui.components.*
import com.mohdaie.baki.ui.theme.*
import com.mohdaie.baki.util.Fmt
import com.mohdaie.baki.util.Permissions
import com.mohdaie.baki.util.toAmount

@Composable
fun SettingsScreen(vm: AppViewModel, onClose: () -> Unit) {
    val context = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val sources by vm.sources.collectAsStateWithLifecycle()

    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }
    val listenerOn = remember(tick) { Permissions.listenerEnabled(context) }
    val notifyOn = remember(tick) { Permissions.canPostNotifications(context) }
    val overlayOn = remember(tick) { Permissions.canDrawOverlays(context) }

    var rateText by rememberSyncedText(
        Fmt.plain(settings?.get(Repository.SGD_RATE)?.toDoubleOrNull() ?: Repository.DEFAULT_SGD_RATE),
        ready = settings != null,
    )
    var testSent by remember { mutableIntStateOf(0) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Paper)
            .paperGrid()
            // Swallow taps so they don't reach the screen underneath.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 18.dp, end = 15.dp, top = 14.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Settings", color = Ink, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Capture, accounts and currency", color = Muted, fontSize = 13.sp)
                }
                IconSquare(Icons.Filled.Close, "Close settings", onClose)
            }

            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 18.dp, end = 14.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    BrutalCard {
                        SectionTitle("Permissions")
                        Spacer(Modifier.height(12.dp))
                        PermissionRow(
                            "Notification access",
                            "Required. Lets Baki read bank and e-wallet notifications.",
                            listenerOn,
                        ) { Permissions.openListenerSettings(context) }
                        Spacer(Modifier.height(12.dp))
                        PermissionRow(
                            "Show notifications",
                            "Baki's own \"new transaction\" alert with Save / Ignore.",
                            notifyOn,
                        ) { Permissions.openAppNotificationSettings(context) }
                        Spacer(Modifier.height(12.dp))
                        PermissionRow(
                            "Display over other apps",
                            "Optional. Opens the review popup on top of whatever app you're in.",
                            overlayOn,
                        ) { Permissions.openOverlaySettings(context) }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Switch greyed out? Open App info → ⋮ (top right) → Allow restricted settings, then come back.",
                            color = Muted,
                            fontSize = 12.5.sp,
                        )
                        Spacer(Modifier.height(8.dp))
                        SmallButton("Open App info", onClick = { Permissions.openAppInfo(context) })
                    }
                }

                item {
                    BrutalCard {
                        SectionTitle("Try it")
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Sends a sample bank notification through the same capture pipeline, so you can see the popup.",
                            color = Muted,
                            fontSize = 13.sp,
                        )
                        Spacer(Modifier.height(12.dp))
                        BrutalButton(
                            if (testSent == 0) "Send a test notification" else "Send another (sent $testSent)",
                            onClick = {
                                vm.sendTestNotification()
                                testSent++
                            },
                        )
                    }
                }

                item {
                    BrutalCard {
                        SectionTitle("Exchange rate")
                        Spacer(Modifier.height(4.dp))
                        Text("Used for Wise / card spending in SGD.", color = Muted, fontSize = 13.sp)
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("1 SGD =", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(10.dp))
                            BrutalField(
                                value = rateText,
                                onValueChange = {
                                    rateText = it
                                    vm.setSgdRate(it.toAmount())
                                },
                                prefix = "RM",
                                keyboardType = KeyboardType.Decimal,
                                textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                item {
                    BrutalCard {
                        SectionTitle("Apps sending money notifications")
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Rename how each app shows as an account, or ignore it. Chat apps start ignored.",
                            color = Muted,
                            fontSize = 13.sp,
                        )
                        if (sources.isEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            Text("None yet. They appear here after the first captured notification.", color = Muted, fontSize = 13.sp)
                        }
                    }
                }

                items(sources, key = { it.packageName }) { s ->
                    SourceRow(
                        s = s,
                        onRename = { vm.setSourceAccount(s.packageName, it) },
                        onIgnore = { vm.setSourceIgnored(s.packageName, it) },
                    )
                }

                item {
                    Text(
                        "Everything stays on this phone. Baki has no internet permission and no account.",
                        color = Muted,
                        fontSize = 12.5.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(title: String, sub: String, on: Boolean, onOpen: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                StatusPill(if (on) "On" else "Off", if (on) AccentSoft else RedSoft, if (on) Green else RedText)
            }
            Text(sub, color = Muted, fontSize = 12.5.sp)
        }
        Spacer(Modifier.width(10.dp))
        SmallButton(if (on) "Manage" else "Turn on", onClick = onOpen)
    }
}

@Composable
private fun SourceRow(s: AppSourceEntity, onRename: (String) -> Unit, onIgnore: (Boolean) -> Unit) {
    var name by remember(s.packageName) { mutableStateOf(s.accountName) }
    BrutalCard(contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(s.label, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(s.packageName, color = Muted, fontSize = 11.5.sp)
            }
            Pill(if (s.ignored) "Ignored" else "Tracking", selected = !s.ignored, onClick = { onIgnore(!s.ignored) })
        }
        Spacer(Modifier.height(10.dp))
        CapsLabel("Show as account")
        Spacer(Modifier.height(6.dp))
        BrutalField(
            value = name,
            onValueChange = {
                name = it
                if (it.isNotBlank()) onRename(it.trim())
            },
            placeholder = s.label,
            capitalizeWords = true,
            height = 46.dp,
        )
    }
}
