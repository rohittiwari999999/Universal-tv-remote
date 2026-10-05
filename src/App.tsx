import React, { useState } from 'react';
import { 
  Tv, Radio, Layers, Database, FolderCheck, Cpu, Download,
  ExternalLink, Sparkles, Shield, Wifi, Bluetooth
} from 'lucide-react';
import { TvBrand, TV_BRANDS_DATABASE, TvKeyDefinition } from './data/tvBrandsData';
import { RemoteSimulator } from './components/RemoteSimulator';
import { SignalInspector, SignalEvent } from './components/SignalInspector';
import { PlayConsoleHub } from './components/PlayConsoleHub';
import { ArchitectureGuide } from './components/ArchitectureGuide';
import { DatabaseExplorer } from './components/DatabaseExplorer';
import { ConnectivityProtocols } from './components/ConnectivityProtocols';

export default function App() {
  const [activeBrand, setActiveBrand] = useState<TvBrand>(TV_BRANDS_DATABASE[0]); // Samsung default
  const [activeMode, setActiveMode] = useState<'IR' | 'BLE' | 'Wi-Fi'>('IR');
  const [isTransmitting, setIsTransmitting] = useState(false);
  const [currentSignal, setCurrentSignal] = useState<SignalEvent | null>(null);
  const [history, setHistory] = useState<SignalEvent[]>([]);

  // Navigation tab state
  const [currentTab, setCurrentTab] = useState<
    'simulator' | 'protocols' | 'database' | 'architecture' | 'play_console'
  >('simulator');

  const handleTransmitSignal = (
    keyName: string,
    keyDef: TvKeyDefinition,
    brand: TvBrand,
    mode: 'IR' | 'BLE' | 'Wi-Fi'
  ) => {
    setIsTransmitting(true);

    const now = new Date();
    const timeStr = now.toLocaleTimeString([], { hour12: false, hour: '2-digit', minute: '2-digit', second: '2-digit' }) + '.' + now.getMilliseconds().toString().padStart(3, '0');

    const event: SignalEvent = {
      id: Math.random().toString(36).substring(2, 9),
      timestamp: timeStr,
      brand: brand.name,
      keyLabel: keyDef.label,
      hex: keyDef.necHex,
      protocol: brand.protocol,
      frequency: brand.carrierFrequency,
      mode: mode,
      pulses: keyDef.pulses || [9000, 4500, 560, 1690, 560, 560],
    };

    setCurrentSignal(event);
    setHistory(prev => [event, ...prev.slice(0, 24)]);

    setTimeout(() => {
      setIsTransmitting(false);
    }, 280);
  };

  const handleReplaySignal = (signal: SignalEvent) => {
    setIsTransmitting(true);
    setCurrentSignal(signal);
    setTimeout(() => {
      setIsTransmitting(false);
    }, 280);
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-sans selection:bg-cyan-500/30 selection:text-cyan-200">
      
      {/* 1. Universal Top Bar Contract: Zone 1 (Wordmark) - Zone 2 (Nav Links) - Zone 3 (Action) */}
      <header className="sticky top-0 z-50 bg-slate-950/90 backdrop-blur-md border-b border-slate-800/80 px-6 py-3.5 flex items-center justify-between">
        {/* Zone 1: Single text wordmark */}
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-lg bg-gradient-to-tr from-cyan-600 to-indigo-600 flex items-center justify-center shadow-md shadow-cyan-500/20">
            <Tv className="w-4 h-4 text-white" />
          </div>
          <span className="text-base font-bold tracking-tight text-white">
            Universal TV Remote Studio
          </span>
          <span className="hidden sm:inline-block text-[11px] font-mono text-slate-500">
            Flutter Cross-Platform · Play Store 2026
          </span>
        </div>

        {/* Zone 2: Navigation Links */}
        <nav className="hidden md:flex items-center gap-1 p-1 bg-slate-900 border border-slate-800 rounded-xl text-xs font-medium">
          <button
            onClick={() => setCurrentTab('simulator')}
            className={`px-3 py-1.5 rounded-lg transition-colors ${
              currentTab === 'simulator'
                ? 'bg-slate-800 text-white shadow-sm'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            Live Simulator
          </button>
          <button
            onClick={() => setCurrentTab('protocols')}
            className={`px-3 py-1.5 rounded-lg transition-colors ${
              currentTab === 'protocols'
                ? 'bg-slate-800 text-white shadow-sm'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            Protocols Lab
          </button>
          <button
            onClick={() => setCurrentTab('database')}
            className={`px-3 py-1.5 rounded-lg transition-colors ${
              currentTab === 'database'
                ? 'bg-slate-800 text-white shadow-sm'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            Firestore DB ({TV_BRANDS_DATABASE.length})
          </button>
          <button
            onClick={() => setCurrentTab('architecture')}
            className={`px-3 py-1.5 rounded-lg transition-colors ${
              currentTab === 'architecture'
                ? 'bg-slate-800 text-white shadow-sm'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            Flutter Architecture
          </button>
          <button
            onClick={() => setCurrentTab('play_console')}
            className={`px-3 py-1.5 rounded-lg transition-colors flex items-center gap-1.5 ${
              currentTab === 'play_console'
                ? 'bg-indigo-600 text-white shadow-sm'
                : 'text-indigo-400 hover:text-indigo-300'
            }`}
          >
            <FolderCheck className="w-3.5 h-3.5" />
            <span>Play Console Release</span>
          </button>
        </nav>

        {/* Zone 3: Primary Action */}
        <div className="flex items-center gap-2">
          <button
            onClick={() => setCurrentTab('play_console')}
            className="flex items-center gap-2 px-3.5 py-1.5 bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold rounded-lg transition-colors shadow-sm"
          >
            <FolderCheck className="w-3.5 h-3.5" />
            <span className="hidden sm:inline">Release Files</span>
            <span className="sm:hidden">Publish</span>
          </button>
        </div>
      </header>

      {/* Mobile Sub-Navigation Bar */}
      <div className="md:hidden flex items-center gap-1 p-2 bg-slate-900 border-b border-slate-800 text-xs overflow-x-auto">
        <button
          onClick={() => setCurrentTab('simulator')}
          className={`px-3 py-1 rounded-md shrink-0 ${currentTab === 'simulator' ? 'bg-slate-800 text-white' : 'text-slate-400'}`}
        >
          Simulator
        </button>
        <button
          onClick={() => setCurrentTab('protocols')}
          className={`px-3 py-1 rounded-md shrink-0 ${currentTab === 'protocols' ? 'bg-slate-800 text-white' : 'text-slate-400'}`}
        >
          Protocols
        </button>
        <button
          onClick={() => setCurrentTab('database')}
          className={`px-3 py-1 rounded-md shrink-0 ${currentTab === 'database' ? 'bg-slate-800 text-white' : 'text-slate-400'}`}
        >
          Firestore
        </button>
        <button
          onClick={() => setCurrentTab('architecture')}
          className={`px-3 py-1 rounded-md shrink-0 ${currentTab === 'architecture' ? 'bg-slate-800 text-white' : 'text-slate-400'}`}
        >
          Flutter Core
        </button>
        <button
          onClick={() => setCurrentTab('play_console')}
          className={`px-3 py-1 rounded-md shrink-0 font-medium ${currentTab === 'play_console' ? 'bg-indigo-600 text-white' : 'text-indigo-400'}`}
        >
          Play Console
        </button>
      </div>

      {/* Main Content Viewport */}
      <main className="flex-1 max-w-7xl w-full mx-auto p-4 sm:p-6 lg:p-8">
        
        {/* TAB 1: LIVE SIMULATOR & SIGNAL INSPECTOR */}
        {currentTab === 'simulator' && (
          <div className="space-y-6">
            {/* Header Kicker */}
            <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-4 border-b border-slate-800 gap-3">
              <div>
                <h1 className="text-xl font-bold text-white tracking-tight">
                  Universal Remote Simulator &amp; Pulse Inspector
                </h1>
                <p className="text-xs text-slate-400 mt-0.5">
                  Interactive Flutter hardware simulation covering Global (Samsung, LG, Sony) &amp; Indian (Vu, Mi, Thomson, Lloyd, Onida, BPL) TVs.
                </p>
              </div>

              <div className="flex items-center gap-3 text-xs font-mono">
                <div className="flex items-center gap-1.5 text-slate-400">
                  <div className={`w-2 h-2 rounded-full ${isTransmitting ? 'bg-red-400 animate-ping' : 'bg-slate-600'}`} />
                  <span>Blaster: {isTransmitting ? 'Emitting Carrier' : 'Standby'}</span>
                </div>
              </div>
            </div>

            {/* Dual Grid: Remote on Left, Oscilloscope on Right */}
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
              {/* Virtual Smartphone Remote */}
              <div className="lg:col-span-5 flex justify-center">
                <RemoteSimulator
                  activeBrand={activeBrand}
                  onBrandChange={setActiveBrand}
                  activeMode={activeMode}
                  onModeChange={setActiveMode}
                  onTransmitSignal={handleTransmitSignal}
                  isTransmitting={isTransmitting}
                />
              </div>

              {/* Signal Oscilloscope & Telemetry Console */}
              <div className="lg:col-span-7">
                <SignalInspector
                  currentSignal={currentSignal}
                  history={history}
                  onReplaySignal={handleReplaySignal}
                  activeBrand={activeBrand}
                />
              </div>
            </div>
          </div>
        )}

        {/* TAB 2: CONNECTIVITY PROTOCOLS LAB */}
        {currentTab === 'protocols' && (
          <ConnectivityProtocols />
        )}

        {/* TAB 3: FIREBASE FIRESTORE DATABASE EXPLORER */}
        {currentTab === 'database' && (
          <DatabaseExplorer />
        )}

        {/* TAB 4: FLUTTER SYSTEM ARCHITECTURE */}
        {currentTab === 'architecture' && (
          <ArchitectureGuide />
        )}

        {/* TAB 5: GOOGLE PLAY CONSOLE RELEASE & ASSETS */}
        {currentTab === 'play_console' && (
          <PlayConsoleHub />
        )}

      </main>

      {/* Clean Editorial Footer */}
      <footer className="border-t border-slate-900 bg-slate-950 py-6 px-6 text-center text-xs text-slate-500 font-mono">
        <div className="flex flex-col sm:flex-row items-center justify-between max-w-7xl mx-auto gap-3">
          <div className="flex items-center gap-2">
            <span className="text-slate-400">Universal TV Remote Control Studio</span>
            <span>·</span>
            <span>Target SDK 35</span>
            <span>·</span>
            <span>IR / BLE / Wi-Fi Hybrid</span>
          </div>

          <div className="flex items-center gap-4 text-slate-400">
            <button onClick={() => setCurrentTab('play_console')} className="hover:text-white transition-colors">
              Play Store Files
            </button>
            <button onClick={() => setCurrentTab('architecture')} className="hover:text-white transition-colors">
              Dart Code
            </button>
            <button onClick={() => setCurrentTab('database')} className="hover:text-white transition-colors">
              Firestore Schema
            </button>
          </div>
        </div>
      </footer>
    </div>
  );
}
