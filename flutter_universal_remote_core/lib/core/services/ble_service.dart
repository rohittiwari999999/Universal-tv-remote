import 'dart:async';
import 'dart:io';
import 'package:flutter_blue_plus/flutter_blue_plus.dart';

/// TV BLE Command Payload Structure
class BleTvCommand {
  final String serviceUuid;
  final String characteristicUuid;
  final List<int> bytes;

  const BleTvCommand({
    required this.serviceUuid,
    required this.characteristicUuid,
    required this.bytes,
  });
}

/// Production-ready Bluetooth Low Energy (BLE) Service for Smart TVs & Remotes.
/// Compatible with iOS & Android using flutter_blue_plus.
class BleTvService {
  BluetoothDevice? _connectedDevice;
  BluetoothCharacteristic? _writeCharacteristic;
  StreamSubscription<BluetoothConnectionState>? _connectionStateSub;

  BluetoothDevice? get connectedDevice => _connectedDevice;
  bool get isConnected => _connectedDevice != null;

  /// Known Smart TV GATT Service UUIDs
  static final List<Guid> knownTvServiceUuids = [
    Guid("00001812-0000-1000-8000-00805f9b34fb"), // HID (Human Interface Device)
    Guid("0000ffe0-0000-1000-8000-00805f9b34fb"), // Android TV / Xiaomi Remote
    Guid("0000fe95-0000-1000-8000-00805f9b34fb"), // Mi Smart TV / BLE Beacon
    Guid("0000fff0-0000-1000-8000-00805f9b34fb"), // Generic Chinese / Indian Smart TV
    Guid("180a"), // Device Information Service
  ];

  /// 1. Initialize & verify Bluetooth adapter state
  Future<bool> isBluetoothAvailable() async {
    final adapterState = await FlutterBluePlus.adapterState.first;
    return adapterState == BluetoothAdapterState.on;
  }

  /// 2. Scan for nearby Smart TVs
  /// Returns a stream of ScanResult containing device names, RSSI, and advertised UUIDs
  Stream<List<ScanResult>> scanForSmartTvs({Duration timeout = const Duration(seconds: 12)}) {
    // Start scan with targeted filters or open discovery
    FlutterBluePlus.startScan(
      timeout: timeout,
      androidUsesFineLocation: false, // Important for Android 12+ neverForLocation compliance
    );

    return FlutterBluePlus.scanResults.map((results) {
      // Filter results: prioritize named devices or known TV profiles
      return results.where((result) {
        final name = result.device.platformName.toLowerCase();
        final advName = result.advertisementData.advName.toLowerCase();
        final target = name.isNotEmpty ? name : advName;

        if (target.isEmpty) return false;

        // Common TV keywords (Global & Indian brands)
        return target.contains('tv') ||
            target.contains('samsung') ||
            target.contains('lg') ||
            target.contains('sony') ||
            target.contains('bravia') ||
            target.contains('mi') ||
            target.contains('xiaomi') ||
            target.contains('vu') ||
            target.contains('tcl') ||
            target.contains('thomson') ||
            target.contains('onida') ||
            target.contains('micromax') ||
            target.contains('realme') ||
            target.contains('oneplus');
      }).toList();
    });
  }

  /// 3. Pair and establish connection with a selected TV device
  Future<bool> connectAndPair(BluetoothDevice device) async {
    try {
      // Stop ongoing scan to optimize radio bandwidth
      if (FlutterBluePlus.isScanningNow) {
        await FlutterBluePlus.stopScan();
      }

      // Connect with autoConnect: false and reasonable timeout
      await device.connect(
        timeout: const Duration(seconds: 15),
        autoConnect: false,
      );

      _connectedDevice = device;

      // Listen for unexpected disconnects
      _connectionStateSub = device.connectionState.listen((state) {
        if (state == BluetoothConnectionState.disconnected) {
          _connectedDevice = null;
          _writeCharacteristic = null;
        }
      });

      // Negotiate higher MTU for low latency key transfers (Android only)
      if (Platform.isAndroid) {
        try {
          await device.requestMtu(256);
        } catch (_) {
          // Standard 23-byte MTU fallback is acceptable
        }
      }

      // Discover GATT services and locate writable remote characteristic
      final List<BluetoothService> services = await device.discoverServices();
      for (final service in services) {
        for (final characteristic in service.characteristics) {
          // Look for writable characteristic (write or writeWithoutResponse)
          if (characteristic.properties.write ||
              characteristic.properties.writeWithoutResponse) {
            _writeCharacteristic = characteristic;
            return true;
          }
        }
      }

      return _writeCharacteristic != null;
    } catch (e) {
      await disconnect();
      rethrow;
    }
  }

  /// 4. Transmit a key command to the connected Bluetooth TV
  Future<void> sendCommand(List<int> commandBytes) async {
    if (_connectedDevice == null || _writeCharacteristic == null) {
      throw StateError("No Bluetooth TV is currently connected.");
    }

    try {
      final withoutResponse =
          _writeCharacteristic!.properties.writeWithoutResponse;
      await _writeCharacteristic!.write(
        commandBytes,
        withoutResponse: withoutResponse,
      );
    } catch (e) {
      throw Exception("Failed to send BLE command: $e");
    }
  }

  /// Helper: Send standard HID keyboard / consumer control report
  /// e.g. Consumer report for Power: [0x01, 0x30, 0x00]
  Future<void> sendHidKey(int usageCode) async {
    // 3-byte HID consumer control report: [Report ID, Usage LSB, Usage MSB]
    final payload = [0x02, usageCode & 0xFF, (usageCode >> 8) & 0xFF];
    await sendCommand(payload);
    // Send release key
    await Future.delayed(const Duration(milliseconds: 50));
    await sendCommand([0x02, 0x00, 0x00]);
  }

  /// 5. Gracefully disconnect
  Future<void> disconnect() async {
    await _connectionStateSub?.cancel();
    _connectionStateSub = null;
    if (_connectedDevice != null) {
      await _connectedDevice!.disconnect();
      _connectedDevice = null;
      _writeCharacteristic = null;
    }
  }
}
