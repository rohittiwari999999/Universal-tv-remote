import React, { useState } from 'react';
import { 
  Database, Search, Download, Copy, Check, Filter, Tv, Radio,
  Layers, HardDrive, Cpu, Code
} from 'lucide-react';
import { TV_BRANDS_DATABASE, TvBrand } from '../data/tvBrandsData';

export const DatabaseExplorer: React.FC = () => {
  const [searchQuery, setSearchQuery] = useState('');
  const [filterType, setFilterType] = useState<'All' | 'Global' | 'Indian Local'>('All');
  const [selectedBrand, setSelectedBrand] = useState<TvBrand>(TV_BRANDS_DATABASE[0]);
  const [copied, setCopied] = useState(false);

  const filteredBrands = TV_BRANDS_DATABASE.filter(brand => {
    const matchesSearch = brand.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
                          brand.headquarters.toLowerCase().includes(searchQuery.toLowerCase()) ||
                          brand.protocol.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesFilter = filterType === 'All' || brand.category === filterType;
    return matchesSearch && matchesFilter;
  });

  const handleExportJson = () => {
    const exportData = {
      version: '1.0.0',
      exportedAt: new Date().toISOString(),
      brandsCount: TV_BRANDS_DATABASE.length,
      brands: TV_BRANDS_DATABASE,
    };
    const blob = new Blob([JSON.stringify(exportData, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'tv_remotes_firestore_seed.json';
    link.click();
    URL.revokeObjectURL(url);
  };

  const handleCopyHex = (hex: string) => {
    navigator.clipboard.writeText(hex);
    setCopied(true);
    setTimeout(() => setCopied(false), 1500);
  };

  return (
    <div className="flex flex-col gap-6">
      {/* Header and Summary */}
      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <Database className="w-5 h-5 text-amber-400" />
              <h2 className="text-base font-semibold text-white">
                Firebase Firestore IR &amp; Smart TV Database
              </h2>
            </div>
            <p className="text-xs text-slate-400 max-w-2xl leading-relaxed">
              Dynamically loads IR carrier pulse HEX mappings, BLE service profiles, and local network Wi-Fi endpoints for {TV_BRANDS_DATABASE.length} leading Global &amp; Indian TV manufacturers.
            </p>
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={handleExportJson}
              className="flex items-center gap-2 px-3 py-2 bg-amber-600 hover:bg-amber-500 text-white rounded-lg text-xs font-medium transition-colors shadow-sm"
            >
              <Download className="w-3.5 h-3.5" />
              <span>Export Firestore JSON</span>
            </button>
          </div>
        </div>

        {/* Firestore Architecture Structure Cards */}
        <div className="mt-4 pt-4 border-t border-slate-800 grid grid-cols-1 md:grid-cols-3 gap-3 text-xs font-mono">
          <div className="bg-slate-950 p-3 rounded-lg border border-slate-800">
            <div className="text-[10px] text-slate-500 uppercase">Collection 1</div>
            <div className="text-amber-400 font-semibold mt-0.5">/tv_brands/{'{brand_id}'}</div>
            <div className="text-[11px] text-slate-400 mt-1">Metadata, carrier frequency, origin, brand logos.</div>
          </div>

          <div className="bg-slate-950 p-3 rounded-lg border border-slate-800">
            <div className="text-[10px] text-slate-500 uppercase">Subcollection</div>
            <div className="text-amber-400 font-semibold mt-0.5">.../remote_profiles/{'{profile_id}'}</div>
            <div className="text-[11px] text-slate-400 mt-1">Key mappings (Power, Vol, Ch, D-Pad, OTT apps).</div>
          </div>

          <div className="bg-slate-950 p-3 rounded-lg border border-slate-800">
            <div className="text-[10px] text-slate-500 uppercase">Offline Caching Strategy</div>
            <div className="text-emerald-400 font-semibold mt-0.5">Hive Local Box Storage</div>
            <div className="text-[11px] text-slate-400 mt-1">Zero internet latency: cached locally on first sync.</div>
          </div>
        </div>
      </div>

      {/* Main Database Browser Layout */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-5">
        {/* Left: Brand Search & List */}
        <div className="lg:col-span-4 flex flex-col gap-3">
          {/* Search Box */}
          <div className="relative">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5 pointer-events-none" />
            <input
              type="text"
              placeholder="Search brands (e.g., Vu, Mi, Samsung)..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full bg-slate-900 border border-slate-800 text-xs text-white rounded-lg pl-9 pr-3 py-2 focus:outline-none focus:border-amber-500"
            />
          </div>

          {/* Filter Pills */}
          <div className="flex items-center gap-1 p-1 bg-slate-900 border border-slate-800 rounded-lg text-xs">
            {(['All', 'Global', 'Indian Local'] as const).map((type) => (
              <button
                key={type}
                onClick={() => setFilterType(type)}
                className={`flex-1 py-1 px-2 rounded-md font-medium transition-colors text-center text-xs ${
                  filterType === type
                    ? 'bg-slate-800 text-white shadow-sm'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                {type}
              </button>
            ))}
          </div>

          {/* Brand Cards List */}
          <div className="space-y-1.5 max-h-[520px] overflow-y-auto pr-1">
            {filteredBrands.map((brand) => {
              const isSelected = selectedBrand.id === brand.id;
              return (
                <div
                  key={brand.id}
                  onClick={() => setSelectedBrand(brand)}
                  className={`p-3 rounded-xl border text-left cursor-pointer transition-all ${
                    isSelected
                      ? 'bg-slate-900 border-amber-500/50 shadow-md ring-1 ring-amber-500/20'
                      : 'bg-slate-950/70 border-slate-800/80 hover:bg-slate-900/60 hover:border-slate-700'
                  }`}
                >
                  <div className="flex items-center justify-between mb-1">
                    <div className="flex items-center gap-2">
                      <div
                        className="w-2.5 h-2.5 rounded-full"
                        style={{ backgroundColor: brand.accentColor }}
                      />
                      <span className="font-semibold text-xs text-white">
                        {brand.name}
                      </span>
                    </div>
                    <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-slate-800 text-slate-300 border border-slate-700">
                      {brand.protocol}
                    </span>
                  </div>

                  <div className="flex items-center justify-between text-[11px] text-slate-400 font-mono mt-1">
                    <span>{brand.category}</span>
                    <span className="text-amber-400">{brand.carrierFrequency / 1000} kHz</span>
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Right: Selected Brand Document Detail & Key Matrix */}
        <div className="lg:col-span-8 bg-slate-900 border border-slate-800 rounded-2xl flex flex-col overflow-hidden">
          {/* Brand Info Banner */}
          <div className="p-4 bg-slate-950/80 border-b border-slate-800 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div>
              <div className="flex items-center gap-2">
                <span className="text-base font-bold text-white">{selectedBrand.name}</span>
                <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-amber-950/80 text-amber-300 border border-amber-800/50">
                  {selectedBrand.category}
                </span>
              </div>
              <p className="text-xs text-slate-400 mt-1">
                {selectedBrand.description}
              </p>
            </div>

            <div className="text-right shrink-0 font-mono text-xs">
              <div className="text-slate-400">Carrier Frequency</div>
              <div className="text-cyan-400 font-bold">{selectedBrand.carrierFrequency} Hz</div>
            </div>
          </div>

          {/* Supported Modes Badges */}
          <div className="px-4 py-2 bg-slate-950 border-b border-slate-800/60 flex items-center gap-4 text-xs font-mono">
            <span className="text-slate-400">Supported Protocols:</span>
            <div className="flex items-center gap-1.5">
              {selectedBrand.supportedModes.map(mode => (
                <span
                  key={mode}
                  className={`px-2 py-0.5 rounded text-[10px] font-medium ${
                    mode === 'IR' ? 'bg-red-950 text-red-300 border border-red-900/60' :
                    mode === 'BLE' ? 'bg-blue-950 text-blue-300 border border-blue-900/60' :
                    'bg-emerald-950 text-emerald-300 border border-emerald-900/60'
                  }`}
                >
                  {mode}
                </span>
              ))}
            </div>
          </div>

          {/* Keycode Matrix Grid */}
          <div className="p-4 flex-1 overflow-y-auto max-h-[460px]">
            <div className="text-xs font-mono text-slate-400 mb-2">
              Assigned IR / Bluetooth HEX Codes ({Object.keys(selectedBrand.keys).length} mapped keys):
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
              {Object.entries(selectedBrand.keys).map(([keyName, keyDef]) => (
                <div
                  key={keyName}
                  className="bg-slate-950 border border-slate-800/80 rounded-xl p-2.5 flex items-center justify-between hover:border-slate-700 transition-colors"
                >
                  <div className="flex flex-col">
                    <span className="text-xs font-medium text-white">
                      {keyDef.label}
                    </span>
                    <span className="text-[10px] font-mono text-slate-500 uppercase">
                      {keyDef.category}
                    </span>
                  </div>

                  <div className="flex items-center gap-2">
                    <code className="text-xs font-mono text-amber-400 bg-slate-900 px-2 py-1 rounded border border-slate-800">
                      {keyDef.necHex}
                    </code>
                    <button
                      onClick={() => handleCopyHex(keyDef.necHex)}
                      className="p-1 text-slate-400 hover:text-white rounded hover:bg-slate-800"
                      title="Copy HEX"
                    >
                      <Copy className="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
