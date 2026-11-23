package com.vanipy.unggun.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.vanipy.unggun.R
import com.vanipy.unggun.data.AppStore
import com.vanipy.unggun.data.LogStore
import com.vanipy.unggun.data.Prefs
import com.vanipy.unggun.net.RateLimit
import com.vanipy.unggun.net.UpdateChecker
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: AppsAdapter
    private lateinit var progress: LinearProgressIndicator
    private lateinit var statusLine: TextView
    private lateinit var emptyView: TextView
    private val storeListener = { runOnUiThread { refreshList() } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        progress = findViewById(R.id.progress)
        statusLine = findViewById(R.id.status_line)
        emptyView = findViewById(R.id.empty_view)

        adapter = AppsAdapter { app -> DetailSheet.newInstance(app.id).show(supportFragmentManager, "detail") }
        val list = findViewById<RecyclerView>(R.id.app_list)
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter

        findViewById<FloatingActionButton>(R.id.fab_add).setOnClickListener {
            AddSheet().show(supportFragmentManager, "add")
        }

        maybeAskNotifications()
        refreshList()
    }

    override fun onResume() {
        super.onResume()
        AppStore.addListener(storeListener)
        refreshList()
        showRateLimitBanner()
    }

    override fun onPause() {
        AppStore.removeListener(storeListener)
        super.onPause()
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean = when (item.itemId) {
        R.id.action_refresh -> { startCheck(); true }
        R.id.action_logs -> { startActivity(Intent(this, LogsActivity::class.java)); true }
        R.id.action_settings -> { SettingsSheet().show(supportFragmentManager, "settings"); true }
        R.id.action_export -> { exportList(); true }
        else -> super.onOptionsItemSelected(item)
    }

    private fun refreshList() {
        val apps = AppStore.all()
        adapter.submit(apps)
        emptyView.visibility = if (apps.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showRateLimitBanner() {
        val until = Prefs.rateLimitedUntil
        if (until > System.currentTimeMillis() / 1000) {
            statusLine.text = "github limit hit, retry after ${RateLimit.formatHhMm(until)}"
            statusLine.visibility = View.VISIBLE
        } else {
            statusLine.visibility = View.GONE
        }
    }

    private fun startCheck() {
        if (UpdateChecker.running) {
            Toast.makeText(this, "check already running", Toast.LENGTH_SHORT).show()
            return
        }
        progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            val summary = UpdateChecker.checkAll(
                applicationContext,
                progress = object : UpdateChecker.Progress {
                    override fun onProgress(done: Int, total: Int, current: String) {
                        runOnUiThread {
                            statusLine.text = "checking $current ($done/$total)"
                            statusLine.visibility = View.VISIBLE
                        }
                    }
                }
            )
            progress.visibility = View.GONE
            showRateLimitBanner()
            val msg = when {
                summary.rateLimitedUntil > System.currentTimeMillis() / 1000 ->
                    "github limit hit, retry after ${RateLimit.formatHhMm(summary.rateLimitedUntil)}"
                summary.updates.isNotEmpty() -> "${summary.updates.size} update(s) available"
                else -> "all up to date (${summary.checked} checked, ${summary.failed} failed)"
            }
            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
            LogStore.log("ui", msg)
        }
    }

    private fun exportList() {
        val text = AppStore.export()
        if (text.isBlank()) {
            Toast.makeText(this, "nothing to export", Toast.LENGTH_SHORT).show()
            return
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(send, "export app list"))
    }

    private fun maybeAskNotifications() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }
}
