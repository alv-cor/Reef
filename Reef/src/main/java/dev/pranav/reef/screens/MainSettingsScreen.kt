package dev.pranav.reef.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import dev.pranav.reef.AboutActivity
import dev.pranav.reef.R
import dev.pranav.reef.util.append
import dev.pranav.reef.util.prefs

@Composable
fun MainSettingsContent(
    contentPadding: PaddingValues = PaddingValues(),
    onNavigate: (SettingsScreenRoute) -> Unit
) {
    val context = LocalContext.current
    var enableDND by remember { mutableStateOf(prefs.getBoolean("enable_dnd", false)) }

    val isPerAppLanguageSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val topGroupSize = if (isPerAppLanguageSupported) 2 else 1

    val menuItems = listOf(
        SettingsMenuItem(
            icon = Icons.Rounded.Timer,
            title = stringResource(R.string.pomodoro),
            subtitle = stringResource(R.string.pomodoro_subtitle),
            destination = SettingsScreenRoute.Pomodoro
        ),
        SettingsMenuItem(
            icon = Icons.Rounded.HourglassEmpty,
            title = stringResource(R.string.mindful_launch),
            subtitle = stringResource(R.string.mindful_launch_subtitle),
            destination = SettingsScreenRoute.MindfulLaunch
        ),
        SettingsMenuItem(
            icon = Icons.Rounded.Notifications,
            title = stringResource(R.string.notifications),
            subtitle = stringResource(R.string.notifications_subtitle),
            destination = SettingsScreenRoute.Notifications
        ),
        SettingsMenuItem(
            icon = Icons.Rounded.Info,
            title = stringResource(R.string.about),
            subtitle = stringResource(R.string.about_subtitle),
            destination = SettingsScreenRoute.Main
        )
    )

    LazyColumn(
        contentPadding = contentPadding.append(horizontal = 16.dp)
    ) {
        item {
            SettingsCard(index = 0, listSize = topGroupSize) {
                ListItem(
                    modifier = Modifier
                        .clickable {
                            enableDND = !enableDND
                            prefs.edit { putBoolean("enable_dnd", enableDND) }
                        }
                        .padding(4.dp),
                    headlineContent = {
                        Text(
                            text = stringResource(R.string.enable_dnd),
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    supportingContent = {
                        Text(
                            text = stringResource(R.string.dnd_description),
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = enableDND,
                            onCheckedChange = {
                                enableDND = it
                                prefs.edit { putBoolean("enable_dnd", it) }
                            }
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }

        if (isPerAppLanguageSupported) {
            item {
                SettingsCard(index = 1, listSize = topGroupSize) {
                    ListItem(
                        modifier = Modifier
                            .clickable {
                                val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            }
                            .padding(4.dp),
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Rounded.Language,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        },
                        headlineContent = {
                            Text(
                                text = stringResource(R.string.app_language),
                                style = MaterialTheme.typography.titleMedium
                            )
                        },
                        supportingContent = {
                            Text(
                                text = stringResource(R.string.app_language_description),
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }

        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp))
        }

        itemsIndexed(
            items = menuItems,
            key = { _, item -> item.title }
        ) { index, item ->
            SettingsMenuItemRow(
                item = item,
                index = index,
                listSize = menuItems.size,
                onClick = {
                    when (item.destination) {
                        SettingsScreenRoute.Pomodoro -> onNavigate(SettingsScreenRoute.Pomodoro)
                        SettingsScreenRoute.MindfulLaunch -> onNavigate(SettingsScreenRoute.MindfulLaunch)
                        SettingsScreenRoute.Notifications -> onNavigate(SettingsScreenRoute.Notifications)
                        SettingsScreenRoute.Main -> context.startActivity(
                            Intent(context, AboutActivity::class.java)
                        )

                        else -> { /* No-op */
                        }
                    }
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            DonateButton()
        }
    }
}


@Composable
fun SettingsCard(
    index: Int,
    listSize: Int,
    content: @Composable () -> Unit
) {
    val shape = when {
        listSize == 1 -> RoundedCornerShape(24.dp)
        index == 0 -> RoundedCornerShape(
            topStart = 24.dp,
            topEnd = 24.dp,
            bottomStart = 6.dp,
            bottomEnd = 6.dp
        )

        index == listSize - 1 -> RoundedCornerShape(
            topStart = 6.dp,
            topEnd = 6.dp,
            bottomStart = 24.dp,
            bottomEnd = 24.dp
        )

        else -> RoundedCornerShape(6.dp)
    }

    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + scaleIn(
            initialScale = 0.95f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        ),
        exit = fadeOut() + shrinkVertically()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 1.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = shape
        ) {
            content()
        }
    }
}
