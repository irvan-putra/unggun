import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:update_check_service/update_check_service.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  const channel = MethodChannel('com.vanipy.unggun/update_check_service');
  final messenger =
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;

  tearDown(() {
    messenger.setMockMethodCallHandler(channel, null);
  });

  test('start returns true when the platform accepts', () async {
    messenger.setMockMethodCallHandler(
      channel,
      (call) async => call.method == 'start' ? true : null,
    );
    expect(await UpdateCheckServiceApi.start(title: 't', text: 'x'), isTrue);
  });

  test('start returns false when the platform refuses', () async {
    messenger.setMockMethodCallHandler(channel, (call) async => false);
    expect(await UpdateCheckServiceApi.start(title: 't', text: 'x'), isFalse);
  });

  test('start degrades to false when the plugin is missing', () async {
    // No handler registered: the invoke throws MissingPluginException and the
    // caller must be able to fall back to the notification-only path.
    expect(await UpdateCheckServiceApi.start(title: 't', text: 'x'), isFalse);
  });

  test('update passes progress arguments through, stop takes none', () async {
    final calls = <MethodCall>[];
    messenger.setMockMethodHandlerCalls(calls, channel);
    expect(
      await UpdateCheckServiceApi.update(
        title: 't',
        text: 'x',
        progress: 3,
        max: 10,
      ),
      isTrue,
    );
    expect(await UpdateCheckServiceApi.stop(), isTrue);
    expect(calls.map((c) => c.method), ['update', 'stop']);
    expect(calls.first.arguments, {
      'title': 't',
      'text': 'x',
      'progress': 3,
      'max': 10,
    });
  });
}

extension on TestDefaultBinaryMessenger {
  void setMockMethodHandlerCalls(List<MethodCall> calls, MethodChannel ch) {
    setMockMethodCallHandler(ch, (call) async {
      calls.add(call);
      return true;
    });
  }
}
