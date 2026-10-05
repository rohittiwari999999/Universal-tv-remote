import React, { useState } from 'react';
import { 
  Layers, Package, Code2, Copy, Check, Cpu, Radio, Bluetooth, Wifi,
  Database, Shield, ChevronRight, CheckCircle2
} from 'lucide-react';
import { FLUTTER_CODE_SNIPPETS, CodeSnippet } from '../data/flutterCodeSnippets';

export const ArchitectureGuide: React.FC = () => {
  const [selectedSnippet, setSelectedSnippet] = useState<CodeSnippet>(FLUTTER_CODE_SNIPPETS[0]);
  const [copied, setCopied] = useState(false);

  const handleCopy = () => {
    navigator.clipboard.writeText(selectedSnippet.code);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="flex flex-col gap-6">
      {/* Architecture Overview Diagram & Explanation */}
      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5">
        <div className="flex items-center gap-2 mb-2">
          <Layers className="w-5 h-5 text-cyan-400" />
          <h2 className="text-base font-semibold text-white">
            Universal TV Remote Flutter System Architecture
          </h2>
        </div>
        <p className="text-xs text-slate-400 max-w-3xl leading-relaxed mb-5">
          To achieve production performance across millions of devices, we use Clean Architecture combined with BLoC state management and a dynamic hardware protocol abstraction layer.
        </p>

        {/* Visual Clean Architecture Flow */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-3 text-xs">
          <div className="bg-slate-950 p-3.5 rounded-xl border border-slate-800 flex flex-col justify-between">
            <div>
              <div className="text-[10px] font-mono text-cyan-400 uppercase tracking-wider mb-1">
                Presentation Layer
              </div>
              <div className="font-semibold text-white mb-1.5">
                Dynamic Remote UI &amp; BLoC
              </div>
              <p className="text-slate-400 text-[11px] leading-relaxed">
                Adapts virtual buttons based on selected brand (Mi PatchWall vs Samsung Rockers vs Indian Numpad). Dispatches user taps as semantic key events.
              </p>
            </div>
            <div className="mt-3 pt-2 border-t border-slate-800 text-[10px] font-mono text-slate-500">
              flutter_bloc · flutter_vibrate
            </div>
          </div>

          <div className="bg-slate-950 p-3.5 rounded-xl border border-slate-800 flex flex-col justify-between">
            <div>
              <div className="text-[10px] font-mono text-indigo-400 uppercase tracking-wider mb-1">
                Domain / Routing
              </div>
              <div className="font-semibold text-white mb-1.5">
                Protocol Router &amp; Fallback
              </div>
              <p className="text-slate-400 text-[11px] leading-relaxed">
                Determines device capabilities: if hardware IR emitter is present, sends 38kHz NEC pulses; if absent (iPhone or Pixel), routes to BLE or Wi-Fi.
              </p>
            </div>
            <div className="mt-3 pt-2 border-t border-slate-800 text-[10px] font-mono text-slate-500">
              Auto Protocol Negotiation
            </div>
          </div>

          <div className="bg-slate-950 p-3.5 rounded-xl border border-slate-800 flex flex-col justify-between">
            <div>
              <div className="text-[10px] font-mono text-emerald-400 uppercase tracking-wider mb-1">
                Hardware &amp; Transmitters
              </div>
              <div className="font-semibold text-white mb-1.5">
                IR, BLE &amp; Wi-Fi Transmitters
              </div>
              <p className="text-slate-400 text-[11px] leading-relaxed">
                ConsumerIrManager JNI calls, GATT writes via flutter_blue_plus, and SSDP UPnP / WebSockets for Samsung Tizen &amp; LG webOS.
              </p>
            </div>
            <div className="mt-3 pt-2 border-t border-slate-800 text-[10px] font-mono text-slate-500">
              ir_sensor · flutter_blue_plus
            </div>
          </div>

          <div className="bg-slate-950 p-3.5 rounded-xl border border-slate-800 flex flex-col justify-between">
            <div>
              <div className="text-[10px] font-mono text-amber-400 uppercase tracking-wider mb-1">
                Data &amp; Persistence
              </div>
              <div className="font-semibold text-white mb-1.5">
                Firestore &amp; Offline Cache
              </div>
              <p className="text-slate-400 text-[11px] leading-relaxed">
                Syncs massive database of IR Hex codes and TV profiles from Firestore; stores in local Hive box for zero-latency offline operation.
              </p>
            </div>
            <div className="mt-3 pt-2 border-t border-slate-800 text-[10px] font-mono text-slate-500">
              cloud_firestore · hive_flutter
            </div>
          </div>
        </div>
      </div>

      {/* Recommended Packages Matrix */}
      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5">
        <div className="flex items-center gap-2 mb-3">
          <Package className="w-5 h-5 text-indigo-400" />
          <h3 className="text-sm font-semibold text-white">
            Recommended Flutter Production Packages (Tested &amp; Verified)
          </h3>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs font-mono">
            <thead>
              <tr className="border-b border-slate-800 text-slate-400">
                <th className="pb-2.5 font-medium">Package</th>
                <th className="pb-2.5 font-medium">Purpose</th>
                <th className="pb-2.5 font-medium">Platform</th>
                <th className="pb-2.5 font-medium">Why This Is Chosen</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 text-slate-300">
              <tr>
                <td className="py-2.5 font-semibold text-cyan-400">ir_sensor_plugin: ^0.1.0</td>
                <td className="py-2.5">Infrared Blaster Control</td>
                <td className="py-2.5 text-slate-400">Android Only</td>
                <td className="py-2.5 text-slate-400">Direct JNI integration with Android <code className="text-slate-200">ConsumerIrManager</code>. Supports raw microsecond timing arrays.</td>
              </tr>
              <tr>
                <td className="py-2.5 font-semibold text-blue-400">flutter_blue_plus: ^1.34.5</td>
                <td className="py-2.5">Bluetooth Low Energy (BLE)</td>
                <td className="py-2.5 text-slate-400">iOS &amp; Android</td>
                <td className="py-2.5 text-slate-400">Fast connection handshake, GATT characteristic discovery, compliant with Android 12+ <code className="text-slate-200">neverForLocation</code>.</td>
              </tr>
              <tr>
                <td className="py-2.5 font-semibold text-emerald-400">web_socket_channel: ^2.4.5</td>
                <td className="py-2.5">Samsung Tizen &amp; LG webOS</td>
                <td className="py-2.5 text-slate-400">Cross-Platform</td>
                <td className="py-2.5 text-slate-400">Low-latency JSON-RPC key dispatch over local Wi-Fi LAN sockets (ports 8001/8002 and 3000).</td>
              </tr>
              <tr>
                <td className="py-2.5 font-semibold text-amber-400">cloud_firestore: ^5.4.4</td>
                <td className="py-2.5">Remote Codes Cloud DB</td>
                <td className="py-2.5 text-slate-400">Cross-Platform</td>
                <td className="py-2.5 text-slate-400">Stores global &amp; Indian TV HEX libraries dynamically without bloating client APK binary size.</td>
              </tr>
              <tr>
                <td className="py-2.5 font-semibold text-purple-400">hive_flutter: ^1.1.0</td>
                <td className="py-2.5">Local Offline Fast Storage</td>
                <td className="py-2.5 text-slate-400">Cross-Platform</td>
                <td className="py-2.5 text-slate-400">Pure Dart lightweight key-value store. Guarantees remote operates seamlessly when internet is disconnected.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      {/* Production Dart Code Snippet Viewer */}
      <div className="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden">
        {/* Header Tabs */}
        <div className="p-3 bg-slate-950 border-b border-slate-800 flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-1.5 overflow-x-auto">
            {FLUTTER_CODE_SNIPPETS.map(snippet => (
              <button
                key={snippet.id}
                onClick={() => setSelectedSnippet(snippet)}
                className={`px-3 py-1.5 rounded-lg text-xs font-mono font-medium transition-colors whitespace-nowrap ${
                  selectedSnippet.id === snippet.id
                    ? 'bg-slate-800 text-white shadow-sm'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                {snippet.filename.split('/').pop()}
              </button>
            ))}
          </div>

          <button
            onClick={handleCopy}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-xs font-medium text-slate-200 transition-colors"
          >
            {copied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
            <span>{copied ? 'Copied to Clipboard' : 'Copy Dart Code'}</span>
          </button>
        </div>

        {/* Snippet Details */}
        <div className="p-4 border-b border-slate-800/60 bg-slate-950/40">
          <div className="flex items-center gap-2 mb-1">
            <Code2 className="w-4 h-4 text-cyan-400" />
            <h4 className="text-xs font-semibold text-white">
              {selectedSnippet.title}
            </h4>
          </div>
          <p className="text-[11px] text-slate-400">
            {selectedSnippet.description} · <span className="font-mono text-slate-500">{selectedSnippet.filename}</span>
          </p>
        </div>

        {/* Code Content */}
        <div className="p-4 bg-slate-950 font-mono text-xs text-slate-300 overflow-x-auto max-h-[500px] leading-relaxed whitespace-pre select-all">
          {selectedSnippet.code}
        </div>
      </div>
    </div>
  );
};
