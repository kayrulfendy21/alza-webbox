package com.example.alzabox

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefs = getSharedPreferences("alza_prefs", Context.MODE_PRIVATE)
        val editUrl = findViewById<EditText>(R.id.editDefaultUrl)
        val btnSave = findViewById<Button>(R.id.btnSaveUrl)

        editUrl.setText(prefs.getString("home_url", "https://m.youtube.com"))

        // Check if user shared a video link from the YouTube phone app
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrEmpty()) {
                val extractedUrl = Regex("(https?://\\S+)").find(sharedText)?.value ?: sharedText
                editUrl.setText(extractedUrl)
                sendUrlToCar(extractedUrl)
            }
        }

        btnSave.setOnClickListener {
            val url = editUrl.text.toString().trim().ifEmpty { "https://m.youtube.com" }
            sendUrlToCar(url)
        }
    }

    private fun sendUrlToCar(url: String) {
        val prefs = getSharedPreferences("alza_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("home_url", url)
            .putLong("trigger_time", System.currentTimeMillis())
            .apply()
        Toast.makeText(this, "Sent to Car Display!", Toast.LENGTH_SHORT).show()
    }
}
