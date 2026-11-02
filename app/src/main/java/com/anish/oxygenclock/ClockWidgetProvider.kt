package com.anish.oxygenclock
import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.widget.RemoteViews
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

class ClockWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(c: Context, m: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateOne(c, m, it) }; schedule(c)
    }
    override fun onReceive(c: Context, i: Intent) {
        super.onReceive(c, i)
        if (i.action == ACTION_REFRESH) {
            val m = AppWidgetManager.getInstance(c)
            m.getAppWidgetIds(ComponentName(c, ClockWidgetProvider::class.java)).forEach { updateOne(c, m, it) }
            schedule(c)
        }
    }
    override fun onEnabled(c: Context) = schedule(c)
    override fun onDisabled(c: Context) {
        (c.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pending(c))
    }
    companion object {
        const val ACTION_REFRESH = "com.anish.oxygenclock.REFRESH"
        private fun pending(c: Context) = PendingIntent.getBroadcast(c, 991, Intent(c, ClockWidgetProvider::class.java).setAction(ACTION_REFRESH), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        private fun schedule(c: Context) {
            val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime()+60000L, 60000L, pending(c))
        }
        fun updateOne(c: Context, m: AppWidgetManager, id: Int) {
            val v = RemoteViews(c.packageName, R.layout.widget_clock)
            val now = Date(); val hh = SimpleDateFormat("HH", Locale.getDefault()).format(now)
            v.setTextViewText(R.id.day_text, SimpleDateFormat("EEEE", Locale.getDefault()).format(now))
            v.setTextViewText(R.id.hour_first, hh.substring(0,1))
            v.setTextViewText(R.id.hour_rest, hh.substring(1) + ":" + SimpleDateFormat("mm", Locale.getDefault()).format(now))
            v.setTextViewText(R.id.date_text, SimpleDateFormat("d MMM", Locale.getDefault()).format(now))
            val cfg = PendingIntent.getActivity(c, id, Intent(c, SettingsActivity::class.java).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            v.setOnClickPendingIntent(R.id.widget_root, cfg)
            m.updateAppWidget(id, v); weather(c,m,id)
        }
        private fun weather(c: Context, m: AppWidgetManager, id: Int) {
            val city = c.getSharedPreferences("clock_settings",Context.MODE_PRIVATE).getString("city","Delhi") ?: "Delhi"
            thread {
                var label = "☁ --°"
                try {
                    val q=URLEncoder.encode(city,"UTF-8")
                    val g=URL("https://geocoding-api.open-meteo.com/v1/search?name=$q&count=1&language=en&format=json").openConnection() as HttpURLConnection
                    g.connectTimeout=8000; g.readTimeout=8000
                    val gj=JSONObject(g.inputStream.bufferedReader().use{it.readText()}); g.disconnect()
                    val place=gj.optJSONArray("results")?.optJSONObject(0)
                    if(place!=null) {
                        val lat=place.getDouble("latitude"); val lon=place.getDouble("longitude")
                        val w=URL("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,weather_code&temperature_unit=celsius").openConnection() as HttpURLConnection
                        w.connectTimeout=8000; w.readTimeout=8000
                        val cur=JSONObject(w.inputStream.bufferedReader().use{it.readText()}).getJSONObject("current"); w.disconnect()
                        val code=cur.getInt("weather_code"); val temp=cur.getDouble("temperature_2m").toInt()
                        val icon=when(code){0->"☀";1,2->"🌤";3->"☁";45,48->"🌫";in 51..67->"🌧";in 71..77->"❄";in 80..82->"🌦";in 95..99->"⛈";else->"☁"}
                        label="$icon $temp°"
                    }
                } catch (_:Exception) {}
                val v=RemoteViews(c.packageName,R.layout.widget_clock)
                v.setTextViewText(R.id.weather_text,label); m.updateAppWidget(id,v)
            }
        }
    }
}
