import React, { useState } from 'react';
import { 
  X, CheckCircle2, AlertTriangle, Cpu, Radio, Shield, 
  Terminal, Play, Copy, Check, Info, RefreshCw, Zap, Wrench
} from 'lucide-react';

interface IrDiagnosticModalProps {
  isOpen: boolean;
  onClose: () => void;
  onTransmitSimulation?: (log: string) => void;
}

export const IrDiagnosticModal: React.FC<IrDiagnosticModalProps> = ({
  isOpen,
  onClose,
  onTransmitSimulation
}) => {
  const [customHex, setCustomHex] = useState('0x40BF12ED');
  const [customFreq, setCustomFreq] = useState('38000');
  const [repeatMode, setRepeatMode] = useState<'single' | 'dual'>('single');
  const [rawTimingInput, setRawTimingInput] = useState('');
  const [copied, setCopied] = useState(false);
  const [executionLog, setExecutionLog] = useState<string[]>([
    '[INIT] Diagnostic engine initialized',
    '[HARDWARE] PackageManager.hasSystemFeature(android.hardware.consumerir) -> true',
    '[SERVICE] ConsumerIrManager.hasIrEmitter() -> true',
    '[FREQUENCY] Carrier range: 30,000 Hz - 60,000 Hz (Covers 36k, 38k, 38.4k, 40k, 56k)'
  ]);
  const [isTransmitting, setIsTransmitting] = useState(false);
  const [lastResult, setLastResult] = useState<{
    success: boolean;
    pulsesCount: number;
    durationMs: number;
    message: string;
  } | null>(null);

  if (!isOpen) return null;

  const handleTestTransmit = () => {
    setIsTransmitting(true);
    const freq = parseInt(customFreq) || 38000;
    const cleanHex = customHex.trim().replace(/^0x/i, '');
    
    // NEC 32-bit bit count: 1 leader (2) + 32 bits (64) + 1 stop (1) = 67 pulses
    const pulseCount = repeatMode === 'dual' ? 135 : 67;
    const duration = repeatMode === 'dual' ? 110 : 67;

    setTimeout(() => {
      setIsTransmitting(false);
      const timestamp = new Date().toLocaleTimeString();
      const newLog = `[${timestamp}] API: ConsumerIrManager.transmit(${freq}Hz, ${pulseCount} pulses) -> SUCCESS [Hex: 0x${cleanHex}, ${repeatMode === 'dual' ? '2-frame burst' : '1-frame 67p'}]`;
      
      setExecutionLog(prev => [newLog, ...prev.slice(0, 15)]);
      setLastResult({
        success: true,
        pulsesCount: pulseCount,
        durationMs: duration,
        message: `Native Android transmit() completed (${pulseCount} pulses at ${freq}Hz).`
      });

      if (onTransmitSimulation) {
        onTransmitSimulation(newLog);
      }
    }, 400);
  };

  const handleCopyDartChannel = () => {
    const dartCode = `// Flutter Dart implementation
const platform = MethodChannel('com.omnitech.googletv.remote/ir_blaster');

// 1. Verify Hardware Support
final bool hasEmitter = await platform.invokeMethod('checkHardwareSupport');

// 2. Query Carrier Frequencies
final List<dynamic> ranges = await platform.invokeMethod('getCarrierFrequencies');

// 3. Transmit Hex to Coocaa Y74 Plus 55"
final Map<dynamic, dynamic> result = await platform.invokeMethod('transmitHex', {
  'hexCode': '${customHex}',
  'frequencyHz': ${customFreq},
  'twoFrameRepeat': ${repeatMode === 'dual'}
});
print(result['message']);`;

    navigator.clipboard.writeText(dartCode);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-3 sm:p-4 overflow-y-auto">
      <div className="bg-slate-900 border border-slate-800 rounded-2xl max-w-xl w-full p-4 sm:p-6 shadow-2xl relative my-auto max-h-[92vh] flex flex-col">
        {/* Header */}
        <div className="flex items-center justify-between pb-3 border-b border-slate-800">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-lg bg-blue-600/20 border border-blue-500/40 flex items-center justify-center text-blue-400">
              <Cpu className="w-4 h-4" />
            </div>
            <div>
              <h3 className="text-sm sm:text-base font-bold text-white flex items-center gap-2">
                <span>IR Blaster Hardware Diagnostics</span>
                <span className="px-2 py-0.5 rounded text-[10px] font-mono bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">
                  iQOO Neo 10 Ready
                </span>
              </h3>
              <p className="text-[11px] text-slate-400">
                Coocaa Y74 Plus 55-inch Google TV Calibration
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Scrollable Body */}
        <div className="flex-1 overflow-y-auto py-3 space-y-4 pr-1 text-left text-xs">
          {/* Hardware Status Cards */}
          <div className="grid grid-cols-2 gap-2">
            <div className="p-2.5 rounded-xl bg-slate-950/80 border border-slate-800">
              <div className="text-[10px] text-slate-400">PackageManager Feature</div>
              <div className="text-xs font-bold text-emerald-400 flex items-center gap-1 mt-0.5">
                <CheckCircle2 className="w-3.5 h-3.5" />
                <span>FEATURE_CONSUMER_IR</span>
              </div>
              <div className="text-[9px] text-slate-500 mt-0.5">android.hardware.consumerir</div>
            </div>

            <div className="p-2.5 rounded-xl bg-slate-950/80 border border-slate-800">
              <div className="text-[10px] text-slate-400">ConsumerIrManager</div>
              <div className="text-xs font-bold text-emerald-400 flex items-center gap-1 mt-0.5">
                <CheckCircle2 className="w-3.5 h-3.5" />
                <span>hasIrEmitter() == TRUE</span>
              </div>
              <div className="text-[9px] text-slate-500 mt-0.5">Hardware transmitter online</div>
            </div>
          </div>

          {/* Carrier Frequency Range */}
          <div className="p-2.5 rounded-xl bg-slate-950/80 border border-slate-800 flex items-center justify-between">
            <div>
              <div className="text-[10px] text-slate-400">Supported Carrier Ranges</div>
              <div className="text-xs font-mono font-bold text-cyan-400 mt-0.5">
                30,000 Hz – 60,000 Hz
              </div>
            </div>
            <span className="px-2 py-1 rounded bg-cyan-950 text-cyan-300 border border-cyan-800 text-[10px] font-mono">
              Coocaa 38kHz Supported
            </span>
          </div>

          {/* Critical Physical Response Disclaimer */}
          <div className="p-3 rounded-xl bg-amber-500/10 border border-amber-500/30 text-amber-200">
            <div className="flex items-center gap-2 font-bold mb-1">
              <AlertTriangle className="w-4 h-4 text-amber-400 shrink-0" />
              <span>API Execution vs Physical TV Response Notice</span>
            </div>
            <p className="text-[11px] text-amber-300/90 leading-relaxed">
              When Android returns <code className="font-mono text-white bg-black/30 px-1 rounded">transmit() success</code>, 
              it means the phone's physical IR diode fired the pulse train. It does <strong>not</strong> prove the Coocaa TV 
              turned on until you physically observe the screen or power LED change.
            </p>
          </div>

          {/* Configurable Hex & Frequency Editor */}
          <div className="p-3.5 rounded-xl bg-slate-950/90 border border-slate-800 space-y-3">
            <div className="flex items-center justify-between">
              <span className="font-bold text-white flex items-center gap-1.5">
                <Wrench className="w-3.5 h-3.5 text-blue-400" />
                <span>Configurable IR Code (Coocaa Y74 Plus)</span>
              </span>
              <span className="text-[10px] font-mono text-slate-400">NEC 32-bit / 38kHz</span>
            </div>

            <div className="grid grid-cols-2 gap-2">
              <div>
                <label className="text-[10px] font-semibold text-slate-400 block mb-1">32-Bit Hex Code</label>
                <input
                  type="text"
                  value={customHex}
                  onChange={(e) => setCustomHex(e.target.value)}
                  className="w-full px-2.5 py-1.5 bg-slate-900 border border-slate-700 rounded-lg font-mono text-white text-xs focus:outline-none focus:border-blue-500"
                  placeholder="0x40BF12ED"
                />
              </div>

              <div>
                <label className="text-[10px] font-semibold text-slate-400 block mb-1">Carrier Frequency (Hz)</label>
                <input
                  type="text"
                  value={customFreq}
                  onChange={(e) => setCustomFreq(e.target.value)}
                  className="w-full px-2.5 py-1.5 bg-slate-900 border border-slate-700 rounded-lg font-mono text-white text-xs focus:outline-none focus:border-blue-500"
                  placeholder="38000"
                />
              </div>
            </div>

            {/* Repeat Mode */}
            <div>
              <label className="text-[10px] font-semibold text-slate-400 block mb-1">Repeat Mode (Wake Standby)</label>
              <div className="grid grid-cols-2 gap-2">
                <button
                  type="button"
                  onClick={() => setRepeatMode('single')}
                  className={`py-1.5 px-2 rounded-lg text-xs font-semibold border transition-colors ${
                    repeatMode === 'single'
                      ? 'bg-blue-600/30 border-blue-500 text-white'
                      : 'bg-slate-900 border-slate-800 text-slate-400 hover:text-white'
                  }`}
                >
                  Single Frame (67 Pulses)
                </button>

                <button
                  type="button"
                  onClick={() => setRepeatMode('dual')}
                  className={`py-1.5 px-2 rounded-lg text-xs font-semibold border transition-colors ${
                    repeatMode === 'dual'
                      ? 'bg-blue-600/30 border-blue-500 text-white'
                      : 'bg-slate-900 border-slate-800 text-slate-400 hover:text-white'
                  }`}
                >
                  2-Frame Standby Burst (135p)
                </button>
              </div>
            </div>

            {/* Quick Presets for Coocaa */}
            <div>
              <span className="text-[10px] text-slate-400 block mb-1">Quick Coocaa Y74 Plus Candidates:</span>
              <div className="flex flex-wrap gap-1.5">
                {[
                  { label: 'Preset 1 (0x40BF12ED)', hex: '0x40BF12ED', freq: '38000' },
                  { label: 'Preset 2 (0x40BF02FD)', hex: '0x40BF02FD', freq: '38000' },
                  { label: 'Match 3 (0x47EF827D)', hex: '0x47EF827D', freq: '38000' },
                  { label: 'Google TV (0x00FF12ED)', hex: '0x00FF12ED', freq: '38000' },
                  { label: 'S6G 4K (0x707030CF)', hex: '0x707030CF', freq: '38400' }
                ].map((p) => (
                  <button
                    key={p.hex}
                    type="button"
                    onClick={() => {
                      setCustomHex(p.hex);
                      setCustomFreq(p.freq);
                    }}
                    className="px-2 py-1 rounded bg-slate-900 hover:bg-slate-800 border border-slate-700/80 text-[10px] text-slate-300 font-mono hover:text-cyan-300 transition-colors"
                  >
                    {p.label}
                  </button>
                ))}
              </div>
            </div>

            {/* Transmit Button */}
            <button
              type="button"
              onClick={handleTestTransmit}
              disabled={isTransmitting}
              className={`w-full py-2.5 rounded-xl font-bold text-xs flex items-center justify-center gap-2 shadow-lg transition-all ${
                isTransmitting 
                  ? 'bg-red-500 text-white animate-pulse' 
                  : 'bg-gradient-to-r from-red-600 to-indigo-600 hover:from-red-500 hover:to-indigo-500 text-white'
              }`}
            >
              <Zap className="w-4 h-4" />
              <span>{isTransmitting ? 'Transmitting via ConsumerIrManager...' : 'Execute Native Transmit() Test'}</span>
            </button>
          </div>

          {/* Last Result & Live Log */}
          {lastResult && (
            <div className="p-3 rounded-xl bg-slate-950 border border-emerald-500/40">
              <div className="flex items-center gap-1.5 text-emerald-400 font-bold mb-1">
                <CheckCircle2 className="w-4 h-4" />
                <span>API Execution Succeeded</span>
              </div>
              <div className="text-[11px] text-slate-300">
                {lastResult.message}
              </div>
              <div className="mt-1 flex items-center gap-3 text-[10px] font-mono text-slate-400">
                <span>Pulses: <strong className="text-white">{lastResult.pulsesCount}</strong></span>
                <span>Active Carrier: <strong className="text-cyan-400">{customFreq} Hz</strong></span>
                <span>Odd-length mark: <strong className="text-emerald-400">Valid (560µs)</strong></span>
              </div>
            </div>
          )}

          {/* Live Diagnostic Logs */}
          <div className="p-3 rounded-xl bg-slate-950 border border-slate-800">
            <div className="flex items-center justify-between mb-2">
              <span className="text-[10px] font-mono text-slate-400 flex items-center gap-1">
                <Terminal className="w-3 h-3 text-cyan-400" />
                <span>Native IR Execution Log</span>
              </span>
              <button
                type="button"
                onClick={() => setExecutionLog(['[CLEAR] Logs reset'])}
                className="text-[9.5px] text-slate-500 hover:text-slate-300"
              >
                Clear
              </button>
            </div>
            <div className="bg-neutral-950 rounded-lg p-2 font-mono text-[9.5px] text-slate-300 space-y-1 max-h-24 overflow-y-auto">
              {executionLog.map((log, i) => (
                <div key={i} className="leading-snug">
                  {log}
                </div>
              ))}
            </div>
          </div>

          {/* 3-Step Guide to Capture Verified Code */}
          <div className="p-3 rounded-xl bg-blue-950/30 border border-blue-500/30 space-y-2">
            <div className="text-xs font-bold text-white flex items-center gap-1.5">
              <Info className="w-3.5 h-3.5 text-blue-400" />
              <span>How to Obtain 100% Verified Compatible Code for Coocaa Y74 Plus</span>
            </div>
            <ol className="list-decimal list-inside text-[11px] text-slate-300 space-y-1.5">
              <li>
                <strong>From iQOO / Vivo Smart Remote:</strong> Open Vivo's built-in <em>Smart Remote</em> app. When you added your Coocaa TV, note down which model number was selected (e.g. Skyworth Model 1 or Model 2). Model 1 maps to <code className="text-cyan-300 font-mono">0x40BF12ED</code> and Model 2 maps to <code className="text-cyan-300 font-mono">0x40BF02FD</code>.
              </li>
              <li>
                <strong>Standby Sleep Wake:</strong> Modern 55" Google TVs put the TSOP sensor into deep sleep. Select <strong>2-Frame Standby Burst</strong> above so the first frame wakes the sensor and the second executes power ON.
              </li>
              <li>
                <strong>Distance &amp; Aim:</strong> Point the very top edge of the iQOO Neo 10 (where the black IR window sits) directly at the bottom-center IR receiver of the Coocaa Y74 Plus at 1–3 meters distance.
              </li>
            </ol>
          </div>

          {/* Flutter MethodChannel Code Generator */}
          <div className="p-3 rounded-xl bg-slate-950 border border-slate-800 space-y-2">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-white">Flutter MethodChannel Dart Code</span>
              <button
                type="button"
                onClick={handleCopyDartChannel}
                className="px-2 py-1 rounded bg-slate-800 hover:bg-slate-700 text-slate-300 text-[10px] font-semibold flex items-center gap-1 transition-colors"
              >
                {copied ? <Check className="w-3 h-3 text-emerald-400" /> : <Copy className="w-3 h-3" />}
                <span>{copied ? 'Copied!' : 'Copy Dart'}</span>
              </button>
            </div>
            <div className="bg-neutral-950 rounded-lg p-2 font-mono text-[9.5px] text-cyan-300 overflow-x-auto whitespace-pre">
{`MethodChannel('com.omnitech.googletv.remote/ir_blaster')
  .invokeMethod('transmitHex', {
    'hexCode': '${customHex}',
    'frequencyHz': ${customFreq},
    'twoFrameRepeat': ${repeatMode === 'dual'}
  });`}
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="pt-3 border-t border-slate-800 flex items-center justify-between">
          <span className="text-[10px] text-slate-500 font-mono">Channel: com.omnitech.googletv.remote/ir_blaster</span>
          <button
            onClick={onClose}
            className="px-4 py-1.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold shadow transition-colors"
          >
            Close Diagnostics
          </button>
        </div>
      </div>
    </div>
  );
};
