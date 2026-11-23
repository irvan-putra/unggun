import 'package:flutter/services.dart';

/// Bridge to the native UpdateCheckService (foreground-service promotion of
/// update checks, Mihon-style). Every method degrades to `false` when the
/// platform cannot honor the request (channel missing, background FGS start
/// refused), so callers can fall back to notification-only behavior.
class UpdateCheckServiceApi {
  static const MethodChannel _channel =
      MethodChannel('com.vanipy.unggun/update_check_service');

  /// Starts the foreground service with an indeterminate "checking"
  /// notification. Returns false when the system refused the start (e.g.
  /// Android 12+ background-start restrictions); callers must then keep the
  /// notification-only path.
  static Future<bool> start({required String title, required String text}) async {
    try {
      final ok = await _channel
          .invokeMethod<bool>('start', {'title': title, 'text': text});
      return ok ?? false;
    } on MissingPluginException {
      return false;
    } on PlatformException {
      return false;
    }
  }

  /// Updates the service's notification progress (determinate).
  static Future<bool> update({
    required String title,
    required String text,
    required int progress,
    required int max,
  }) async {
    try {
      final ok = await _channel.invokeMethod<bool>('update', {
        'title': title,
        'text': text,
        'progress': progress,
        'max': max,
      });
      return ok ?? false;
    } on MissingPluginException {
      return false;
    } on PlatformException {
      return false;
    }
  }

  /// Stops the service and removes its notification.
  static Future<bool> stop() async {
    try {
      final ok = await _channel.invokeMethod<bool>('stop');
      return ok ?? false;
    } on MissingPluginException {
      return false;
    } on PlatformException {
      return false;
    }
  }
}
