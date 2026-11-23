// Per-host request limiting for metadata requests: bounds concurrency and
// short-circuits requests to hosts that recently challenged or rate-limited us.

import 'dart:async';
import 'dart:math';

import 'package:obtainium/custom_errors.dart';

class HostRequestGate {
  HostRequestGate({
    DateTime Function()? now,
    this.baseRequestInterval = Duration.zero,
    this.maxPaceInterval = const Duration(seconds: 10),
    this.paceWhenRemainingAtOrBelow = 15,
  }) : _now = now ?? DateTime.now;

  static final HostRequestGate instance = HostRequestGate();

  static const Duration challengeBlockDuration = Duration(minutes: 5);
  static const Duration defaultRateLimitBlockDuration = Duration(seconds: 60);

  final DateTime Function() _now;

  /// Minimum spacing between request starts for the same request class.
  /// Zero by default: check-level concurrency bounds the burst, and spacing
  /// only matters once the host reports its quota running low.
  final Duration baseRequestInterval;

  /// Upper bound for adaptive pacing when the remaining quota is low.
  final Duration maxPaceInterval;

  /// Adaptive pacing kicks in once the host reports this many requests or
  /// fewer left in the current quota window.
  final int paceWhenRemainingAtOrBelow;

  final Map<String, _HostState> _hosts = {};
  final Map<String, DateTime> _blockedUntil = {};
  final Map<String, _Quota> _quota = {};
  final Map<String, DateTime> _lastStart = {};

  _HostState _state(String host) => _hosts.putIfAbsent(host, _HostState.new);

  int? _remainingBlockMinutes(String blockKey) {
    final until = _blockedUntil[blockKey];
    if (until == null) return null;
    final now = _now();
    if (!now.isBefore(until)) {
      _blockedUntil.remove(blockKey);
      return null;
    }
    return max(1, (until.difference(now).inSeconds / 60).ceil());
  }

  /// Runs [action] honoring [host]'s concurrency limit. [blockKey] identifies
  /// the request class (e.g. api/feed/html) so a challenge on one class does
  /// not suppress the others. Throws [RateLimitError] without invoking
  /// [action] while the class is blocked. Otherwise waits out any pacing delay
  /// owed to the class before starting the request.
  Future<T> run<T>(
    String host,
    String blockKey,
    int maxConcurrent,
    Future<T> Function() action,
  ) async {
    final remainingMinutes = _remainingBlockMinutes(blockKey);
    if (remainingMinutes != null) {
      throw RateLimitError(remainingMinutes);
    }
    return _state(host).run(maxConcurrent, () async {
      await _pace(blockKey);
      return action();
    });
  }

  /// Spacing owed before the next request of class [blockKey] may start: the
  /// base stagger, widened adaptively once the host reports its quota running
  /// low so the remaining requests are spread across the rest of the window.
  Duration paceInterval(String blockKey, {DateTime? now}) {
    final at = now ?? _now();
    var interval = baseRequestInterval;
    final quota = _quota[blockKey];
    if (quota != null &&
        quota.remaining > 0 &&
        quota.remaining <= paceWhenRemainingAtOrBelow) {
      final secondsLeft = quota.resetAt.difference(at).inSeconds;
      if (secondsLeft > 0) {
        final spread = Duration(
          milliseconds: (secondsLeft * 1000 / quota.remaining).round(),
        );
        if (spread > interval) {
          interval = spread > maxPaceInterval ? maxPaceInterval : spread;
        }
      }
    }
    return interval;
  }

  Future<void> _pace(String blockKey) async {
    // Reserve a start slot synchronously (no await between read and write) so
    // concurrent callers chain their slots instead of sharing one.
    final now = _now();
    final last = _lastStart[blockKey];
    var start = now;
    if (last != null) {
      final earliest = last.add(paceInterval(blockKey, now: last));
      if (earliest.isAfter(start)) start = earliest;
    }
    _lastStart[blockKey] = start;
    final wait = start.difference(now);
    if (wait.inMilliseconds > 0) {
      await Future.delayed(wait);
    }
  }

