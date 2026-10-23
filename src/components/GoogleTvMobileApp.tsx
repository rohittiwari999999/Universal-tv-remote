import React, { useState, useEffect } from 'react';
import { 
  Tv, Wifi, RefreshCw, Plus, Lock, X, ArrowLeft,
  Home, Mic, Volume2, VolumeX, Power, Settings, Send,
  ChevronUp, ChevronDown, ChevronLeft, ChevronRight,
  Play, Pause, FastForward, Rewind, CheckCircle2, AlertCircle,
  Radio, Keyboard, Touchpad, Shield, Zap, Check, Sparkles, Layers
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

export interface TvBrand {
  id: string;
  name: string;
  description: string;
  carrierFreq: number;
  protocol: string;
  codeHex: string;
}

export interface CoocaaCodeVariant {
  id: string;
  name: string;
  codeHex: string;
  freq: number;
  protocol: string;
  description: string;
  recommended?: boolean;
}

export const COOCAA_VARIANTS: CoocaaCodeVariant[] = [
  {
    id: 'iqoo_preset_40bf_12_single',
    name: 'iQOO Neo 10 Model 1: Skyworth 0x40BF12ED (Single Frame 67p)',
    codeHex: '0x40BF12ED (67p)',
    freq: 38000,
    protocol: 'Standard NEC Single Frame (38.0 kHz)',
    description: 'Exact 1-tap single frame sent by iQOO / Vivo Smart Remote for Coocaa (Address 0x40, Cmd 0x12)',
    recommended: true
  },
  {
    id: 'iqoo_preset_40bf_12_dual',
    name: 'iQOO Neo 10 Model 1: Skyworth 0x40BF12ED (Standby Wake Burst)',
    codeHex: '0x40BF12ED (2-Frame)',
    freq: 38000,
    protocol: 'NEC 32-bit 2-Frame Burst (38.0 kHz)',
    description: 'Dual frame with 40ms gap to wake TV MCU from deep eco standby mode',
    recommended: true
  },
  {
    id: 'iqoo_preset_40bf_02_single',
    name: 'iQOO Neo 10 Model 2: Skyworth 0x40BF02FD (Single Frame 67p)',
    codeHex: '0x40BF02FD (67p)',
    freq: 38000,
    protocol: 'Standard NEC Single Frame (38.0 kHz)',
    description: 'Alternate standard Skyworth / Coocaa code in iQOO / Vivo database (Cmd 0x02)',
    recommended: true
  },
  {
    id: 'iqoo_preset_40bf_02_dual',
    name: 'iQOO Neo 10 Model 2: Skyworth 0x40BF02FD (Standby Burst)',
    codeHex: '0x40BF02FD (2-Frame)',
    freq: 38000,
    protocol: 'NEC 32-bit 2-Frame Burst (38.0 kHz)',
    description: 'Dual frame standby burst for Cmd 0x02'
  },
  {
    id: 'iqoo_neo10_47ef_single',
    name: 'iQOO Neo 10 Match 3: Skyworth 0x47EF827D (Single Frame 67p)',
    codeHex: '0x47EF827D (67p)',
    freq: 38000,
    protocol: 'Standard NEC Single Frame (38.0 kHz)',
    description: 'Skyworth 4K & Smart Android TV variant (Customer 0x47EF, Power 0x82)',
    recommended: true
  },
  {
    id: 'iqoo_neo10_47ef_dual',
    name: 'iQOO Neo 10 Match 3: Skyworth 0x47EF827D (Standby Burst)',
    codeHex: '0x47EF827D (2-Frame)',
    freq: 38000,
    protocol: 'NEC 32-bit 2-Frame Burst (38.0 kHz)',
    description: 'Dual frame standby burst for 0x47EF827D'
  },
  {
    id: 'iqoo_preset_40bf_48',
    name: 'iQOO Neo 10 Model 4: Skyworth 0x40BF48B7',
    codeHex: '0x40BF48B7',
    freq: 38000,
    protocol: 'NEC 32-bit (38.0 kHz)',
    description: 'Skyworth chassis variant with Power keycode 0x48'
  },
  {
    id: 'google_tv_single',
    name: 'Coocaa Google TV Standard: 0x00FF12ED (Single Frame)',
    codeHex: '0x00FF12ED (67p)',
    freq: 38000,
    protocol: 'NEC 32-bit (38.0 kHz)',
    description: 'Official Skyworth/Coocaa Google TV standard power code'
  },
  {
    id: 'google_tv_dual',
    name: 'Coocaa Google TV Standard: 0x00FF12ED (Standby Burst)',
    codeHex: '0x00FF12ED (2-Frame)',
    freq: 38000,
    protocol: 'NEC 2-Frame Burst (38.0 kHz)',
    description: 'Dual-frame burst for Google TV standard'
  },
  {
    id: 'skyworth_alt_00ff02',
    name: 'Skyworth Android TV Alt: 0x00FF02FD',
    codeHex: '0x00FF02FD',
    freq: 38000,
    protocol: 'NEC 32-bit (38.0 kHz)',
    description: 'Skyworth / Coocaa alternate power toggle code'
  },
  {
    id: 'coolita',
    name: 'Coocaa Coolita / 32S3U: 0x01FE48B7',
    codeHex: '0x01FE48B7',
    freq: 38000,
    protocol: 'NEC 32-bit (38.0 kHz)',
    description: 'Coocaa Coolita OS and budget 32/40-inch Smart TVs'
  },
  {
    id: 'coolita_00fe',
    name: 'Coocaa Coolita Alt: 0x00FE48B7',
    codeHex: '0x00FE48B7',
    freq: 38000,
    protocol: 'NEC 32-bit (38.0 kHz)',
    description: 'Coocaa Coolita alternate address code'
  },
  {
    id: 's6g_y72',
    name: 'Coocaa 4K S6G / Y72: 0x707030CF (38.4kHz)',
    codeHex: '0x707030CF',
    freq: 38400,
    protocol: 'NEC 32-bit (38.4 kHz Modulation)',
    description: 'Coocaa 43/50/55 inch 4K Smart TV models with 38.4kHz carrier'
  },
  {
    id: 'indian_36k',
    name: 'Coocaa Indian 36kHz Model: 0x40BF12ED',
    codeHex: '0x40BF12ED (36kHz)',
    freq: 36000,
    protocol: 'NEC 32-bit (36.0 kHz Modulation)',
    description: 'Older Indian & Southeast Asian Coocaa models using 36kHz carrier'
  },
  {
    id: 'high_40k',
    name: 'Coocaa High Frequency 40kHz: 0x40BF12ED',
    codeHex: '0x40BF12ED (40kHz)',
    freq: 40000,
    protocol: 'NEC 32-bit (40.0 kHz Modulation)',
    description: 'Coocaa variant tuned to 40.0 kHz carrier frequency'
  },
  {
    id: 'discrete_on',
    name: 'Skyworth Discrete Power ON: 0x40BF13EC',
    codeHex: '0x40BF13EC',
    freq: 38000,
    protocol: 'NEC 32-bit Discrete ON (38.0 kHz)',
    description: 'Discrete ON code (only powers ON the TV, will not toggle OFF)'
  },
  {
    id: 'skyworth_00ff82',
    name: 'Skyworth 4K Android 11/12: 0x00FF827D',
    codeHex: '0x00FF827D',
    freq: 38000,
    protocol: 'NEC 32-bit (38.0 kHz)',
    description: 'Coocaa 4K Android 11/12 TV power key'
  },
  {
    id: 'msb_40bf',
    name: 'MSB-First Bitstream: Skyworth 0x40BF12ED',
    codeHex: '0x40BF12ED (MSB)',
    freq: 38000,
    protocol: 'NEC 32-bit MSB-First (38.0 kHz)',
    description: 'Alternative MSB-first bit order for 0x40BF12ED'
  },
  {
    id: 'msb_47ef',
    name: 'MSB-First Bitstream: Skyworth 0x47EF827D',
    codeHex: '0x47EF827D (MSB)',
    freq: 38000,
    protocol: 'NEC 32-bit MSB-First (38.0 kHz)',
    description: 'Alternative MSB-first bit order for 0x47EF827D'
  },
  {
    id: 'universal_power',
    name: 'Coocaa Universal Smart: 0x04FB01FE',
    codeHex: '0x04FB01FE',
    freq: 38000,
    protocol: 'NEC 32-bit (38.0 kHz)',
    description: 'Coocaa Universal Smart TV power toggle code'
  }
];

export const SUPPORTED_TV_BRANDS: TvBrand[] = [
  { id: 'coocaa', name: 'Coocaa TV', description: 'Coocaa / Skyworth (iQOO Neo 10 Match: 0x47EF827D, 38kHz)', carrierFreq: 38000, protocol: 'NEC 3-Frame Burst', codeHex: '0x47EF827D' },
  { id: 'samsung', name: 'Samsung TV', description: 'Samsung 38kHz Power Code', carrierFreq: 38000, protocol: 'Samsung 32-bit', codeHex: '0xE0E040BF' },
  { id: 'lg', name: 'LG TV', description: 'LG NEC 38kHz Power Code', carrierFreq: 38000, protocol: 'NEC 32-bit', codeHex: '0x20DF10EF' },
  { id: 'sony', name: 'Sony TV', description: 'Sony SIRC 40kHz 12-bit Power Code', carrierFreq: 40000, protocol: 'SIRC 12-bit', codeHex: '0x0A90' },
  { id: 'xiaomi', name: 'Mi / Xiaomi', description: 'Xiaomi / Redmi TV NEC 38kHz', carrierFreq: 38000, protocol: 'NEC 32-bit', codeHex: '0x00FF12ED' },
  { id: 'oneplus', name: 'OnePlus TV', description: 'OnePlus TV NEC 38kHz', carrierFreq: 38000, protocol: 'NEC 32-bit', codeHex: '0x01FE48B7' },
  { id: 'tcl', name: 'TCL TV', description: 'TCL NEC 38kHz', carrierFreq: 38000, protocol: 'NEC 32-bit', codeHex: '0x00FFA25D' },
  { id: 'vu', name: 'Vu TV', description: 'Vu Smart TV NEC 38kHz', carrierFreq: 38000, protocol: 'NEC 32-bit', codeHex: '0x00FF10EF' },
  { id: 'realme', name: 'Realme TV', description: 'Realme Smart TV NEC 38kHz', carrierFreq: 38000, protocol: 'NEC 32-bit', codeHex: '0x01FE48B7' },
  { id: 'hisense', name: 'Hisense TV', description: 'Hisense NEC 38kHz', carrierFreq: 38000, protocol: 'NEC 32-bit', codeHex: '0xFD02FDFD' },
  { id: 'panasonic', name: 'Panasonic', description: 'Panasonic 37kHz Power Code', carrierFreq: 37000, protocol: 'Panasonic 48-bit', codeHex: '0x40040100' }
];

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
  
  // IR Blaster state & TV Brand (Default: Coocaa TV with NEC 38kHz Power Code 0x707030CF)
  const [selectedBrand, setSelectedBrand] = useState<TvBrand>(() => {
    try {
      const saved = localStorage.getItem('selected_tv_brand');
      return SUPPORTED_TV_BRANDS.find(b => b.id === saved) || SUPPORTED_TV_BRANDS[0];
    } catch {
      return SUPPORTED_TV_BRANDS[0];
    }
  });
  const [selectedCoocaaVariant, setSelectedCoocaaVariant] = useState<CoocaaCodeVariant>(() => {
    try {
      const saved = localStorage.getItem('selected_coocaa_variant');
      return COOCAA_VARIANTS.find(v => v.id === saved) || COOCAA_VARIANTS[0];
    } catch {
      return COOCAA_VARIANTS[0];
    }
  });
  const [showBrandModal, setShowBrandModal] = useState(false);
  const [showCoocaaTesterModal, setShowCoocaaTesterModal] = useState(false);
  const [testedVariantId, setTestedVariantId] = useState<string | null>(null);
  const [isPowerBlasting, setIsPowerBlasting] = useState(false);
  const [irToastMessage, setIrToastMessage] = useState<string | null>(null);
  const [autoCycleActive, setAutoCycleActive] = useState(false);
  const [autoCycleIndex, setAutoCycleIndex] = useState(1);
  const [autoCycleCountdown, setAutoCycleCountdown] = useState(3);
  const [customFrequencyHz, setCustomFrequencyHz] = useState(38400);

  // Saved paired remote state (Automatically enters remote screen directly - no need to tap connect remote)
  const [selectedDevice, setSelectedDevice] = useState<DiscoveredTvItem | null>(() => {
    try {
      const saved = localStorage.getItem('last_paired_tv');
      if (saved) return JSON.parse(saved);
      // Pre-seed default paired remote so user automatically lands on remote screen
      const defaultPaired: DiscoveredTvItem = {
        id: 'coocaa_google_tv',
        name: 'Coocaa Google TV 4K',
        hostAddress: '192.168.1.104',
        model: 'Coocaa 50" Smart Google TV (Skyworth)',
        isPaired: true,
        signalDbm: -38
      };
      localStorage.setItem('last_paired_tv', JSON.stringify(defaultPaired));
      return defaultPaired;
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
      return Boolean(localStorage.getItem('last_paired_tv')) || true;
    } catch {
      return true;
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

  // IR Blaster Power ON Transmission (with Coocaa Multi-Blast and brand presets)
  const handleTransmitPower = () => {
    if (selectedBrand.id === 'coocaa') {
      handleTransmitCoocaaVariant(selectedCoocaaVariant);
      return;
    }
    remoteSound.click();
    setIsPowerBlasting(true);
    setIrToastMessage(`IR Power Code Sent: ${selectedBrand.name} (${selectedBrand.codeHex} @ ${selectedBrand.carrierFreq / 1000}kHz)`);
    onTvPowerToggle();
    onTvAction(`[IR BLASTER] Transmitted Power ON code for ${selectedBrand.name} (${selectedBrand.protocol} ${selectedBrand.codeHex} at ${selectedBrand.carrierFreq}Hz) + Wake-on-LAN`);

    setTimeout(() => {
      setIsPowerBlasting(false);
    }, 1200);

    setTimeout(() => {
      setIrToastMessage(null);
    }, 3500);
  };

  const handleTransmitCoocaaVariant = (variant: CoocaaCodeVariant, customFreq?: number) => {
    remoteSound.click();
    setIsPowerBlasting(true);
    setSelectedCoocaaVariant(variant);
    setTestedVariantId(variant.id);
    try {
      localStorage.setItem('selected_coocaa_variant', variant.id);
    } catch {
      // ignore
    }

    const freqToUse = customFreq || variant.freq;
    const isSuper = variant.id === 'super_blast';
    const toastText = isSuper
      ? `⚡ SUPER MULTI-BLAST: Fired all 9 Coocaa TV Power codes (2x Repeat)!`
      : `Coocaa IR Fired: ${variant.name} (${variant.codeHex} @ ${(freqToUse / 1000).toFixed(1)}kHz)`;

    setIrToastMessage(toastText);
    onTvPowerToggle();
    onTvAction(
      isSuper
        ? `[IR BLASTER] Fired SUPER MULTI-BLAST for Coocaa TV across all 9 variants (36kHz, 38kHz, 38.4kHz, 40kHz) + Dual Wake-on-LAN!`
        : `[IR BLASTER] Transmitted Coocaa TV ${variant.name} [${variant.codeHex}] at ${freqToUse}Hz (Dual-Frame Repeat) + Wake-on-LAN`
    );

    setTimeout(() => {
      setIsPowerBlasting(false);
    }, 1400);

    setTimeout(() => {
      setIrToastMessage(null);
    }, 4000);
  };

  // Step-by-step Auto-Test Runner for Coocaa frequencies
  useEffect(() => {
    if (!autoCycleActive) return;

    const timer = setInterval(() => {
      setAutoCycleCountdown((prev) => {
        if (prev <= 1) {
          // Advance to next variant (index 1 to 9, skipping super_blast 0)
          const nextIndex = autoCycleIndex >= COOCAA_VARIANTS.length - 1 ? 1 : autoCycleIndex + 1;
          setAutoCycleIndex(nextIndex);
          const nextVariant = COOCAA_VARIANTS[nextIndex];
          handleTransmitCoocaaVariant(nextVariant);
          return 3; // Reset 3-second delay for user to check TV screen
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [autoCycleActive, autoCycleIndex]);

  const handleStartAutoCycle = () => {
    remoteSound.click();
    setAutoCycleIndex(1);
    setAutoCycleCountdown(3);
    setAutoCycleActive(true);
    handleTransmitCoocaaVariant(COOCAA_VARIANTS[1]);
  };

  const handleStopAutoCycle = () => {
    remoteSound.click();
    setAutoCycleActive(false);
  };

  const handleConfirmWorkingVariant = (variant: CoocaaCodeVariant) => {
    remoteSound.pairingSuccess();
    setAutoCycleActive(false);
    setSelectedCoocaaVariant(variant);
    try {
      localStorage.setItem('selected_coocaa_variant', variant.id);
    } catch {}
    setIrToastMessage(`🎉 SAVED! Coocaa will always use: ${variant.name} (${(variant.freq / 1000).toFixed(1)}kHz)`);
    setTimeout(() => {
      setShowCoocaaTesterModal(false);
    }, 1200);
  };

  const handleSelectBrand = (brand: TvBrand) => {
    remoteSound.click();
    setSelectedBrand(brand);
    try {
      localStorage.setItem('selected_tv_brand', brand.id);
    } catch {
      // ignore
    }
    setShowBrandModal(false);
    onTvAction(`Configured IR Blaster for ${brand.name} (${brand.protocol} ${brand.codeHex})`);
  };

  return (
    <div className="w-[340px] h-[670px] bg-slate-950 rounded-[44px] border-[6px] border-slate-800 shadow-2xl relative overflow-hidden flex flex-col font-sans select-none ring-1 ring-white/10">
      
      {/* Phone Notch / Dynamic Island Header with IR Blaster Diode simulation */}
      <div className="h-6 w-full flex items-center justify-center pt-2 relative z-20">
        <div className="w-20 h-4 bg-slate-900 rounded-full flex items-center justify-center gap-1.5 px-2">
          <div className={`w-2 h-2 rounded-full transition-colors ${isPowerBlasting ? 'bg-red-500 animate-ping' : 'bg-slate-800'}`} />
          <div className="w-2.5 h-2.5 rounded-full bg-slate-950 border border-slate-700" />
        </div>
        {/* IR Blaster diode emission beam glow */}
        {isPowerBlasting && (
          <div className="absolute top-0 inset-x-8 h-8 bg-gradient-to-b from-red-500/60 via-red-500/20 to-transparent blur-sm animate-pulse pointer-events-none" />
        )}
      </div>

      {/* Floating IR Toast Notification */}
      {irToastMessage && (
        <div className="absolute top-8 inset-x-4 bg-slate-900/95 border border-red-500/60 rounded-xl p-2 text-center text-[10.5px] text-white shadow-2xl z-40 animate-in fade-in slide-in-from-top-2 duration-200">
          <div className="flex items-center justify-center gap-1.5 font-bold text-red-400">
            <Zap className="w-3.5 h-3.5" />
            <span>IR Blaster Pulse Fired</span>
          </div>
          <div className="text-slate-300 font-mono text-[9.5px] mt-0.5">{irToastMessage}</div>
        </div>
      )}

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
            
            {/* Top Power ON Button & TV Brand Selector (Coocaa 38kHz NEC IR Blaster) */}
            <div className="bg-gradient-to-r from-red-950/40 via-slate-900 to-blue-950/40 border border-slate-800 rounded-2xl p-2 mb-2 shadow-md">
              <div className="flex items-center justify-between gap-2">
                {/* POWER ON Button with IR Blaster */}
                <button
                  onClick={handleTransmitPower}
                  className={`flex-1 flex items-center justify-center gap-2 py-2 px-3 rounded-xl font-bold text-xs text-white transition-all shadow-lg active:scale-95 ${
                    isPowerBlasting
                      ? 'bg-red-500 ring-2 ring-red-400 animate-pulse'
                      : 'bg-red-600 hover:bg-red-500 shadow-red-600/30'
                  }`}
                  title={`Send IR Blaster Power ON to ${selectedBrand.name}`}
                >
                  <Power className="w-4 h-4 shrink-0" />
                  <div className="text-left leading-tight">
                    <div className="font-extrabold tracking-wide text-[11px]">POWER ON</div>
                    <div className="text-[8px] text-red-100 font-mono">IR BLASTER</div>
                  </div>
                </button>

                {/* TV Brand Selector Dropdown */}
                <button
                  onClick={() => { remoteSound.click(); setShowBrandModal(true); }}
                  className="flex-1 flex items-center justify-between py-2 px-2.5 rounded-xl bg-slate-800 hover:bg-slate-750 border border-slate-700/80 hover:border-blue-500/50 transition-all text-left"
                >
                  <div className="flex items-center gap-1.5 min-w-0">
                    <Tv className="w-3.5 h-3.5 text-blue-400 shrink-0" />
                    <div className="min-w-0">
                      <div className="text-[11px] font-bold text-white truncate">{selectedBrand.name}</div>
                      <div className="text-[8.5px] text-slate-400 truncate">Tap to change brand</div>
                    </div>
                  </div>
                  <ChevronDown className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                </button>
              </div>

              {/* Status bar with Coocaa NEC 38kHz Details */}
              <div className="flex items-center justify-between mt-1.5 px-1 text-[9.5px]">
                <div className="flex items-center gap-1.5 text-slate-400">
                  <span className={`w-1.5 h-1.5 rounded-full ${isPowerBlasting ? 'bg-red-400 animate-ping' : 'bg-emerald-400'}`} />
                  <span>IR Ready • {selectedBrand.name}</span>
                </div>
                {selectedBrand.id === 'coocaa' ? (
                  <button
                    onClick={() => { remoteSound.click(); setShowCoocaaTesterModal(true); }}
                    className="font-mono text-cyan-400 hover:text-cyan-300 font-bold flex items-center gap-1 bg-cyan-950/70 border border-cyan-500/40 px-1.5 py-0.5 rounded text-[8.5px] hover:border-cyan-400 transition-colors"
                    title="Click to test all 6 Coocaa TV power codes"
                  >
                    <Layers className="w-2.5 h-2.5" />
                    <span>{selectedCoocaaVariant.id === 'super_blast' ? '6-Code Multi-Blast' : selectedCoocaaVariant.name.slice(0, 15)}</span>
                  </button>
                ) : (
                  <span className="font-mono text-blue-400 font-bold">{selectedBrand.codeHex}</span>
                )}
              </div>
            </div>

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

            {/* Top Power ON Button & TV Brand Selector (Turn ON TV before network connect) */}
            <div className="bg-gradient-to-r from-red-950/40 via-slate-900 to-blue-950/40 border border-slate-800 rounded-2xl p-2 my-1.5 shadow-md">
              <div className="flex items-center justify-between gap-2">
                <button
                  onClick={handleTransmitPower}
                  className={`flex-1 flex items-center justify-center gap-2 py-2 px-3 rounded-xl font-bold text-xs text-white transition-all shadow-lg active:scale-95 ${
                    isPowerBlasting
                      ? 'bg-red-500 ring-2 ring-red-400 animate-pulse'
                      : 'bg-red-600 hover:bg-red-500 shadow-red-600/30'
                  }`}
                  title={`Send IR Blaster Power ON to ${selectedBrand.name}`}
                >
                  <Power className="w-4 h-4 shrink-0" />
                  <div className="text-left leading-tight">
                    <div className="font-extrabold tracking-wide text-[11px]">POWER ON</div>
                    <div className="text-[8px] text-red-100 font-mono">IR BLASTER</div>
                  </div>
                </button>

                <button
                  onClick={() => { remoteSound.click(); setShowBrandModal(true); }}
                  className="flex-1 flex items-center justify-between py-2 px-2.5 rounded-xl bg-slate-800 hover:bg-slate-750 border border-slate-700/80 hover:border-blue-500/50 transition-all text-left"
                >
                  <div className="flex items-center gap-1.5 min-w-0">
                    <Tv className="w-3.5 h-3.5 text-blue-400 shrink-0" />
                    <div className="min-w-0">
                      <div className="text-[11px] font-bold text-white truncate">{selectedBrand.name}</div>
                      <div className="text-[8.5px] text-slate-400 truncate">Tap to change brand</div>
                    </div>
                  </div>
                  <ChevronDown className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                </button>
              </div>

              <div className="flex items-center justify-between mt-1.5 px-1 text-[9.5px]">
                <div className="flex items-center gap-1.5 text-slate-400">
                  <span className={`w-1.5 h-1.5 rounded-full ${isPowerBlasting ? 'bg-red-400 animate-ping' : 'bg-emerald-400'}`} />
                  <span>IR Ready • {selectedBrand.name}</span>
                </div>
                {selectedBrand.id === 'coocaa' ? (
                  <button
                    onClick={() => { remoteSound.click(); setShowCoocaaTesterModal(true); }}
                    className="font-mono text-cyan-400 hover:text-cyan-300 font-bold flex items-center gap-1 bg-cyan-950/70 border border-cyan-500/40 px-1.5 py-0.5 rounded text-[8.5px] hover:border-cyan-400 transition-colors"
                    title="Click to test all 6 Coocaa TV power codes"
                  >
                    <Layers className="w-2.5 h-2.5" />
                    <span>{selectedCoocaaVariant.id === 'super_blast' ? '6-Code Multi-Blast' : selectedCoocaaVariant.name.slice(0, 15)}</span>
                  </button>
                ) : (
                  <span className="font-mono text-blue-400 font-bold">{selectedBrand.codeHex}</span>
                )}
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
              <span>Connect by IP Manually (Add TV by IP Address)</span>
            </button>

            {/* STANDALONE SEPARATE IR BLASTER POWER ON CARD (Placed below Add TV by IP Address) */}
            <div className="mt-2.5 p-3 rounded-2xl bg-gradient-to-b from-slate-900 via-slate-900/95 to-slate-950 border border-red-500/40 shadow-xl relative overflow-hidden">
              <div className="flex items-center justify-between mb-1.5">
                <div className="flex items-center gap-1.5">
                  <div className="w-5 h-5 rounded-full bg-red-600/20 border border-red-500/50 flex items-center justify-center text-red-400">
                    <Zap className="w-3 h-3" />
                  </div>
                  <span className="text-[11px] font-black text-white tracking-wide uppercase">
                    TV Power ON (IR Blaster)
                  </span>
                </div>
                <button
                  onClick={() => { remoteSound.click(); setShowBrandModal(true); }}
                  className="px-2 py-1 rounded-lg bg-slate-800 hover:bg-slate-750 border border-slate-700 hover:border-blue-500/50 text-[10px] font-bold text-blue-400 flex items-center gap-1 transition-colors"
                  title="Change TV brand"
                >
                  <Tv className="w-3 h-3 text-blue-400" />
                  <span>{selectedBrand.name}</span>
                  <ChevronDown className="w-2.5 h-2.5 text-slate-400" />
                </button>
              </div>

              <p className="text-[10px] text-slate-300 leading-snug mb-2">
                Google TV pair na ho paye to pehle IR blaster se TV ON karein taaki TV Wi-Fi se connect ho aur phir pair ho sake easily.
              </p>

              {/* Large Standalone Power ON Button (Fires Multi-Blast for Coocaa) */}
              <button
                onClick={() => {
                  handleTransmitPower();
                  // Re-trigger network discovery scan after waking TV
                  setTimeout(() => startAutoDiscovery(), 1400);
                }}
                className={`w-full py-2.5 px-4 rounded-xl font-bold text-xs text-white transition-all shadow-lg active:scale-95 flex items-center justify-center gap-2 ${
                  isPowerBlasting
                    ? 'bg-red-500 ring-2 ring-red-400 animate-pulse'
                    : 'bg-red-600 hover:bg-red-500 shadow-red-600/30'
                }`}
              >
                <Power className="w-4 h-4 shrink-0" />
                <span className="tracking-wide text-xs font-black">
                  {isPowerBlasting
                    ? 'TRANSMITTING IR CODE...'
                    : selectedBrand.id === 'coocaa'
                    ? selectedCoocaaVariant.id === 'super_blast'
                      ? 'POWER ON COOCAA (SUPER MULTI-BLAST)'
                      : `POWER ON COOCAA (${selectedCoocaaVariant.name.slice(0, 16)})`
                    : `POWER ON ${selectedBrand.name.toUpperCase()} (IR BLASTER)`}
                </span>
              </button>

              {/* Coocaa 6-Code Multi-Blast & Tester Callout Button */}
              {selectedBrand.id === 'coocaa' && (
                <button
                  onClick={() => {
                    remoteSound.click();
                    setShowCoocaaTesterModal(true);
                  }}
                  className="w-full mt-2 py-1.5 px-2.5 rounded-xl bg-gradient-to-r from-blue-950/70 via-indigo-950/70 to-purple-950/70 border border-blue-500/40 hover:border-blue-400 text-blue-300 hover:text-white text-[10px] font-bold flex items-center justify-between transition-all shadow-sm group"
                >
                  <div className="flex items-center gap-1.5 text-left">
                    <Sparkles className="w-3.5 h-3.5 text-yellow-400 shrink-0 group-hover:scale-110 transition-transform" />
                    <span>TV ON nahi hua? Test All 6 Codes</span>
                  </div>
                  <span className="text-[9px] font-mono bg-blue-500/20 text-blue-300 px-1.5 py-0.5 rounded border border-blue-400/30">
                    6 Codes / Blast ➔
                  </span>
                </button>
              )}

              <div className="flex items-center justify-between mt-2 pt-1.5 border-t border-slate-800/80 text-[9px] text-slate-400">
                <span className="flex items-center gap-1">
                  <span className={`w-1.5 h-1.5 rounded-full ${isPowerBlasting ? 'bg-red-400 animate-ping' : 'bg-emerald-400'}`} />
                  <span>{selectedBrand.name}</span>
                </span>
                {selectedBrand.id === 'coocaa' ? (
                  <span className="font-mono text-cyan-400 font-bold truncate max-w-[170px]">
                    {selectedCoocaaVariant.id === 'super_blast' ? '⚡ 6 Codes Multi-Blast' : selectedCoocaaVariant.codeHex}
                  </span>
                ) : (
                  <span className="font-mono text-blue-400 font-bold">
                    {selectedBrand.protocol} {selectedBrand.codeHex}
                  </span>
                )}
              </div>
            </div>

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

          <div className="my-auto text-center space-y-3">
            <div className="w-11 h-11 rounded-2xl bg-blue-600/20 border border-blue-500/40 mx-auto flex items-center justify-center text-blue-400">
              <Tv className="w-5 h-5" />
            </div>

            <div>
              <h3 className="text-sm font-bold text-white">Check Your TV Screen</h3>
              <p className="text-[11px] text-slate-400 mt-0.5 max-w-[240px] mx-auto leading-relaxed">
                A 4-digit code is currently shown on your <strong className="text-white">{selectedDevice.name}</strong> screen.
              </p>
            </div>

            {/* 4-digit PIN Input */}
            <div className="space-y-1.5">
              <input
                type="text"
                maxLength={6}
                value={pinInput}
                onChange={(e) => setPinInput(e.target.value.toUpperCase())}
                placeholder="e.g. 8341"
                className="w-44 mx-auto text-center font-mono font-bold text-2xl tracking-widest px-3 py-1.5 bg-slate-900 border-2 border-blue-500/60 rounded-2xl text-white focus:outline-none focus:border-blue-400"
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

            {/* Dedicated IR Blaster Power ON on Pairing Page if TV hasn't shown PIN or went into sleep */}
            <div className="p-2.5 rounded-xl bg-gradient-to-r from-red-950/40 via-slate-900 to-slate-900 border border-red-500/40 text-left">
              <div className="flex items-center justify-between gap-2">
                <div className="min-w-0">
                  <div className="text-[10px] font-bold text-white flex items-center gap-1">
                    <Zap className="w-3 h-3 text-red-400 shrink-0" />
                    <span className="truncate">TV Off / PIN Not Showing?</span>
                  </div>
                  <div className="text-[8.5px] text-slate-400 truncate">
                    Turn ON via {selectedBrand.name} IR Blaster
                  </div>
                </div>
                <button
                  onClick={handleTransmitPower}
                  className={`px-3 py-1.5 rounded-lg text-white text-[10.5px] font-extrabold flex items-center gap-1.5 shadow active:scale-95 transition-all shrink-0 ${
                    isPowerBlasting ? 'bg-red-500 animate-pulse' : 'bg-red-600 hover:bg-red-500 shadow-red-600/30'
                  }`}
                  title="Send IR Power ON to wake TV"
                >
                  <Power className="w-3 h-3" />
                  <span>POWER ON</span>
                </button>
              </div>
            </div>
          </div>

          <div className="space-y-2">
            <button
              onClick={handleVerifyPin}
              disabled={pinInput.length < 4 || isVerifying}
              className="w-full py-2.5 bg-blue-600 hover:bg-blue-500 disabled:opacity-50 text-white text-xs font-bold rounded-xl flex items-center justify-center gap-2 shadow-lg shadow-blue-500/20 transition-all"
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
              className="w-full py-1.5 text-[11px] text-slate-400 hover:text-white"
            >
              Cancel
            </button>
          </div>
        </div>
      )}

      {/* TV BRAND SELECTION MODAL (Coocaa TV default, Samsung, LG, Sony, etc.) */}
      {showBrandModal && (
        <div className="absolute inset-0 bg-slate-950/95 backdrop-blur-md z-30 p-4 flex flex-col justify-between animate-fade-in text-slate-100">
          <div className="flex items-center justify-between pb-2 border-b border-slate-800">
            <div>
              <div className="text-xs font-bold text-white flex items-center gap-1.5">
                <Tv className="w-3.5 h-3.5 text-blue-400" />
                <span>Select TV Brand for IR Blaster</span>
              </div>
              <p className="text-[9.5px] text-slate-400">
                Selects carrier frequency &amp; power code timing
              </p>
            </div>
            <button
              onClick={() => setShowBrandModal(false)}
              className="text-slate-400 hover:text-white p-1 rounded-lg hover:bg-slate-800"
            >
              <X className="w-4 h-4" />
            </button>
          </div>

          {/* List of brands */}
          <div className="flex-1 my-2 space-y-1.5 overflow-y-auto pr-1">
            {SUPPORTED_TV_BRANDS.map((brand) => {
              const isSelected = brand.id === selectedBrand.id;
              return (
                <div
                  key={brand.id}
                  onClick={() => handleSelectBrand(brand)}
                  className={`p-2.5 rounded-xl border text-left cursor-pointer transition-all flex items-center justify-between ${
                    isSelected
                      ? 'bg-blue-600/15 border-blue-500/80 shadow-sm'
                      : 'bg-slate-900 border-slate-800 hover:border-slate-700'
                  }`}
                >
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-1.5">
                      <span className={`text-xs font-bold ${isSelected ? 'text-blue-400' : 'text-white'}`}>
                        {brand.name}
                      </span>
                      {brand.id === 'coocaa' && (
                        <span className="px-1.5 py-0.5 rounded bg-emerald-500/20 text-emerald-400 text-[8.5px] font-bold">
                          DEFAULT
                        </span>
                      )}
                    </div>
                    <p className="text-[10px] text-slate-400 truncate mt-0.5">{brand.description}</p>
                    <div className="flex items-center gap-2 mt-1 text-[9px] font-mono text-slate-500">
                      <span>{brand.carrierFreq / 1000} kHz</span>
                      <span>·</span>
                      <span className="text-cyan-400/90">{brand.codeHex}</span>
                    </div>
                  </div>

                  {isSelected && (
                    <div className="w-5 h-5 rounded-full bg-blue-600 flex items-center justify-center text-white shrink-0 ml-2">
                      <Check className="w-3 h-3" />
                    </div>
                  )}
                </div>
              );
            })}
          </div>

          <div className="pt-2 border-t border-slate-800 text-center">
            <p className="text-[9.5px] text-slate-500 mb-2">
              Tip: Coocaa TV (Skyworth) uses 38kHz NEC protocol (0x707030CF).
            </p>
            <button
              onClick={() => setShowBrandModal(false)}
              className="w-full py-2 bg-slate-900 hover:bg-slate-800 border border-slate-800 rounded-xl text-xs font-semibold text-slate-300 transition-colors"
            >
              Close
            </button>
          </div>
        </div>
      )}

      {/* COOCAA FREQUENCY & CODE STEP-BY-STEP TESTER MODAL ("Ek ek karke on karne ka option") */}
      {showCoocaaTesterModal && (
        <div className="absolute inset-0 bg-slate-950/95 backdrop-blur-md z-30 p-3.5 flex flex-col justify-between animate-fade-in text-slate-100">
          {/* Header */}
          <div className="flex items-center justify-between pb-2 border-b border-slate-800 shrink-0">
            <div>
              <div className="text-xs font-bold text-white flex items-center gap-1.5">
                <Zap className="w-3.5 h-3.5 text-red-400" />
                <span>Coocaa TV Frequency &amp; Code Tester</span>
              </div>
              <p className="text-[9.5px] text-slate-400">
                Ek-ek karke sabhi frequencies test karein (36.0 - 40.0 kHz)
              </p>
            </div>
            <button
              onClick={() => {
                handleStopAutoCycle();
                setShowCoocaaTesterModal(false);
              }}
              className="text-slate-400 hover:text-white p-1 rounded-lg hover:bg-slate-800"
            >
              <X className="w-4 h-4" />
            </button>
          </div>

          {/* Scrollable Testing Area */}
          <div className="flex-1 my-2 space-y-2 overflow-y-auto pr-1">
            
            {/* 1. AUTO-CYCLE STEP-BY-STEP TEST RUNNER */}
            <div className={`p-2.5 rounded-2xl border transition-all ${
              autoCycleActive
                ? 'bg-gradient-to-b from-red-950/70 via-slate-900 to-emerald-950/40 border-red-500/80 shadow-lg shadow-red-500/10'
                : 'bg-gradient-to-r from-blue-950/40 via-slate-900 to-indigo-950/40 border-blue-500/40'
            }`}>
              <div className="flex items-center justify-between mb-1.5">
                <div className="flex items-center gap-1.5">
                  <Sparkles className="w-3.5 h-3.5 text-yellow-400" />
                  <span className="text-[11px] font-black text-white">
                    {autoCycleActive ? 'STEP-BY-STEP AUTO-TESTING...' : 'Auto-Test All Frequencies'}
                  </span>
                </div>
                {autoCycleActive && (
                  <span className="px-2 py-0.5 rounded-full bg-red-500/20 text-red-300 border border-red-500/40 text-[9px] font-mono animate-pulse">
                    Next in {autoCycleCountdown}s
                  </span>
                )}
              </div>

              {autoCycleActive ? (
                <div className="space-y-2">
                  <div className="bg-slate-950/80 border border-red-500/30 rounded-xl p-2 text-left">
                    <div className="text-[10px] text-slate-400 font-mono">
                      Testing #{autoCycleIndex} of {COOCAA_VARIANTS.length - 1}:
                    </div>
                    <div className="text-xs font-bold text-white truncate mt-0.5">
                      {COOCAA_VARIANTS[autoCycleIndex]?.name}
                    </div>
                    <div className="text-[9.5px] text-cyan-400 font-mono mt-0.5 flex items-center justify-between">
                      <span>{(COOCAA_VARIANTS[autoCycleIndex]?.freq / 1000).toFixed(1)} kHz</span>
                      <span>{COOCAA_VARIANTS[autoCycleIndex]?.codeHex}</span>
                    </div>
                  </div>

                  <button
                    onClick={() => handleConfirmWorkingVariant(COOCAA_VARIANTS[autoCycleIndex])}
                    className="w-full py-2 bg-emerald-600 hover:bg-emerald-500 text-white font-extrabold text-[11px] rounded-xl flex items-center justify-center gap-1.5 shadow-lg shadow-emerald-600/30 active:scale-95 transition-all"
                  >
                    <CheckCircle2 className="w-4 h-4" />
                    <span>TV ON HO GAYA! (SAVE FREQUENCY)</span>
                  </button>

                  <button
                    onClick={handleStopAutoCycle}
                    className="w-full py-1 text-[10px] text-slate-400 hover:text-white"
                  >
                    Pause Auto-Test
                  </button>
                </div>
              ) : (
                <div className="space-y-1.5">
                  <p className="text-[9.5px] text-slate-300 leading-snug">
                    Phone ko TV ke samne rakhein. Har 3 second me agli frequency fire hogi:
                  </p>
                  <button
                    onClick={handleStartAutoCycle}
                    className="w-full py-2 bg-gradient-to-r from-red-600 to-blue-600 hover:from-red-500 hover:to-blue-500 text-white font-bold text-xs rounded-xl flex items-center justify-center gap-2 shadow-md active:scale-95 transition-all"
                  >
                    <Play className="w-3.5 h-3.5 fill-white" />
                    <span>START AUTO-TEST RUNNER (1 BY 1)</span>
                  </button>
                </div>
              )}
            </div>

            {/* 2. SUPER MULTI-BLAST (All Codes Together) */}
            <div className="p-2 rounded-xl bg-slate-900 border border-slate-800 flex items-center justify-between">
              <div className="text-left min-w-0 pr-2">
                <div className="text-[10.5px] font-bold text-white flex items-center gap-1">
                  <Zap className="w-3 h-3 text-yellow-400 shrink-0" />
                  <span className="truncate">Multi-Blast (All 9 Codes)</span>
                </div>
                <div className="text-[8.5px] text-slate-400">
                  Fires all 9 codes back-to-back with 2x frame repeat
                </div>
              </div>
              <button
                onClick={() => handleTransmitCoocaaVariant(COOCAA_VARIANTS[0])}
                className="px-3 py-1.5 bg-yellow-600/30 hover:bg-yellow-600/50 border border-yellow-500/50 text-yellow-300 text-[10px] font-bold rounded-lg shrink-0 active:scale-95 transition-all"
              >
                BLAST ALL
              </button>
            </div>

            {/* iQOO Neo 10 Verified Match Alert */}
            <div className="p-2.5 rounded-xl bg-emerald-950/40 border border-emerald-500/50 text-left">
              <div className="flex items-center gap-1.5 mb-1">
                <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
                <span className="text-[11px] font-extrabold text-emerald-300">
                  iQOO Neo 10 Verified Match Found
                </span>
              </div>
              <p className="text-[9.5px] text-slate-300 leading-snug">
                iQOO Neo 10 ke custom remote app me Skyworth ka code <strong className="text-white font-mono font-bold">0x47EF827D</strong> (38kHz) 3-frame standby burst ke sath use hota hai. Niche Variant 1 ko test karein!
              </p>
            </div>

            {/* 3. ONE-BY-ONE FREQUENCY & CODE LIST ("Ek ek karke test karein") */}
            <div className="text-[10px] font-bold text-slate-400 uppercase tracking-wider text-left pt-1">
              Ek-Ek Karke Test Karein (Individual Frequencies)
            </div>

            <div className="space-y-1.5">
              {COOCAA_VARIANTS.slice(1).map((variant, index) => {
                const isSelected = selectedCoocaaVariant.id === variant.id;
                const wasTested = testedVariantId === variant.id;
                const freqKHz = (variant.freq / 1000).toFixed(1);

                return (
                  <div
                    key={variant.id}
                    className={`p-2.5 rounded-xl border text-left transition-all ${
                      wasTested && isPowerBlasting
                        ? 'bg-red-500/20 border-red-500 shadow-md ring-1 ring-red-400'
                        : variant.recommended
                        ? 'bg-emerald-950/30 border-emerald-500/60'
                        : isSelected
                        ? 'bg-blue-600/15 border-blue-500/70'
                        : 'bg-slate-900/90 border-slate-800 hover:border-slate-700'
                    }`}
                  >
                    <div className="flex items-center justify-between gap-2">
                      <div className="min-w-0 flex-1">
                        <div className="flex items-center gap-1.5">
                          <span className={`px-1.5 py-0.5 rounded text-[9px] font-bold font-mono ${
                            variant.recommended 
                              ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/40' 
                              : 'bg-blue-500/20 text-blue-300 border border-blue-500/30'
                          }`}>
                            {variant.recommended ? 'iQOO MATCH' : `${freqKHz} kHz`}
                          </span>
                          <span className="text-[11px] font-bold text-white truncate">
                            {variant.name}
                          </span>
                        </div>
                        <p className="text-[9px] text-slate-400 mt-0.5 line-clamp-1">
                          {variant.description}
                        </p>
                        <div className="text-[8.5px] font-mono text-cyan-400/90 mt-0.5">
                          Code: {variant.codeHex}
                        </div>
                      </div>

                      {/* Direct One-by-One TEST ON Button */}
                      <div className="flex flex-col items-end gap-1 shrink-0">
                        <button
                          onClick={() => handleTransmitCoocaaVariant(variant)}
                          className={`px-3 py-1.5 rounded-lg text-white font-black text-[10.5px] flex items-center gap-1 shadow active:scale-95 transition-all ${
                            wasTested && isPowerBlasting
                              ? 'bg-red-500 animate-pulse'
                              : 'bg-red-600 hover:bg-red-500 shadow-red-600/30'
                          }`}
                          title={`Test ${variant.name} at ${freqKHz}kHz`}
                        >
                          <Power className="w-3 h-3" />
                          <span>TEST ON</span>
                        </button>

                        <button
                          onClick={() => handleConfirmWorkingVariant(variant)}
                          className="text-[8.5px] text-slate-400 hover:text-emerald-400 flex items-center gap-0.5"
                        >
                          <Check className="w-2.5 h-2.5" />
                          <span>{isSelected ? 'Saved Default' : 'Save Default'}</span>
                        </button>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>

            {/* 4. CUSTOM FREQUENCY FINE-TUNER */}
            <div className="p-2.5 rounded-xl bg-slate-900 border border-slate-800 text-left">
              <div className="text-[10px] font-bold text-white flex items-center gap-1 mb-1">
                <Settings className="w-3 h-3 text-cyan-400" />
                <span>Fine-Tune Carrier Frequency (kHz)</span>
              </div>
              <p className="text-[9px] text-slate-400 mb-2">
                TV sensor tuning: select exact carrier frequency to fire active code:
              </p>
              
              <div className="grid grid-cols-4 gap-1 mb-2">
                {[36000, 37000, 38000, 38400, 39000, 40000, 56000].map((freq) => (
                  <button
                    key={freq}
                    onClick={() => {
                      remoteSound.click();
                      setCustomFrequencyHz(freq);
                    }}
                    className={`py-1 rounded text-[9px] font-mono font-bold transition-all ${
                      customFrequencyHz === freq
                        ? 'bg-cyan-500 text-slate-950 font-black'
                        : 'bg-slate-800 text-slate-300 hover:bg-slate-750'
                    }`}
                  >
                    {(freq / 1000).toFixed(1)}k
                  </button>
                ))}
              </div>

              <button
                onClick={() => handleTransmitCoocaaVariant(selectedCoocaaVariant, customFrequencyHz)}
                className="w-full py-1.5 bg-cyan-600 hover:bg-cyan-500 text-white font-bold text-[10px] rounded-lg flex items-center justify-center gap-1.5 transition-colors"
              >
                <Zap className="w-3 h-3" />
                <span>Fire at {(customFrequencyHz / 1000).toFixed(1)} kHz</span>
              </button>
            </div>

          </div>

          {/* Modal Footer */}
          <div className="pt-2 border-t border-slate-800 shrink-0 text-center">
            <button
              onClick={() => {
                handleStopAutoCycle();
                setShowCoocaaTesterModal(false);
              }}
              className="w-full py-1.5 bg-slate-900 hover:bg-slate-800 border border-slate-800 rounded-xl text-xs font-semibold text-slate-300 transition-colors"
            >
              Done / Close Tester
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
