package iiotca.frontdoorassistant

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import iiotca.frontdoorassistant.ui.authenticate.AuthenticateActivity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class NotificationService : FirebaseMessagingService() {
    companion object {
        private var count = 0
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val name = message.data["name"]!!
        val timeStamp = message.data["timeStamp"]!!.toLong()
        val dateTime = DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochSecond(timeStamp))

        val pendingIntent = PendingIntent.getActivity(
            this,
            (System.currentTimeMillis() / 1000).toInt(),
            Intent(this, AuthenticateActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder: NotificationCompat.Builder =
            NotificationCompat.Builder(this, getString(R.string.channel_id))
                .setContentTitle("$name spotted at your front door!")
                .setContentText("$name was spotted at your front door at $dateTime!")
                .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.bad_person))
                .setSmallIcon(R.drawable.bad_person).setPriority(NotificationCompat.PRIORITY_MAX)
                .setStyle(NotificationCompat.BigTextStyle()).setSound(
                    RingtoneManager.getDefaultUri(
                        RingtoneManager.TYPE_NOTIFICATION
                    )
                )
                .setContentIntent(pendingIntent)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.notify(count, notificationBuilder.build())

        count++
    }
}
