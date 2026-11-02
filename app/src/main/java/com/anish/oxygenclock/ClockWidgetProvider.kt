package com.anish.oxygenclock
import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread
class ClockWidgetProvider:AppWidgetProvider(){
 override fun onUpdate(c:Context,m:AppWidgetManager,ids:IntArray){ids.forEach{updateOne(c,m,it)};schedule(c)}
 override fun onReceive(c:Context,i:Intent){super.onReceive(c,i);if(i.action==ACTION_REFRESH||i.action==Intent.ACTION_TIME_CHANGED||i.action==Intent.ACTION_TIMEZONE_CHANGED||i.action==Intent.ACTION_DATE_CHANGED||i.action==Intent.ACTION_BOOT_COMPLETED||i.action==Intent.ACTION_MY_PACKAGE_REPLACED){val m=AppWidgetManager.getInstance(c);m.getAppWidgetIds(ComponentName(c,ClockWidgetProvider::class.java)).forEach{updateOne(c,m,it)};schedule(c)}}
 override fun onEnabled(c:Context){schedule(c)}
 override fun onDisabled(c:Context){(c.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pending(c))}
 companion object{
  const val ACTION_REFRESH="com.anish.oxygenclock.REFRESH"
  private fun pending(c:Context)=PendingIntent.getBroadcast(c,991,Intent(c,ClockWidgetProvider::class.java).setAction(ACTION_REFRESH),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  private fun schedule(c:Context){val am=c.getSystemService(Context.ALARM_SERVICE) as AlarmManager;val pi=pending(c);if(Build.VERSION.SDK_INT>=31&&!am.canScheduleExactAlarms())am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,SystemClock.elapsedRealtime()+60000L,60000L,pi)else am.setExact(AlarmManager.ELAPSED_REALTIME,SystemClock.elapsedRealtime()+60000L,pi)}
  fun updateOne(c:Context,m:AppWidgetManager,id:Int){
   val v=RemoteViews(c.packageName,R.layout.widget_clock);val p=c.getSharedPreferences("clock_settings",Context.MODE_PRIVATE);val loc=Locale.getDefault();val now=Date();val is24=p.getBoolean("24h",true)
   val time=SimpleDateFormat(if(is24)"HH:mm" else "h:mm",loc).format(now);val hour=SimpleDateFormat(if(is24)"HH" else "h",loc).format(now);val minute=SimpleDateFormat("mm",loc).format(now);val day=SimpleDateFormat("EEEE",loc).format(now);val shortDay=SimpleDateFormat("EEE",loc).format(now);val date=SimpleDateFormat("d MMM",loc).format(now)
   val style=p.getInt("style",0).coerceIn(0,3);val views=intArrayOf(R.id.style_split,R.id.style_classic,R.id.style_vertical,R.id.style_minimal);views.forEachIndexed{n,r->v.setViewVisibility(r,if(n==style)View.VISIBLE else View.GONE)}
   v.setTextViewText(R.id.split_hour,hour);v.setTextViewText(R.id.split_minute,minute);v.setTextViewText(R.id.split_date,date+" "+shortDay)
   v.setTextViewText(R.id.classic_day,day);v.setTextViewText(R.id.classic_time,time);v.setTextViewText(R.id.classic_meta,date+" · ◌")
   v.setTextViewText(R.id.vertical_day,day.uppercase(loc));v.setTextViewText(R.id.vertical_time,time);v.setTextViewText(R.id.vertical_meta,date+" · ◌")
   v.setTextViewText(R.id.minimal_time,time);v.setTextViewText(R.id.minimal_day,shortDay+", "+date)
   val accent=p.getInt("accent",0xFFFFD6D0.toInt());intArrayOf(R.id.split_hour,R.id.split_minute,R.id.classic_time,R.id.vertical_time,R.id.minimal_time).forEach{v.setTextColor(it,accent)}
   val cfg=PendingIntent.getActivity(c,id,Intent(c,SettingsActivity::class.java).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE);v.setOnClickPendingIntent(R.id.widget_root,cfg);m.updateAppWidget(id,v);weather(c,m,id)
  }
  private fun weather(c:Context,m:AppWidgetManager,id:Int){val p=c.getSharedPreferences("clock_settings",Context.MODE_PRIVATE);if(!p.getBoolean("show_weather",true))return;val city=p.getString("city","Delhi")?:"Delhi";thread{
   var icon="☁";var temp="--";var condition="Weather unavailable";var highLow=""
   try{val q=URLEncoder.encode(city,"UTF-8");val g=URL("https://geocoding-api.open-meteo.com/v1/search?name="+q+"&count=1&language=en&format=json").openConnection() as HttpURLConnection;g.connectTimeout=8000;g.readTimeout=8000;val gj=JSONObject(g.inputStream.bufferedReader().use{it.readText()});g.disconnect();val place=gj.optJSONArray("results")?.optJSONObject(0)
    if(place!=null){val lat=place.getDouble("latitude");val lon=place.getDouble("longitude");val w=URL("https://api.open-meteo.com/v1/forecast?latitude="+lat+"&longitude="+lon+"&current=temperature_2m,weather_code&daily=temperature_2m_max,temperature_2m_min&forecast_days=1&temperature_unit=celsius").openConnection() as HttpURLConnection;w.connectTimeout=8000;w.readTimeout=8000;val json=JSONObject(w.inputStream.bufferedReader().use{it.readText()});w.disconnect();val cur=json.getJSONObject("current");val code=cur.getInt("weather_code");temp=cur.getDouble("temperature_2m").toInt().toString();val daily=json.optJSONObject("daily");if(daily!=null)highLow=daily.getJSONArray("temperature_2m_min").optDouble(0).toInt().toString()+"° / "+daily.getJSONArray("temperature_2m_max").optDouble(0).toInt()+"°";val pair=when(code){0->"☼" to "Clear";1,2->"◒" to "Partly cloudy";3->"☁" to "Overcast";45,48->"≋" to "Fog";in 51..67->"⌁" to "Rain";in 71..77->"✳" to "Snow";in 80..82->"◌" to "Showers";in 95..99->"ϟ" to "Thunderstorm";else->"☁" to "Cloudy"};icon=pair.first;condition=pair.second}
   }catch(_:Exception){}
   val label=icon+" "+temp+"°";val v=RemoteViews(c.packageName,R.layout.widget_clock);val d=SimpleDateFormat("d MMM",Locale.getDefault()).format(Date());v.setTextViewText(R.id.split_weather,label);v.setTextViewText(R.id.split_city,city);v.setTextViewText(R.id.split_condition,temp+"° "+condition);v.setTextViewText(R.id.split_highlow,highLow);v.setTextViewText(R.id.classic_meta,d+" · "+label);v.setTextViewText(R.id.vertical_meta,d+" · "+label);v.setTextViewText(R.id.minimal_weather,label);m.updateAppWidget(id,v)
  }}
 }
}