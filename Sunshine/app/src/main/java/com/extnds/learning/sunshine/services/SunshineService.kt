package com.extnds.learning.sunshine.services

import android.app.IntentService
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.support.v4.app.TaskStackBuilder
import android.support.v7.app.NotificationCompat
import android.util.Log
import com.extnds.learning.sunshine.R
import com.extnds.learning.sunshine.models.retrofit.Forcast
import com.extnds.learning.sunshine.ui.MainActivity
import com.extnds.learning.sunshine.utils.api.apiInterface
import com.extnds.learning.sunshine.utils.database.DatabaseUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

class SunshineService() : IntentService(SERVICE_NAME) {

    companion object {
        const val TAG = "SunshineService"
        const val LOG_TAG = MainActivity.LOG_TAG_BASE + TAG
        const val SERVICE_NAME = "Sunshine"

        const val CITY_NAME_KEY = "cityName"
        const val UNITS_KEY = "units"
    }

    override fun onHandleIntent(p0: Intent?) {
        val cityName = p0!!.getStringExtra(CITY_NAME_KEY)
        val units = p0.getStringExtra(UNITS_KEY)
        apiInterface?.forcastQuery(cityName, units)?.enqueue(object : Callback<Forcast> {
            override fun onResponse(call: Call<Forcast>, response: Response<Forcast>) {

                if (response.isSuccessful) {
                    DatabaseUtils.ForcastDatabase(this@SunshineService).addForcasts(response.body(), object : DatabaseUtils.ForcastDatabase.ForcastDatabaseCallback {
                        override fun onDatabaseProperlySaved(dbForcasts: MutableList<com.extnds.learning.sunshine.models.sugarORM.Forcast>?) {

                            val mBuilder = NotificationCompat.Builder(this@SunshineService)
                                    .setSmallIcon(R.mipmap.ic_launcher)
                                    .setContentTitle("Sunshine")
                                    .setContentText("Weather Data Updated")

                            val resultIntent = Intent(this@SunshineService, MainActivity::class.java)

                            val stackBuilder = TaskStackBuilder.create(this@SunshineService)
                            stackBuilder.addParentStack(MainActivity::class.java)
                            stackBuilder.addNextIntent(resultIntent)
                            val resultPendingIntent = stackBuilder.getPendingIntent(
                                    0,
                                    PendingIntent.FLAG_UPDATE_CURRENT)
                            mBuilder.setContentIntent(resultPendingIntent)
                            val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            val mNotificationId = 1
                            mNotificationManager.notify(mNotificationId, mBuilder.build())

                            val calendar = Calendar.getInstance()
                            Log.d(LOG_TAG, "Data Received, Stored And Notification Generated by Background Service At [${calendar.get(Calendar.HOUR_OF_DAY)}:${calendar.get(Calendar.MINUTE)}:${calendar.get(Calendar.SECOND)}]")
                        }
                    })
                }
            }

            override fun onFailure(call: Call<Forcast>, t: Throwable) {
            }
        })
    }

    class AlarmReceiver() : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            val intent = Intent(p0, SunshineService::class.java)
            intent.putExtra(SunshineService.CITY_NAME_KEY, p1!!.getStringExtra(CITY_NAME_KEY))
            intent.putExtra(SunshineService.UNITS_KEY, p1.getStringExtra(UNITS_KEY))
            p0?.startService(intent)
            Log.d(LOG_TAG, "Alarm Service Initiated")
        }

    }


}