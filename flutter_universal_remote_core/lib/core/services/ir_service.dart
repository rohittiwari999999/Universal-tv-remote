import 'dart:io';
import 'package:flutter/services.dart';
import 'package:ir_sensor_plugin/ir_sensor_plugin.dart';

/// Production-ready Infrared (IR) Blaster Service for Android Devices.
/// Handles hardware verification, carrier frequency validation,
/// NEC protocol pulse modulation, and raw Pronto Hex transmission.
class IrService {
  static const MethodChannel _platformChannel =
      MethodChannel('com.omnitech.universal.tv.remote/consumer_ir');

  /// 1. Verifies if the smartphone has a physical IR emitter hardware component.
  /// Note: iOS devices do NOT have IR emitters, so this always returns false on iOS.
  static Future<bool> hasIrEmitter() async {
    if (!Platform.isAndroid) {
      return false;
    }
    try {
      final bool hasEmitter = await IrSensorPlugin.hasIrEmitter;
      return hasEmitter;
    } on PlatformException catch (e) {
      // Fallback to direct Android ConsumerIrManager check via platform channel
      try {
        final bool result =
            await _platformChannel.invokeMethod('hasIrEmitter') ?? false;
        return result;
      } catch (fallbackError) {
        // Device lacks ConsumerIrManager system service
        return false;
      }
    } catch (_) {
      return false;
    }
  }

  /// 2. Returns supported carrier frequency ranges (e.g. 36000Hz to 40000Hz).
  static Future<List<String>> getCarrierFrequencies() async {
    if (!Platform.isAndroid) return [];
    try {
      final String frequencies = await IrSensorPlugin.getCarrierFrequencies;
      return frequencies.split(';');
    } catch (e) {
      return ['38000Hz (Default standard)'];
    }
  }

  /// 3. Transmits an IR command using Pronto HEX format.
  /// Example Pronto Hex: "0000 006D 0022 0002 0155 00AA 0015 0040 ..."
  static Future<bool> transmitProntoHex({
    required String prontoHex,
    int carrierFrequency = 38000,
  }) async {
    final bool available = await hasIrEmitter();
    if (!available) {
      throw UnsupportedError(
          'This device does not have an Infrared (IR) Blaster.');
    }

    try {
      // Method A: Using ir_sensor_plugin
      final String cleanHex = prontoHex.trim().replaceAll(RegExp(r'\s+'), ' ');
      final String response =
          await IrSensorPlugin.transmitString(pattern: cleanHex);
      return response == 'Emitting' || response == 'Success';
    } catch (pluginError) {
      // Method B: Convert Pronto HEX string to microsecond pulse array and invoke ConsumerIrManager
      try {
        final List<int> pattern = prontoToMicroseconds(prontoHex);
        return await transmitPattern(
          carrierFrequency: carrierFrequency,
          pattern: pattern,
        );
      } catch (e) {
        rethrow;
      }
    }
  }

  /// 4. Transmits an IR command using standard 32-bit NEC Hex (e.g., "0x20DF10EF").
  /// Generates the exact microsecond timings for 38kHz NEC modulation:
  /// - Leader: 9000µs Mark, 4500µs Space
  /// - Logical 0: 560µs Mark, 560µs Space
  /// - Logical 1: 560µs Mark, 1690µs Space
  /// - Stop bit: 560µs Mark
  static Future<bool> transmitNecHex({
    required String necHex,
    int carrierFrequency = 38000,
  }) async {
    final List<int> pattern = necHexToMicroseconds(necHex);
    return await transmitPattern(
      carrierFrequency: carrierFrequency,
      pattern: pattern,
    );
  }

  /// 5. Low-level invocation of Android ConsumerIrManager.transmit(carrierFrequency, pattern)
  static Future<bool> transmitPattern({
    required int carrierFrequency,
    required List<int> pattern,
  }) async {
    if (!Platform.isAndroid) return false;
    try {
      final bool? success = await _platformChannel.invokeMethod('transmit', {
        'frequency': carrierFrequency,
        'pattern': pattern,
      });
      return success ?? true;
    } on PlatformException {
      // Fallback to ir_sensor_plugin list transmission
      try {
        final String patternStr = pattern.join(',');
        await IrSensorPlugin.transmitListInt(list: pattern);
        return true;
      } catch (_) {
        return false;
      }
    }
  }

  /// Helper: Decodes standard 32-bit NEC Hex string into microsecond timing array
  static List<int> necHexToMicroseconds(String hexStr) {
    String clean = hexStr.toLowerCase().replaceAll('0x', '').padLeft(8, '0');
    int value = int.parse(clean, radix: 16);

    List<int> pulses = [];
    // 1. Leader Code
    pulses.add(9000); // 9ms Mark
    pulses.add(4500); // 4.5ms Space

    // 2. 32 Data bits (Address, ~Address, Command, ~Command) transmitted LSB first
    for (int i = 0; i < 32; i++) {
      bool isOne = ((value >> (31 - i)) & 1) == 1;
      pulses.add(560); // Mark
      if (isOne) {
        pulses.add(1690); // Space for '1'
      } else {
        pulses.add(560); // Space for '0'
      }
    }

    // 3. Stop Bit
    pulses.add(560);
    return pulses;
  }

  /// Helper: Converts raw Pronto Hex string into microsecond duration array
  static List<int> prontoToMicroseconds(String pronto) {
    List<String> tokens =
        pronto.trim().split(RegExp(r'\s+')).where((s) => s.isNotEmpty).toList();
    if (tokens.length < 4) {
      throw FormatException('Invalid Pronto HEX format: too few tokens.');
    }

    // Token 0: preamble (0000 = raw learned code)
    // Token 1: frequency divisor: frequency = 1000000 / (divisor * 0.241246)
    int freqDivisor = int.parse(tokens[1], radix: 16);
    double unitMicros = freqDivisor * 0.241246;

    List<int> microseconds = [];
    for (int i = 4; i < tokens.length; i++) {
      int count = int.parse(tokens[i], radix: 16);
      int duration = (count * unitMicros).round();
      microseconds.add(duration);
    }
    return microseconds;
  }
}
