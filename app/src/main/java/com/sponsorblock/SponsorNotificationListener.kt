package com.sponsorblock

import android.content.Intent
import android.media.session.MediaSession.Token
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification


class SponsorNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn?.packageName == "com.google.android.youtube") {
            val mNotification = sbn.notification
            val extras = mNotification.extras

            if (extras.containsKey("android.mediaSession")) {
                @Suppress("DEPRECATION")
                val token = extras.get("android.mediaSession") as Token

                val serviceIntent = Intent(this, SponsorBlockService::class.java)
                serviceIntent.putExtra("mediaToken", token)
                startForegroundService(serviceIntent)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn?.packageName == "com.google.android.youtube") {
            val serviceIntent = Intent(this, SponsorBlockService::class.java)
            serviceIntent.putExtra("remove", true)
            startForegroundService(serviceIntent)
        }
    }
}
