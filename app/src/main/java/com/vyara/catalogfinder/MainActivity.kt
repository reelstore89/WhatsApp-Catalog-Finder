package com.vyara.catalogfinder

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(60, 60, 60, 60)
            setBackgroundColor(Color.parseColor("#0F172A"))
        }

        val title = TextView(this).apply {
            text = "WhatsApp Catalog ID Finder"
            textSize = 22f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 30)
        }

        val instructions = TextView(this).apply {
            text = "Start karne ke liye neeche diye dono buttons daba kar Permission ON karein:\n\n1. Overlay Permission (Floating Icon ke liye)\n2. Accessibility Service (WhatsApp Catalog Search ke liye)"
            textSize = 15f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, 0, 0, 50)
        }

        val btnOverlay = Button(this).apply {
            text = "Step 1: Allow Floating Popup"
            setBackgroundColor(Color.parseColor("#25D366"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                } else {
                    Toast.makeText(this@MainActivity, "Floating Permission Pehle se ON hai! ✅", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val spacer = TextView(this).apply { height = 30 }

        val btnAccessibility = Button(this).apply {
            text = "Step 2: Turn ON Catalog Finder Service"
            setBackgroundColor(Color.parseColor("#3B82F6"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
                Toast.makeText(
                    this@MainActivity,
                    "List mein 'WhatsApp Catalog Finder' dhundhein aur ON karein",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        layout.addView(title)
        layout.addView(instructions)
        layout.addView(btnOverlay)
        layout.addView(spacer)
        layout.addView(btnAccessibility)

        setContentView(layout)
    }
}