  /// Records a completed response and blocks that request class when it
  /// carries a Cloudflare challenge or a rate-limit signal.
  void observe(
    String blockKey,
    int statusCode,
    Map<String, String> headers, {
    String? body,
  }) {
    _recordQuota(blockKey, headers);
    final reset = _githubQuotaReset(headers);
    if (reset != null) {
      _blockUntil(blockKey, reset);
      return;
    }
    if (statusCode != 403 && statusCode != 429 && statusCode != 503) return;
    final challenge =
        headers['cf-mitigated']?.toLowerCase().trim() == 'challenge';
    if (challenge) {
      _blockUntil(blockKey, _now().add(challengeBlockDuration));
      return;
    }
    if (statusCode == 429 ||
        statusCode == 503 ||
        retryAfterOn403(statusCode, headers) ||
        (statusCode == 403 && _isSecondaryLimitBody(body))) {
      final retryAfter = int.tryParse(headers['retry-after']?.trim() ?? '');
      _blockUntil(
        blockKey,
        _now().add(
          retryAfter != null && retryAfter > 0
              ? Duration(seconds: retryAfter)
              : defaultRateLimitBlockDuration,
        ),
      );
    }
  }

  static bool _isSecondaryLimitBody(String? body) =>
      body != null && body.toLowerCase().contains('secondary rate limit');

  static bool retryAfterOn403(int statusCode, Map<String, String> headers) =>
      statusCode == 403 &&
      (int.tryParse(headers['retry-after']?.trim() ?? '') ?? 0) > 0;

  /// Remembers the host's own accounting (x-ratelimit-remaining /
  /// x-ratelimit-reset, sent on every GitHub API response) so [paceInterval]
  /// can spread the remaining budget instead of spending it in one burst.
  void _recordQuota(String blockKey, Map<String, String> headers) {
    final remaining = int.tryParse(
      headers['x-ratelimit-remaining']?.trim() ?? '',
    );
    final epoch = int.tryParse(headers['x-ratelimit-reset']?.trim() ?? '');
    if (remaining == null || epoch == null) return;
    _quota[blockKey] = _Quota(
      remaining,
      DateTime.fromMillisecondsSinceEpoch(epoch * 1000, isUtc: true),
    );
  }

  /// GitHub reports an exhausted quota with x-ratelimit-remaining 0 and the
  /// reset time in x-ratelimit-reset (epoch seconds), on any status.
  DateTime? _githubQuotaReset(Map<String, String> headers) {
    if (headers['x-ratelimit-remaining']?.trim() != '0') return null;
    final epoch = int.tryParse(headers['x-ratelimit-reset']?.trim() ?? '');
    if (epoch == null) {
      // Exhausted but no usable reset time: wait the documented minimum.
      return _now().add(defaultRateLimitBlockDuration);
    }
    final until = DateTime.fromMillisecondsSinceEpoch(
      epoch * 1000,
      isUtc: true,
    );
    return until.isAfter(_now())
        ? until
        : _now().add(defaultRateLimitBlockDuration);
  }

  /// Epoch milliseconds a class is blocked until, or null. For persistence.
  int? blockedUntilMillis(String blockKey) =>
      _blockedUntil[blockKey]?.millisecondsSinceEpoch;

  /// Restores a persisted block (e.g. from another isolate or a previous run).
  void restoreBlock(String blockKey, int untilMillis) => _blockUntil(
    blockKey,
    DateTime.fromMillisecondsSinceEpoch(untilMillis, isUtc: true),
  );

  void _blockUntil(String blockKey, DateTime until) {
    final current = _blockedUntil[blockKey];
    if (current == null || until.isAfter(current)) {
      _blockedUntil[blockKey] = until;
    }
  }
}

class _Quota {
  _Quota(this.remaining, this.resetAt);
  final int remaining;
  final DateTime resetAt;
}

class _HostState {
  int _active = 0;
  final List<Completer<void>> _waiters = [];

  Future<T> run<T>(int maxConcurrent, Future<T> Function() action) async {
    while (_active >= maxConcurrent) {
      final completer = Completer<void>();
      _waiters.add(completer);
      await completer.future;
    }
    _active++;
    try {
      return await action();
    } finally {
      _active--;
      if (_waiters.isNotEmpty) {
        _waiters.removeAt(0).complete();
      }
    }
  }
}
