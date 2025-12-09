package com.vasyerp.freshvegetables.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.activity.DashBoardActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random


class FirebaseMessageService : FirebaseMessagingService() {
    override fun onNewToken(fcmToken: String) {
        Log.d("MessageReceiver", "onNewToken: called.")
        super.onNewToken(fcmToken)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        remoteMessage.data.let {
//            Log.e("MessageReceiver", "onMessageReceived: ${remoteMessage.data["title"]}")
//            Log.e("MessageReceiver", "onMessageReceived: ${remoteMessage.data["body"]}")
//            Log.e("MessageReceiver", "onMessageReceived: ${remoteMessage.data["image"]}")
//            Log.e("MessageReceiver", "onMessageReceived: ${remoteMessage.data["channelId"]}")
//            Log.e("MessageReceiver", "onMessageReceived: ${remoteMessage.data["isLogout"]}")
            val title = remoteMessage.notification?.title
            val body = remoteMessage.notification?.body
            sendNotification(title, body, remoteMessage.data["image"], null, remoteMessage.data["channelId"], DashBoardActivity::class.java, 0)

            if (remoteMessage.data["isLogout"]?.toInt() == 1) {

            }else{
                sendNotification(remoteMessage.data["title"], remoteMessage.data["body"], remoteMessage.data["image"], null, remoteMessage.data["channelId"], DashBoardActivity::class.java, 0)
            }

        }
    }

     private fun sendNotification(title: String?, messageBody: String?, uri: String?, banner: String?, mChannel: String?, className: Class<*>, pendingIntentId: Int) {

        var intent = Intent(this, className)
        intent.putExtra("IS_FROM_NOTIFICATION", true)


        if (pendingIntentId == 7) {
            intent.putExtra("INDEPENDENT_URL", true)
        }

        val resultPendingIntent: PendingIntent? = TaskStackBuilder.create(this).run {
            addNextIntentWithParentStack(intent)
            getPendingIntent(pendingIntentId, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        val channelId = mChannel
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder =
            NotificationCompat.Builder(this, channelId ?: "DEFAULT_CHANNEL").setSmallIcon(R.drawable.truck_new_logo).setContentTitle(title).setContentText(messageBody).setVibrate(longArrayOf(1000, 1000, 1000, 1000)).setAutoCancel(true).setSound(defaultSoundUri)
                .setPriority(Notification.PRIORITY_HIGH).setCategory(Notification.CATEGORY_MESSAGE).setVisibility(
                    NotificationCompat.VISIBILITY_PUBLIC
                ).setFullScreenIntent(resultPendingIntent, true).setContentIntent(resultPendingIntent).setOnlyAlertOnce(false).setWhen(System.currentTimeMillis()).setShowWhen(true)

        if (!banner.isNullOrEmpty()) {
            val imageBitmap = getBitmapFromUrl(banner)
            notificationBuilder.setStyle(NotificationCompat.BigPictureStyle().bigPicture(imageBitmap))
        } else {
            val icon = BitmapFactory.decodeResource(
                getResources(),
             R.drawable.truck_new_logo
            )
            notificationBuilder.setStyle(NotificationCompat.BigPictureStyle().bigPicture(icon))
        }

        val notificationManager = NotificationManagerCompat.from(this)

        val audioAttributes = AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).setUsage(
            AudioAttributes.USAGE_NOTIFICATION_EVENT
        ).build()

        //   Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId ?: "DEFAULT_CHANNEL", channelId ?: "CHANNEL_NAME", NotificationManager.IMPORTANCE_HIGH)
            channel.setSound(alarmSound, audioAttributes)
            notificationManager.createNotificationChannel(channel)
        }
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        notificationManager.notify(Random.nextInt(), notificationBuilder.build())
    }

    private fun getBitmapFromUrl(imageUrl: String?): Bitmap? {
        return try {
            val connection: HttpURLConnection = URL(imageUrl).openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            BitmapFactory.decodeStream(connection.inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

