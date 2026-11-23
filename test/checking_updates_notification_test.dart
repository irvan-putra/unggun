import 'dart:convert';
import 'dart:io';

// ignore: implementation_imports
import 'package:easy_localization/src/localization.dart';
// ignore: implementation_imports
import 'package:easy_localization/src/translations.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:obtainium/providers/notifications_provider.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  test('checking-updates notification is ongoing, silent and progress-capable',
      () {
    Localization.load(
      const Locale('en'),
      translations: Translations(
        jsonDecode(
              File('assets/translations/en.json').readAsStringSync(),
            )
            as Map<String, dynamic>,
      ),
    );

    final initial = CheckingUpdatesNotification('19 apps', progPercent: 0);
    expect(initial.id, 4);
    expect(initial.channelCode, 'BG_UPDATE_CHECK');
    expect(initial.onlyAlertOnce, isTrue);
    expect(initial.ongoing, isTrue);
    expect(initial.progPercent, 0);

    final halfway = CheckingUpdatesNotification('9/19: tuju, kemas',
        progPercent: 47);
    expect(halfway.id, initial.id);
    expect(halfway.progPercent, 47);
    expect(halfway.onlyAlertOnce, isTrue);
    expect(halfway.ongoing, isTrue);
  });
}
