import React, { useState, useEffect } from 'react';
import { 
  Wifi, 
  Smartphone, 
  ArrowLeftRight, 
  ShieldCheck, 
  RefreshCw, 
  CheckCircle2, 
  Clock, 
  Hash, 
  X, 
  AlertTriangle, 
  Send, 
  DownloadCloud,
  Unlink,
  Radio
} from 'lucide-react';
import { OrganizedPhoto } from '../types';

interface WifiSyncModalProps {
  isOpen: boolean;
  onClose: () => void;
  onPhotosOrganized: (newPhotos: OrganizedPhoto[]) => void;
}

interface SyncLog {
  id: string;
  time: string;
  fileName: string;
  direction: 'SENT' | 'RECEIVED';
  status: 'SUCCESS' | 'FAILED';
  details: string;
}

export const WifiSyncModal: React.FC<WifiSyncModalProps> = ({
  isOpen,
  onClose,
  onPhotosOrganized,
}) => {
  const [isPaired, setIsPaired] = useState(true);
  const [pairedDeviceName, setPairedDeviceName] = useState('Samsung Galaxy A04e');
  const [pairedDeviceIp, setPairedDeviceIp] = useState('192.168.1.102');
  const [thisDeviceName] = useState('Vivo Y18t (This Phone)');
  const [thisDeviceIp] = useState('192.168.1.105');
  const [pairingPin, setPairingPin] = useState('849201');
  const [syncStatus, setSyncStatus] = useState<'IDLE' | 'SYNCING' | 'SUCCESS'>('IDLE');
  const [lastSyncTime, setLastSyncTime] = useState('Just now');
  const [sentCount, setSentCount] = useState(3);
  const [receivedCount, setReceivedCount] = useState(5);
  const [logs, setLogs] = useState<SyncLog[]>([
    {
      id: '1',
      time: '10:15 AM',
      fileName: 'IMG_20261012_101245.jpg',
      direction: 'RECEIVED',
      status: 'SUCCESS',
      details: 'Organized into Pictures/Dussehra_2026/Sri_Gayatri_Devi/Home/'
    },
    {
      id: '2',
      time: '10:14 AM',
      fileName: 'IMG_20261012_094512.jpg',
      direction: 'SENT',
      status: 'SUCCESS',
      details: 'Synced to Samsung Galaxy A04e (SHA-256 verified)'
    },
    {
      id: '3',
      time: '09:30 AM',
      fileName: 'IMG_20261011_182010.jpg',
      direction: 'RECEIVED',
      status: 'SUCCESS',
      details: 'Organized into Pictures/Dussehra_2026/Sri_Bala_Tripura_Sundari_Devi/Yeendda_Durga_Devi_Temple/'
    }
  ]);

  if (!isOpen) return null;

  const handleSimulateNewPhotoCapture = () => {
    setSyncStatus('SYNCING');
    setTimeout(() => {
      const now = new Date();
      const timeStr = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
      const newFileName = `IMG_20261014_${Math.floor(100000 + Math.random() * 900000)}.jpg`;
      
      const newLog: SyncLog = {
        id: String(Date.now()),
        time: timeStr,
        fileName: newFileName,
        direction: 'SENT',
        status: 'SUCCESS',
        details: `Eligible Dussehra photo (Sri Kathyayani Devi) auto-transferred to ${pairedDeviceName}`
      };

      setLogs((prev) => [newLog, ...prev]);
      setSentCount((c) => c + 1);
      setLastSyncTime(timeStr);
      setSyncStatus('SUCCESS');

      setTimeout(() => setSyncStatus('IDLE'), 2500);
    }, 1000);
  };

  const handleSimulateExcludedImage = (type: 'whatsapp' | 'screenshot' | 'old' | 'non_festival') => {
    setSyncStatus('SYNCING');
    setTimeout(() => {
      const now = new Date();
      const timeStr = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
      let fileName = '';
      let details = '';

      if (type === 'whatsapp') {
        fileName = `IMG-20261014-WA${Math.floor(1000 + Math.random() * 9000)}.jpg`;
        details = 'BLOCKED before queue: WhatsApp media strictly excluded from Dussehra sync';
      } else if (type === 'screenshot') {
        fileName = `Screenshot_20261014-103000.png`;
        details = 'BLOCKED before queue: Screenshot media strictly excluded from sync';
      } else if (type === 'old') {
        fileName = `IMG_20250915_142010.jpg`;
        details = 'BLOCKED before queue: Old photo captured prior to sync setup baseline';
      } else {
        fileName = `IMG_20260810_110000.jpg`;
        details = 'BLOCKED before queue: Capture date outside configured Dussehra 2026 schedule';
      }

      const newLog: SyncLog = {
        id: String(Date.now()),
        time: timeStr,
        fileName,
        direction: 'SENT',
        status: 'FAILED',
        details
      };

      setLogs((prev) => [newLog, ...prev]);
      setSyncStatus('IDLE');
    }, 600);
  };

  const handleSimulateMissingGpsPhoto = () => {
    setSyncStatus('SYNCING');
    setTimeout(() => {
      const now = new Date();
      const timeStr = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
      const newFileName = `IMG_20261012_${Math.floor(100000 + Math.random() * 900000)}.jpg`;

      const newLog: SyncLog = {
        id: String(Date.now()),
        time: timeStr,
        fileName: newFileName,
        direction: 'RECEIVED',
        status: 'SUCCESS',
        details: 'Eligible (Missing GPS) -> Saved into fallback: Sri_Gayatri_Devi/2026-10-12/'
      };

      setLogs((prev) => [newLog, ...prev]);
      setReceivedCount((c) => c + 1);
      setLastSyncTime(timeStr);
      setSyncStatus('SUCCESS');

      setTimeout(() => setSyncStatus('IDLE'), 2500);
    }, 1000);
  };

  const handleSimulateIncomingPhoto = () => {
    setSyncStatus('SYNCING');
    setTimeout(() => {
      const now = new Date();
      const timeStr = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
      const newFileName = `SAM_${Math.floor(100000 + Math.random() * 900000)}.jpg`;

      const newLog: SyncLog = {
        id: String(Date.now()),
        time: timeStr,
        fileName: newFileName,
        direction: 'RECEIVED',
        status: 'SUCCESS',
        details: 'Received & organized into Sri_Lalitha_Tripura_Sundari_Devi/Yeendda_Durga_Devi_Temple/'
      };

      setLogs((prev) => [newLog, ...prev]);
      setReceivedCount((c) => c + 1);
      setLastSyncTime(timeStr);
      setSyncStatus('SUCCESS');

      setTimeout(() => setSyncStatus('IDLE'), 2500);
    }, 1000);
  };

  const handleRegeneratePin = () => {
    const newPin = String(Math.floor(100000 + Math.random() * 900000));
    setPairingPin(newPin);
  };

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-stone-900/60 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-2xl max-w-2xl w-full border border-amber-200 overflow-hidden my-8 animate-in fade-in zoom-in-95 duration-150">
        
        {/* Header */}
        <div className="bg-gradient-to-r from-[#780016] to-[#4A000E] text-white p-5 flex items-center justify-between border-b-2 border-amber-400">
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 rounded-xl bg-amber-400/20 border border-amber-400/40 flex items-center justify-center">
              <Wifi className="w-6 h-6 text-amber-300" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-amber-100 flex items-center space-x-2">
                <span>Automatic Wi-Fi Photo Sync</span>
                <span className="text-[10px] bg-emerald-500/30 text-emerald-300 border border-emerald-400/30 px-2 py-0.5 rounded-full font-mono uppercase">
                  P2P Active
                </span>
              </h2>
              <p className="text-xs text-amber-200/90 font-telugu">
                రెండు ఫోన్ల మధ్య ఆటోమేటిక్ గా కొత్త ఫోటోలను సమకాలీకరించండి
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-lg text-amber-200 hover:text-white hover:bg-white/10 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content Body */}
        <div className="p-6 space-y-6 max-h-[75vh] overflow-y-auto bg-[#FFFDF8]">

          {/* Status Banner */}
          <div className="bg-[#FFF8EE] border border-amber-200 rounded-xl p-4 flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <div className="relative">
                <Radio className="w-5 h-5 text-emerald-600 animate-pulse" />
              </div>
              <div>
                <p className="text-xs font-bold uppercase tracking-wider text-emerald-800">
                  {syncStatus === 'SYNCING' ? 'Synchronizing Photos...' : syncStatus === 'SUCCESS' ? 'Photos Synchronized' : 'Automatic Sync Ready'}
                </p>
                <p className="text-xs text-stone-600">
                  {isPaired 
                    ? `Connected with ${pairedDeviceName} on Wi-Fi subnet` 
                    : 'Pair with another phone to begin automatic synchronization'}
                </p>
              </div>
            </div>
            <div className="text-right">
              <span className="text-[11px] font-mono bg-stone-100 text-stone-700 px-2.5 py-1 rounded-md border border-stone-200">
                Last: {lastSyncTime}
              </span>
            </div>
          </div>

          {/* Two Phones Pairing Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            
            {/* Device A (This Device) */}
            <div className="bg-white p-4 rounded-xl border border-stone-200 shadow-sm relative">
              <div className="flex items-center justify-between mb-2">
                <span className="text-xs font-bold text-amber-800 uppercase tracking-wide flex items-center space-x-1.5">
                  <Smartphone className="w-4 h-4 text-amber-700" />
                  <span>This Device</span>
                </span>
                <span className="text-[10px] bg-emerald-100 text-emerald-800 px-2 py-0.5 rounded-full font-medium">
                  Host Active
                </span>
              </div>
              <p className="font-bold text-sm text-stone-900">{thisDeviceName}</p>
              <p className="text-xs text-stone-500 font-mono mt-0.5">IP: {thisDeviceIp}</p>
              
              <div className="mt-3 pt-3 border-t border-stone-100">
                <span className="text-[11px] text-stone-500 block mb-1">6-Digit Pairing PIN:</span>
                <div className="flex items-center space-x-2">
                  <div className="bg-amber-50 text-amber-900 font-mono font-bold text-lg px-3 py-1 rounded-lg border border-amber-300 tracking-widest">
                    {pairingPin}
                  </div>
                  <button
                    onClick={handleRegeneratePin}
                    className="p-1.5 text-stone-400 hover:text-amber-800 rounded-lg hover:bg-stone-50"
                    title="Generate New PIN"
                  >
                    <RefreshCw className="w-4 h-4" />
                  </button>
                </div>
              </div>
            </div>

            {/* Device B (Paired Device) */}
            <div className="bg-white p-4 rounded-xl border border-stone-200 shadow-sm relative">
              <div className="flex items-center justify-between mb-2">
                <span className="text-xs font-bold text-[#780016] uppercase tracking-wide flex items-center space-x-1.5">
                  <Smartphone className="w-4 h-4 text-[#780016]" />
                  <span>Paired Device</span>
                </span>
                {isPaired && (
                  <span className="text-[10px] bg-emerald-100 text-emerald-800 px-2 py-0.5 rounded-full font-medium">
                    Paired
                  </span>
                )}
              </div>

              {isPaired ? (
                <>
                  <p className="font-bold text-sm text-stone-900">{pairedDeviceName}</p>
                  <p className="text-xs text-stone-500 font-mono mt-0.5">IP: {pairedDeviceIp}</p>
                  
                  <div className="mt-3 pt-3 border-t border-stone-100 flex items-center justify-between">
                    <span className="text-xs text-stone-500 flex items-center space-x-1">
                      <ShieldCheck className="w-3.5 h-3.5 text-emerald-600" />
                      <span>Mutual HMAC-SHA256</span>
                    </span>
                    <button
                      onClick={() => setIsPaired(false)}
                      className="text-xs text-rose-600 hover:text-rose-800 font-medium flex items-center space-x-1"
                    >
                      <Unlink className="w-3 h-3" />
                      <span>Unpair</span>
                    </button>
                  </div>
                </>
              ) : (
                <div className="py-2">
                  <p className="text-xs text-stone-500 mb-3">No paired phone connected.</p>
                  <button
                    onClick={() => setIsPaired(true)}
                    className="w-full py-2 bg-gradient-to-r from-amber-600 to-amber-700 text-white text-xs font-bold rounded-lg shadow hover:opacity-95"
                  >
                    Pair Phone Now
                  </button>
                </div>
              )}
            </div>

          </div>

          {/* Ledger Statistics */}
          <div className="bg-stone-50 border border-stone-200 rounded-xl p-4">
            <div className="flex items-center justify-between mb-3">
              <h3 className="text-xs font-bold text-stone-700 uppercase tracking-wider flex items-center space-x-1.5">
                <Hash className="w-4 h-4 text-amber-600" />
                <span>Persistent SHA-256 Ledger (Zero Duplicates)</span>
              </h3>
              <span className="text-[10px] bg-blue-100 text-blue-800 px-2 py-0.5 rounded-full font-medium">
                Verified Storage
              </span>
            </div>

            <div className="grid grid-cols-3 gap-3 text-center">
              <div className="bg-white p-3 rounded-lg border border-stone-200">
                <p className="text-xl font-bold text-amber-700">{sentCount}</p>
                <p className="text-[11px] text-stone-500">Photos Sent ↑</p>
              </div>
              <div className="bg-white p-3 rounded-lg border border-stone-200">
                <p className="text-xl font-bold text-emerald-700">{receivedCount}</p>
                <p className="text-[11px] text-stone-500">Photos Received ↓</p>
              </div>
              <div className="bg-white p-3 rounded-lg border border-stone-200">
                <p className="text-xl font-bold text-[#780016]">{sentCount + receivedCount}</p>
                <p className="text-[11px] text-stone-500">Total in Sync</p>
              </div>
            </div>

            {/* Simulation Triggers */}
            <div className="mt-4 pt-3 border-t border-stone-200">
              <p className="text-[11px] font-bold text-stone-600 mb-2 uppercase tracking-wide">
                Strict Dussehra Filter Simulator:
              </p>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                <button
                  onClick={handleSimulateNewPhotoCapture}
                  disabled={syncStatus === 'SYNCING'}
                  className="flex items-center justify-center space-x-1.5 py-2 px-3 bg-amber-600 hover:bg-amber-700 text-white text-xs font-bold rounded-lg shadow-sm transition-all text-left"
                >
                  <Send className="w-3.5 h-3.5 shrink-0" />
                  <span>Eligible Dussehra Photo (Send)</span>
                </button>

                <button
                  onClick={handleSimulateIncomingPhoto}
                  disabled={syncStatus === 'SYNCING'}
                  className="flex items-center justify-center space-x-1.5 py-2 px-3 bg-emerald-700 hover:bg-emerald-800 text-white text-xs font-bold rounded-lg shadow-sm transition-all text-left"
                >
                  <DownloadCloud className="w-3.5 h-3.5 shrink-0" />
                  <span>Eligible Dussehra Photo (Receive)</span>
                </button>

                <button
                  onClick={handleSimulateMissingGpsPhoto}
                  disabled={syncStatus === 'SYNCING'}
                  className="flex items-center justify-center space-x-1.5 py-2 px-3 bg-indigo-700 hover:bg-indigo-800 text-white text-xs font-bold rounded-lg shadow-sm transition-all text-left"
                >
                  <span>Eligible (Missing GPS Fallback)</span>
                </button>

                <button
                  onClick={() => handleSimulateExcludedImage('whatsapp')}
                  disabled={syncStatus === 'SYNCING'}
                  className="flex items-center justify-center space-x-1.5 py-2 px-3 bg-rose-50 hover:bg-rose-100 text-rose-800 border border-rose-300 text-xs font-semibold rounded-lg shadow-sm transition-all text-left"
                >
                  <span>Test WhatsApp Image (Blocked)</span>
                </button>

                <button
                  onClick={() => handleSimulateExcludedImage('screenshot')}
                  disabled={syncStatus === 'SYNCING'}
                  className="flex items-center justify-center space-x-1.5 py-2 px-3 bg-rose-50 hover:bg-rose-100 text-rose-800 border border-rose-300 text-xs font-semibold rounded-lg shadow-sm transition-all text-left"
                >
                  <span>Test Screenshot (Blocked)</span>
                </button>

                <button
                  onClick={() => handleSimulateExcludedImage('old')}
                  disabled={syncStatus === 'SYNCING'}
                  className="flex items-center justify-center space-x-1.5 py-2 px-3 bg-rose-50 hover:bg-rose-100 text-rose-800 border border-rose-300 text-xs font-semibold rounded-lg shadow-sm transition-all text-left"
                >
                  <span>Test Pre-Sync Old Photo (Blocked)</span>
                </button>

                <button
                  onClick={() => handleSimulateExcludedImage('non_festival')}
                  disabled={syncStatus === 'SYNCING'}
                  className="sm:col-span-2 flex items-center justify-center space-x-1.5 py-2 px-3 bg-amber-50 hover:bg-amber-100 text-amber-900 border border-amber-300 text-xs font-semibold rounded-lg shadow-sm transition-all text-left"
                >
                  <span>Test Non-Festival Date Photo (Blocked: Outside Dussehra Schedule)</span>
                </button>
              </div>
            </div>
          </div>

          {/* Sync Activity History Log */}
          <div>
            <h3 className="text-xs font-bold text-stone-700 uppercase tracking-wider mb-2 flex items-center space-x-1.5">
              <Clock className="w-4 h-4 text-stone-500" />
              <span>Recent Activity History</span>
            </h3>

            <div className="space-y-2 max-h-48 overflow-y-auto pr-1">
              {logs.map((log) => (
                <div 
                  key={log.id} 
                  className="bg-white p-3 rounded-lg border border-stone-200 flex items-start justify-between text-xs"
                >
                  <div className="flex items-start space-x-2.5">
                    <span className={`w-6 h-6 rounded flex items-center justify-center text-xs font-bold shrink-0 mt-0.5 ${
                      log.direction === 'RECEIVED' 
                        ? 'bg-emerald-100 text-emerald-800' 
                        : 'bg-amber-100 text-amber-800'
                    }`}>
                      {log.direction === 'RECEIVED' ? '↓' : '↑'}
                    </span>
                    <div>
                      <p className="font-bold text-stone-900 font-mono text-[11px]">{log.fileName}</p>
                      <p className="text-[11px] text-stone-500 mt-0.5">{log.details}</p>
                    </div>
                  </div>
                  <div className="text-right shrink-0 ml-3">
                    <span className="text-[10px] font-bold text-emerald-700 block">SUCCESS</span>
                    <span className="text-[10px] text-stone-400">{log.time}</span>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Router / AP Isolation Guidance */}
          <div className="bg-amber-50/80 border border-amber-300/80 rounded-xl p-3.5 text-xs text-amber-950 flex items-start space-x-2.5">
            <AlertTriangle className="w-4 h-4 text-amber-700 shrink-0 mt-0.5" />
            <div>
              <p className="font-bold text-amber-900">Wi-Fi Router AP Isolation Information:</p>
              <p className="text-amber-800 mt-0.5 leading-relaxed">
                Both phones must be on the same local Wi-Fi network. Some public or office Wi-Fi routers enable "AP / Client Isolation", which prevents devices on the same Wi-Fi from talking to each other.
              </p>
              <p className="text-amber-900 font-semibold mt-1">
                Tip: Turning on Android Mobile Hotspot on one phone and connecting the other phone to it bypasses router isolation completely with maximum transfer speeds!
              </p>
            </div>
          </div>

        </div>

        {/* Footer */}
        <div className="bg-stone-100 px-6 py-4 flex items-center justify-between border-t border-stone-200">
          <span className="text-xs text-stone-500">
            Native Android Service: <code className="text-stone-700 font-bold">SyncForegroundService</code>
          </span>
          <button
            onClick={onClose}
            className="px-5 py-2 bg-stone-800 hover:bg-stone-900 text-white rounded-lg text-xs font-bold transition-colors"
          >
            Close
          </button>
        </div>

      </div>
    </div>
  );
};
