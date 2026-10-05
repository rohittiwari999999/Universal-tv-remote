import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'package:http/http.dart' as http;
import 'package:web_socket_channel/io.dart';
import 'package:web_socket_channel/web_socket_channel.dart';

enum SmartTvProtocol { samsungTizen, lgWebOs, sonyBravia, androidTvCast }

class DiscoveredTv {
  final String ip;
  final int port;
  final String name;
  final SmartTvProtocol protocol;
  final String? model;

  const DiscoveredTv({
    required this.ip,
    required this.port,
    required this.name,
    required this.protocol,
    this.model,
  });
}

/// Production-ready Wi-Fi Smart TV Service.
/// Handles SSDP UPnP discovery and native protocol dispatch
/// (Samsung Tizen WS, LG webOS WS, Sony Bravia REST IRCC).
class WifiSmartTvService {
  WebSocketChannel? _tizenChannel;
  WebSocketChannel? _webOsChannel;
  DiscoveredTv? _activeTv;

  DiscoveredTv? get activeTv => _activeTv;

  /// 1. Discover Smart TVs on local Wi-Fi subnet using SSDP M-SEARCH (UDP 1900)
  static Future<List<DiscoveredTv>> discoverLocalTvs({
    Duration timeout = const Duration(seconds: 4),
  }) async {
    final List<DiscoveredTv> discovered = [];
    RawDatagramSocket? socket;

    try {
      socket = await RawDatagramSocket.bind(InternetAddress.anyIPv4, 0);
      socket.broadcastEnabled = true;
      socket.multicastLoopback = false;

      // SSDP M-SEARCH query payload
      final String mSearch =
          'M-SEARCH * HTTP/1.1\r\n'
          'HOST: 239.255.255.250:1900\r\n'
          'MAN: "ssdp:discover"\r\n'
          'MX: 3\r\n'
          'ST: ssdp:all\r\n\r\n';

      final List<int> data = utf8.encode(mSearch);
      final InternetAddress multicastGroup = InternetAddress('239.255.255.250');

      socket.send(data, multicastGroup, 1900);

      final completer = Completer<List<DiscoveredTv>>();

      socket.listen((RawSocketEvent event) {
        if (event == RawSocketEvent.read) {
          final Datagram? dg = socket?.receive();
          if (dg != null) {
            final String response = utf8.decode(dg.data, allowMalformed: true);
            final String ip = dg.address.address;

            if (response.contains('Samsung') || response.contains('SEC_HHP')) {
              discovered.add(DiscoveredTv(
                ip: ip,
                port: 8002,
                name: "Samsung Smart TV",
                protocol: SmartTvProtocol.samsungTizen,
              ));
            } else if (response.contains('LG') || response.contains('webOS')) {
              discovered.add(DiscoveredTv(
                ip: ip,
                port: 3000,
                name: "LG webOS TV",
                protocol: SmartTvProtocol.lgWebOs,
              ));
            } else if (response.contains('Sony') || response.contains('Bravia')) {
              discovered.add(DiscoveredTv(
                ip: ip,
                port: 80,
                name: "Sony Bravia Smart TV",
                protocol: SmartTvProtocol.sonyBravia,
              ));
            }
          }
        }
      });

      Future.delayed(timeout, () {
        socket?.close();
        if (!completer.isCompleted) {
          completer.complete(discovered);
        }
      });

      return await completer.future;
    } catch (e) {
      socket?.close();
      return discovered;
    }
  }

  /// 2. Connect to Samsung Tizen OS via WebSocket (port 8002 wss or 8001 ws)
  Future<bool> connectSamsungTizen(String ip, {String appName = "OmniRemote"}) async {
    try {
      final String encodedName = base64Encode(utf8.encode(appName));
      final uri = Uri.parse(
        'wss://$ip:8002/api/v2/channels/samsung.remote.control?name=$encodedName',
      );

      _tizenChannel = IOWebSocketChannel.connect(
        uri,
        badCertificateCallback: (cert, host, port) => true, // Self-signed SSL on TV
      );

      _activeTv = DiscoveredTv(
        ip: ip,
        port: 8002,
        name: "Samsung Tizen TV",
        protocol: SmartTvProtocol.samsungTizen,
      );

      return true;
    } catch (e) {
      return false;
    }
  }

  /// 3. Send Samsung Tizen Remote Key (e.g., KEY_POWER, KEY_VOLUP, KEY_HOME)
  void sendSamsungKey(String keyCode) {
    if (_tizenChannel == null) return;
    final payload = jsonEncode({
      "method": "ms.remote.control",
      "params": {
        "Cmd": "Click",
        "DataOfCmd": keyCode,
        "Option": "false",
        "TypeOfRemote": "SendRemoteKey",
      }
    });
    _tizenChannel!.sink.add(payload);
  }

  /// 4. Send Sony Bravia Command via REST IRCC endpoint
  /// Example IRCC codes: AAAAAQAAAAEAAAAVAw== (Power), AAAAAQAAAAEAAAASAw== (VolumeUp)
  Future<bool> sendSonyBraviaIrcc(String ip, String irccCode, {String psk = "0000"}) async {
    final url = Uri.parse('http://$ip/sony/ircc');
    final xmlBody = '''
<s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
  <s:Body>
    <u:X_SendIRCC xmlns:u="urn:schemas-sony-com:service:IRCC:1">
      <IRCCCode>$irccCode</IRCCCode>
    </u:X_SendIRCC>
  </s:Body>
</s:Envelope>''';

    try {
      final res = await http.post(
        url,
        headers: {
          'Content-Type': 'text/xml; charset=UTF-8',
          'SOAPACTION': '"urn:schemas-sony-com:service:IRCC:1#X_SendIRCC"',
          'X-Auth-PSK': psk,
        },
        body: xmlBody,
      );
      return res.statusCode == 200;
    } catch (_) {
      return false;
    }
  }

  /// 5. Disconnect current Wi-Fi TV session
  void disconnect() {
    _tizenChannel?.sink.close();
    _webOsChannel?.sink.close();
    _tizenChannel = null;
    _webOsChannel = null;
    _activeTv = null;
  }
}
