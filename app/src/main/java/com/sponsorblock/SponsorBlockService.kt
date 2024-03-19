package com.sponsorblock

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession.Token
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import fuel.Fuel
import fuel.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject


class SponsorBlockService : Service() {
    private val notificationId = 1001
    private val channelId = "SPONSOR_BLOCK_CHANNEL"

    private var controller: MediaController? = null
    private var metadata: Triple<String, String, String?>? = null
    private var sbMetadata: ArrayList<JSONObject>? = null

    private var thread: Thread? = null

    private var lastNotificationMessage = ""

    private lateinit var preferencesManager: PreferencesManager

    override fun onCreate() {
        super.onCreate()
        preferencesManager = PreferencesManager(this)
        metadata = preferencesManager.restoreYTMetadata()
        sbMetadata = preferencesManager.restoreSBMetadata()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (metadata != null) {
            preferencesManager.saveYTMetadata(metadata!!)
        }
        if (sbMetadata != null) {
            preferencesManager.saveSBMetadata(sbMetadata!!)
        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    @Suppress("DEPRECATION")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.extras?.containsKey("remove") == true) {
            updateNotification("Stopping...")
            removeNotification(startId = startId)
        }
        if (intent?.extras?.containsKey("mediaToken") == true) {
            val token = intent.extras?.get("mediaToken") as Token
            if (controller?.sessionToken != token) {
                controller = MediaController(this, token)
            }
            val channel = controller!!.metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            val title = controller!!.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            if (channel?.isNotBlank() != false && title?.isNotBlank() != false) {
                if (metadata?.first != title || metadata?.second != channel) {
                    metadata = Triple<String, String, String?>(title!!, channel!!, null)
                    sbMetadata = null
                }
                if (thread?.isAlive != true) {
                    thread = Thread {
                        while (controller!!.playbackState?.state == PlaybackState.STATE_PLAYING) {
                            if (metadata == null || metadata?.first != title || metadata?.second != channel || metadata?.third == null) {
                                if (metadata?.third == "not_found") {
                                    this@SponsorBlockService.updateNotification("Can't found video ID")
                                } else {
                                    this@SponsorBlockService.updateNotification("Loading youtube data")
                                    runBlocking {
                                        this@SponsorBlockService.updateYTMetadata()
                                    }
                                }
                            }
                            if (sbMetadata == null) {
                                this@SponsorBlockService.updateNotification("Loading SponsorBlock data")
                                runBlocking {
                                    this@SponsorBlockService.updateSBMetadata()
                                }
                            }
                            if (sbMetadata != null) {
                                val sbMeta = sbMetadata!!
                                val categoriesToSkip = mutableListOf<String>()
                                if (preferencesManager.getData(
                                        PreferencesKeys.SKIP_SPONSOR, true
                                    )
                                ) {
                                    categoriesToSkip.add("sponsor")
                                }
                                if (preferencesManager.getData(
                                        PreferencesKeys.SKIP_SELF_PROMOTION, true
                                    )
                                ) {
                                    categoriesToSkip.add("selfpromo")
                                }
                                if (preferencesManager.getData(
                                        PreferencesKeys.SKIP_INTERACTION, true
                                    )
                                ) {
                                    categoriesToSkip.add("interaction")
                                }
                                if (preferencesManager.getData(PreferencesKeys.SKIP_INTRO)) {
                                    categoriesToSkip.add("intro")
                                }
                                if (preferencesManager.getData(PreferencesKeys.SKIP_OUTRO)) {
                                    categoriesToSkip.add("outro")
                                }
                                if (preferencesManager.getData(PreferencesKeys.SKIP_PREVIEW)) {
                                    categoriesToSkip.add("preview")
                                }
                                if (preferencesManager.getData(PreferencesKeys.SKIP_MUSIC_OFFTOPIC)) {
                                    categoriesToSkip.add("music_offtopic")
                                }
                                val skippedSegments = sbMeta.filter {
                                    it.getString("actionType") != "full" && it.getString("category") in categoriesToSkip
                                }

                                if (skippedSegments.isEmpty()) {
                                    this@SponsorBlockService.updateNotification("Nothing to skip")
                                } else {
                                    val position = controller?.playbackState?.position
                                    if (position != null) {
                                        var skipped = 0

                                        skippedSegments.forEach {
                                            val segments = it.getJSONArray("segment")
                                            val start = segments.getDouble(0) * 1000
                                            val finish = segments.getDouble(1) * 1000
                                            if (position > finish) {
                                                skipped++
                                            }
                                            if (position > start && position < finish) {
                                                controller?.transportControls?.seekTo(finish.toLong())
                                                if (preferencesManager.getData(
                                                        PreferencesKeys.SHOW_SKIPPED_TOAST, true
                                                    )
                                                ) {
                                                    val whatSkipped =
                                                        when (it.getString("category")) {
                                                            "sponsor" -> "Sponsor"
                                                            "selfpromo" -> "Unpaid/Self Promotion"
                                                            "interaction" -> "Interaction Reminder (Subscribe)"
                                                            "intro" -> "Intermission/Intro Animation"
                                                            "outro" -> "Endcards/Credits"
                                                            "preview" -> "Preview/Recap/Hook"
                                                            "music_offtopic" -> "Music: Non-Music Section"
                                                            else -> ""

                                                        }
                                                    postToastMessage("SponsorBlock: Skipped $whatSkipped!")
                                                }
                                                CoroutineScope(Dispatchers.Default).launch {
                                                    this@SponsorBlockService.sendSkipNotification(
                                                        it.getString("UUID")
                                                    )
                                                }
                                            }
                                        }

                                        this@SponsorBlockService.updateNotification("$skipped/${skippedSegments.size} skipped")
                                    }
                                }
                            }
                            try {
                                Thread.sleep(500)
                            } catch (e: InterruptedException) {
                                e.printStackTrace()
                            }
                        }
                        updateNotification("Stopping...")
                        removeNotification(startId = startId)
                    }
                    thread!!.start()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun postToastMessage(message: String?) {
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun updateNotification(add: String?) {
        if (add == null) return
        val message = "$add\n${metadata!!.first}\n${metadata!!.second}"
        if (message == lastNotificationMessage) return
        lastNotificationMessage = message
        val notificationChannel = NotificationChannel(
            channelId, channelId, NotificationManager.IMPORTANCE_HIGH
        )
        NotificationManagerCompat.from(this).createNotificationChannel(
            notificationChannel
        )
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentText(message)
            .setContentTitle("SponsorBlock")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setOngoing(true)
            .setSilent(true)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(notificationId, notification)
        }
    }

    private fun removeNotification(startId: Int) {
        stopSelf()
        stopSelfResult(startId)
    }

    private var ytRequest = false
    private suspend fun updateYTMetadata() {
        if (controller == null || ytRequest) return
        ytRequest = true
        try {
            val channel = controller?.metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            val title = controller?.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            if (channel == null || title == null) return
            var metas: List<Triple<String, String, String>> =
                if (preferencesManager.getData(PreferencesKeys.USE_YT_API)) {
                    ytRequestApi("$title $channel")
                } else {
                    ytRequest("$title $channel")
                }

            for (meta in metas) {
                if (meta.first == title && meta.second == channel) {
                    metadata = meta
                    break
                }
            }
            if (metadata?.third == null) {
                metas = if (preferencesManager.getData(PreferencesKeys.USE_YT_API)) {
                    ytRequestApi("$title")
                } else {
                    ytRequest("$title")
                }

                for (meta in metas) {
                    if (meta.first == title) {
                        metadata = Triple(meta.first, channel, meta.third)
                        break
                    }
                }
                if (metadata?.third == null) {
                    metadata = Triple(title, channel, "not_found")
                }
            }
        } finally {
            ytRequest = false
        }
    }

    private suspend fun ytRequestApi(query: String): List<Triple<String, String, String>> {
        val response = Fuel.get(
            "https://www.googleapis.com/youtube/v3/search", listOf(
                "q" to query,
                "key" to preferencesManager.getData(PreferencesKeys.YT_API_KEY, ""),
                "part" to "snippet"
            )
        ).body
        val aMetas = JSONObject(response).getJSONArray("items").toArrayList()
            .filter { it.getJSONObject("id").has("videoId") }.map {
                Triple(
                    it.getJSONObject("snippet").getString("title"),
                    it.getJSONObject("snippet").getString("channelTitle"),
                    it.getJSONObject("id").getString("videoId")
                )
            }
        return aMetas
    }

    private suspend fun ytRequest(query: String): List<Triple<String, String, String>> {
        val response = Fuel.get(
            "https://www.youtube.com/results", listOf(
                "search_query" to query,
            )
        ).body
        val data = response.split("<script").firstOrNull { it.contains("ytInitialData") }
            ?.split("</script>")?.firstOrNull()
        if (data != null) {
            val jData = JSONObject(
                data.substring(
                    data.indexOf("{") - 1, data.lastIndexOf(";")
                )
            )
            val oj = jData.getJSONObject("contents").getJSONObject("twoColumnSearchResultsRenderer")
                .getJSONObject("primaryContents").getJSONObject("sectionListRenderer")
                .getJSONArray("contents").getJSONObject(0).getJSONObject("itemSectionRenderer")
                .getJSONArray("contents")

            val fMetas = oj.toArrayList().filter { it.has("videoRenderer") }.map {
                val ren = it.getJSONObject("videoRenderer")
                Triple(
                    ren.getJSONObject("title").getJSONArray("runs").getJSONObject(0)
                        .getString("text"),
                    ren.getJSONObject("ownerText").getJSONArray("runs").getJSONObject(0)
                        .getString("text"),
                    ren.getString("videoId")
                )
            }
            return fMetas
        }
        return listOf()
    }

    private var sbRequest = false
    private suspend fun updateSBMetadata() {
        if (metadata?.third == null || sbRequest) return
        sbRequest = true
        try {
            val sbResponse = Fuel.get(
                "https://sponsor.ajay.app/api/skipSegments", listOf(
                    "videoID" to metadata!!.third!!,
                    "categories" to "[\"sponsor\",\"selfpromo\",\"interaction\",\"intro\",\"outro\",\"preview\",\"music_offtopic\",\"exclusive_access\"]",
                    "actionTypes" to "[\"skip\",\"full\"]"
                )
            )
            if (sbResponse.statusCode == 404) {
                sbMetadata = arrayListOf()
            } else if (sbResponse.statusCode == 200) {
                sbMetadata = JSONArray(sbResponse.body).toArrayList()
                sbMetadata?.filter { it.getString("actionType") == "full" }?.forEach {
                    when (it.getString("category")) {
                        "sponsor" -> if (preferencesManager.getData(
                                PreferencesKeys.SHOW_SPONSOR_TOAST, true
                            )
                        ) {
                            postToastMessage("SponsorBlock: Full video Sponsor!")
                        }

                        "selfpromo" -> if (preferencesManager.getData(
                                PreferencesKeys.SHOW_SELF_PROMOTION_TOAST, true
                            )
                        ) {
                            postToastMessage("SponsorBlock: Full video Unpaid/Self Promotion!")
                        }

                        "exclusive_access" -> if (preferencesManager.getData(
                                PreferencesKeys.SHOW_EXCLUSIVE_ACCESS_TOAST, true
                            )
                        ) {
                            postToastMessage("SponsorBlock: Full video Exclusive Access!")
                        }
                    }
                }
            }
        } finally {
            sbRequest = false
        }
    }

    private var sbSkipRequest = false
    private suspend fun sendSkipNotification(uuid: String) {
        if (sbSkipRequest) return
        sbSkipRequest = true
        try {
            Fuel.get(
                "https://sponsor.ajay.app/api/viewedVideoSponsorTime", listOf(
                    "UUID" to uuid,
                )
            )
        } finally {
            sbSkipRequest = false
        }
    }

    private fun JSONArray.toArrayList(): ArrayList<JSONObject> {
        val list = arrayListOf<JSONObject>()
        for (i in 0 until this.length()) {
            list.add(this.getJSONObject(i))
        }

        return list
    }
}

