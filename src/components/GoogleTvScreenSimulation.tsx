import React from 'react';
import { 
  Tv, Volume2, VolumeX, Mic, Search, Wifi, CheckCircle2,
  Lock, Play, Youtube, Film, Radio
} from 'lucide-react';

interface GoogleTvScreenProps {
  tvName: string;
  ipAddress: string;
  isPairing: boolean;
  pairingCode: string;
  isConnected: boolean;
  activeApp: string;
  volume: number;
  isMuted: boolean;
  selectedCardIndex: number;
  searchQuery: string;
  isAssistantActive: boolean;
  powerState: boolean;
  lastActionLog: string;
}

export const GoogleTvScreenSimulation: React.FC<GoogleTvScreenProps> = ({
  tvName,
  ipAddress,
  isPairing,
  pairingCode,
  isConnected,
  activeApp,
  volume,
  isMuted,
  selectedCardIndex,
  searchQuery,
  isAssistantActive,
  powerState,
  lastActionLog
}) => {
  const apps = [
    { id: 'youtube', name: 'YouTube', color: 'from-red-600 to-red-700', icon: Youtube, desc: 'Trending Videos' },
    { id: 'netflix', name: 'Netflix', color: 'from-red-900 to-neutral-900', icon: Film, desc: 'Movies & Series' },
    { id: 'prime', name: 'Prime Video', color: 'from-sky-700 to-blue-900', icon: Film, desc: 'Amazon Originals' },
    { id: 'hotstar', name: 'Disney+ Hotstar', color: 'from-blue-900 to-indigo-950', icon: Film, desc: 'Live Sports & Shows' },
    { id: 'spotify', name: 'Spotify', color: 'from-emerald-700 to-green-900', icon: Radio, desc: 'Music & Podcasts' }
  ];

  if (!powerState) {
    return (
      <div className="w-full aspect-video bg-black rounded-2xl border-4 border-slate-800 flex flex-col items-center justify-center p-6 text-slate-600 shadow-2xl relative overflow-hidden">
        <Tv className="w-12 h-12 mb-3 opacity-30" />
        <span className="text-sm font-medium tracking-wide">Google TV is in Standby</span>
        <span className="text-xs text-slate-700 mt-1">Press Power button on remote to turn on</span>
        <div className="absolute bottom-3 left-3 flex items-center gap-1.5 text-[10px] text-slate-700 font-mono">
          <div className="w-1.5 h-1.5 rounded-full bg-red-500/50" />
          <span>{ipAddress} · Standby</span>
        </div>
      </div>
    );
  }

  return (
    <div className="w-full aspect-video bg-gradient-to-br from-slate-950 via-slate-900 to-slate-950 rounded-2xl border-4 border-slate-800 shadow-2xl relative overflow-hidden flex flex-col select-none">
      
      {/* TV Screen Top Navigation Bar */}
      <div className="h-12 border-b border-white/5 px-6 flex items-center justify-between bg-black/30 backdrop-blur-sm z-10">
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-2">
            <div className="w-5 h-5 rounded-full bg-gradient-to-tr from-blue-500 via-green-400 to-yellow-400 flex items-center justify-center shadow-sm">
              <Tv className="w-3 h-3 text-slate-950" />
            </div>
            <span className="font-bold text-sm tracking-tight text-white">Google TV</span>
          </div>

          <div className="flex items-center gap-3 text-xs text-slate-400 pl-4 border-l border-white/10">
            <span className="text-white font-medium">For You</span>
            <span className="hover:text-white transition-colors">Live</span>
            <span className="hover:text-white transition-colors">Movies</span>
            <span className="hover:text-white transition-colors">Shows</span>
            <span className="hover:text-white transition-colors">Apps</span>
          </div>
        </div>

        <div className="flex items-center gap-4 text-xs font-mono">
          {/* Active Search Query Bar */}
          {searchQuery && (
            <div className="flex items-center gap-2 px-3 py-1 bg-blue-500/10 border border-blue-500/30 rounded-full text-blue-300 animate-pulse">
              <Search className="w-3 h-3" />
              <span>Search: "{searchQuery}"</span>
            </div>
          )}

          {/* Volume Indicator Overlay */}
          <div className="flex items-center gap-1.5 px-2.5 py-1 bg-white/5 rounded-full border border-white/10 text-slate-300">
            {isMuted ? (
              <VolumeX className="w-3.5 h-3.5 text-red-400" />
            ) : (
              <Volume2 className="w-3.5 h-3.5 text-cyan-400" />
            )}
            <span className="font-semibold">{isMuted ? 'MUTE' : volume}</span>
          </div>

          <div className="flex items-center gap-1.5 text-slate-400">
            <Wifi className="w-3.5 h-3.5 text-emerald-400" />
            <span className="text-[11px]">{ipAddress}</span>
          </div>
        </div>
      </div>

      {/* Main TV Screen Content Area */}
      <div className="flex-1 p-6 flex flex-col justify-between relative z-0">
        
        {/* Hero Banner / Active App Backdrop */}
        <div className="flex-1 rounded-xl bg-gradient-to-r from-blue-900/30 via-indigo-950/40 to-slate-900/40 border border-white/5 p-6 flex flex-col justify-end relative overflow-hidden">
          <div className="absolute top-4 right-4 flex items-center gap-2">
            {isConnected && (
              <span className="flex items-center gap-1.5 text-[11px] font-mono text-emerald-400 bg-emerald-500/10 border border-emerald-500/20 px-2.5 py-1 rounded-full">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-ping" />
                TLS Channel Active · Port 6466
              </span>
            )}
          </div>

          <span className="text-xs font-semibold uppercase tracking-wider text-blue-400">
            Featured on Google TV
          </span>
          <h2 className="text-2xl font-bold text-white mt-1">
            {activeApp === 'home' ? 'Discover Movies, Series & Live Channels' : `Now Playing: ${activeApp.toUpperCase()}`}
          </h2>
          <p className="text-xs text-slate-300 max-w-xl mt-1 leading-relaxed">
            Personalized recommendations based on your Google account across streaming subscriptions.
          </p>

          {/* Google Assistant Listening Dots Simulation */}
          {isAssistantActive && (
            <div className="mt-4 flex items-center gap-3 p-3 bg-slate-900/90 border border-blue-500/40 rounded-xl backdrop-blur-md max-w-md animate-fade-in">
              <div className="flex items-center gap-1.5">
                <span className="w-2.5 h-2.5 rounded-full bg-blue-500 animate-bounce" style={{ animationDelay: '0ms' }} />
                <span className="w-2.5 h-2.5 rounded-full bg-red-500 animate-bounce" style={{ animationDelay: '150ms' }} />
                <span className="w-2.5 h-2.5 rounded-full bg-yellow-400 animate-bounce" style={{ animationDelay: '300ms' }} />
                <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-bounce" style={{ animationDelay: '450ms' }} />
              </div>
              <span className="text-xs text-slate-200 font-medium">Google Assistant is listening... Speak into phone</span>
            </div>
          )}
        </div>

        {/* Row of Apps / Cards with Active Selection Ring */}
        <div className="mt-4">
          <div className="flex items-center justify-between text-xs text-slate-400 mb-2">
            <span className="font-semibold text-slate-300">Your Apps &amp; Channels</span>
            <span className="font-mono text-[11px] text-slate-500">Navigate using D-Pad on Remote</span>
          </div>

          <div className="grid grid-cols-5 gap-3">
            {apps.map((app, idx) => {
              const isSelected = selectedCardIndex === idx;
              const IconComp = app.icon;
              return (
                <div
                  key={app.id}
                  className={`p-3 rounded-xl border transition-all duration-200 flex flex-col justify-between ${
                    isSelected
                      ? 'border-blue-400 bg-blue-500/20 shadow-lg shadow-blue-500/20 scale-[1.03] ring-2 ring-blue-400'
                      : 'border-white/5 bg-white/5 hover:border-white/10'
                  }`}
                >
                  <div className={`w-8 h-8 rounded-lg bg-gradient-to-tr ${app.color} flex items-center justify-center mb-2 shadow`}>
                    <IconComp className="w-4 h-4 text-white" />
                  </div>
                  <div>
                    <div className="text-xs font-bold text-white tracking-tight">{app.name}</div>
                    <div className="text-[10px] text-slate-400 truncate">{app.desc}</div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>

      </div>

      {/* Bottom Live Action Telemetry Strip */}
      <div className="h-8 border-t border-white/5 bg-black/40 px-4 flex items-center justify-between text-[11px] font-mono text-slate-400">
        <div className="flex items-center gap-2">
          <span className="text-slate-500">Last Remote Event:</span>
          <span className="text-cyan-300 font-semibold">{lastActionLog || 'Listening for commands on 6466...'}</span>
        </div>
        <div className="flex items-center gap-3 text-slate-500 text-[10px]">
          <span>Protocol: ATV Remote v2</span>
          <span>·</span>
          <span>Target: 3840×2160 @ 60Hz</span>
        </div>
      </div>

      {/* AUTHENTIC GOOGLE TV PAIRING POPUP MODAL (Appears on TV screen during pairing) */}
      {isPairing && (
        <div className="absolute inset-0 bg-black/85 backdrop-blur-md flex items-center justify-center p-6 z-50 animate-fade-in">
          <div className="max-w-md w-full bg-slate-900 border-2 border-blue-500 rounded-3xl p-8 text-center shadow-2xl shadow-blue-500/30">
            <div className="w-14 h-14 mx-auto rounded-2xl bg-blue-600/20 border border-blue-500/40 flex items-center justify-center text-blue-400 mb-4">
              <Lock className="w-7 h-7" />
            </div>

            <h3 className="text-xl font-bold text-white tracking-tight">
              Remote Control Pairing Request
            </h3>
            <p className="text-xs text-slate-400 mt-1.5 leading-relaxed">
              A device is requesting to pair as your Google TV remote over secure TLS. Enter this code into your phone:
            </p>

            {/* Large 4-digit PIN Code on TV Screen */}
            <div className="my-6 py-4 px-6 bg-slate-950 rounded-2xl border border-blue-500/30 flex items-center justify-center gap-4">
              {pairingCode.split('').map((char, index) => (
                <div
                  key={index}
                  className="w-12 h-14 rounded-xl bg-blue-500/10 border-2 border-blue-400 flex items-center justify-center text-3xl font-mono font-bold text-white shadow-inner"
                >
                  {char}
                </div>
              ))}
            </div>

            <div className="flex items-center justify-center gap-2 text-[11px] font-mono text-slate-400">
              <div className="w-2 h-2 rounded-full bg-yellow-400 animate-pulse" />
              <span>Awaiting secret verification on Port 6467...</span>
            </div>
          </div>
        </div>
      )}

    </div>
  );
};
