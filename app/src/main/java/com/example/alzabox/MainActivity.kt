package com.example.alzabox

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var txtStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefs = getSharedPreferences("alza_prefs", Context.MODE_PRIVATE)
        txtStatus = findViewById(R.id.txtInstallStatus)
        val btnSelfInstall = findViewById<Button>(R.id.btnSelfInstall)
        val editUrl = findViewById<EditText>(R.id.editDefaultUrl)
        val btnSave = findViewById<Button>(R.id.btnSaveUrl)

        editUrl.setText(prefs.getString("home_url", "https://m.youtube.com"))
        updateInstallerStatus()

        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrEmpty()) {
                val extractedUrl = Regex("(https?://\\S+)").find(sharedText)?.value ?: sharedText
                editUrl.setText(extractedUrl)
                sendUrlToCar(extractedUrl)
            }
        }

        btnSelfInstall.setOnClickListener {
            activateForAndroidAuto()
        }

        btnSave.setOnClickListener {
            val url = editUrl.text.toString().trim().ifEmpty { "https://m.youtube.com" }
            sendUrlToCar(url)
        }
    }

    override fun onResume() {
        super.onResume()
        updateInstallerStatus()
    }

    private fun updateInstallerStatus() {
        try {
            val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                packageManager.getInstallSourceInfo(packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstallerPackageName(packageName)
            }
            txtStatus.text = if (installer == "com.android.vending") {
                "Status: Play Store Tag Active (com.android.vending)\nReady for Android Auto!"
            } else {
                "Current Installer: ${installer ?: "Normal Package Installer"}\nTap Button 1 below to activate for Android Auto."
            }
        } catch (e: Exception) {
            txtStatus.text = "Status: Ready"
        }
    }

    @Suppress("DEPRECATION")
    private fun activateForAndroidAuto() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!packageManager.canRequestPackageInstalls()) {
                Toast.makeText(this, "Please allow 'Install unknown apps' for Alza WebBox, then tap the button again.", Toast.LENGTH_LONG).show()
                val permIntent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:$packageName")
                )
                startActivity(permIntent)
                return
            }
        }

        try {
            val sourceApk = File(applicationInfo.sourceDir)
            val tempApk = File(cacheDir, "alza-auto-activate.apk")
            sourceApk.inputStream().use { input ->
                tempApk.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            val apkUri = FileProvider.getUriForFile(
                this,
                "$packageName.provider",
                tempApk
            )

            val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                data = apkUri
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, "com.android.vending")
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                putExtra(Intent.EXTRA_RETURN_RESULT, true)
            }
            startActivity(installIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
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
