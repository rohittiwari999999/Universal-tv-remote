import React, { useState, useEffect } from 'react';
import { 
  Power, Volume2, VolumeX, ChevronUp, ChevronDown, ChevronLeft, ChevronRight,
  Tv, Radio, Wifi, Bluetooth, RotateCcw, Home, Menu, Grid, ArrowLeft,
  Play, Film, Sparkles, Check
} from 'lucide-react';
import { TvBrand, TV_BRANDS_DATABASE, TvKeyDefinition } from '../data/tvBrandsData';
import { remoteSound } from '../utils/audioFeedback';

interface RemoteSimulatorProps {
  activeBrand: TvBrand;
  onBrandChange: (brand: TvBrand) => void;
  activeMode: 'IR' | 'BLE' | 'Wi-Fi';
  onModeChange: (mode: 'IR' | 'BLE' | 'Wi-Fi') => void;
  onTransmitSignal: (keyName: string, keyDef: TvKeyDefinition, brand: TvBrand, mode: 'IR' | 'BLE' | 'Wi-Fi') => void;
  isTransmitting: boolean;
}

export const RemoteSimulator: React.FC<RemoteSimulatorProps> = ({
  activeBrand,
  onBrandChange,
  activeMode,
  onModeChange,
  onTransmitSignal,
  isTransmitting,
}) => {
  const [selectedKey, setSelectedKey] = useState<string | null>(null);
  const [showNumpad, setShowNumpad] = useState<boolean>(activeBrand.layoutType === 'classic_keypad');

  useEffect(() => {
    setShowNumpad(activeBrand.layoutType === 'classic_keypad');
  }, [activeBrand]);

  const handleKeyPress = (keyName: string, fallbackLabel: string, category: TvKeyDefinition['category']) => {
    remoteSound.playClick();
    if (keyName === 'power') {
      remoteSound.playPower();
    } else {
      remoteSound.playIrChirp();
    }

    const keyDef = activeBrand.keys[keyName] || {
      label: fallbackLabel,
      necHex: '0x00FF' + Math.floor(Math.random() * 65535).toString(16).toUpperCase().padStart(4, '0'),
      category,
    };

    setSelectedKey(keyName);
    onTransmitSignal(keyName, keyDef, activeBrand, activeMode);

    setTimeout(() => {
      setSelectedKey(null);
    }, 220);
  };

  return (
    <div className="flex flex-col items-center">
      {/* Remote Top Controls & Mode Switcher */}
      <div className="w-full max-w-[340px] mb-3 flex items-center justify-between text-xs text-slate-400">
        <div className="flex items-center gap-1.5 p-1 bg-slate-900 border border-slate-800 rounded-lg">
          <button
            onClick={() => onModeChange('IR')}
            className={`flex items-center gap-1 px-2.5 py-1 rounded text-xs font-medium transition-colors ${
              activeMode === 'IR' ? 'bg-red-500/20 text-red-400 border border-red-500/30' : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <Radio className="w-3 h-3" />
            <span>IR (38kHz)</span>
          </button>
          <button
            onClick={() => onModeChange('BLE')}
            className={`flex items-center gap-1 px-2.5 py-1 rounded text-xs font-medium transition-colors ${
              activeMode === 'BLE' ? 'bg-blue-500/20 text-blue-400 border border-blue-500/30' : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <Bluetooth className="w-3 h-3" />
            <span>BLE</span>
          </button>
          <button
            onClick={() => onModeChange('Wi-Fi')}
            className={`flex items-center gap-1 px-2.5 py-1 rounded text-xs font-medium transition-colors ${
              activeMode === 'Wi-Fi' ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30' : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <Wifi className="w-3 h-3" />
            <span>Wi-Fi</span>
          </button>
        </div>

        <button
          onClick={() => setShowNumpad(!showNumpad)}
          className={`px-2 py-1 rounded border text-xs font-mono transition-colors flex items-center gap-1 ${
            showNumpad ? 'bg-slate-800 text-amber-400 border-amber-500/30' : 'bg-slate-900 text-slate-400 border-slate-800'
          }`}
        >
          <Grid className="w-3 h-3" />
          <span>123</span>
        </button>
      </div>

      {/* Brand Selection Bar */}
      <div className="w-full max-w-[340px] mb-3">
        <label className="block text-[11px] font-mono text-slate-400 mb-1">
          Active TV Brand Preset:
        </label>
        <div className="relative">
          <select
            value={activeBrand.id}
            onChange={(e) => {
              const brand = TV_BRANDS_DATABASE.find(b => b.id === e.target.value);
              if (brand) onBrandChange(brand);
            }}
            className="w-full bg-slate-900 border border-slate-800 text-slate-100 text-xs rounded-lg px-3 py-2 pr-8 appearance-none focus:outline-none focus:border-cyan-500 cursor-pointer"
          >
            <optgroup label="Global Flagship Brands">
              {TV_BRANDS_DATABASE.filter(b => b.category === 'Global').map(b => (
                <option key={b.id} value={b.id}>
                  {b.name} ({b.protocol} · {b.carrierFrequency / 1000}kHz)
                </option>
              ))}
            </optgroup>
            <optgroup label="Indian Local Brands">
              {TV_BRANDS_DATABASE.filter(b => b.category === 'Indian Local').map(b => (
                <option key={b.id} value={b.id}>
                  {b.name} ({b.protocol} · {b.carrierFrequency / 1000}kHz)
                </option>
              ))}
            </optgroup>
          </select>
          <ChevronDown className="w-4 h-4 text-slate-400 absolute right-2.5 top-2.5 pointer-events-none" />
        </div>
      </div>

      {/* Physical Virtual Smartphone Remote Device */}
      <div className="relative w-[340px] bg-gradient-to-b from-neutral-900 via-neutral-950 to-black rounded-[36px] p-5 shadow-2xl border border-neutral-800 ring-1 ring-white/10">
        
        {/* Hardware IR Blaster Emitter LED on top bezel */}
        <div className="absolute -top-1.5 left-1/2 -translate-x-1/2 flex items-center justify-center">
          <div 
            className={`w-3.5 h-2 rounded-t-full transition-all duration-100 ${
              isTransmitting
                ? activeMode === 'IR' 
                  ? 'bg-red-500 shadow-[0_0_16px_#ef4444]' 
                  : activeMode === 'BLE' 
                  ? 'bg-blue-500 shadow-[0_0_16px_#3b82f6]' 
                  : 'bg-emerald-500 shadow-[0_0_16px_#10b981]'
                : 'bg-neutral-800'
            }`}
          />
        </div>

        {/* Remote Head Bar: Brand Wordmark + Protocol Status */}
        <div className="flex items-center justify-between border-b border-neutral-800/80 pb-3 mb-4">
          <div className="flex items-center gap-2">
            <div 
              className="w-2.5 h-2.5 rounded-full" 
              style={{ backgroundColor: activeBrand.accentColor || '#38bdf8' }} 
            />
            <span className="font-semibold text-sm tracking-wide text-white">
              {activeBrand.name}
            </span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="text-[10px] font-mono text-neutral-400 bg-neutral-900 px-2 py-0.5 rounded border border-neutral-800">
              {activeMode} {activeMode === 'IR' ? `${activeBrand.carrierFrequency / 1000}kHz` : ''}
            </span>
          </div>
        </div>

        {/* Row 1: Power & Mute */}
        <div className="flex items-center justify-between mb-4">
          <button
            onClick={() => handleKeyPress('power', 'Power', 'power')}
            className={`w-12 h-12 rounded-full flex items-center justify-center transition-all duration-100 shadow-md ${
              selectedKey === 'power'
                ? 'bg-red-500 text-white scale-95 shadow-red-500/50'
                : 'bg-red-950/80 text-red-400 border border-red-900/60 hover:bg-red-900/80'
            }`}
            title="Power ON/OFF"
          >
            <Power className="w-5 h-5" />
          </button>

          <div className="text-center">
            <span className="text-[10px] font-mono uppercase tracking-wider text-neutral-500">
              {activeBrand.category}
            </span>
          </div>

          <button
            onClick={() => handleKeyPress('mute', 'Mute', 'volume')}
            className={`w-12 h-12 rounded-full flex items-center justify-center transition-all duration-100 ${
              selectedKey === 'mute'
                ? 'bg-neutral-600 text-white scale-95'
                : 'bg-neutral-900 text-neutral-400 border border-neutral-800 hover:text-white'
            }`}
            title="Mute Audio"
          >
            <VolumeX className="w-5 h-5" />
          </button>
        </div>

        {/* Optional 0-9 Numeric Keypad Overlay for Classic Indian CRT/LED TVs */}
        {showNumpad && (
          <div className="bg-neutral-900/90 border border-neutral-800 rounded-2xl p-3 mb-4 animate-in fade-in duration-200">
            <div className="grid grid-cols-3 gap-2">
              {[1, 2, 3, 4, 5, 6, 7, 8, 9].map((num) => (
                <button
                  key={num}
                  onClick={() => handleKeyPress(`num_${num}`, `${num}`, 'number')}
                  className={`h-9 rounded-lg font-mono text-sm font-semibold transition-all ${
                    selectedKey === `num_${num}`
                      ? 'bg-cyan-500 text-black scale-95'
                      : 'bg-neutral-800 text-neutral-200 hover:bg-neutral-700'
                  }`}
                >
                  {num}
                </button>
              ))}
              <button
                onClick={() => handleKeyPress('source', 'AV/TV', 'function')}
                className="h-9 rounded-lg font-mono text-[11px] font-medium bg-neutral-800 text-neutral-400 hover:text-white"
              >
                AV
              </button>
              <button
                onClick={() => handleKeyPress('num_0', '0', 'number')}
                className={`h-9 rounded-lg font-mono text-sm font-semibold transition-all ${
                  selectedKey === 'num_0'
                    ? 'bg-cyan-500 text-black scale-95'
                    : 'bg-neutral-800 text-neutral-200 hover:bg-neutral-700'
                }`}
              >
                0
              </button>
              <button
                onClick={() => handleKeyPress('menu', 'Menu', 'function')}
                className="h-9 rounded-lg font-mono text-[11px] font-medium bg-neutral-800 text-neutral-400 hover:text-white"
              >
                MENU
              </button>
            </div>
          </div>
        )}

        {/* Function Row: Source / Input, Menu, Home */}
        <div className="grid grid-cols-3 gap-2 mb-4">
          <button
            onClick={() => handleKeyPress('source', 'Input', 'function')}
            className={`py-2 px-2 rounded-xl text-xs font-medium transition-all ${
              selectedKey === 'source'
                ? 'bg-neutral-600 text-white scale-95'
                : 'bg-neutral-900 text-neutral-300 border border-neutral-800 hover:bg-neutral-800'
            }`}
          >
            Source
          </button>
          <button
            onClick={() => handleKeyPress('home', 'Home', 'nav')}
            className={`py-2 px-2 rounded-xl text-xs font-medium flex items-center justify-center gap-1 transition-all ${
              selectedKey === 'home'
                ? 'bg-cyan-600 text-white scale-95'
                : 'bg-neutral-900 text-cyan-400 border border-neutral-800 hover:bg-neutral-800'
            }`}
          >
            <Home className="w-3.5 h-3.5" />
            <span>Home</span>
          </button>
          <button
            onClick={() => handleKeyPress('menu', 'Settings', 'function')}
            className={`py-2 px-2 rounded-xl text-xs font-medium transition-all ${
              selectedKey === 'menu'
                ? 'bg-neutral-600 text-white scale-95'
                : 'bg-neutral-900 text-neutral-300 border border-neutral-800 hover:bg-neutral-800'
            }`}
          >
            Menu
          </button>
        </div>

        {/* Directional Navigation D-Pad */}
        <div className="relative w-48 h-48 mx-auto my-3 bg-neutral-900/90 rounded-full border border-neutral-800 p-2 shadow-inner flex items-center justify-center">
          {/* UP */}
          <button
            onClick={() => handleKeyPress('dpad_up', 'Up', 'nav')}
            className={`absolute top-2 w-12 h-10 flex items-center justify-center text-neutral-400 hover:text-white transition-all ${
              selectedKey === 'dpad_up' ? 'scale-90 text-cyan-400' : ''
            }`}
            title="Navigate Up"
          >
            <ChevronUp className="w-6 h-6" />
          </button>

          {/* DOWN */}
          <button
            onClick={() => handleKeyPress('dpad_down', 'Down', 'nav')}
            className={`absolute bottom-2 w-12 h-10 flex items-center justify-center text-neutral-400 hover:text-white transition-all ${
              selectedKey === 'dpad_down' ? 'scale-90 text-cyan-400' : ''
            }`}
            title="Navigate Down"
          >
            <ChevronDown className="w-6 h-6" />
          </button>

          {/* LEFT */}
          <button
            onClick={() => handleKeyPress('dpad_left', 'Left', 'nav')}
            className={`absolute left-2 w-10 h-12 flex items-center justify-center text-neutral-400 hover:text-white transition-all ${
              selectedKey === 'dpad_left' ? 'scale-90 text-cyan-400' : ''
            }`}
            title="Navigate Left"
          >
            <ChevronLeft className="w-6 h-6" />
          </button>

          {/* RIGHT */}
          <button
            onClick={() => handleKeyPress('dpad_right', 'Right', 'nav')}
            className={`absolute right-2 w-10 h-12 flex items-center justify-center text-neutral-400 hover:text-white transition-all ${
              selectedKey === 'dpad_right' ? 'scale-90 text-cyan-400' : ''
            }`}
            title="Navigate Right"
          >
            <ChevronRight className="w-6 h-6" />
          </button>

          {/* OK / SELECT CENTER */}
          <button
            onClick={() => handleKeyPress('dpad_ok', 'OK', 'nav')}
            className={`w-20 h-20 rounded-full bg-gradient-to-b from-neutral-800 to-neutral-850 border border-neutral-700 font-semibold text-xs tracking-wider flex items-center justify-center shadow-md transition-all ${
              selectedKey === 'dpad_ok'
                ? 'bg-cyan-500 text-black scale-95 shadow-cyan-500/40'
                : 'text-neutral-200 hover:text-white hover:border-neutral-600'
            }`}
          >
            OK
          </button>
        </div>

        {/* Back and Return Row */}
        <div className="flex items-center justify-between px-4 mb-4">
          <button
            onClick={() => handleKeyPress('back', 'Back', 'nav')}
            className={`flex items-center gap-1.5 py-1.5 px-3 rounded-lg text-xs font-medium transition-all ${
              selectedKey === 'back'
                ? 'bg-neutral-700 text-white scale-95'
                : 'bg-neutral-900 text-neutral-400 border border-neutral-800 hover:text-white'
            }`}
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            <span>Back</span>
          </button>

          {/* Mi PatchWall or Brand Special Key */}
          {activeBrand.keys['patchwall'] ? (
            <button
              onClick={() => handleKeyPress('patchwall', 'PatchWall', 'function')}
              className={`py-1.5 px-3 rounded-lg text-xs font-medium font-mono text-orange-400 bg-orange-950/40 border border-orange-800/60 transition-all ${
                selectedKey === 'patchwall' ? 'scale-95 bg-orange-800 text-white' : ''
              }`}
            >
              PatchWall
            </button>
          ) : (
            <span className="text-[10px] font-mono text-neutral-600">
              {activeBrand.protocol}
            </span>
          )}

          <button
            onClick={() => handleKeyPress('menu', 'Info', 'function')}
            className={`flex items-center gap-1.5 py-1.5 px-3 rounded-lg text-xs font-medium transition-all ${
              selectedKey === 'menu'
                ? 'bg-neutral-700 text-white scale-95'
                : 'bg-neutral-900 text-neutral-400 border border-neutral-800 hover:text-white'
            }`}
          >
            <Menu className="w-3.5 h-3.5" />
            <span>Info</span>
          </button>
        </div>

        {/* Dual Rockers: Volume (Left) & Channel (Right) */}
        <div className="flex items-center justify-between px-4 mb-5">
          {/* Volume Rocker */}
          <div className="flex flex-col items-center bg-neutral-900 border border-neutral-800 rounded-2xl p-1 w-14">
            <button
              onClick={() => handleKeyPress('vol_up', 'Vol +', 'volume')}
              className={`w-full py-2.5 flex items-center justify-center text-neutral-300 hover:text-white transition-all ${
                selectedKey === 'vol_up' ? 'text-cyan-400 scale-90' : ''
              }`}
              title="Volume Up"
            >
              <ChevronUp className="w-5 h-5" />
            </button>
            <span className="text-[10px] font-mono font-medium text-neutral-500 py-1">
              VOL
            </span>
            <button
              onClick={() => handleKeyPress('vol_down', 'Vol -', 'volume')}
              className={`w-full py-2.5 flex items-center justify-center text-neutral-300 hover:text-white transition-all ${
                selectedKey === 'vol_down' ? 'text-cyan-400 scale-90' : ''
              }`}
              title="Volume Down"
            >
              <ChevronDown className="w-5 h-5" />
            </button>
          </div>

          {/* Quick TV Logo Badge */}
          <div className="flex flex-col items-center">
            <Tv className="w-6 h-6 text-neutral-700 mb-1" />
            <span className="text-[9px] font-mono text-neutral-600 uppercase">
              {activeBrand.name}
            </span>
          </div>

          {/* Channel Rocker */}
          <div className="flex flex-col items-center bg-neutral-900 border border-neutral-800 rounded-2xl p-1 w-14">
            <button
              onClick={() => handleKeyPress('ch_up', 'CH +', 'channel')}
              className={`w-full py-2.5 flex items-center justify-center text-neutral-300 hover:text-white transition-all ${
                selectedKey === 'ch_up' ? 'text-cyan-400 scale-90' : ''
              }`}
              title="Channel Up"
            >
              <ChevronUp className="w-5 h-5" />
            </button>
            <span className="text-[10px] font-mono font-medium text-neutral-500 py-1">
              CH
            </span>
            <button
              onClick={() => handleKeyPress('ch_down', 'CH -', 'channel')}
              className={`w-full py-2.5 flex items-center justify-center text-neutral-300 hover:text-white transition-all ${
                selectedKey === 'ch_down' ? 'text-cyan-400 scale-90' : ''
              }`}
              title="Channel Down"
            >
              <ChevronDown className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Dedicated OTT Shortcuts (Netflix, YouTube, Prime, Hotstar) */}
        <div className="grid grid-cols-2 gap-2 pt-2 border-t border-neutral-900">
          <button
            onClick={() => handleKeyPress('netflix', 'Netflix', 'ott')}
            className={`py-2 px-3 rounded-xl text-[11px] font-bold tracking-wider transition-all flex items-center justify-center gap-1.5 ${
              selectedKey === 'netflix'
                ? 'bg-red-600 text-white scale-95 shadow-red-600/50 shadow-md'
                : 'bg-neutral-900 text-red-500 border border-red-950/60 hover:bg-neutral-850'
            }`}
          >
            <span className="font-extrabold text-xs">NETFLIX</span>
          </button>

          <button
            onClick={() => handleKeyPress('youtube', 'YouTube', 'ott')}
            className={`py-2 px-3 rounded-xl text-[11px] font-bold tracking-wider transition-all flex items-center justify-center gap-1.5 ${
              selectedKey === 'youtube'
                ? 'bg-red-700 text-white scale-95 shadow-red-700/50 shadow-md'
                : 'bg-neutral-900 text-neutral-200 border border-neutral-800 hover:bg-neutral-850'
            }`}
          >
            <Play className="w-3.5 h-3.5 fill-red-500 text-red-500" />
            <span>YouTube</span>
          </button>

          <button
            onClick={() => handleKeyPress('prime', 'Prime Video', 'ott')}
            className={`py-2 px-3 rounded-xl text-[11px] font-bold tracking-wider transition-all flex items-center justify-center gap-1.5 ${
              selectedKey === 'prime'
                ? 'bg-sky-600 text-white scale-95 shadow-sky-600/50 shadow-md'
                : 'bg-neutral-900 text-sky-400 border border-sky-950/60 hover:bg-neutral-850'
            }`}
          >
            <Film className="w-3.5 h-3.5 text-sky-400" />
            <span>Prime</span>
          </button>

          <button
            onClick={() => handleKeyPress('hotstar', 'Hotstar', 'ott')}
            className={`py-2 px-3 rounded-xl text-[11px] font-bold tracking-wider transition-all flex items-center justify-center gap-1.5 ${
              selectedKey === 'hotstar'
                ? 'bg-blue-600 text-white scale-95 shadow-blue-600/50 shadow-md'
                : 'bg-neutral-900 text-blue-400 border border-blue-950/60 hover:bg-neutral-850'
            }`}
          >
            <Sparkles className="w-3.5 h-3.5 text-blue-400" />
            <span>Hotstar</span>
          </button>
        </div>

      </div>

      <div className="mt-3 text-center text-[11px] text-slate-500 font-mono">
        Tap any button to emit live carrier pulses &amp; inspect signal
      </div>
    </div>
  );
};
