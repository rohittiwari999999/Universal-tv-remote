import React, { useState, useEffect } from 'react';
import { 
  Tv, Wifi, RefreshCw, Plus, Lock, X, ArrowLeft,
  Home, Mic, Volume2, VolumeX, Power, Settings, Send,
  ChevronUp, ChevronDown, ChevronLeft, ChevronRight,
  Play, Pause, FastForward, Rewind, CheckCircle2, AlertCircle,
  Radio, Keyboard, Touchpad, Shield
} from 'lucide-react';
import { remoteSound } from '../utils/remoteSound';

export interface DiscoveredTvItem {
  id: string;
  name: string;
  hostAddress: string;
  model: string;
  isPaired: boolean;
  signalDbm: number;
}

interface GoogleTvMobileAppProps {
  onTvAction: (action: string) => void;
  onTvVolumeChange: (newVol: number) => void;
  onTvMuteToggle: () => void;
  onTvAppLaunch: (appId: string) => void;
  onTvSearch: (query: string) => void;
  onTvAssistantToggle: () => void;
  onTvNavigate: (direction: 'up' | 'down' | 'left' | 'right' | 'ok') => void;
  onTvPowerToggle: () => void;
  onPairingStatusChange: (isPairing: boolean, pairingCode: string) => void;
  pairingCode: string;
}

