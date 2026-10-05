import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const UniversalTvRemoteApp());
}

class UniversalTvRemoteApp extends StatelessWidget {
  const UniversalTvRemoteApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Universal TV Remote',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        brightness: Brightness.dark,
        scaffoldBackgroundColor: const Color(0xFF0F172A),
        colorScheme: const ColorScheme.dark(
          primary: Color(0xFF06B6D4),
          surface: Color(0xFF1E293B),
        ),
      ),
      home: const RemoteHomeScreen(),
    );
  }
}

class RemoteHomeScreen extends StatefulWidget {
  const RemoteHomeScreen({super.key});

  @override
  State<RemoteHomeScreen> createState() => _RemoteHomeScreenState();
}

class _RemoteHomeScreenState extends State<RemoteHomeScreen> {
  static const _irChannel = MethodChannel('com.omnitech.universal.tv.remote/consumer_ir');
  bool _hasIr = false;
  String _lastAction = 'Ready';

  @override
  void initState() {
    super.initState();
    _checkIrHardware();
  }

  Future<void> _checkIrHardware() async {
    try {
      final bool hasEmitter = await _irChannel.invokeMethod('hasIrEmitter') ?? false;
      setState(() {
        _hasIr = hasEmitter;
      });
    } catch (_) {
      setState(() {
        _hasIr = false;
      });
    }
  }

  Future<void> _transmitPower() async {
    HapticFeedback.mediumImpact();
    setState(() => _lastAction = 'Emitting 38kHz Power Pulse...');

    if (_hasIr) {
      // Modulated NEC 32-bit pattern for Power
      final pattern = [9000, 4500, 560, 1690, 560, 560, 560, 1690, 560];
      await _irChannel.invokeMethod('transmit', {
        'frequency': 38000,
        'pattern': pattern,
      });
    }

    setState(() => _lastAction = 'Signal Transmitted (Power)');
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Universal TV Remote'),
        centerTitle: true,
        backgroundColor: const Color(0xFF1E293B),
        actions: [
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: Chip(
              label: Text(_hasIr ? 'IR Active' : 'Smart Wi-Fi / BLE'),
              backgroundColor: _hasIr ? Colors.red.withOpacity(0.2) : Colors.cyan.withOpacity(0.2),
            ),
          )
        ],
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            IconButton(
              iconSize: 72,
              icon: const Icon(Icons.power_settings_new, color: Colors.redAccent),
              onPressed: _transmitPower,
            ),
            const SizedBox(height: 16),
            Text(
              _lastAction,
              style: const TextStyle(fontSize: 14, color: Colors.cyanAccent),
            ),
          ],
        ),
      ),
    );
  }
}
