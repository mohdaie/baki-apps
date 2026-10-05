package com.mohdaie.baki.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mohdaie.baki.ui.sheets.PendingReview
import com.mohdaie.baki.ui.theme.BakiTheme
import com.mohdaie.baki.ui.theme.Ink

/**
 * The popup that appears over other apps (or when you tap Baki's notification):
 * review a captured transaction, then Save or Ignore.
 */
class ReviewActivity : ComponentActivity() {

    private var focusId by mutableLongStateOf(-1L)
    private var openLink by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(AndroidColor.WHITE, AndroidColor.WHITE),
        )
        focusId = intent.getLongExtra(EXTRA_ID, -1L)
        openLink = intent.getBooleanExtra(EXTRA_LINK, false)

        setContent {
            BakiTheme {
                val vm: AppViewModel = viewModel()
                val pendingOrNull by vm.pending.collectAsStateWithLifecycle()
                val accounts by vm.accounts.collectAsStateWithLifecycle()
                val commitments by vm.commitments.collectAsStateWithLifecycle()
                val payments by vm.payments.collectAsStateWithLifecycle()

                val list = pendingOrNull
                if (list != null && list.isEmpty()) {
                    LaunchedEffect(Unit) { finish() }
                }
                val ordered = list.orEmpty().sortedBy { if (it.id == focusId) 0 else 1 }
                val p = ordered.firstOrNull()

                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color(0x8015171A))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { finish() }
                        .statusBarsPadding(),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    if (p != null) {
                        val shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(shape)
                                .background(Color.White)
                                .border(2.5.dp, Ink, shape)
                                // Taps inside the sheet must not close it.
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                                .verticalScroll(rememberScrollState())
                                .navigationBarsPadding()
                                .imePadding()
                                .padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 22.dp),
                        ) {
                            Box(
                                Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(bottom = 12.dp)
                                    .size(width = 46.dp, height = 5.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Ink),
                            )
                            PendingReview(
                                p = p,
                                index = 1,
                                total = ordered.size,
                                accounts = accounts,
                                unpaidCommitments = unpaidCommitments(commitments, payments),
                                onSave = { vm.confirmPending(p.id, it) },
                                onIgnore = { vm.ignorePending(p.id) },
                                onLinkCommitment = { vm.markPendingAsCommitment(p.id, it) },
                                startLinking = openLink && p.id == focusId,
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        focusId = intent.getLongExtra(EXTRA_ID, -1L)
        openLink = intent.getBooleanExtra(EXTRA_LINK, false)
    }

    companion object {
        const val EXTRA_ID = "pending_id"
        const val EXTRA_LINK = "open_link_commitment"

        fun newIntent(context: Context, pendingId: Long, linkCommitment: Boolean = false): Intent =
            Intent(context, ReviewActivity::class.java)
                .putExtra(EXTRA_ID, pendingId)
                .putExtra(EXTRA_LINK, linkCommitment)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
