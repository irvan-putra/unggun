package com.vanipy.unggun.data

import android.content.Context

object Prefs {
    private const val NAME = "unggun_prefs"
    private const val KEY_NOTIFY_UPDATES = "notify_updates"
    private const val KEY_NOTIFY_ERRORS = "notify_errors"
    private const val KEY_RATE_LIMITED_UNTIL = "rate_limited_until"
    private const val KEY_DELETE_AFTER_INSTALL = "delete_after_install"

    private fun prefs(context: Context) =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    var notifyUpdates: Boolean
        get() = prefsHolder!!.getBoolean(KEY_NOTIFY_UPDATES, true)
        set(v) = prefsHolder!!.edit().putBoolean(KEY_NOTIFY_UPDATES, v).apply()

    var notifyErrors: Boolean
        get() = prefsHolder!!.getBoolean(KEY_NOTIFY_ERRORS, true)
        set(v) = prefsHolder!!.edit().putBoolean(KEY_NOTIFY_ERRORS, v).apply()

    var deleteAfterInstall: Boolean
        get() = prefsHolder!!.getBoolean(KEY_DELETE_AFTER_INSTALL, true)
        set(v) = prefsHolder!!.edit().putBoolean(KEY_DELETE_AFTER_INSTALL, v).apply()

    var rateLimitedUntil: Long
        get() = prefsHolder!!.getLong(KEY_RATE_LIMITED_UNTIL, 0)
        set(v) = prefsHolder!!.edit().putLong(KEY_RATE_LIMITED_UNTIL, v).apply()

    private var prefsHolder: android.content.SharedPreferences? = null

    @Synchronized
    fun init(context: Context) {
        if (prefsHolder == null) prefsHolder = prefs(context.applicationContext)
    }
}
