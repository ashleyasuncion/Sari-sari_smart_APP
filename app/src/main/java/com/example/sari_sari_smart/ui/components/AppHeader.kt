package com.example.sari_sari_smart.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sari_sari_smart.ui.theme.Green600
import com.example.sari_sari_smart.ui.theme.SariSariSmartTheme

/**
 * App Header composable — provides the green TopAppBar with greeting/title and action buttons.
 *
 * Two variants:
 * 1. Moment header (Morning, Day, Closing): Shows owner greeting + tutorial (?) + manage store (grid)
 * 2. Support header (Inventory, Debts): Shows back arrow + page title + tutorial (?) + settings (⚙)
 */

// ── Moment Header (for Morning, Day, Closing screens) ─────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MomentAppHeader(
    greeting: String,
    onTutorialClick: (() -> Unit)? = null,
    onInventoryClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Text(
                text = greeting,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        },
        actions = {
            if (onTutorialClick != null) {
                IconButton(onClick = onTutorialClick) {
                    Text(
                        text = "?",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            if (onInventoryClick != null) {
                IconButton(onClick = onInventoryClick) {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = "Inventory",
                        tint = Color.White
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Green600,
            titleContentColor = Color.White,
            actionIconContentColor = Color.White
        ),
        modifier = modifier
    )
}

@Preview(showBackground = true, name = "Moment App Header")
@Composable
fun MomentAppHeaderPreview() {
    SariSariSmartTheme {
        MomentAppHeader(
            greeting = "Good day! Aling Maria 👋",
            onTutorialClick = {},
            onInventoryClick = {}
        )
    }
}

// ── Support Header (for Inventory, Debts screens) ────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportAppHeader(
    title: String,
    onBackClick: (() -> Unit)? = null,
    onTutorialClick: (() -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        },
        navigationIcon = {
            if (onBackClick != null) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }
        },
        actions = {
            if (onTutorialClick != null) {
                IconButton(onClick = onTutorialClick) {
                    Text(
                        text = "?",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            if (onSettingsClick != null) {
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Green600,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White
        ),
        modifier = modifier
    )
}

@Preview(showBackground = true, name = "Support App Header")
@Composable
fun SupportAppHeaderPreview() {
    SariSariSmartTheme {
        SupportAppHeader(
            title = "Inventory",
            onBackClick = {},
            onTutorialClick = {},
            onSettingsClick = {}
        )
    }
}
