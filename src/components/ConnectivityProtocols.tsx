import React, { useState } from 'react';
import { 
  Radio, Bluetooth, Wifi, RefreshCw, Send, CheckCircle2,
  AlertCircle, ShieldAlert, Cpu, Terminal, Play
} from 'lucide-react';
import { remoteSound } from '../utils/audioFeedback';

export const ConnectivityProtocols: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'IR' | 'BLE' | 'Wi-Fi'>('IR');

  // IR State
  const [irFrequency, setIrFrequency] = useState(38000);
  const [irProtocol, setIrProtocol] = useState<'NEC' | 'RC5' | 'SONY_SIRC'>('NEC');
  const [irHexInput, setIrHexInput] = useState('0xE0E040BF');
  const [irTransmissionStatus, setIrTransmissionStatus] = useState<string | null>(null);

  // BLE State
  const [isScanningBle, setIsScanningBle] = useState(false);
  const [bleDevices, setBleDevices] = useState([
    { id: '1', name: 'Samsung QLED 65" (BLE Remote)', rssi: -58, paired: true },
    { id: '2', name: 'Mi TV 4X 50 (PatchWall BLE)', rssi: -64, paired: false },
    { id: '3', name: 'Vu 55" Masterpiece Google TV', rssi: -72, paired: false },
  ]);
  const [bleCommandByte, setBleCommandByte] = useState('0x01, 0x30, 0x00');
  const [bleLog, setBleLog] = useState<string[]>(['BLE subsystem initialized. Ready to scan.']);

  // Wi-Fi State
  const [isDiscoveringWifi, setIsDiscoveringWifi] = useState(false);
  const [wifiTvs, setWifiTvs] = useState([
    { ip: '192.168.1.104', name: 'Samsung Tizen OS (Smart Hub)', port: 8002, status: 'Connected' },
    { ip: '192.168.1.118', name: 'LG OLED webOS (Living Room)', port: 3000, status: 'Ready' },
    { ip: '192.168.1.122', name: 'Sony Bravia 4K (IP Control)', port: 80, status: 'Ready' },
  ]);
  const [wifiSocketLog, setWifiSocketLog] = useState<string[]>([
    'SSDP M-SEARCH query prepared for 239.255.255.250:1900',
  ]);

  const handleTestIrTransmit = () => {
    remoteSound.playIrChirp();
    setIrTransmissionStatus(`Transmitting ${irFrequency}Hz carrier via ConsumerIrManager...`);
    setTimeout(() => {
      setIrTransmissionStatus(`SUCCESS: Modulated 32-bit ${irProtocol} code ${irHexInput} emitted (67 pulses, 38kHz).`);
    }, 400);
  };

  const handleScanBle = () => {
    setIsScanningBle(true);
    setBleLog(prev => [`[${new Date().toLocaleTimeString()}] Starting BLE scan with neverForLocation flag...`, ...prev]);
    setTimeout(() => {
      setIsScanningBle(false);
      setBleLog(prev => [
        `[${new Date().toLocaleTimeString()}] Found 3 nearby Smart TV GATT targets.`,
        ...prev
      ]);
    }, 1200);
  };

  const handleSendBleCommand = () => {
    remoteSound.playClick();
    setBleLog(prev => [
      `[${new Date().toLocaleTimeString()}] TX GATT Characteristic write: [${bleCommandByte}] to Samsung QLED`,
      ...prev
    ]);
  };

  const handleDiscoverWifi = () => {
    setIsDiscoveringWifi(true);
    setWifiSocketLog(prev => [
      `[${new Date().toLocaleTimeString()}] Broadcasting SSDP M-SEARCH (ST: urn:schemas-upnp-org:device:MediaRenderer:1)...`,
      ...prev
    ]);
    setTimeout(() => {
      setIsDiscoveringWifi(false);
      setWifiSocketLog(prev => [
        `[${new Date().toLocaleTimeString()}] Discovered 3 active Smart TVs on subnet 192.168.1.0/24`,
        ...prev
      ]);
    }, 1000);
  };

  return (
    <div className="flex flex-col gap-6">
      {/* Protocol Selection Switcher */}
      <div className="flex items-center gap-2 p-1.5 bg-slate-900 border border-slate-800 rounded-xl text-xs">
        <button
          onClick={() => setActiveTab('IR')}
          className={`flex-1 flex items-center justify-center gap-2 py-2 px-3 rounded-lg font-medium transition-colors ${
            activeTab === 'IR'
              ? 'bg-red-500/20 text-red-400 border border-red-500/30'
              : 'text-slate-400 hover:text-slate-200'
          }`}
        >
          <Radio className="w-4 h-4" />
          <span>Infrared (IR Blaster)</span>
        </button>

        <button
          onClick={() => setActiveTab('BLE')}
          className={`flex-1 flex items-center justify-center gap-2 py-2 px-3 rounded-lg font-medium transition-colors ${
            activeTab === 'BLE'
              ? 'bg-blue-500/20 text-blue-400 border border-blue-500/30'
              : 'text-slate-400 hover:text-slate-200'
          }`}
        >
          <Bluetooth className="w-4 h-4" />
          <span>Bluetooth Low Energy (BLE)</span>
        </button>

        <button
          onClick={() => setActiveTab('Wi-Fi')}
          className={`flex-1 flex items-center justify-center gap-2 py-2 px-3 rounded-lg font-medium transition-colors ${
            activeTab === 'Wi-Fi'
              ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30'
              : 'text-slate-400 hover:text-slate-200'
          }`}
        >
          <Wifi className="w-4 h-4" />
          <span>Smart Wi-Fi (SSDP/LAN)</span>
        </button>
      </div>

      {/* TAB 1: INFRARED (IR BLASTER) */}
      {activeTab === 'IR' && (
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 space-y-5">
          <div>
            <h3 className="text-sm font-semibold text-white mb-1 flex items-center gap-2">
              <Radio className="w-4 h-4 text-red-400" />
              <span>Consumer Infrared (IR Blaster) Hardware Testing</span>
            </h3>
            <p className="text-xs text-slate-400 leading-relaxed">
              IR pulses are transmitted via Android&apos;s native <code className="text-slate-200 font-mono">ConsumerIrManager</code>. Since iOS devices lack IR transmitters, our architecture automatically detects hardware presence and gracefully falls back to Wi-Fi/Bluetooth.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
            <div>
              <label className="block text-[11px] font-mono text-slate-400 mb-1">
                Carrier Frequency (Hz)
              </label>
              <select
                value={irFrequency}
                onChange={(e) => setIrFrequency(Number(e.target.value))}
                className="w-full bg-slate-950 border border-slate-800 text-xs text-slate-200 rounded-lg p-2.5 font-mono focus:outline-none focus:border-red-500"
              >
                <option value={38000}>38,000 Hz (Standard NEC / Global TVs)</option>
                <option value={36000}>36,000 Hz (Philips RC5 / Onida)</option>
                <option value={40000}>40,000 Hz (Sony Bravia SIRC)</option>
                <option value={56000}>56,000 Hz (High Frequency B&amp;O)</option>
              </select>
            </div>

            <div>
              <label className="block text-[11px] font-mono text-slate-400 mb-1">
                Protocol Encoding
              </label>
              <select
                value={irProtocol}
                onChange={(e) => setIrProtocol(e.target.value as unknown as 'NEC' | 'RC5' | 'SONY_SIRC')}
                className="w-full bg-slate-950 border border-slate-800 text-xs text-slate-200 rounded-lg p-2.5 font-mono focus:outline-none focus:border-red-500"
              >
                <option value="NEC">NEC 32-bit (Samsung, LG, Mi, Vu, TCL)</option>
                <option value="RC5">Philips RC5 (Onida, Older Indian CRT)</option>
                <option value="SONY_SIRC">Sony SIRC (Bravia 12-bit / 15-bit)</option>
              </select>
            </div>

            <div>
              <label className="block text-[11px] font-mono text-slate-400 mb-1">
                Input HEX Code
              </label>
              <div className="flex gap-2">
                <input
                  type="text"
                  value={irHexInput}
                  onChange={(e) => setIrHexInput(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 text-xs text-amber-400 rounded-lg p-2.5 font-mono focus:outline-none focus:border-red-500"
                />
                <button
                  onClick={handleTestIrTransmit}
                  className="px-3 bg-red-600 hover:bg-red-500 text-white rounded-lg text-xs font-medium flex items-center gap-1 transition-colors"
                >
                  <Send className="w-3.5 h-3.5" />
                  <span>Transmit</span>
                </button>
              </div>
            </div>
          </div>

          {irTransmissionStatus && (
            <div className="p-3 bg-slate-950 border border-red-900/50 rounded-xl text-xs font-mono text-slate-300 flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
              <span>{irTransmissionStatus}</span>
            </div>
          )}
        </div>
      )}

      {/* TAB 2: BLUETOOTH LOW ENERGY (BLE) */}
      {activeTab === 'BLE' && (
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 space-y-5">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div>
              <h3 className="text-sm font-semibold text-white mb-1 flex items-center gap-2">
                <Bluetooth className="w-4 h-4 text-blue-400" />
                <span>Bluetooth Low Energy (BLE) Smart TV Controller</span>
              </h3>
              <p className="text-xs text-slate-400 leading-relaxed">
                Uses <code className="text-slate-200 font-mono">flutter_blue_plus</code> with Android 12+ <code className="text-slate-200 font-mono">neverForLocation</code> flag. Works identically on iOS and Android.
              </p>
            </div>

            <button
              onClick={handleScanBle}
              disabled={isScanningBle}
              className="flex items-center gap-2 px-3 py-2 bg-blue-600 hover:bg-blue-500 text-white rounded-lg text-xs font-medium transition-colors disabled:opacity-50"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${isScanningBle ? 'animate-spin' : ''}`} />
              <span>{isScanningBle ? 'Scanning...' : 'Scan Nearby TVs'}</span>
            </button>
          </div>

          {/* Detected Devices Table */}
          <div className="space-y-2">
            {bleDevices.map((dev) => (
              <div
                key={dev.id}
                className="p-3 rounded-xl bg-slate-950 border border-slate-800 flex items-center justify-between"
              >
                <div className="flex items-center gap-3">
                  <Bluetooth className="w-4 h-4 text-blue-400" />
                  <div>
                    <div className="text-xs font-semibold text-white">{dev.name}</div>
                    <div className="text-[10px] font-mono text-slate-500">RSSI: {dev.rssi} dBm · GATT HID Service (0x1812)</div>
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  <span className={`text-[10px] font-mono px-2 py-0.5 rounded ${
                    dev.paired ? 'bg-emerald-950 text-emerald-400 border border-emerald-900/60' : 'bg-slate-800 text-slate-400'
                  }`}>
                    {dev.paired ? 'Paired' : 'Available'}
                  </span>
                  {dev.paired ? (
                    <button
                      onClick={handleSendBleCommand}
                      className="px-2.5 py-1 bg-slate-800 hover:bg-slate-700 text-white rounded text-xs font-medium transition-colors"
                    >
                      Send Key
                    </button>
                  ) : (
                    <button
                      onClick={() => {
                        setBleDevices(prev => prev.map(d => d.id === dev.id ? { ...d, paired: true } : d));
                      }}
                      className="px-2.5 py-1 bg-blue-600 hover:bg-blue-500 text-white rounded text-xs font-medium transition-colors"
                    >
                      Pair
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>

          {/* Command Console */}
          <div className="bg-slate-950 border border-slate-800 rounded-xl p-3">
            <div className="text-[11px] font-mono text-slate-500 mb-1">BLE GATT Communication Log</div>
            <div className="space-y-1 font-mono text-xs text-slate-400 max-h-32 overflow-y-auto">
              {bleLog.map((log, idx) => (
                <div key={idx}>{log}</div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* TAB 3: SMART WI-FI */}
      {activeTab === 'Wi-Fi' && (
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 space-y-5">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div>
              <h3 className="text-sm font-semibold text-white mb-1 flex items-center gap-2">
                <Wifi className="w-4 h-4 text-emerald-400" />
                <span>Smart TV Wi-Fi Network Discovery (SSDP &amp; WebSockets)</span>
              </h3>
              <p className="text-xs text-slate-400 leading-relaxed">
                Sends UPnP SSDP M-SEARCH multicast packets over UDP port 1900, discovering Samsung Tizen, LG webOS, and Sony Bravia on your home Wi-Fi LAN.
              </p>
            </div>

            <button
              onClick={handleDiscoverWifi}
              disabled={isDiscoveringWifi}
              className="flex items-center gap-2 px-3 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg text-xs font-medium transition-colors disabled:opacity-50"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${isDiscoveringWifi ? 'animate-spin' : ''}`} />
              <span>{isDiscoveringWifi ? 'Discovering...' : 'Discover LAN TVs'}</span>
            </button>
          </div>

          {/* Wi-Fi Devices */}
          <div className="space-y-2">
            {wifiTvs.map((tv, idx) => (
              <div
                key={idx}
                className="p-3 rounded-xl bg-slate-950 border border-slate-800 flex items-center justify-between"
              >
                <div className="flex items-center gap-3">
                  <Wifi className="w-4 h-4 text-emerald-400" />
                  <div>
                    <div className="text-xs font-semibold text-white">{tv.name}</div>
                    <div className="text-[10px] font-mono text-slate-500">{tv.ip}:{tv.port} · WebSocket / HTTP REST</div>
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-emerald-950 text-emerald-400 border border-emerald-900/60">
                    {tv.status}
                  </span>
                  <button
                    onClick={() => {
                      remoteSound.playClick();
                      setWifiSocketLog(prev => [
                        `[${new Date().toLocaleTimeString()}] Sent WebSocket payload to ${tv.ip}:${tv.port} -> KEY_POWER`,
                        ...prev
                      ]);
                    }}
                    className="px-2.5 py-1 bg-slate-800 hover:bg-slate-700 text-white rounded text-xs font-medium transition-colors"
                  >
                    Test Ping
                  </button>
                </div>
              </div>
            ))}
          </div>

          {/* Wi-Fi Console */}
          <div className="bg-slate-950 border border-slate-800 rounded-xl p-3">
            <div className="text-[11px] font-mono text-slate-500 mb-1">Local LAN Discovery &amp; Socket Stream</div>
            <div className="space-y-1 font-mono text-xs text-slate-400 max-h-32 overflow-y-auto">
              {wifiSocketLog.map((log, idx) => (
                <div key={idx}>{log}</div>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