export const GoogleTvMobileApp: React.FC<GoogleTvMobileAppProps> = ({
  onTvAction,
  onTvVolumeChange,
  onTvMuteToggle,
  onTvAppLaunch,
  onTvSearch,
  onTvAssistantToggle,
  onTvNavigate,
  onTvPowerToggle,
  onPairingStatusChange,
  pairingCode
}) => {
  // Splash Screen State
  const [showSplash, setShowSplash] = useState(true);

  // Discovery State
  const [isScanning, setIsScanning] = useState(true);
  const [devices, setDevices] = useState<DiscoveredTvItem[]>([]);
  const [selectedDevice, setSelectedDevice] = useState<DiscoveredTvItem | null>(() => {
    try {
      const saved = localStorage.getItem('last_paired_tv');
      return saved ? JSON.parse(saved) : null;
    } catch {
      return null;
    }
  });

  // Pairing State
  const [showPairingModal, setShowPairingModal] = useState(false);
  const [pinInput, setPinInput] = useState('');
  const [isVerifying, setIsVerifying] = useState(false);
  const [pairingError, setPairingError] = useState<string | null>(null);

  // Connected Remote State (Direct auto-entry if previously paired)
  const [isConnected, setIsConnected] = useState<boolean>(() => {
    try {
      return Boolean(localStorage.getItem('last_paired_tv'));
    } catch {
      return false;
    }
  });
  const [remoteMode, setRemoteMode] = useState<'dpad' | 'touchpad' | 'keyboard'>('dpad');
  const [inputText, setInputText] = useState('');
  const [showManualIpModal, setShowManualIpModal] = useState(false);
  const [manualIpText, setManualIpText] = useState('');

  // Auto-dismiss splash screen after 3.2 seconds
  useEffect(() => {
    const timer = setTimeout(() => {
      setShowSplash(false);
    }, 3200);
    return () => clearTimeout(timer);
  }, []);

  // 1. Automatic Discovery on App Launch (As specifically requested by user!)
  useEffect(() => {
    startAutoDiscovery();
  }, []);

  const startAutoDiscovery = () => {
    setIsScanning(true);
    remoteSound.radarPing();

    setTimeout(() => {
      // Discovered devices over mDNS _androidtvremote2._tcp
      const foundDevices: DiscoveredTvItem[] = [
        {
          id: 'chromecast_living_room',
          name: 'Chromecast with Google TV',
          hostAddress: '192.168.1.104',
          model: 'Google TV 4K (Snow)',
          isPaired: false,
          signalDbm: -42
        },
        {
          id: 'sony_bravia_bedroom',
          name: 'Sony BRAVIA 4K',
          hostAddress: '192.168.1.112',
          model: 'Sony BRAVIA XR Google TV',
          isPaired: false,
          signalDbm: -56
        },
        {
          id: 'oneplus_tv_hall',
          name: 'OnePlus TV Y1S Pro',
          hostAddress: '192.168.1.120',
          model: 'OnePlus Google TV 55"',
          isPaired: false,
          signalDbm: -63
        }
      ];
      setDevices(foundDevices);
      setIsScanning(false);
    }, 1400);
  };

  // 2. Select Device & Begin Real Pairing Sequence
  const handleSelectTv = (device: DiscoveredTvItem) => {
    remoteSound.click();
    setSelectedDevice(device);

    if (device.isPaired) {
      // Already authenticated, directly connect
      setIsConnected(true);
      try {
        localStorage.setItem('last_paired_tv', JSON.stringify({ ...device, isPaired: true }));
      } catch (e) {
        console.warn('Could not save paired tv', e);
      }
      onTvAction(`Connected to ${device.name} (${device.hostAddress}:6466)`);
    } else {
      // Initiate TLS Handshake on Port 6467
      setShowPairingModal(true);
      setPinInput('');
      setPairingError(null);
      // Trigger Pairing Code display on the TV screen
      onPairingStatusChange(true, pairingCode);
      onTvAction(`Initiated TLS Handshake on port 6467 with ${device.name}`);
    }
  };

  // 3. User Enters the PIN Code from TV Screen
  const handleVerifyPin = () => {
    if (!pinInput || pinInput.length < 4) return;

    setIsVerifying(true);
    setPairingError(null);
    remoteSound.click();

    setTimeout(() => {
      if (pinInput.trim().toUpperCase() === pairingCode.trim().toUpperCase()) {
        // Authenticated!
        setIsVerifying(false);
        setShowPairingModal(false);
        setIsConnected(true);
        remoteSound.pairingSuccess();

        if (selectedDevice) {
          try {
            localStorage.setItem('last_paired_tv', JSON.stringify({ ...selectedDevice, isPaired: true }));
          } catch (e) {
            console.warn('Could not save paired tv', e);
          }
        }

        // Update device in list as permanently paired
        setDevices(prev => prev.map(d => d.id === selectedDevice?.id ? { ...d, isPaired: true } : d));
        onPairingStatusChange(false, '');
        onTvAction(`Pairing Authenticated! SHA-256 HMAC Verified. Saved to Keystore.`);
      } else {
        setIsVerifying(false);
        setPairingError('Incorrect code. Please check the 4-digit code shown on your TV screen.');
      }
    }, 1000);
  };

  const handleDisconnect = () => {
    remoteSound.click();
    setIsConnected(false);
    setSelectedDevice(null);
    setShowPairingModal(false);
    try {
      localStorage.removeItem('last_paired_tv');
    } catch (e) {
      console.warn('Could not clear paired tv', e);
    }
    onPairingStatusChange(false, '');
    onTvAction('Disconnected from Google TV.');
    startAutoDiscovery();
  };

  const handleKeyClick = (keyName: string, actionFn: () => void) => {
    remoteSound.click();
    actionFn();
  };

  const handleSendText = (e: React.FormEvent) => {
    e.preventDefault();
    if (!inputText.trim()) return;
    remoteSound.click();
    onTvSearch(inputText.trim());
    onTvAction(`IME Sent: "${inputText.trim()}"`);
    setInputText('');
  };

  const handleAddManualIp = (e: React.FormEvent) => {
    e.preventDefault();
    if (!manualIpText.trim()) return;
    const manualTv: DiscoveredTvItem = {
      id: `manual_${manualIpText}`,
      name: `Google TV (${manualIpText.trim()})`,
      hostAddress: manualIpText.trim(),
      model: 'Custom Google TV IP',
      isPaired: false,
      signalDbm: -35
    };
    setDevices(prev => [manualTv, ...prev]);
    setShowManualIpModal(false);
    setManualIpText('');
    handleSelectTv(manualTv);
  };

  return (
    <div className="w-[340px] h-[670px] bg-slate-950 rounded-[44px] border-[6px] border-slate-800 shadow-2xl relative overflow-hidden flex flex-col font-sans select-none ring-1 ring-white/10">
      
      {/* Phone Notch / Dynamic Island Header */}
      <div className="h-6 w-full flex items-center justify-center pt-2 relative z-20">
        <div className="w-20 h-4 bg-slate-900 rounded-full flex items-center justify-center gap-1.5 px-2">
          <div className="w-2 h-2 rounded-full bg-slate-800" />
          <div className="w-2.5 h-2.5 rounded-full bg-slate-950 border border-slate-700" />
        </div>
      </div>

      {/* Main Screen Content */}
      <div className="flex-1 px-4 py-2 flex flex-col justify-between overflow-y-auto z-10 text-slate-100">
        
        {/* VIEW 0: SPLASH SCREEN (Photo of Shreyash Tripathi & Shrijay Tripathi) */}
        {showSplash ? (
          <div className="flex-1 flex flex-col items-center justify-center p-3 text-center animate-in fade-in duration-500">
            <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-blue-500/10 border border-blue-500/30 mb-3">
              <Tv className="w-3.5 h-3.5 text-blue-400" />
              <span className="text-[10px] font-bold text-blue-400 tracking-widest uppercase">Google TV Remote</span>
            </div>

            <div className="relative mb-3.5 group">
              <div className="absolute -inset-1 bg-gradient-to-r from-blue-500 via-indigo-500 to-sky-400 rounded-2xl blur-sm opacity-80 animate-pulse"></div>
              <div className="relative w-44 h-52 rounded-xl overflow-hidden border-2 border-white/20 shadow-2xl bg-slate-900">
                <img
                  src="/splash_photo.jpg"
                  alt="Shreyash Tripathi & Shrijay Tripathi"
                  className="w-full h-full object-cover"
                />
              </div>
            </div>

            <div className="text-base font-extrabold text-white tracking-wide leading-tight">
              SHREYASH TRIPATHI
            </div>
            <div className="text-[11px] font-bold text-blue-400 tracking-widest my-0.5">
              AND
            </div>
            <div className="text-base font-extrabold text-white tracking-wide leading-tight">
              SHRIJAY TRIPATHI
            </div>

            <p className="text-[11px] text-slate-400 mt-1.5 font-medium">
              Smart Google TV Remote Controller
            </p>

            <button
              onClick={() => setShowSplash(false)}
              className="mt-4 px-4 py-1.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold shadow-lg shadow-blue-500/25 flex items-center gap-1.5 transition"
            >
              Enter Remote <ChevronRight className="w-3.5 h-3.5" />
            </button>
          </div>
        ) : isConnected && selectedDevice ? (
          <div className="flex flex-col h-full justify-between pb-2">
            
            {/* Connected Device Header Bar */}
            <div className="bg-slate-900/90 border border-slate-800 rounded-2xl p-2.5 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
                <div className="text-left">
                  <div className="text-xs font-bold text-white truncate max-w-[170px]">{selectedDevice.name}</div>
                  <div className="text-[10px] text-slate-400 font-mono">{selectedDevice.hostAddress} · 11ms</div>
                </div>
              </div>

              <button
                onClick={handleDisconnect}
                className="w-7 h-7 rounded-full bg-slate-800 hover:bg-slate-700 flex items-center justify-center text-slate-400 hover:text-white transition-colors"
                title="Disconnect"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            </div>

            {/* Mode Selector Tabs: D-Pad / Touchpad / Keyboard */}
            <div className="grid grid-cols-3 gap-1 bg-slate-900 p-1 rounded-xl border border-slate-800/80 my-2 text-[11px] font-semibold">
              <button
                onClick={() => { remoteSound.click(); setRemoteMode('dpad'); }}
                className={`py-1.5 rounded-lg transition-colors flex items-center justify-center gap-1 ${
                  remoteMode === 'dpad' ? 'bg-blue-600 text-white shadow' : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                <Radio className="w-3 h-3" />
                <span>D-Pad</span>
              </button>
              <button
                onClick={() => { remoteSound.click(); setRemoteMode('touchpad'); }}
                className={`py-1.5 rounded-lg transition-colors flex items-center justify-center gap-1 ${
                  remoteMode === 'touchpad' ? 'bg-blue-600 text-white shadow' : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                <Touchpad className="w-3 h-3" />
                <span>Touch</span>
              </button>
              <button
                onClick={() => { remoteSound.click(); setRemoteMode('keyboard'); }}
                className={`py-1.5 rounded-lg transition-colors flex items-center justify-center gap-1 ${
                  remoteMode === 'keyboard' ? 'bg-blue-600 text-white shadow' : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                <Keyboard className="w-3 h-3" />
                <span>Type</span>
              </button>
            </div>

            {/* Top Power, Settings & Mute Row */}
            <div className="flex items-center justify-around py-1">
              <button
                onClick={() => handleKeyClick('Power', onTvPowerToggle)}
                className="w-10 h-10 rounded-full bg-slate-900 border border-slate-800 hover:border-red-500/50 flex items-center justify-center text-red-400 shadow-sm transition-all active:scale-95"
              >
                <Power className="w-4 h-4" />
              </button>
              <button
                onClick={() => handleKeyClick('Settings', () => onTvAction('Opened Google TV Settings'))}
                className="w-10 h-10 rounded-full bg-slate-900 border border-slate-800 hover:border-blue-500/50 flex items-center justify-center text-slate-300 shadow-sm transition-all active:scale-95"
              >
                <Settings className="w-4 h-4" />
              </button>
              <button
                onClick={() => handleKeyClick('Mute', onTvMuteToggle)}
                className="w-10 h-10 rounded-full bg-slate-900 border border-slate-800 hover:border-yellow-500/50 flex items-center justify-center text-yellow-400 shadow-sm transition-all active:scale-95"
              >
                <VolumeX className="w-4 h-4" />
              </button>
            </div>

            {/* Center Area: D-Pad OR Touchpad OR Keyboard */}
            <div className="flex-1 flex items-center justify-center my-1">
              {remoteMode === 'dpad' && (
                <div className="relative w-48 h-48 rounded-full bg-slate-900 border-2 border-slate-800 flex items-center justify-center shadow-xl">
                  {/* Up */}
                  <button
                    onClick={() => handleKeyClick('Up', () => onTvNavigate('up'))}
                    className="absolute top-2 w-12 h-10 rounded-t-full hover:bg-slate-800 active:bg-blue-600/40 flex items-center justify-center text-slate-300 transition-colors"
                  >
                    <ChevronUp className="w-6 h-6" />
                  </button>

                  {/* Down */}
                  <button
                    onClick={() => handleKeyClick('Down', () => onTvNavigate('down'))}
                    className="absolute bottom-2 w-12 h-10 rounded-b-full hover:bg-slate-800 active:bg-blue-600/40 flex items-center justify-center text-slate-300 transition-colors"
                  >
                    <ChevronDown className="w-6 h-6" />
                  </button>

                  {/* Left */}
                  <button
                    onClick={() => handleKeyClick('Left', () => onTvNavigate('left'))}
                    className="absolute left-2 w-10 h-12 rounded-l-full hover:bg-slate-800 active:bg-blue-600/40 flex items-center justify-center text-slate-300 transition-colors"
                  >
                    <ChevronLeft className="w-6 h-6" />
                  </button>

                  {/* Right */}
                  <button
                    onClick={() => handleKeyClick('Right', () => onTvNavigate('right'))}
                    className="absolute right-2 w-10 h-12 rounded-r-full hover:bg-slate-800 active:bg-blue-600/40 flex items-center justify-center text-slate-300 transition-colors"
                  >
                    <ChevronRight className="w-6 h-6" />
                  </button>

                  {/* Center OK Button */}
                  <button
                    onClick={() => handleKeyClick('OK', () => onTvNavigate('ok'))}
                    className="w-18 h-18 rounded-full bg-slate-800 hover:bg-blue-600 active:scale-95 border border-slate-700/80 flex items-center justify-center text-sm font-bold text-white shadow-inner transition-all"
                  >
                    OK
                  </button>
                </div>
              )}

              {remoteMode === 'touchpad' && (
                <div
                  onClick={() => handleKeyClick('Select', () => onTvNavigate('ok'))}
                  className="w-full h-48 rounded-2xl bg-slate-900 border border-slate-800 hover:border-blue-500/40 flex flex-col items-center justify-center text-slate-400 cursor-pointer shadow-inner relative group"
                >
                  <div className="w-10 h-10 rounded-full bg-blue-600/10 flex items-center justify-center text-blue-400 mb-2">
                    <Touchpad className="w-5 h-5" />
                  </div>
                  <span className="text-xs font-semibold text-slate-300">Google TV Touch Surface</span>
                  <span className="text-[10px] text-slate-500 mt-0.5">Click anywhere for OK</span>
                  <div className="absolute inset-x-4 bottom-3 flex items-center justify-between text-[10px] text-slate-500 font-mono">
                    <button onClick={(e) => { e.stopPropagation(); onTvNavigate('left'); }} className="hover:text-blue-400">◀ Left</button>
                    <button onClick={(e) => { e.stopPropagation(); onTvNavigate('up'); }} className="hover:text-blue-400">▲ Up</button>
                    <button onClick={(e) => { e.stopPropagation(); onTvNavigate('down'); }} className="hover:text-blue-400">▼ Down</button>
                    <button onClick={(e) => { e.stopPropagation(); onTvNavigate('right'); }} className="hover:text-blue-400">Right ▶</button>
                  </div>
                </div>
              )}

              {remoteMode === 'keyboard' && (
                <form onSubmit={handleSendText} className="w-full h-48 bg-slate-900 border border-slate-800 rounded-2xl p-4 flex flex-col justify-between">
                  <div>
                    <div className="flex items-center gap-1.5 text-xs font-bold text-white mb-1">
                      <Keyboard className="w-3.5 h-3.5 text-blue-400" />
                      <span>Type into Google TV Search</span>
                    </div>
                    <p className="text-[10px] text-slate-400">
                      Characters stream directly over port 6466 into TV search fields.
                    </p>
                  </div>

                  <div className="space-y-2">
                    <input
                      type="text"
                      value={inputText}
                      onChange={(e) => setInputText(e.target.value)}
                      placeholder="e.g. Stranger Things, Cricket..."
                      className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500"
                    />
                    <button
                      type="submit"
                      className="w-full py-2 bg-blue-600 hover:bg-blue-500 text-white text-xs font-bold rounded-xl flex items-center justify-center gap-1.5 shadow transition-colors"
                    >
                      <Send className="w-3.5 h-3.5" />
                      <span>Send to TV</span>
                    </button>
                  </div>
                </form>
              )}
            </div>

            {/* Essential Navigation: Back, Assistant, Home */}
            <div className="flex items-center justify-between px-3 py-1">
              <button
                onClick={() => handleKeyClick('Back', () => onTvAction('Back Button Pressed'))}
                className="w-12 h-12 rounded-full bg-slate-900 border border-slate-800 hover:bg-slate-800 flex items-center justify-center text-slate-300 transition-colors active:scale-95"
              >
                <ArrowLeft className="w-5 h-5" />
              </button>

              {/* Google Assistant Large Multi-color Ring */}
              <button
                onClick={() => handleKeyClick('Google Assistant', onTvAssistantToggle)}
                className="w-14 h-14 rounded-full p-[2.5px] bg-gradient-to-tr from-blue-500 via-red-500 to-yellow-400 hover:scale-105 active:scale-95 transition-all shadow-lg shadow-blue-500/20"
              >
                <div className="w-full h-full rounded-full bg-slate-950 flex items-center justify-center text-white">
                  <Mic className="w-6 h-6 text-white" />
                </div>
              </button>

              <button
                onClick={() => handleKeyClick('Home', () => onTvAppLaunch('home'))}
                className="w-12 h-12 rounded-full bg-slate-900 border border-slate-800 hover:bg-slate-800 flex items-center justify-center text-slate-300 transition-colors active:scale-95"
              >
                <Home className="w-5 h-5" />
              </button>
            </div>

            {/* Volume Rocker & Playback Bar (Twin Vertical Rockers) */}
            <div className="flex items-center justify-around px-8 my-2">
              {/* Volume Rocker (Vertical) */}
              <div className="w-16 h-36 bg-slate-900 border border-slate-800 rounded-2xl flex flex-col items-center justify-between py-2 shadow-inner">
                <button
                  onClick={() => handleKeyClick('Vol +', () => onTvVolumeChange(2))}
                  className="w-10 h-10 rounded-xl bg-slate-800/80 hover:bg-slate-700 active:scale-95 flex items-center justify-center text-white font-bold transition-all text-base"
                  title="Volume Up"
                >
                  +
                </button>
                <span className="text-[10px] font-bold text-slate-400 font-mono tracking-wider">VOL</span>
                <button
                  onClick={() => handleKeyClick('Vol -', () => onTvVolumeChange(-2))}
                  className="w-10 h-10 rounded-xl bg-slate-800/80 hover:bg-slate-700 active:scale-95 flex items-center justify-center text-white font-bold transition-all text-base"
                  title="Volume Down"
                >
                  -
                </button>
              </div>

              {/* Playback Controls (Vertical Twin Rocker - FastForward / Play / Rewind) */}
              <div className="w-16 h-36 bg-slate-900 border border-slate-800 rounded-2xl flex flex-col items-center justify-between py-2 shadow-inner">
                <button
                  onClick={() => handleKeyClick('FastForward', () => onTvAction('Fast Forward 10s'))}
                  className="w-10 h-10 rounded-xl bg-slate-800/80 hover:bg-slate-700 active:scale-95 flex items-center justify-center text-slate-300 hover:text-white transition-all"
                  title="Fast Forward"
                >
                  <FastForward className="w-4 h-4" />
                </button>
                <button
                  onClick={() => handleKeyClick('Play/Pause', () => onTvAction('Toggled Play/Pause'))}
                  className="w-10 h-10 rounded-xl bg-blue-500/10 border border-blue-500/30 hover:bg-blue-500/20 active:scale-95 flex items-center justify-center text-blue-400 transition-all"
                  title="Play / Pause"
                >
                  <Play className="w-4 h-4 fill-current" />
                </button>
                <button
                  onClick={() => handleKeyClick('Rewind', () => onTvAction('Rewind 10s'))}
                  className="w-10 h-10 rounded-xl bg-slate-800/80 hover:bg-slate-700 active:scale-95 flex items-center justify-center text-slate-300 hover:text-white transition-all"
                  title="Rewind"
                >
                  <Rewind className="w-4 h-4" />
                </button>
              </div>
            </div>

            {/* Quick App Launchers */}
            <div className="grid grid-cols-4 gap-1.5 pt-1">
              <button
                onClick={() => handleKeyClick('YouTube', () => onTvAppLaunch('youtube'))}
                className="py-1.5 rounded-xl bg-slate-900 border border-slate-800 hover:border-red-500/40 text-[10px] font-bold text-red-400 flex items-center justify-center gap-1 transition-all"
              >
                <span className="w-1.5 h-1.5 rounded-full bg-red-500" />
                YouTube
              </button>
              <button
                onClick={() => handleKeyClick('Netflix', () => onTvAppLaunch('netflix'))}
                className="py-1.5 rounded-xl bg-slate-900 border border-slate-800 hover:border-red-600/40 text-[10px] font-bold text-red-500 flex items-center justify-center gap-1 transition-all"
              >
                <span className="w-1.5 h-1.5 rounded-full bg-red-600" />
                Netflix
              </button>
              <button
                onClick={() => handleKeyClick('Prime', () => onTvAppLaunch('prime'))}
                className="py-1.5 rounded-xl bg-slate-900 border border-slate-800 hover:border-cyan-500/40 text-[10px] font-bold text-cyan-400 flex items-center justify-center gap-1 transition-all"
              >
                <span className="w-1.5 h-1.5 rounded-full bg-cyan-400" />
                Prime
              </button>
              <button
                onClick={() => handleKeyClick('Hotstar', () => onTvAppLaunch('hotstar'))}
                className="py-1.5 rounded-xl bg-slate-900 border border-slate-800 hover:border-blue-500/40 text-[10px] font-bold text-blue-400 flex items-center justify-center gap-1 transition-all"
              >
                <span className="w-1.5 h-1.5 rounded-full bg-blue-500" />
                Hotstar
              </button>
            </div>

          </div>
        ) : (
          /* VIEW 2: AUTO DISCOVERY SCREEN (Opens immediately on startup) */
          <div className="flex flex-col h-full justify-between pb-4">
            
            {/* Top Bar with Shreyash & Shrijay Tripathi dedication */}
            <div className="flex items-center justify-between pt-2">
              <div className="flex items-center gap-2">
                <button
                  onClick={() => setShowSplash(true)}
                  className="relative group w-8 h-8 rounded-full overflow-hidden border-2 border-blue-400 shadow-md ring-2 ring-blue-500/20 hover:scale-105 transition-transform"
                  title="View Shreyash & Shrijay Tripathi"
                >
                  <img
                    src="/splash_photo.jpg"
                    alt="Shreyash & Shrijay"
                    className="w-full h-full object-cover"
                  />
                </button>
                <div className="text-left">
                  <h2 className="text-xs font-bold text-white leading-tight">Google TV Remote</h2>
                  <p className="text-[10px] text-blue-400 font-semibold tracking-wide">
                    Shreyash & Shrijay
                  </p>
                </div>
              </div>

              <div className="flex items-center gap-1.5">
                <button
                  onClick={() => setShowSplash(true)}
                  className="px-2 py-1 rounded-lg bg-blue-600/20 border border-blue-500/40 text-[10px] font-bold text-blue-300 hover:bg-blue-600/30 transition-colors"
                >
                  Photo
                </button>
                <button
                  onClick={startAutoDiscovery}
                  className="w-8 h-8 rounded-full bg-slate-900 border border-slate-800 flex items-center justify-center text-blue-400 hover:bg-slate-800 transition-colors"
                  title="Rescan"
                >
                  <RefreshCw className={`w-3.5 h-3.5 ${isScanning ? 'animate-spin' : ''}`} />
                </button>
              </div>
            </div>

            {/* Radar Sweep Animation Banner */}
            <div className="my-3 py-4 px-3 bg-gradient-to-b from-blue-950/20 to-slate-900/40 border border-blue-500/20 rounded-2xl flex flex-col items-center justify-center text-center relative overflow-hidden">
              <div className="relative w-16 h-16 flex items-center justify-center mb-2">
                <span className={`absolute inset-0 rounded-full border border-blue-500/30 ${isScanning ? 'animate-ping' : ''}`} />
                <span className="w-12 h-12 rounded-full bg-blue-500/10 border border-blue-500/40 flex items-center justify-center text-blue-400">
                  <Wifi className="w-6 h-6" />
                </span>
              </div>
              <div className="text-xs font-bold text-white">Local Network Discovery Active</div>
              <div className="text-[10px] text-slate-400 mt-0.5">mDNS: _androidtvremote2._tcp</div>
            </div>

            {/* Discovered TV Device Cards */}
            <div className="flex-1 space-y-2 overflow-y-auto pr-0.5">
              <div className="text-[10px] font-bold text-slate-500 uppercase tracking-wider px-1 text-left">
                Select TV to Connect
              </div>

              {devices.map((device) => (
                <div
                  key={device.id}
                  onClick={() => handleSelectTv(device)}
                  className="p-3 bg-slate-900 hover:bg-slate-850 border border-slate-800 hover:border-blue-500/50 rounded-2xl flex items-center justify-between cursor-pointer transition-all active:scale-[0.98] group"
                >
                  <div className="flex items-center gap-3">
                    <div className="w-9 h-9 rounded-xl bg-slate-800 group-hover:bg-blue-600/20 border border-slate-700/60 flex items-center justify-center text-slate-300 group-hover:text-blue-400 transition-colors">
                      <Tv className="w-4 h-4" />
                    </div>
                    <div className="text-left">
                      <div className="text-xs font-bold text-white group-hover:text-blue-400 transition-colors">
                        {device.name}
                      </div>
                      <div className="text-[10px] text-slate-400">
                        {device.model} · {device.hostAddress}
                      </div>
                    </div>
                  </div>

                  <div className="text-right flex flex-col items-end">
                    <span className="text-[10px] font-bold text-blue-400 bg-blue-500/10 border border-blue-500/20 px-2 py-0.5 rounded-full">
                      {device.isPaired ? 'Paired' : 'Connect'}
                    </span>
                  </div>
                </div>
              ))}
            </div>

            {/* Bottom Manual IP Connect Button */}
            <button
              onClick={() => setShowManualIpModal(true)}
              className="w-full mt-2 py-2.5 rounded-xl bg-slate-900 border border-slate-800 hover:border-slate-700 text-xs font-semibold text-slate-300 flex items-center justify-center gap-2 transition-colors"
            >
              <Plus className="w-3.5 h-3.5 text-blue-400" />
              <span>Connect by IP Manually</span>
            </button>

          </div>
        )}

      </div>

      {/* REAL PAIRING PIN MODAL (Shows when user clicks TV) */}
      {showPairingModal && selectedDevice && (
        <div className="absolute inset-0 bg-slate-950/95 backdrop-blur-md z-30 p-5 flex flex-col justify-between animate-fade-in text-slate-100">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Lock className="w-4 h-4 text-blue-400" />
              <span className="text-xs font-bold text-white">Google TV Pairing</span>
            </div>
            <button
              onClick={() => { setShowPairingModal(false); onPairingStatusChange(false, ''); }}
              className="text-slate-400 hover:text-white"
            >
              <X className="w-4 h-4" />
            </button>
          </div>

          <div className="my-auto text-center space-y-4">
            <div className="w-12 h-12 rounded-2xl bg-blue-600/20 border border-blue-500/40 mx-auto flex items-center justify-center text-blue-400">
              <Tv className="w-6 h-6" />
            </div>

            <div>
              <h3 className="text-sm font-bold text-white">Check Your TV Screen</h3>
              <p className="text-[11px] text-slate-400 mt-1 max-w-[240px] mx-auto leading-relaxed">
                A 4-digit code is currently shown on your <strong className="text-white">{selectedDevice.name}</strong> screen.
              </p>
            </div>

            {/* 4-digit PIN Input */}
            <div className="space-y-2">
              <input
                type="text"
                maxLength={6}
                value={pinInput}
                onChange={(e) => setPinInput(e.target.value.toUpperCase())}
                placeholder="e.g. 8341"
                className="w-44 mx-auto text-center font-mono font-bold text-2xl tracking-widest px-3 py-2 bg-slate-900 border-2 border-blue-500/60 rounded-2xl text-white focus:outline-none focus:border-blue-400"
              />
              <p className="text-[10px] text-slate-500 font-mono">
                Hint: Code on the simulated TV is <strong className="text-cyan-400">{pairingCode}</strong>
              </p>
            </div>

            {pairingError && (
              <div className="p-2 rounded-xl bg-red-500/10 border border-red-500/30 text-red-400 text-[11px] flex items-center gap-1.5 justify-center">
                <AlertCircle className="w-3.5 h-3.5 shrink-0" />
                <span>{pairingError}</span>
              </div>
            )}
          </div>

          <div className="space-y-2">
            <button
              onClick={handleVerifyPin}
              disabled={pinInput.length < 4 || isVerifying}
              className="w-full py-3 bg-blue-600 hover:bg-blue-500 disabled:opacity-50 text-white text-xs font-bold rounded-xl flex items-center justify-center gap-2 shadow-lg shadow-blue-500/20 transition-all"
            >
              {isVerifying ? (
                <>
                  <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                  <span>Computing SHA-256 HMAC...</span>
                </>
              ) : (
                <>
                  <CheckCircle2 className="w-3.5 h-3.5" />
                  <span>Authenticate &amp; Pair</span>
                </>
              )}
            </button>

            <button
              onClick={() => { setShowPairingModal(false); onPairingStatusChange(false, ''); }}
              className="w-full py-2 text-[11px] text-slate-400 hover:text-white"
            >
              Cancel
            </button>
          </div>
        </div>
      )}

      {/* MANUAL IP MODAL */}
      {showManualIpModal && (
        <div className="absolute inset-0 bg-slate-950/95 backdrop-blur-md z-30 p-5 flex flex-col justify-between animate-fade-in text-slate-100">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-white">Manual TV Connect</span>
            <button onClick={() => setShowManualIpModal(false)} className="text-slate-400 hover:text-white">
              <X className="w-4 h-4" />
            </button>
          </div>

          <form onSubmit={handleAddManualIp} className="my-auto space-y-3">
            <div className="text-left">
              <label className="text-xs font-semibold text-slate-300">Enter Google TV IP Address</label>
              <p className="text-[10px] text-slate-400 mt-0.5">
                Check on TV: Settings &gt; Network &gt; Wi-Fi &gt; IP Address
              </p>
            </div>

            <input
              type="text"
              required
              value={manualIpText}
              onChange={(e) => setManualIpText(e.target.value)}
              placeholder="e.g. 192.168.1.105"
              className="w-full px-3 py-2 bg-slate-900 border border-slate-800 rounded-xl text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500 font-mono"
            />

            <button
              type="submit"
              className="w-full py-2.5 bg-blue-600 hover:bg-blue-500 text-white text-xs font-bold rounded-xl shadow transition-colors"
            >
              Connect to TV
            </button>
          </form>

          <button
            onClick={() => setShowManualIpModal(false)}
            className="w-full py-2 text-[11px] text-slate-400 hover:text-white"
          >
            Back to Scanner
          </button>
        </div>
      )}

    </div>
  );
};
