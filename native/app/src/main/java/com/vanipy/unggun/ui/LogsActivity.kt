package com.vanipy.unggun.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.vanipy.unggun.R
import com.vanipy.unggun.data.LogStore

class LogsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logs)
        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }
        render()
        findViewById<Button>(R.id.btn_share).setOnClickListener {
            val text = LogStore.lines().joinToString("\n")
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            startActivity(Intent.createChooser(send, "share logs"))
        }
        findViewById<Button>(R.id.btn_clear).setOnClickListener {
            LogStore.clear()
            render()
        }
    }

    private fun render() {
        val lines = LogStore.lines()
        findViewById<TextView>(R.id.log_text).text =
            if (lines.isEmpty()) "no logs yet" else lines.joinToString("\n")
    }
}
