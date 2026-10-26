package com.aistudio.docscanpro.zqwk.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.docscanpro.zqwk.billing.SubscriptionManager
import com.aistudio.docscanpro.zqwk.ui.theme.AccentCyan
import com.aistudio.docscanpro.zqwk.ui.theme.AccentEmerald
import com.aistudio.docscanpro.zqwk.ui.theme.AccentTeal
import com.aistudio.docscanpro.zqwk.ui.theme.PrimaryBlue
import com.aistudio.docscanpro.zqwk.ui.theme.Slate800
import com.aistudio.docscanpro.zqwk.ui.theme.Slate900
import com.aistudio.docscanpro.zqwk.viewmodel.AppScreen
import com.aistudio.docscanpro.zqwk.viewmodel.ScannerViewModel

@Composable
fun AppDrawerContent(
    viewModel: ScannerViewModel,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentScreen by viewModel.screen.collectAsState()
    val trialState by viewModel.trialState.collectAsState()

    ModalDrawerSheet(
        modifier = modifier.width(320.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 16.dp, horizontal = 12.dp)
        ) {
            // User & App Profile Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PrimaryBlue,
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "DocScan Pro",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = SubscriptionManager.PAYPAL_EMAIL,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = PrimaryBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Navigation Items
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Description, contentDescription = null) },
                label = { Text("Documents & Scans", fontWeight = FontWeight.SemiBold) },
                selected = currentScreen == AppScreen.DASHBOARD,
                onClick = {
                    viewModel.navigateTo(AppScreen.DASHBOARD)
                    onCloseDrawer()
                },
                modifier = Modifier
                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                    .testTag("drawer_nav_documents")
            )

            NavigationDrawerItem(
                icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = AccentEmerald) },
                label = { Text("Serial Scanner & Excel", fontWeight = FontWeight.SemiBold) },
                selected = currentScreen == AppScreen.SERIAL_SCANNER || currentScreen == AppScreen.SERIAL_EXCEL_VIEWER,
                onClick = {
                    viewModel.startNewSerialScan()
                    onCloseDrawer()
                },
                modifier = Modifier
                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                    .testTag("drawer_nav_serials")
            )

            NavigationDrawerItem(
                icon = {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = if (trialState.isProUnlocked) AccentEmerald else PrimaryBlue
                    )
                },
                label = { Text("Subscription & Plans", fontWeight = FontWeight.SemiBold) },
                selected = currentScreen == AppScreen.SUBSCRIPTION,
                onClick = {
                    viewModel.navigateTo(AppScreen.SUBSCRIPTION)
                    onCloseDrawer()
                },
                modifier = Modifier
                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                    .testTag("drawer_nav_subscription")
            )

            Spacer(modifier = Modifier.weight(1f))

            // Dedicated Subscription Side Panel Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp)
                    .testTag("side_panel_subscription_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (trialState.isProUnlocked) AccentEmerald else AccentCyan
                        ) {
                            Text(
                                text = if (trialState.isProUnlocked) "PRO ACTIVE" else "15-DAY TRIAL",
                                color = Slate900,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (!trialState.isProUnlocked) {
                            Text(
                                text = "${trialState.daysRemaining}d left",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (trialState.isProUnlocked) "Unlimited Pro VIP Access" else "15-Day Free Access",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )

                    val daysPassed = (15 - trialState.daysRemaining).coerceIn(0, 15)
                    val progress = (daysPassed / 15f).coerceIn(0f, 1f)

                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { if (trialState.isProUnlocked) 1f else progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (trialState.isProUnlocked) AccentEmerald else AccentCyan,
                        trackColor = Color(0xFF334155)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "PayPal: ${SubscriptionManager.PAYPAL_EMAIL}",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            viewModel.navigateTo(AppScreen.SUBSCRIPTION)
                            onCloseDrawer()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .testTag("drawer_upgrade_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (trialState.isProUnlocked) AccentEmerald else PrimaryBlue
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payment,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (trialState.isProUnlocked) "View Pro Details" else "Upgrade / Plans",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
