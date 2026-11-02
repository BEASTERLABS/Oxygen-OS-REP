package com.anish.oxygenclock
import android.app.Activity
import android.app.AlarmManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
class SettingsActivity:Activity(){
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);val p=getSharedPreferences("clock_settings",Context.MODE_PRIVATE);val widgetId=intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);setResult(RESULT_CANCELED)
  val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(32,36,32,24)}
  root.addView(TextView(this).apply{text="Oxygen Clock settings";textSize=25f;setTextColor(0xFF222222.toInt())})
  root.addView(TextView(this).apply{text="Choose a genuinely different layout";setPadding(0,20,0,8)})
  val names=listOf("01 · Split editorial (reference look)","02 · Classic horizontal","03 · Centered vertical","04 · Compact side-by-side")
  val style=Spinner(this);style.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,names);style.setSelection(p.getInt("style",0).coerceIn(0,3));root.addView(style)
  root.addView(TextView(this).apply{text="Clock colour";setPadding(0,18,0,8)})
  val palette=listOf("Soft rose","Warm white","Ice blue","Mint","Lavender","Amber");val values=intArrayOf(0xFFFFD6D0.toInt(),0xFFFFFFFF.toInt(),0xFFBFE8FF.toInt(),0xFFC9F5DF.toInt(),0xFFE3D1FF.toInt(),0xFFFFD89A.toInt())
  val color=Spinner(this);color.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,palette);color.setSelection(p.getInt("accent_index",0).coerceIn(0,5));root.addView(color)
  root.addView(TextView(this).apply{text="Weather city";setPadding(0,18,0,8)})
  val city=EditText(this).apply{hint="e.g. Delhi";setSingleLine(true);setText(p.getString("city","Delhi")?:"Delhi")};root.addView(city)
  val h24=CheckBox(this).apply{text="24-hour clock";isChecked=p.getBoolean("24h",true)};val weather=CheckBox(this).apply{text="Show weather";isChecked=p.getBoolean("show_weather",true)};root.addView(h24);root.addView(weather)
  root.addView(Button(this).apply{text="Enable reliable minute refresh";setOnClickListener{if(Build.VERSION.SDK_INT>=31){val am=getSystemService(Context.ALARM_SERVICE) as AlarmManager;if(!am.canScheduleExactAlarms())try{startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+packageName)))}catch(_:Exception){}}}})
  root.addView(TextView(this).apply{text="Allow exact alarms in Android settings to improve minute updates.";textSize=12f})
  root.addView(Button(this).apply{text="Save & update widget";setOnClickListener{val n=city.text.toString().trim();p.edit().putInt("style",style.selectedItemPosition).putInt("accent_index",color.selectedItemPosition).putInt("accent",values[color.selectedItemPosition]).putBoolean("24h",h24.isChecked).putBoolean("show_weather",weather.isChecked).putString("city",if(n.isEmpty())"Delhi" else n).apply();val mgr=AppWidgetManager.getInstance(this@SettingsActivity);if(widgetId!=AppWidgetManager.INVALID_APPWIDGET_ID){ClockWidgetProvider.updateOne(this@SettingsActivity,mgr,widgetId);setResult(RESULT_OK,Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,widgetId))}else mgr.getAppWidgetIds(ComponentName(this@SettingsActivity,ClockWidgetProvider::class.java)).forEach{ClockWidgetProvider.updateOne(this@SettingsActivity,mgr,it)};finish()}})
  setContentView(root)
 }
}