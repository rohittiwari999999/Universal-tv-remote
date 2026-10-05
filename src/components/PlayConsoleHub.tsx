import React, { useState } from 'react';
import { 
  FolderCheck, FileText, ShieldCheck, CheckCircle2, Copy, Download,
  ExternalLink, Key, AlertTriangle, Terminal, Code2, Sparkles, Check
} from 'lucide-react';
import { PLAY_CONSOLE_FILES, PlayConsoleFile } from '../data/playConsoleData';

export const PlayConsoleHub: React.FC = () => {
  const [selectedFile, setSelectedFile] = useState<PlayConsoleFile>(PLAY_CONSOLE_FILES[0]);
  const [copied, setCopied] = useState(false);
  const [filterCategory, setFilterCategory] = useState<'All' | 'Listing & Policy' | 'Android Config' | 'Publishing Guides'>('All');

  const filteredFiles = filterCategory === 'All' 
    ? PLAY_CONSOLE_FILES 
    : PLAY_CONSOLE_FILES.filter(f => f.category === filterCategory);

  const handleCopyContent = () => {
    navigator.clipboard.writeText(selectedFile.content);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleDownloadFile = () => {
    const blob = new Blob([selectedFile.content], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = selectedFile.name;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  return (
    <div className="flex flex-col gap-6">
      {/* Top Banner: Play Console Compliance & Audit Summary */}
      <div className="bg-gradient-to-r from-slate-900 via-indigo-950/40 to-slate-900 border border-indigo-500/20 rounded-2xl p-5">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <FolderCheck className="w-5 h-5 text-indigo-400" />
              <h2 className="text-base font-semibold text-white">
                Google Play Console Release &amp; Publishing Assets
              </h2>
            </div>
            <p className="text-xs text-slate-400 max-w-2xl leading-relaxed">
              All production files, declarations, store copy, privacy policies, and Android build configurations have been structured and generated in the <code className="text-indigo-300 bg-slate-950 px-1.5 py-0.5 rounded font-mono text-[11px]">/play_console_release/</code> folder.
            </p>
          </div>

          <div className="flex items-center gap-3">
            <div className="text-right hidden sm:block">
              <div className="text-[11px] font-mono text-emerald-400 font-medium">Ready for Submission</div>
              <div className="text-[11px] font-mono text-slate-400">Target SDK 35 · 10 Assets</div>
            </div>
            <button
              onClick={() => {
                const allContent = PLAY_CONSOLE_FILES.map(f => `=== FILE: ${f.name} ===\n${f.content}\n\n`).join('');
                const blob = new Blob([allContent], { type: 'text/plain;charset=utf-8' });
                const url = URL.createObjectURL(blob);
                const link = document.createElement('a');
                link.href = url;
                link.download = 'play_console_all_release_files.txt';
                link.click();
                URL.revokeObjectURL(url);
              }}
              className="flex items-center gap-2 px-3 py-2 bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg text-xs font-medium transition-colors shadow-sm"
            >
              <Download className="w-3.5 h-3.5" />
              <span>Download All Bundle</span>
            </button>
          </div>
        </div>

        {/* Play Console Policy Audit Badges */}
        <div className="mt-4 pt-4 border-t border-indigo-500/10 grid grid-cols-2 sm:grid-cols-4 gap-2.5">
          <div className="flex items-start gap-2 bg-slate-950/60 p-2.5 rounded-lg border border-slate-800">
            <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
            <div>
              <div className="text-xs font-semibold text-white">IR Non-Required</div>
              <div className="text-[10px] text-slate-400">Installs on non-IR phones</div>
            </div>
          </div>

          <div className="flex items-start gap-2 bg-slate-950/60 p-2.5 rounded-lg border border-slate-800">
            <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
            <div>
              <div className="text-xs font-semibold text-white">Target SDK 35</div>
              <div className="text-[10px] text-slate-400">2026 Play Store standard</div>
            </div>
          </div>

          <div className="flex items-start gap-2 bg-slate-950/60 p-2.5 rounded-lg border border-slate-800">
            <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
            <div>
              <div className="text-xs font-semibold text-white">neverForLocation</div>
              <div className="text-[10px] text-slate-400">BLE permissions cleared</div>
            </div>
          </div>

          <div className="flex items-start gap-2 bg-slate-950/60 p-2.5 rounded-lg border border-slate-800">
            <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
            <div>
              <div className="text-xs font-semibold text-white">Data Safety Form</div>
              <div className="text-[10px] text-slate-400">Zero PII collected certified</div>
            </div>
          </div>
        </div>
      </div>

      {/* Main File Explorer & Live Viewer */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-5">
        {/* Left Column: File List with Categories */}
        <div className="lg:col-span-5 flex flex-col gap-3">
          {/* Category Tabs */}
          <div className="flex items-center gap-1 p-1 bg-slate-900 border border-slate-800 rounded-lg text-xs">
            {(['All', 'Listing & Policy', 'Android Config', 'Publishing Guides'] as const).map(cat => (
              <button
                key={cat}
                onClick={() => setFilterCategory(cat)}
                className={`flex-1 py-1.5 px-2 rounded-md font-medium transition-colors text-center truncate ${
                  filterCategory === cat
                    ? 'bg-slate-800 text-white shadow-sm'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                {cat}
              </button>
            ))}
          </div>

          {/* File Item Cards */}
          <div className="space-y-2 max-h-[600px] overflow-y-auto pr-1">
            {filteredFiles.map((file) => {
              const isSelected = selectedFile.id === file.id;
              return (
                <div
                  key={file.id}
                  onClick={() => setSelectedFile(file)}
                  className={`p-3 rounded-xl border text-left cursor-pointer transition-all ${
                    isSelected
                      ? 'bg-slate-900 border-indigo-500/50 shadow-md ring-1 ring-indigo-500/20'
                      : 'bg-slate-950/70 border-slate-800/80 hover:bg-slate-900/60 hover:border-slate-700'
                  }`}
                >
                  <div className="flex items-center justify-between mb-1">
                    <div className="flex items-center gap-2">
                      <FileText className={`w-4 h-4 ${isSelected ? 'text-indigo-400' : 'text-slate-500'}`} />
                      <span className="font-mono text-xs font-medium text-slate-200 truncate max-w-[210px]">
                        {file.name}
                      </span>
                    </div>
                    <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-slate-800 text-slate-300 border border-slate-700">
                      {file.badge}
                    </span>
                  </div>
                  <p className="text-[11px] text-slate-400 line-clamp-2 leading-relaxed">
                    {file.summary}
                  </p>
                </div>
              );
            })}
          </div>
        </div>

        {/* Right Column: Code / Content Inspector */}
        <div className="lg:col-span-7 bg-slate-900 border border-slate-800 rounded-2xl flex flex-col overflow-hidden">
          {/* Viewer Toolbar */}
          <div className="p-3.5 bg-slate-950/80 border-b border-slate-800 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Code2 className="w-4 h-4 text-indigo-400" />
              <div>
                <span className="font-mono text-xs text-white font-medium">
                  {selectedFile.name}
                </span>
                <span className="text-[10px] text-slate-500 font-mono ml-2">
                  {selectedFile.path}
                </span>
              </div>
            </div>

            <div className="flex items-center gap-2">
              <button
                onClick={handleCopyContent}
                className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-xs text-slate-200 transition-colors"
                title="Copy contents"
              >
                {copied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                <span>{copied ? 'Copied!' : 'Copy'}</span>
              </button>

              <button
                onClick={handleDownloadFile}
                className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-xs text-slate-200 transition-colors"
                title="Download file"
              >
                <Download className="w-3.5 h-3.5" />
                <span>Save</span>
              </button>
            </div>
          </div>

          {/* Content Viewer Body */}
          <div className="p-4 bg-slate-950 font-mono text-xs overflow-x-auto max-h-[540px] text-slate-300 leading-relaxed whitespace-pre-wrap select-all">
            {selectedFile.content}
          </div>
        </div>
      </div>

      {/* 20-Tester Closed Testing & Production Checklist */}
      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5">
        <h3 className="text-sm font-semibold text-white mb-2 flex items-center gap-2">
          <Key className="w-4 h-4 text-amber-400" />
          <span>Google Play 2024–2026 Production Rollout Rules (Personal Accounts)</span>
        </h3>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-xs mt-3">
          <div className="bg-slate-950 p-3.5 rounded-xl border border-slate-800">
            <div className="font-semibold text-amber-400 mb-1">1. Closed Testing Track</div>
            <p className="text-slate-400 leading-relaxed">
              Upload <code className="text-slate-200">app-release.aab</code> to Closed Testing. Add 20+ testers via Google Group or opt-in email list.
            </p>
          </div>

          <div className="bg-slate-950 p-3.5 rounded-xl border border-slate-800">
            <div className="font-semibold text-amber-400 mb-1">2. 14 Days Active Enrollment</div>
            <p className="text-slate-400 leading-relaxed">
              Testers must remain opted-in for 14 continuous days. Encourage them to test IR on Xiaomi/Redmi and Wi-Fi on Samsung/LG.
            </p>
          </div>

          <div className="bg-slate-950 p-3.5 rounded-xl border border-slate-800">
            <div className="font-semibold text-amber-400 mb-1">3. Apply for Production</div>
            <p className="text-slate-400 leading-relaxed">
              Click &quot;Apply for Production&quot; in Google Play Console. Submit feedback summary to unlock public listing worldwide.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};
