package com.anish.oxygenclock
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView

class SettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("clock_settings", Context.MODE_PRIVATE)
        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        setResult(RESULT_CANCELED)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(36, 48, 36, 24) }
        root.addView(TextView(this).apply { text = "Oxygen Clock settings"; textSize = 24f })
        root.addView(TextView(this).apply { text = "Clock style"; textSize = 16f; setPadding(0, 24, 0, 8) })
        val styles = listOf("Classic · Oxygen", "Vertical · Centered", "Minimal · Clean")
        val stylePicker = Spinner(this)
        stylePicker.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, styles)
        stylePicker.setSelection(prefs.getInt("style", 0).coerceIn(0, 2))
        root.addView(stylePicker, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(TextView(this).apply { text = "Weather city"; textSize = 16f; setPadding(0, 24, 0, 8) })
        val city = EditText(this).apply { hint = "e.g. Delhi"; setSingleLine(true); setText(prefs.getString("city", "Delhi") ?: "Delhi") }
        root.addView(city)
        root.addView(Button(this).apply {
            text = "Save settings"
            setOnClickListener {
                val value = city.text.toString().trim()
                prefs.edit().putInt("style", stylePicker.selectedItemPosition).apply()
                if (value.isNotEmpty()) prefs.edit().putString("city", value).apply()
                val mgr = AppWidgetManager.getInstance(this@SettingsActivity)
                if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    ClockWidgetProvider.updateOne(this@SettingsActivity, mgr, widgetId)
                    setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
                } else {
                    mgr.getAppWidgetIds(ComponentName(this@SettingsActivity, ClockWidgetProvider::class.java))
                        .forEach { ClockWidgetProvider.updateOne(this@SettingsActivity, mgr, it) }
                }
                finish()
            }
        })
        setContentView(root)
    }
}
