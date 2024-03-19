package com.sponsorblock

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.alorma.compose.settings.storage.preferences.rememberPreferenceBooleanSettingState
import com.alorma.compose.settings.ui.SettingsCheckbox
import com.alorma.compose.settings.ui.SettingsMenuLink
import com.sponsorblock.ui.theme.SponsorBlockTheme

class MainActivity : ComponentActivity() {

    private var hasNotificationsAccess: Boolean = false
    private var postNotificationPermission: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hasNotificationsAccess =
            NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)

        postNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        setContent {
            SponsorBlockTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    Settings(
                        hasNotificationsAccess = hasNotificationsAccess,
                        hasPostNotificationsPermission = postNotificationPermission,
                        onClick = ::openListenPermissionSettings,
                        onClick2 = ::requestPostNotificationPermission
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hasNotificationsAccess =
            NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)

        postNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        setContent {
            SponsorBlockTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    Settings(
                        hasNotificationsAccess = hasNotificationsAccess,
                        hasPostNotificationsPermission = postNotificationPermission,
                        onClick = ::openListenPermissionSettings,
                        onClick2 = ::requestPostNotificationPermission
                    )
                }
            }
        }
    }

    private fun openListenPermissionSettings() {
        val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
        startActivity(intent)
    }

    private fun requestPostNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            postNotificationPermission = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            val permissionState =
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)

            if (permissionState == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Settings(
    hasNotificationsAccess: Boolean,
    hasPostNotificationsPermission: Boolean,
    onClick: () -> Unit,
    onClick2: () -> Unit,
) {
    val context = LocalContext.current
    val preferencesManager = PreferencesManager(context)
    val scrollState = rememberScrollState()
    val useYTApiState =
        rememberPreferenceBooleanSettingState(
            key = PreferencesKeys.USE_YT_API,
            defaultValue = false
        )
    var apiKeyState by remember {
        mutableStateOf(
            TextFieldValue(
                preferencesManager.getData(
                    PreferencesKeys.YT_API_KEY,
                    ""
                )
            )
        )
    }


    val skippedToastState =
        rememberPreferenceBooleanSettingState(
            key = PreferencesKeys.SHOW_SKIPPED_TOAST,
            defaultValue = true
        )
    val sponsorToastState =
        rememberPreferenceBooleanSettingState(
            key = PreferencesKeys.SHOW_SPONSOR_TOAST,
            defaultValue = true
        )
    val selfPromotionToastState =
        rememberPreferenceBooleanSettingState(
            key = PreferencesKeys.SHOW_SELF_PROMOTION_TOAST,
            defaultValue = true
        )
    val exclusiveToastState =
        rememberPreferenceBooleanSettingState(
            key = PreferencesKeys.SHOW_EXCLUSIVE_ACCESS_TOAST,
            defaultValue = true
        )


    val sponsorState =
        rememberPreferenceBooleanSettingState(
            key = PreferencesKeys.SKIP_SPONSOR,
            defaultValue = true
        )
    val selfPromotionState =
        rememberPreferenceBooleanSettingState(
            key = PreferencesKeys.SKIP_SELF_PROMOTION,
            defaultValue = true
        )
    val interactionReminderState = rememberPreferenceBooleanSettingState(
        key = PreferencesKeys.SKIP_INTERACTION,
        defaultValue = true
    )
    val introState =
        rememberPreferenceBooleanSettingState(
            key = PreferencesKeys.SKIP_INTRO,
            defaultValue = false
        )
    val outroState =
        rememberPreferenceBooleanSettingState(
            key = PreferencesKeys.SKIP_OUTRO,
            defaultValue = false
        )
    val previewState =
        rememberPreferenceBooleanSettingState(
            key = PreferencesKeys.SKIP_PREVIEW,
            defaultValue = false
        )
    val nonMusicSectionState =
        rememberPreferenceBooleanSettingState(
            key = PreferencesKeys.SKIP_MUSIC_OFFTOPIC,
            defaultValue = false
        )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                Modifier.background(MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .consumeWindowInsets(padding)
                .verticalScroll(scrollState)
                .padding(top = padding.calculateTopPadding()),
        ) {
            SettingsMenuLink(
                title = { Text(text = "Access to notifications") },
                subtitle = { Text(text = if (hasNotificationsAccess) "Granted" else "Not granted (Tap to fix)") },
                onClick = onClick,
            )
            SettingsMenuLink(
                title = { Text(text = "Access to send notification") },
                subtitle = { Text(text = if (hasPostNotificationsPermission) "Granted" else "Not granted (Tap to fix)") },
                onClick = onClick2,
            )
            SettingsCheckbox(
                title = { Text(text = "Use YouTube API") },
                subtitle = {
                    val annotatedString = buildAnnotatedString {
                        append("Required YouTube API KEY. ")

                        pushStringAnnotation(
                            tag = "link",
                            annotation = "https://developers.google.com/youtube/v3/getting-started"
                        )
                        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                            append("Learn more")
                        }
                        pop()
                    }
                    ClickableText(text = annotatedString,
                        style = TextStyle(color = ListItemDefaults.colors().supportingTextColor),
                        onClick = { offset ->
                            annotatedString.getStringAnnotations(
                                tag = "link",
                                start = offset,
                                end = offset
                            ).firstOrNull()?.let {
                                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(it.item))
                                context.startActivity(webIntent)
                            } ?: run {

                                useYTApiState.value = !useYTApiState.value
                            }
                        }
                    )
                },
                state = useYTApiState.value,
                onCheckedChange = { useYTApiState.value = it },
            )
            if (useYTApiState.value)
                TextField(
                    value = apiKeyState,
                    onValueChange = {
                        apiKeyState = it
                        preferencesManager.saveData(PreferencesKeys.YT_API_KEY, it.text)
                    },
                    label = { Text(text = "YouTube API KEY") },
                    placeholder = { Text(text = "YouTube API KEY") },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                )

            Text(
                text = "Toasts",
                fontSize = 20.sp,
                modifier = Modifier
                    .padding(25.dp)
            )
            SettingsCheckbox(
                title = { Text(text = "Skipped toast") },
                subtitle = { Text(text = "Show toast when skipped") },
                state = skippedToastState.value,
                onCheckedChange = { skippedToastState.value = it },
            )
            SettingsCheckbox(
                title = { Text(text = "Sponsor") },
                subtitle = { Text(text = "Show toast when the creator has been paid in money to advertise something shown in the video that cannot be completely removed by cuts or the topic of the video was decided by a paying party.") },
                state = sponsorToastState.value,
                onCheckedChange = { sponsorToastState.value = it },
            )
            SettingsCheckbox(
                title = { Text(text = "Unpaid/Self Promotion") },
                subtitle = { Text(text = "Show toast when a video Unpaid/Self Promotion") },
                state = selfPromotionToastState.value,
                onCheckedChange = { selfPromotionToastState.value = it },
            )
            SettingsCheckbox(
                title = { Text(text = "Exclusive Access") },
                subtitle = { Text(text = "Show toast when a video showcases a product, service or location that they've received free or subsidized access to.") },
                state = exclusiveToastState.value,
                onCheckedChange = { exclusiveToastState.value = it },
            )


            Text(
                text = "Categories to skip",
                fontSize = 20.sp,
                modifier = Modifier
                    .padding(25.dp)
            )
            SettingsCheckbox(
                title = { Text(text = "Sponsor") },
                subtitle = { Text(text = "Paid promotion, paid referrals and direct advertisements. Not for self-promotion or free shoutouts to causes/creators/websites/products they like.") },
                state = sponsorState.value,
                onCheckedChange = { sponsorState.value = it },
            )
            SettingsCheckbox(
                title = { Text(text = "Unpaid/Self Promotion") },
                subtitle = { Text(text = "Similar to \"sponsor\" except for unpaid or self promotion. This includes sections about merchandise, donations, or information about who they collaborated with.") },
                state = selfPromotionState.value,
                onCheckedChange = { selfPromotionState.value = it },
            )
            SettingsCheckbox(
                title = { Text(text = "Interaction Reminder (Subscribe)") },
                subtitle = { Text(text = "When there is a short reminder to like, subscribe or follow them in the middle of content. If it is long or about something specific, it should be under self promotion instead.") },
                state = interactionReminderState.value,
                onCheckedChange = { interactionReminderState.value = it },
            )
            SettingsCheckbox(
                title = { Text(text = "Intermission/Intro Animation") },
                subtitle = { Text(text = "An interval without actual content. Could be a pause, static frame, repeating animation. This should not be used for transitions containing information.") },
                state = introState.value,
                onCheckedChange = { introState.value = it },
            )
            SettingsCheckbox(
                title = { Text(text = "Endcards/Credits") },
                subtitle = { Text(text = "Credits or when the YouTube endcards appear. Not for conclusions with information.") },
                state = outroState.value,
                onCheckedChange = { outroState.value = it },
            )
            SettingsCheckbox(
                title = { Text(text = "Preview/Recap/Hook") },
                subtitle = { Text(text = "Collection of clips that show what is coming up in in this video or other videos in a series where all information is repeated later in the video.") },
                state = previewState.value,
                onCheckedChange = { previewState.value = it },
            )
            SettingsCheckbox(
                title = { Text(text = "Music: Non-Music Section") },
                subtitle = { Text(text = "Only for use in music videos. This only should be used for sections of music videos that aren't already covered by another category.") },
                state = nonMusicSectionState.value,
                onCheckedChange = { nonMusicSectionState.value = it },
            )
        }
    }
}
