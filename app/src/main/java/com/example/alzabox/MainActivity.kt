package com.example.alzabox

import android.content.Context
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

        btnSave.setOnClickListener {
            val url = editUrl.text.toString().trim().ifEmpty { "https://m.youtube.com" }
            prefs.edit().putString("home_url", url).apply()
            Toast.makeText(this, "Saved for Car Display!", Toast.LENGTH_SHORT).show()
        }
    }
}
