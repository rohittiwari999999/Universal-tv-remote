import React, { useState } from 'react';
import { Radio, Copy, Check, Terminal, Play, Cpu, ArrowRight } from 'lucide-react';
import { TvBrand, TvKeyDefinition } from '../data/tvBrandsData';

export interface SignalEvent {
  id: string;
  timestamp: string;
  brand: string;
  keyLabel: string;
  hex: string;
  protocol: string;
  frequency: number;
  mode: 'IR' | 'BLE' | 'Wi-Fi';
  pulses: number[];
}

interface SignalInspectorProps {
  currentSignal: SignalEvent | null;
  history: SignalEvent[];
  onReplaySignal: (signal: SignalEvent) => void;
  activeBrand: TvBrand;
}

export const SignalInspector: React.FC<SignalInspectorProps> = ({
  currentSignal,
  history,
  onReplaySignal,
  activeBrand,
}) => {
  const [copiedId, setCopiedId] = useState<string | null>(null);
  const [customHex, setCustomHex] = useState('0x20DF10EF');
  const [customCarrier, setCustomCarrier] = useState(38000);

  const copyToClipboard = (text: string, id: string) => {
    navigator.clipboard.writeText(text);
    setCopiedId(id);
    setTimeout(() => setCopiedId(null), 1500);
  };

  // Convert Hex string into microsecond breakdown for visualization
  const getPulseBreakdown = (hex: string) => {
    return [
      { type: 'LEADER_MARK', duration: '9000µs', state: 'HIGH' },
      { type: 'LEADER_SPACE', duration: '4500µs', state: 'LOW' },
      { type: 'DATA_BIT_1', duration: '560µs / 1690µs', state: 'MODULATED' },
      { type: 'DATA_BIT_0', duration: '560µs / 560µs', state: 'MODULATED' },
      { type: 'STOP_BIT', duration: '560µs', state: 'HIGH' },
    ];
  };

  return (
    <div className="flex flex-col gap-4">
      {/* Live Carrier & Oscilloscope Monitor */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-4">
        <div className="flex items-center justify-between pb-3 border-b border-slate-800">
          <div className="flex items-center gap-2">
            <Radio className="w-4 h-4 text-red-400 animate-pulse" />
            <span className="text-sm font-semibold text-white">
              Signal &amp; Protocol Inspector
            </span>
          </div>

          <div className="flex items-center gap-2 text-xs font-mono">
            <span className="text-slate-400">Carrier:</span>
            <span className="text-cyan-400 font-semibold tabular-nums">
              {currentSignal ? currentSignal.frequency : activeBrand.carrierFrequency} Hz
            </span>
          </div>
        </div>

        {/* Real-time Oscilloscope Canvas / SVG */}
        <div className="mt-3 bg-neutral-950 border border-slate-800/80 rounded-lg p-3 relative overflow-hidden">
          <div className="flex items-center justify-between text-[11px] font-mono text-slate-500 mb-1">
            <span>38kHz Sub-Carrier Burst Waveform</span>
            <span className="text-emerald-400 font-medium">
              {currentSignal ? `${currentSignal.mode} Active` : 'Waiting for keypress...'}
            </span>
          </div>

          {/* Oscilloscope Grid & Wave */}
          <div className="h-20 w-full relative flex items-center justify-center">
            {/* Background Grid Lines */}
            <div className="absolute inset-0 grid grid-cols-12 grid-rows-3 opacity-15 pointer-events-none">
              {Array.from({ length: 36 }).map((_, i) => (
                <div key={i} className="border-r border-b border-cyan-500" />
              ))}
            </div>

            {currentSignal ? (
              <svg className="w-full h-full text-cyan-400" viewBox="0 0 600 80" preserveAspectRatio="none">
                {/* Simulated Square Carrier Pulses */}
                <path
                  d="M 0 60 
                     L 30 60 L 30 15 L 75 15 L 75 60 L 110 60 
                     L 110 20 L 130 20 L 130 60 L 160 60 
                     L 160 20 L 175 20 L 175 60 L 195 60 
                     L 195 20 L 220 20 L 220 60 L 245 60 
                     L 245 20 L 260 20 L 260 60 L 280 60 
                     L 280 20 L 310 20 L 310 60 L 340 60
                     L 340 20 L 355 20 L 355 60 L 375 60
                     L 375 20 L 405 20 L 405 60 L 435 60
                     L 435 20 L 450 20 L 450 60 L 470 60
                     L 470 20 L 495 20 L 495 60 L 530 60
                     L 530 20 L 545 20 L 545 60 L 600 60"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2.5"
                  className="animate-pulse"
                />
              </svg>
            ) : (
              <div className="text-center text-xs text-slate-600 font-mono">
                Press any remote button to view modulated IR carrier pulse train
              </div>
            )}
          </div>

          {/* Current Signal Active Metadata */}
          {currentSignal && (
            <div className="mt-2 pt-2 border-t border-slate-900 grid grid-cols-2 sm:grid-cols-4 gap-2 text-xs font-mono">
              <div className="bg-slate-900/60 p-1.5 rounded">
                <div className="text-[10px] text-slate-500">BUTTON PRESSED</div>
                <div className="text-white font-semibold">{currentSignal.keyLabel}</div>
              </div>
              <div className="bg-slate-900/60 p-1.5 rounded">
                <div className="text-[10px] text-slate-500">HEX CODE</div>
                <div className="text-amber-400 font-semibold">{currentSignal.hex}</div>
              </div>
              <div className="bg-slate-900/60 p-1.5 rounded">
                <div className="text-[10px] text-slate-500">PROTOCOL</div>
                <div className="text-cyan-400 font-semibold">{currentSignal.protocol}</div>
              </div>
              <div className="bg-slate-900/60 p-1.5 rounded">
                <div className="text-[10px] text-slate-500">CARRIER FREQ</div>
                <div className="text-emerald-400 font-semibold">{currentSignal.frequency} Hz</div>
              </div>
            </div>
          )}
        </div>

        {/* Pulse Microsecond Structure Table */}
        <div className="mt-3">
          <div className="text-xs font-mono text-slate-400 mb-1.5 flex items-center justify-between">
            <span>NEC Modulation Timing Specifications:</span>
            <span className="text-[10px] text-slate-500">Android ConsumerIrManager</span>
          </div>

          <div className="grid grid-cols-5 gap-1.5 text-center text-xs font-mono">
            {getPulseBreakdown(currentSignal ? currentSignal.hex : '0x00').map((pulse, idx) => (
              <div key={idx} className="bg-slate-950/80 border border-slate-800 p-2 rounded-lg">
                <div className="text-[10px] text-slate-400 truncate">{pulse.type}</div>
                <div className="text-cyan-400 font-semibold text-xs mt-0.5">{pulse.duration}</div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Real-time Transmission Telemetry Log */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-4">
        <div className="flex items-center justify-between pb-2 border-b border-slate-800 mb-2">
          <div className="flex items-center gap-2">
            <Terminal className="w-4 h-4 text-slate-400" />
            <span className="text-xs font-mono uppercase tracking-wider text-slate-300">
              Transmission Event Stream ({history.length})
            </span>
          </div>
          {history.length > 0 && (
            <span className="text-[11px] font-mono text-slate-500">
              Latest at {history[0].timestamp}
            </span>
          )}
        </div>

        {history.length === 0 ? (
          <div className="py-6 text-center text-xs text-slate-500 font-mono">
            No signals emitted yet. Tap buttons on the remote simulator to populate the log.
          </div>
        ) : (
          <div className="max-h-52 overflow-y-auto space-y-1.5 pr-1 font-mono text-xs">
            {history.slice(0, 8).map((evt) => (
              <div
                key={evt.id}
                className="flex items-center justify-between p-2 rounded-lg bg-slate-950/60 border border-slate-800/80 hover:border-slate-700 transition-colors"
              >
                <div className="flex items-center gap-2.5">
                  <span className="text-[10px] text-slate-500 tabular-nums">
                    {evt.timestamp}
                  </span>
                  <span className={`px-1.5 py-0.5 rounded text-[10px] font-semibold ${
                    evt.mode === 'IR' ? 'bg-red-950 text-red-400 border border-red-900/60' :
                    evt.mode === 'BLE' ? 'bg-blue-950 text-blue-400 border border-blue-900/60' :
                    'bg-emerald-950 text-emerald-400 border border-emerald-900/60'
                  }`}>
                    {evt.mode}
                  </span>
                  <span className="text-white font-medium">
                    {evt.brand} · {evt.keyLabel}
                  </span>
                </div>

                <div className="flex items-center gap-2">
                  <code className="text-amber-400 bg-slate-900 px-2 py-0.5 rounded text-[11px]">
                    {evt.hex}
                  </code>

                  <button
                    onClick={() => copyToClipboard(evt.hex, evt.id)}
                    className="p-1 rounded text-slate-400 hover:text-white hover:bg-slate-800"
                    title="Copy HEX Code"
                  >
                    {copiedId === evt.id ? (
                      <Check className="w-3.5 h-3.5 text-emerald-400" />
                    ) : (
                      <Copy className="w-3.5 h-3.5" />
                    )}
                  </button>

                  <button
                    onClick={() => onReplaySignal(evt)}
                    className="p-1 rounded text-slate-400 hover:text-cyan-400 hover:bg-slate-800"
                    title="Re-transmit pulse"
                  >
                    <Play className="w-3 h-3 fill-current" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};
