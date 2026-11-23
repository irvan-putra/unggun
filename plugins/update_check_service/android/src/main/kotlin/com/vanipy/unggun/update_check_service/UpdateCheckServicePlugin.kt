package com.vanipy.unggun.update_check_service

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

/**
 * Bridge between Dart (foreground UI engine and the headless Workmanager
 * engine alike) and [UpdateCheckService]. Registered as a FlutterPlugin so
 * GeneratedPluginRegistrant attaches it to every engine the app spawns.
 */
class UpdateCheckServicePlugin : FlutterPlugin, MethodChannel.MethodCallHandler {
    private lateinit var channel: MethodChannel
    private lateinit var context: Context

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        context = binding.applicationContext
        channel = MethodChannel(
            binding.binaryMessenger,
            "com.vanipy.unggun/update_check_service",
        )
        channel.setMethodCallHandler(this)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "start" -> {
                val intent = Intent(context, UpdateCheckService::class.java)
                    .setAction(UpdateCheckService.ACTION_START)
                    .putExtra(
                        UpdateCheckService.EXTRA_TITLE,
                        call.argument<String>("title"),
                    )
                    .putExtra(
                        UpdateCheckService.EXTRA_TEXT,
                        call.argument<String>("text"),
                    )
                try {
                    ContextCompat.startForegroundService(context, intent)
                    result.success(true)
                } catch (e: Exception) {
                    // Android 12+ can refuse background FGS starts (no
                    // exemption applies in some states). The caller degrades
                    // to notification-only behavior on false.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        e is android.app.ForegroundServiceStartNotAllowedException
                    ) {
                        result.success(false)
                    } else {
                        result.success(false)
                    }
                }
            }
            "update" -> {
                val intent = Intent(context, UpdateCheckService::class.java)
                    .setAction(UpdateCheckService.ACTION_UPDATE)
                    .putExtra(
                        UpdateCheckService.EXTRA_TITLE,
                        call.argument<String>("title"),
                    )
                    .putExtra(
                        UpdateCheckService.EXTRA_TEXT,
                        call.argument<String>("text"),
                    )
                    .putExtra(
                        UpdateCheckService.EXTRA_PROGRESS,
                        call.argument<Int>("progress") ?: 0,
                    )
                    .putExtra(
                        UpdateCheckService.EXTRA_MAX,
                        call.argument<Int>("max") ?: 0,
                    )
                try {
                    context.startService(intent)
                    result.success(true)
                } catch (_: Exception) {
                    result.success(false)
                }
            }
            "stop" -> {
                try {
                    context.startService(
                        Intent(context, UpdateCheckService::class.java)
                            .setAction(UpdateCheckService.ACTION_STOP),
                    )
                    result.success(true)
                } catch (_: Exception) {
                    result.success(false)
                }
            }
            else -> result.notImplemented()
        }
    }
}
