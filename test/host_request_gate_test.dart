import 'dart:math';

import 'package:flutter_test/flutter_test.dart';
import 'package:obtainium/custom_errors.dart';
import 'package:obtainium/services/host_request_gate.dart';

void main() {
  group('HostRequestGate', () {
    test('caps per-host concurrency', () async {
      final gate = HostRequestGate();
      var active = 0;
      var maxActive = 0;
      await Future.wait(
        List.generate(6, (_) {
          return gate.run('host-a', 'host-a|api', 2, () async {
            active++;
            maxActive = max(maxActive, active);
            await Future.delayed(const Duration(milliseconds: 20));
            active--;
          });
        }),
      );
      expect(maxActive, 2);
    });

    test('blocks one request class on challenge, not others', () async {
      var now = DateTime.utc(2026, 1, 1);
      final gate = HostRequestGate(now: () => now);
      gate.observe('host-b|html', 403, {'cf-mitigated': 'challenge'});
      expect(
        () => gate.run('host-b', 'host-b|html', 2, () async {}),
        throwsA(isA<RateLimitError>()),
      );
      await gate.run('host-b', 'host-b|api', 2, () async {});
      now = now.add(const Duration(minutes: 6));
      await gate.run('host-b', 'host-b|html', 2, () async {});
    });

    test('honors Retry-After and defaults to 60s', () async {
      var now = DateTime.utc(2026, 1, 1);
      final gate = HostRequestGate(now: () => now);
      gate.observe('host-c|api', 429, {'retry-after': '2'});
      expect(
        () => gate.run('host-c', 'host-c|api', 2, () async {}),
        throwsA(isA<RateLimitError>()),
      );
      now = now.add(const Duration(seconds: 3));
      await gate.run('host-c', 'host-c|api', 2, () async {});

      gate.observe('host-d|feed', 503, {});
      expect(
        () => gate.run('host-d', 'host-d|feed', 2, () async {}),
        throwsA(isA<RateLimitError>()),
      );
      now = now.add(const Duration(seconds: 61));
      await gate.run('host-d', 'host-d|feed', 2, () async {});
    });

    test('plain 403 does not block', () async {
      var now = DateTime.utc(2026, 1, 1);
      final gate = HostRequestGate(now: () => now);
      gate.observe('host-e|api', 403, {});
      await gate.run('host-e', 'host-e|api', 2, () async {});
      now = now.add(const Duration(seconds: 1));
      await gate.run('host-e', 'host-e|api', 2, () async {});
    });

    test('blocks until GitHub x-ratelimit-reset on 200 with none left', () async {
      var now = DateTime.utc(2026, 1, 1);
      final gate = HostRequestGate(now: () => now);
      final reset = now.add(const Duration(minutes: 42));
      gate.observe('api.github.com|html', 200, {
        'x-ratelimit-remaining': '0',
        'x-ratelimit-reset': '${reset.millisecondsSinceEpoch ~/ 1000}',
      });
      var called = false;
      await expectLater(
        () => gate.run('api.github.com', 'api.github.com|html', 2, () async {
          called = true;
        }),
        throwsA(
          isA<RateLimitError>().having(
            (e) => e.remainingMinutes,
            'remainingMinutes',
            42,
          ),
        ),
      );
      expect(called, false);
      now = now.add(const Duration(minutes: 43));
      await gate.run('api.github.com', 'api.github.com|html', 2, () async {});
    });

    test('remaining quota does not block; 403 Retry-After does', () async {
      var now = DateTime.utc(2026, 1, 1);
      final gate = HostRequestGate(now: () => now);
      gate.observe('h|html', 200, {
        'x-ratelimit-remaining': '10',
        'x-ratelimit-reset': '${now.millisecondsSinceEpoch ~/ 1000 + 3600}',
      });
      await gate.run('h', 'h|html', 2, () async {});
      gate.observe('h|html', 403, {'retry-after': '120'});
      expect(
        () => gate.run('h', 'h|html', 2, () async {}),
        throwsA(isA<RateLimitError>()),
      );
      now = now.add(const Duration(seconds: 121));
      await gate.run('h', 'h|html', 2, () async {});
    });

    test('a persisted block is restored and exported', () async {
      final now = DateTime.utc(2026, 1, 1);
      final gate = HostRequestGate(now: () => now);
      final until = now.add(const Duration(minutes: 10));
      gate.restoreBlock('k|html', until.millisecondsSinceEpoch);
      expect(gate.blockedUntilMillis('k|html'), until.millisecondsSinceEpoch);
      expect(
        () => gate.run('k', 'k|html', 2, () async {}),
        throwsA(isA<RateLimitError>()),
      );
    });

    test('secondary limit 403 without Retry-After waits a minute', () async {
      var now = DateTime.utc(2026, 1, 1);
      final gate = HostRequestGate(now: () => now);
      gate.observe(
        'g|html',
        403,
        {},
        body: '{"message":"You have exceeded a secondary rate limit."}',
      );
      expect(
        () => gate.run('g', 'g|html', 2, () async {}),
        throwsA(isA<RateLimitError>()),
      );
      now = now.add(const Duration(seconds: 61));
      await gate.run('g', 'g|html', 2, () async {});
    });

    test('exhausted quota with a missing or past reset still pauses', () async {
      var now = DateTime.utc(2026, 1, 1);
      final gate = HostRequestGate(now: () => now);
      gate.observe('m|html', 403, {'x-ratelimit-remaining': '0'});
      expect(
        () => gate.run('m', 'm|html', 2, () async {}),
        throwsA(isA<RateLimitError>()),
      );
      now = now.add(const Duration(seconds: 61));
      await gate.run('m', 'm|html', 2, () async {});
    });
  });
}
