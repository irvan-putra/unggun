package com.vanipy.unggun.net

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DownloadCancelReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_CANCEL) {
            intent.getStringExtra(EXTRA_APP_ID)?.let { Downloader.cancel(it) }
        }
    }

    companion object {
        const val ACTION_CANCEL = "com.vanipy.unggun.CANCEL_DOWNLOAD"
        const val EXTRA_APP_ID = "app_id"
    }
}
