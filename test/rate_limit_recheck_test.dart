import 'package:flutter_test/flutter_test.dart';
import 'package:obtainium/custom_errors.dart';
import 'package:obtainium/providers/apps_provider.dart';
import 'package:obtainium/providers/settings_provider.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  test('rate-limit errors default to the manual retry wording', () {
    final err = RateLimitError(58);
    expect(err.remainingMinutes, 58);
    expect(err.autoRetryScheduled, isFalse);
    expect(err.data['autoRetryScheduled'], isFalse);
  });

  test('a queued rate-limit error carries the auto-retry flag', () {
    final err = RateLimitError(58, autoRetryScheduled: true);
    expect(err.autoRetryScheduled, isTrue);
    expect(err.data['autoRetryScheduled'], isTrue);
  });

  test('scheduleRateLimitedRecheck is a no-op off Android and on empty input',
      () async {
    // Platform.isAndroid is false under the test VM, so no Workmanager task
    // is registered and the caller learns nothing was queued.
    final scheduled = await scheduleRateLimitedRecheck(
      'test',
      [const MapEntry('com.example.app', 1)],
      60,
      SettingsProvider(),
    );
    expect(scheduled, isFalse);
    expect(
      await scheduleRateLimitedRecheck('test', const [], 60, SettingsProvider()),
      isFalse,
    );
  });
}
