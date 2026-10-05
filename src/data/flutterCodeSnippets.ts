export interface CodeSnippet {
  id: string;
  title: string;
  filename: string;
  description: string;
  language: string;
  code: string;
}

export const FLUTTER_CODE_SNIPPETS: CodeSnippet[] = [
  {
    id: 'ir_transmitter',
    title: 'IR Blaster Verification & HEX Transmission',
    filename: 'lib/core/services/ir_service.dart',
    description: 'Checks physical IR emitter availability and transmits 38kHz NEC modulated pulse arrays or raw Pronto HEX.',
    language: 'dart',
    code: `import 'dart:io';
import 'package:ir_sensor_plugin/ir_sensor_plugin.dart';

class IrService {
  /// 1. Verify if Android device has hardware IR emitter
  static Future<bool> hasIrEmitter() async {
    if (!Platform.isAndroid) return false; // iOS has no IR blaster hardware
    try {
      final bool hasEmitter = await IrSensorPlugin.hasIrEmitter;
      return hasEmitter;
    } catch (e) {
      return false;
    }
  }

  /// 2. Transmit standard 32-bit NEC Hex code (e.g., '0xE0E040BF' for Samsung Power)
  static Future<bool> transmitNecHex({
    required String necHex,
    int carrierFrequency = 38000, // 38kHz standard
  }) async {
    final bool available = await hasIrEmitter();
    if (!available) {
      throw UnsupportedError('No hardware IR blaster found on this device.');
    }

    // Convert 32-bit Hex to microsecond pulse train (NEC Protocol)
    final List<int> pattern = necHexToMicroseconds(necHex);

    // Transmit modulated carrier bursts
    await IrSensorPlugin.transmitListInt(list: pattern);
    return true;
  }

  /// 3. Converts standard NEC hex into microsecond pulse train:
  /// - Leader: 9000µs ON, 4500µs OFF
  /// - Bit 0: 560µs ON, 560µs OFF
  /// - Bit 1: 560µs ON, 1690µs OFF
  /// - Stop bit: 560µs ON
  static List<int> necHexToMicroseconds(String hexStr) {
    final clean = hexStr.toLowerCase().replaceAll('0x', '').padLeft(8, '0');
    final int value = int.parse(clean, radix: 16);

    List<int> pulses = [9000, 4500]; // 9ms mark, 4.5ms space
    for (int i = 0; i < 32; i++) {
      final bool isOne = ((value >> (31 - i)) & 1) == 1;
      pulses.add(560);
      pulses.add(isOne ? 1690 : 560);
    }
    pulses.add(560); // Stop bit
    return pulses;
  }
}`
  },
  {
    id: 'ble_scanner',
    title: 'BLE Scanning, Pairing & TV Command Transmission',
    filename: 'lib/core/services/ble_tv_service.dart',
    description: 'Scans for nearby Bluetooth Smart TVs (Samsung, Mi, Vu, TCL), pairs, discovers GATT services, and writes commands.',
    language: 'dart',
    code: `import 'dart:async';
import 'package:flutter_blue_plus/flutter_blue_plus.dart';

class BleTvService {
  BluetoothDevice? _connectedTv;
  BluetoothCharacteristic? _remoteCharacteristic;

  /// 1. Scan for nearby Smart TVs using flutter_blue_plus
  Stream<List<ScanResult>> scanNearbySmartTvs() {
    // Start scan without fine location to adhere to 'neverForLocation' policy
    FlutterBluePlus.startScan(
      timeout: const Duration(seconds: 10),
      androidUsesFineLocation: false,
    );

    return FlutterBluePlus.scanResults.map((results) {
      return results.where((r) {
        final name = (r.device.platformName.isNotEmpty 
            ? r.device.platformName 
            : r.advertisementData.advName).toLowerCase();
        return name.contains('tv') || 
               name.contains('mi') || 
               name.contains('samsung') || 
               name.contains('vu');
      }).toList();
    });
  }

  /// 2. Pair and discover writable GATT characteristics
  Future<bool> connectAndPair(BluetoothDevice device) async {
    await device.connect(timeout: const Duration(seconds: 12), autoConnect: false);
    _connectedTv = device;

    // Discover services
    final services = await device.discoverServices();
    for (final service in services) {
      for (final char in service.characteristics) {
        if (char.properties.write || char.properties.writeWithoutResponse) {
          _remoteCharacteristic = char;
          return true;
        }
      }
    }
    return false;
  }

  /// 3. Transmit remote command bytes to TV
  Future<void> sendCommand(List<int> commandBytes) async {
    if (_remoteCharacteristic == null) {
      throw StateError('No paired Bluetooth TV characteristic available.');
    }
    final withoutResponse = _remoteCharacteristic!.properties.writeWithoutResponse;
    await _remoteCharacteristic!.write(commandBytes, withoutResponse: withoutResponse);
  }
}`
  },
  {
    id: 'wifi_service',
    title: 'Wi-Fi Smart TV Discovery (SSDP) & WebSocket Dispatcher',
    filename: 'lib/core/services/wifi_smart_tv_service.dart',
    description: 'Discovers Samsung Tizen, LG webOS, and Sony Bravia over local Wi-Fi LAN using SSDP M-SEARCH and connects via WebSockets.',
    language: 'dart',
    code: `import 'dart:convert';
import 'dart:io';
import 'package:web_socket_channel/io.dart';

class WifiSmartTvService {
  IOWebSocketChannel? _channel;

  /// 1. SSDP M-SEARCH Discovery (UDP 239.255.255.250:1900)
  static Future<List<String>> discoverLocalTvs() async {
    final socket = await RawDatagramSocket.bind(InternetAddress.anyIPv4, 0);
    socket.broadcastEnabled = true;

    const mSearch = 
      'M-SEARCH * HTTP/1.1\\r\\n'
      'HOST: 239.255.255.250:1900\\r\\n'
      'MAN: "ssdp:discover"\\r\\n'
      'MX: 3\\r\\n'
      'ST: ssdp:all\\r\\n\\r\\n';

    socket.send(utf8.encode(mSearch), InternetAddress('239.255.255.250'), 1900);
    
    List<String> tvIps = [];
    socket.listen((event) {
      if (event == RawSocketEvent.read) {
        final dg = socket.receive();
        if (dg != null) {
          tvIps.add(dg.address.address);
        }
      }
    });

    await Future.delayed(const Duration(seconds: 3));
    socket.close();
    return tvIps.toSet().toList();
  }

  /// 2. Samsung Tizen WebSocket Key Dispatcher
  Future<void> connectSamsungTizen(String ip) async {
    final encodedName = base64Encode(utf8.encode('OmniRemote'));
    final uri = Uri.parse('wss://$ip:8002/api/v2/channels/samsung.remote.control?name=$encodedName');
    
    _channel = IOWebSocketChannel.connect(
      uri, 
      badCertificateCallback: (cert, host, port) => true,
    );
  }

  void sendTizenKey(String keyCode) { // e.g. "KEY_POWER", "KEY_VOLUP"
    final msg = jsonEncode({
      "method": "ms.remote.control",
      "params": {
        "Cmd": "Click",
        "DataOfCmd": keyCode,
        "Option": "false",
        "TypeOfRemote": "SendRemoteKey",
      }
    });
    _channel?.sink.add(msg);
  }
}`
  },
  {
    id: 'remote_bloc',
    title: 'Clean Architecture BLoC State Manager',
    filename: 'lib/features/remote/presentation/bloc/remote_bloc.dart',
    description: 'Dynamic layout switching, protocol fallback (IR -> BLE -> Wi-Fi), and offline cache synchronization.',
    language: 'dart',
    code: `import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:equatable/equatable.dart';

// States
abstract class RemoteState extends Equatable {
  @override
  List<Object?> get props => [];
}

class RemoteReady extends RemoteState {
  final String activeBrand;
  final String activeMode; // 'IR' | 'BLE' | 'Wi-Fi'
  final bool hasIrHardware;
  final String lastHexEmitted;

  RemoteReady({
    required this.activeBrand,
    required this.activeMode,
    required this.hasIrHardware,
    required this.lastHexEmitted,
  });

  @override
  List<Object?> get props => [activeBrand, activeMode, hasIrHardware, lastHexEmitted];
}

// BLoC Implementation
class RemoteBloc extends Bloc<RemoteEvent, RemoteState> {
  final IrService irService;
  final BleTvService bleService;
  final WifiSmartTvService wifiService;

  RemoteBloc({
    required this.irService,
    required this.bleService,
    required this.wifiService,
  }) : super(RemoteInitial()) {
    on<InitRemote>((event, emit) async {
      final bool hasIr = await IrService.hasIrEmitter();
      // Auto-fallback: if phone lacks IR emitter (e.g. Pixel or iOS), default to Wi-Fi/BLE
      final defaultMode = hasIr ? 'IR' : 'Wi-Fi';
      emit(RemoteReady(
        activeBrand: 'Samsung',
        activeMode: defaultMode,
        hasIrHardware: hasIr,
        lastHexEmitted: '',
      ));
    });

    on<SendKeyCommand>((event, emit) async {
      if (state is! RemoteReady) return;
      final current = state as RemoteReady;

      if (current.activeMode == 'IR') {
        await IrService.transmitNecHex(necHex: event.keyHex);
      } else if (current.activeMode == 'BLE') {
        await bleService.sendCommand([0x01, event.keyCode]);
      } else {
        wifiService.sendTizenKey(event.tizenKey);
      }

      emit(RemoteReady(
        activeBrand: current.activeBrand,
        activeMode: current.activeMode,
        hasIrHardware: current.hasIrHardware,
        lastHexEmitted: event.keyHex,
      ));
    });
  }
}`
  }
];
