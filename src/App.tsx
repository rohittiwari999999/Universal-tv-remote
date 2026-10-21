import React, { useState } from 'react';
import { 
  Tv, Radio, Code2, FolderCheck, Terminal, Shield,
  Wifi, Sparkles, Check, Copy, ExternalLink, Play, Lock, CheckCircle2
} from 'lucide-react';
import { GoogleTvMobileApp } from './components/GoogleTvMobileApp';
import { GoogleTvScreenSimulation } from './components/GoogleTvScreenSimulation';
import { NativeAndroidInspector } from './components/NativeAndroidInspector';

export default function App() {
  const [activeTab, setActiveTab] = useState<'app' | 'code' | 'build'>('app');

  // Simulated TV State
  const [tvName, setTvName] = useState('Chromecast with Google TV');
  const [ipAddress, setIpAddress] = useState('192.168.1.104');
  const [isPairing, setIsPairing] = useState(false);
  const [pairingCode, setPairingCode] = useState('8341');
  const [isConnected, setIsConnected] = useState(false);
  const [activeApp, setActiveApp] = useState('home');
  const [volume, setVolume] = useState(24);
  const [isMuted, setIsMuted] = useState(false);
  const [selectedCardIndex, setSelectedCardIndex] = useState(0);
  const [searchQuery, setSearchQuery] = useState('');
  const [isAssistantActive, setIsAssistantActive] = useState(false);
  const [powerState, setPowerState] = useState(true);
  const [lastActionLog, setLastActionLog] = useState('System ready. Scanning local network for Google TVs...');
  const [telemetryLogs, setTelemetryLogs] = useState<string[]>([
    '[mDNS] Started NsdManager listening on _androidtvremote2._tcp',
    '[mDNS] Resolved Chromecast with Google TV at 192.168.1.104:6467',
    '[mDNS] Resolved Sony BRAVIA 4K at 192.168.1.112:6467'
  ]);

  const addLog = (msg: string) => {
    const time = new Date().toLocaleTimeString();
    setLastActionLog(msg);
    setTelemetryLogs(prev => [`[${time}] ${msg}`, ...prev.slice(0, 30)]);
  };

  const handlePairingStatusChange = (pairing: boolean, code: string) => {
    setIsPairing(pairing);
    if (code) setPairingCode(code);
    if (!pairing && isPairing) {
      setIsConnected(true);
      addLog('Pairing Successful: X.509 Client Certificate registered in Google TV Keystore.');
    }
  };

  const handleTvVolumeChange = (delta: number) => {
    setIsMuted(false);
    setVolume(prev => {
      const next = Math.max(0, Math.min(100, prev + delta));
      addLog(`Volume changed: ${next}%`);
      return next;
    });
  };

  const handleTvMuteToggle = () => {
    setIsMuted(prev => {
      const next = !prev;
      addLog(`Mute toggled: ${next ? 'MUTED' : 'UNMUTED'}`);
      return next;
    });
  };

  const handleTvAppLaunch = (appId: string) => {
    setActiveApp(appId);
    addLog(`Launched TV App: ${appId.toUpperCase()} (Deep Link packet sent over 6466)`);
  };

  const handleTvSearch = (query: string) => {
    setSearchQuery(query);
    addLog(`IME Streamed to TV: "${query}"`);
  };

  const handleTvAssistantToggle = () => {
    setIsAssistantActive(prev => {
      const next = !prev;
      addLog(next ? 'Google Assistant Activated (Voice intent sent)' : 'Google Assistant Dismissed');
      return next;
    });
  };

  const handleTvNavigate = (direction: 'up' | 'down' | 'left' | 'right' | 'ok') => {
    if (direction === 'left') {
      setSelectedCardIndex(prev => Math.max(0, prev - 1));
      addLog('D-Pad Left (Keycode 21)');
    } else if (direction === 'right') {
      setSelectedCardIndex(prev => Math.min(4, prev + 1));
      addLog('D-Pad Right (Keycode 22)');
    } else if (direction === 'up') {
      addLog('D-Pad Up (Keycode 19)');
    } else if (direction === 'down') {
      addLog('D-Pad Down (Keycode 20)');
    } else if (direction === 'ok') {
      addLog('D-Pad Center Select / OK (Keycode 23)');
      // Launch selected app card
      const apps = ['youtube', 'netflix', 'prime', 'hotstar', 'spotify'];
      if (apps[selectedCardIndex]) {
        setActiveApp(apps[selectedCardIndex]);
      }
    }
  };

  const handleTvPowerToggle = () => {
    setPowerState(prev => {
      const next = !prev;
      addLog(`Power key pressed: TV is now ${next ? 'ON' : 'OFF/STANDBY'}`);
      return next;
    });
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-sans selection:bg-blue-500/30 selection:text-blue-200">
      
      {/* Top Header */}
      <header className="sticky top-0 z-50 bg-slate-950/90 backdrop-blur-md border-b border-slate-800/80 px-6 py-3.5 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-lg bg-gradient-to-tr from-blue-600 to-indigo-600 flex items-center justify-center shadow-md shadow-blue-500/20">
            <Tv className="w-4 h-4 text-white" />
          </div>
          <div>
            <span className="text-base font-bold tracking-tight text-white block leading-none">
              Google TV Remote Control
            </span>
            <span className="text-[11px] font-mono text-slate-400 mt-0.5 block">
              Native Android Kotlin · Auto-Discovery · Real TLS PIN Pairing
            </span>
          </div>
        </div>

        {/* Tab Controls */}
        <nav className="flex items-center gap-1 p-1 bg-slate-900 border border-slate-800 rounded-xl text-xs font-medium">
          <button
            onClick={() => setActiveTab('app')}
            className={`px-3 py-1.5 rounded-lg transition-colors flex items-center gap-1.5 ${
              activeTab === 'app'
                ? 'bg-blue-600 text-white shadow-sm'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <Radio className="w-3.5 h-3.5" />
            <span>Interactive Remote &amp; TV</span>
          </button>

          <button
            onClick={() => setActiveTab('code')}
            className={`px-3 py-1.5 rounded-lg transition-colors flex items-center gap-1.5 ${
              activeTab === 'code'
                ? 'bg-blue-600 text-white shadow-sm'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <Code2 className="w-3.5 h-3.5" />
            <span>Native Kotlin Code (9 Files)</span>
          </button>

          <button
            onClick={() => setActiveTab('build')}
            className={`px-3 py-1.5 rounded-lg transition-colors flex items-center gap-1.5 ${
              activeTab === 'build'
                ? 'bg-indigo-600 text-white shadow-sm'
                : 'text-indigo-400 hover:text-indigo-300'
            }`}
          >
            <FolderCheck className="w-3.5 h-3.5" />
            <span>Build APK &amp; AAB</span>
          </button>
        </nav>
      </header>

      {/* Main Content Area */}
      <main className="flex-1 max-w-7xl w-full mx-auto p-4 sm:p-6 lg:p-8">
        
        {/* TAB 1: INTERACTIVE APP & TV SCREEN */}
        {activeTab === 'app' && (
          <div className="space-y-6">
            
            {/* Header Kicker */}
            <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-4 border-b border-slate-800 gap-3">
              <div>
                <h1 className="text-xl font-bold text-white tracking-tight flex items-center gap-2">
                  <span>Google TV Remote Experience</span>
                  <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 font-mono">
                    Auto-Discovery on Launch
                  </span>
                </h1>
                <p className="text-xs text-slate-400 mt-0.5">
                  The mobile app automatically searches Wi-Fi for Google TVs, initiates the real TLS handshake on port 6467, prompts for the PIN on your TV screen, and controls Google TV on port 6466.
                </p>
              </div>

              <div className="flex items-center gap-3 text-xs font-mono">
                <span className="text-slate-500">Protocol:</span>
                <span className="text-blue-400 font-semibold">_androidtvremote2._tcp</span>
              </div>
            </div>

            {/* Split Grid: Phone on Left, Google TV on Right */}
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
              
              {/* Virtual Smartphone Remote Container */}
              <div className="lg:col-span-5 flex flex-col items-center">
                <div className="text-xs font-semibold text-slate-400 mb-2 font-mono flex items-center gap-1.5">
                  <div className="w-2 h-2 rounded-full bg-blue-500 animate-ping" />
                  <span>NATIVE ANDROID PHONE</span>
                </div>
                
                <GoogleTvMobileApp
                  onTvAction={addLog}
                  onTvVolumeChange={handleTvVolumeChange}
                  onTvMuteToggle={handleTvMuteToggle}
                  onTvAppLaunch={handleTvAppLaunch}
                  onTvSearch={handleTvSearch}
                  onTvAssistantToggle={handleTvAssistantToggle}
                  onTvNavigate={handleTvNavigate}
                  onTvPowerToggle={handleTvPowerToggle}
                  onPairingStatusChange={handlePairingStatusChange}
                  pairingCode={pairingCode}
                />
              </div>

              {/* Google TV Screen Simulation & Live Telemetry Console */}
              <div className="lg:col-span-7 space-y-5">
                <div className="text-xs font-semibold text-slate-400 mb-2 font-mono flex items-center gap-1.5">
                  <Tv className="w-3.5 h-3.5 text-blue-400" />
                  <span>CONNECTED GOOGLE TV SCREEN</span>
                </div>

                <GoogleTvScreenSimulation
                  tvName={tvName}
                  ipAddress={ipAddress}
                  isPairing={isPairing}
                  pairingCode={pairingCode}
                  isConnected={isConnected}
                  activeApp={activeApp}
                  volume={volume}
                  isMuted={isMuted}
                  selectedCardIndex={selectedCardIndex}
                  searchQuery={searchQuery}
                  isAssistantActive={isAssistantActive}
                  powerState={powerState}
                  lastActionLog={lastActionLog}
                />

                {/* Real-time Protocol Packet Telemetry Logger */}
                <div className="bg-slate-900 border border-slate-800 rounded-2xl p-4">
                  <div className="flex items-center justify-between pb-2 mb-2 border-b border-slate-800 text-xs">
                    <span className="font-bold text-white font-mono flex items-center gap-1.5">
                      <Terminal className="w-3.5 h-3.5 text-blue-400" />
                      <span>Google TV TLS Packet Log</span>
                    </span>
                    <span className="text-[10px] text-slate-500 font-mono">Port 6467 (Pairing) · Port 6466 (Command)</span>
                  </div>

                  <div className="space-y-1 max-h-36 overflow-y-auto font-mono text-[11px] text-slate-400 pr-1">
                    {telemetryLogs.map((log, idx) => (
                      <div key={idx} className="flex items-start gap-2">
                        <span className="text-blue-500 shrink-0">›</span>
                        <span className={idx === 0 ? 'text-cyan-300 font-semibold' : 'text-slate-400'}>{log}</span>
                      </div>
                    ))}
                  </div>
                </div>

              </div>

            </div>

          </div>
        )}

        {/* TAB 2: NATIVE KOTLIN CODEBASE */}
        {activeTab === 'code' && (
          <NativeAndroidInspector />
        )}

        {/* TAB 3: GITHUB ACTIONS BUILD & RELEASE APK/AAB */}
        {activeTab === 'build' && (
          <div className="space-y-6">
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6">
              <div className="flex items-center gap-3 mb-2">
                <div className="w-9 h-9 rounded-xl bg-indigo-600/20 border border-indigo-500/40 flex items-center justify-center text-indigo-400">
                  <FolderCheck className="w-5 h-5" />
                </div>
                <div>
                  <h2 className="text-lg font-bold text-white">
                    GitHub Actions Native Android Release Pipeline
                  </h2>
                  <p className="text-xs text-slate-400">
                    Workflow file is located at <code className="text-cyan-300">.github/workflows/build_native_android_apk_aab.yml</code>
                  </p>
                </div>
              </div>

              <div className="my-4 p-4 bg-slate-950 rounded-xl border border-slate-800 font-mono text-xs text-slate-300 space-y-2">
                <div className="text-slate-500"># Run these commands in your project terminal:</div>
                <div className="text-emerald-400 font-bold">git add -A</div>
                <div className="text-emerald-400 font-bold">git commit -m "feat: complete native android google tv remote with auto discovery &amp; tls pairing"</div>
                <div className="text-emerald-400 font-bold">git push origin main</div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-6">
                <div className="p-4 bg-slate-950 rounded-xl border border-slate-800">
                  <div className="text-sm font-bold text-white mb-1">📦 Release APK (Direct Install)</div>
                  <p className="text-xs text-slate-400 mb-3">
                    Installs directly on any Android phone running Android 7.0+ to Android 15.
                  </p>
                  <div className="text-[11px] font-mono text-slate-500">
                    Output: <span className="text-cyan-300">android/app/build/outputs/apk/release/app-release.apk</span>
                  </div>
                </div>

                <div className="p-4 bg-slate-950 rounded-xl border border-slate-800">
                  <div className="text-sm font-bold text-white mb-1">🚀 Google Play Bundle (.aab)</div>
                  <p className="text-xs text-slate-400 mb-3">
                    Target SDK 35 compliant for Google Play Console submission.
                  </p>
                  <div className="text-[11px] font-mono text-slate-500">
                    Output: <span className="text-cyan-300">android/app/build/outputs/bundle/release/app-release.aab</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}

      </main>

      {/* Footer */}
      <footer className="border-t border-slate-900 bg-slate-950 py-4 px-6 text-center text-xs text-slate-500 font-mono">
        <div className="flex flex-col sm:flex-row items-center justify-between max-w-7xl mx-auto gap-2">
          <span>Google TV Native Remote Architecture · Android 15 / Target SDK 35</span>
          <div className="flex items-center gap-3">
            <button onClick={() => setActiveTab('app')} className="hover:text-white">Simulator</button>
            <span>·</span>
            <button onClick={() => setActiveTab('code')} className="hover:text-white">Kotlin Code</button>
            <span>·</span>
            <button onClick={() => setActiveTab('build')} className="hover:text-white">Build APK</button>
          </div>
        </div>
      </footer>
    </div>
  );
}
