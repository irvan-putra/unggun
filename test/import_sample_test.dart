import 'dart:convert';
import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:obtainium/models/app.dart';
import 'package:obtainium/providers/app_json_migration.dart';

// Code-level check of the import path with a bare-list import file of public repositories
// (the same shape the Import screen accepts). It does not exercise the UI or storage.
void main() {
  test('import sample parses into apps with their per-app settings', () {
    final raw = File('test/fixtures/import-sample.json').readAsStringSync();
    final decoded = jsonDecode(raw);
    expect(decoded, isA<List>());
    final apps = (decoded as List)
        .map((e) => appFromStoredJson(Map<String, dynamic>.from(e as Map)))
        .toList();
    expect(apps.length, 14);
    expect(apps.every((a) => a.url.startsWith('https://github.com/')), isTrue);
    expect(apps.every((a) => a.id.startsWith('com.vanipy.')), isTrue);
    final settings = jsonDecode(apps.first.additionalSettings) as Map;
    expect(settings['exemptFromBackgroundUpdates'], true);
    expect(settings['includePrereleases'], true);
    expect(settings['filterReleaseTitlesByRegEx'], 'smoke|debug');
    expect(App, isNotNull);
  });
}
