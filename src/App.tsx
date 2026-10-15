import React, { useState, useEffect } from 'react';
import { Header } from './components/Header';
import { TodayAlankaramCard } from './components/TodayAlankaramCard';
import { LocationOrganizerView } from './components/LocationOrganizerView';
import { FestivalScheduleView } from './components/FestivalScheduleView';
import { GalleryView } from './components/GalleryView';
import { ManageLocationsModal } from './components/ManageLocationsModal';
import { SettingsModal } from './components/SettingsModal';
import { ApkDownloadModal } from './components/ApkDownloadModal';
import { WifiSyncModal } from './components/WifiSyncModal';

import { DEFAULT_FESTIVAL_SCHEDULE } from './data/defaultSchedule';
import { DEFAULT_LOCATIONS } from './data/defaultLocations';
import { SAMPLE_CAMERA_PHOTOS } from './data/samplePhotos';
import { FestivalScheduleItem, LocationConfigItem, OrganizedPhoto } from './types';

import { 
  MapPin, 
  Calendar, 
  Image as ImageIcon, 
  Sparkles, 
  FolderSync, 
  Download, 
  SlidersHorizontal,
  FolderCheck,
  CheckCircle2,
  ShieldCheck,
  Info,
  Wifi,
  ArrowLeftRight
} from 'lucide-react';

export const App: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'dashboard' | 'location' | 'festival' | 'gallery'>('dashboard');

  // Stored state
  const [schedule, setSchedule] = useState<FestivalScheduleItem[]>(() => {
    const saved = localStorage.getItem('dussehra_schedule');
    return saved ? JSON.parse(saved) : DEFAULT_FESTIVAL_SCHEDULE;
  });

  const [locations, setLocations] = useState<LocationConfigItem[]>(() => {
    const saved = localStorage.getItem('dussehra_locations_v2');
    return saved ? JSON.parse(saved) : DEFAULT_LOCATIONS;
  });

  const [photos, setPhotos] = useState<OrganizedPhoto[]>(() => {
    const saved = localStorage.getItem('dussehra_organized_photos_v2');
    return saved ? JSON.parse(saved) : SAMPLE_CAMERA_PHOTOS;
  });

  // Settings
  const [autoOrganizeLocation, setAutoOrganizeLocation] = useState(true);
  const [autoOrganizeFestival, setAutoOrganizeFestival] = useState(false);
  const [includeVideos, setIncludeVideos] = useState(false);

  // Modals
  const [isLocationsModalOpen, setIsLocationsModalOpen] = useState(false);
  const [isSettingsModalOpen, setIsSettingsModalOpen] = useState(false);
  const [isApkModalOpen, setIsApkModalOpen] = useState(false);
  const [isWifiSyncModalOpen, setIsWifiSyncModalOpen] = useState(false);
  const [galleryInitialFolder, setGalleryInitialFolder] = useState<string | null>(null);

  // Save to localStorage
  useEffect(() => {
    localStorage.setItem('dussehra_schedule', JSON.stringify(schedule));
  }, [schedule]);

  useEffect(() => {
    localStorage.setItem('dussehra_locations_v2', JSON.stringify(locations));
  }, [locations]);

  useEffect(() => {
    localStorage.setItem('dussehra_organized_photos_v2', JSON.stringify(photos));
  }, [photos]);

  // Today's Date Calculation (Formatted as dd-MM-yyyy)
  const now = new Date();
  const day = String(now.getDate()).padStart(2, '0');
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const year = now.getFullYear();
  const todayFormatted = `${day}-${month}-${year}`;

  const todayFestivalItem = schedule.find((item) => item.date === todayFormatted) || null;

  const handlePhotosOrganized = (newlyOrganized: OrganizedPhoto[]) => {
    setPhotos((prev) => {
      const existingIds = new Set(prev.map((p) => p.id));
      const added = newlyOrganized.filter((p) => !existingIds.has(p.id));
      return [...added, ...prev];
    });
  };

  const handleOpenGalleryFolder = (folderName: string) => {
    setGalleryInitialFolder(folderName);
    setActiveTab('gallery');
  };

  return (
    <div className="min-h-screen flex flex-col bg-[#FFF9F2] text-stone-900">
      
      {/* App Header */}
      <Header
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        onOpenSettings={() => setIsSettingsModalOpen(true)}
        onOpenLocations={() => setIsLocationsModalOpen(true)}
        onOpenDownload={() => setIsApkModalOpen(true)}
        onOpenWifiSync={() => setIsWifiSyncModalOpen(true)}
      />

      {/* Main Body */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-6">
        
        {activeTab === 'dashboard' && (
          <div className="space-y-6">
            
            {/* Location-Based Photo Organizer Hero Card */}
            <div className="bg-white rounded-2xl border-2 border-[#D94F00]/30 p-6 shadow-md hover:border-[#D94F00] transition-colors relative overflow-hidden">
              <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div className="space-y-1.5 max-w-2xl">
                  <div className="flex items-center space-x-2">
                    <span className="text-xl">📍</span>
                    <h2 className="text-xl sm:text-2xl font-bold text-stone-900 font-serif-temple">
                      Location-Based Photo Organizer
                    </h2>
                    <span className="text-[10px] font-bold text-emerald-800 bg-emerald-100 px-2 py-0.5 rounded-full border border-emerald-300">
                      Auto-Organize Ready
                    </span>
                  </div>

                  <p className="text-xs text-stone-600 font-telugu">
                    కెమెరా ఫోటోలను ఆటోమేటిక్ గా దసరా అలంకారం &amp; లొకేషన్ల వారీగా భద్రపరుస్తుంది (Home, ఆలయం, కార్ షెడ్ &amp; తేది ఫోల్డర్లు).
                  </p>

                  <p className="text-xs text-stone-500">
                    Registered Mappings: <strong>Home</strong>, <strong>Yeendda Durga Devi Temple</strong>, <strong>Carshed Bobleam</strong>, or fallback to <strong>YYYY-MM-DD</strong> for unmatched / GPS-missing photos.
                  </p>

                  <div className="pt-1 text-xs font-mono font-bold text-red-900 flex items-center space-x-1.5">
                    <ShieldCheck className="w-4 h-4 text-[#D94F00]" />
                    <span>Target Directory: <code>Pictures/Dussehra_2026/[Devi_Alankaram]/</code></span>
                  </div>
                </div>

                <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-2.5 shrink-0">
                  <button
                    onClick={() => setActiveTab('location')}
                    className="px-5 py-3 bg-[#D94F00] hover:bg-[#C93B00] text-white font-bold rounded-xl text-sm shadow-md transition-all hover:scale-102 flex items-center justify-center space-x-2"
                  >
                    <MapPin className="w-4 h-4" />
                    <span>Organize by Location</span>
                  </button>

                  <button
                    onClick={() => setIsLocationsModalOpen(true)}
                    className="px-4 py-3 bg-stone-100 hover:bg-stone-200 text-stone-800 font-semibold rounded-xl text-xs flex items-center justify-center space-x-1.5 border border-stone-300"
                  >
                    <SlidersHorizontal className="w-4 h-4 text-stone-600" />
                    <span>Edit Locations</span>
                  </button>
                </div>
              </div>

              {/* Quick Locations Preview */}
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 mt-5 pt-4 border-t border-stone-100">
                {locations.map((loc) => (
                  <div key={loc.id} className="bg-[#FFF9F2] p-3 rounded-xl border border-amber-200 text-xs">
                    <div className="flex items-center justify-between font-bold text-stone-900">
                      <span>{loc.name}</span>
                      <span className="text-[#8C0000] font-mono text-[11px] bg-white px-2 py-0.5 rounded border border-amber-200">
                        {loc.folderName}/
                      </span>
                    </div>
                    <p className="text-stone-500 text-[11px] truncate mt-1">
                      {loc.address}
                    </p>
                    <div className="text-[10px] text-amber-800 mt-1 font-mono">
                      Radius: {loc.radiusMeters}m (Offline EXIF matching)
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Automatic Two-Way Wi-Fi Sync Card */}
            <div className="bg-gradient-to-r from-emerald-950 via-stone-900 to-[#4A000E] text-white rounded-2xl p-6 shadow-md border-2 border-emerald-400/40 relative overflow-hidden">
              <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 relative z-10">
                <div className="space-y-1.5 max-w-2xl">
                  <div className="flex items-center space-x-2">
                    <Wifi className="w-6 h-6 text-emerald-400" />
                    <h2 className="text-xl sm:text-2xl font-bold font-serif-temple text-amber-100">
                      Automatic Two-Way Wi-Fi Photo Sync
                    </h2>
                    <span className="text-[10px] font-bold text-emerald-300 bg-emerald-500/20 px-2.5 py-0.5 rounded-full border border-emerald-400/40 font-mono">
                      P2P ACTIVE
                    </span>
                  </div>

                  <p className="text-xs text-amber-200/90 font-telugu">
                    రెండు ఫోన్ల మధ్య ఆటోమేటిక్ గా కొత్త ఫోటోలను సమకాలీకరించండి — బటన్ నొక్కాల్సిన అవసరం లేదు.
                  </p>

                  <p className="text-xs text-stone-300">
                    Transfers camera photos automatically between paired phones over local Wi-Fi. Saves into exact Dussehra Devi &amp; Location folders with SHA-256 duplicate protection.
                  </p>
                </div>

                <div className="flex items-center space-x-3">
                  <button
                    onClick={() => setIsWifiSyncModalOpen(true)}
                    className="bg-emerald-500 hover:bg-emerald-400 text-stone-950 font-bold px-5 py-3 rounded-xl text-xs flex items-center space-x-2 shadow-lg transition-all hover:scale-102 cursor-pointer"
                  >
                    <ArrowLeftRight className="w-4 h-4" />
                    <span>Manage Wi-Fi Sync &amp; Pairing</span>
                  </button>
                </div>
              </div>
            </div>

            {/* Today's Devi Alankaram & Festival Schedule Card */}
            <TodayAlankaramCard
              todayItem={todayFestivalItem}
              currentDateFormatted={todayFormatted}
            />

            {/* Quick Navigation Cards */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              
              <div
                onClick={() => setActiveTab('festival')}
                className="bg-white rounded-2xl border border-amber-200 p-5 shadow-xs hover:shadow-md cursor-pointer group transition-all"
              >
                <div className="w-10 h-10 rounded-xl bg-amber-100 text-[#D94F00] flex items-center justify-center mb-3 group-hover:scale-110 transition-transform">
                  <Calendar className="w-5 h-5" />
                </div>
                <h3 className="font-serif-temple font-bold text-base text-stone-900">
                  Festival Schedule
                </h3>
                <p className="text-xs text-stone-500 mt-1">
                  11 Visakhapatnam temple Devi Alankarams with special poojas &amp; clean folder names without dates.
                </p>
                <span className="text-xs font-bold text-[#D94F00] mt-3 inline-block group-hover:underline">
                  View Full Schedule &rarr;
                </span>
              </div>

              <div
                onClick={() => setActiveTab('gallery')}
                className="bg-white rounded-2xl border border-amber-200 p-5 shadow-xs hover:shadow-md cursor-pointer group transition-all"
              >
                <div className="w-10 h-10 rounded-xl bg-amber-100 text-red-800 flex items-center justify-center mb-3 group-hover:scale-110 transition-transform">
                  <ImageIcon className="w-5 h-5" />
                </div>
                <h3 className="font-serif-temple font-bold text-base text-stone-900">
                  Organized Gallery
                </h3>
                <p className="text-xs text-stone-500 mt-1">
                  Browse photos grouped by Home, Temple, Carshed, Date folders, or Goddess Alankarams.
                </p>
                <span className="text-xs font-bold text-red-800 mt-3 inline-block group-hover:underline">
                  Open Photo Gallery ({photos.length}) &rarr;
                </span>
              </div>

              <div
                onClick={() => setIsApkModalOpen(true)}
                className="bg-gradient-to-br from-emerald-50 to-teal-50 rounded-2xl border border-emerald-200 p-5 shadow-xs hover:shadow-md cursor-pointer group transition-all"
              >
                <div className="w-10 h-10 rounded-xl bg-emerald-200 text-emerald-800 flex items-center justify-center mb-3 group-hover:scale-110 transition-transform">
                  <Download className="w-5 h-5" />
                </div>
                <h3 className="font-serif-temple font-bold text-base text-stone-900">
                  Android APK &amp; Source
                </h3>
                <p className="text-xs text-stone-600 mt-1">
                  Install the ready-to-run APK (6.5 MB) on Samsung/Vivo devices or grab the full Android project ZIP.
                </p>
                <span className="text-xs font-bold text-emerald-700 mt-3 inline-block group-hover:underline">
                  Get Download Links &rarr;
                </span>
              </div>

            </div>

            {/* Strict Safety Banner */}
            <div className="bg-stone-50 rounded-2xl border border-stone-200 p-4 text-xs text-stone-600 flex items-start space-x-3">
              <Info className="w-4 h-4 text-stone-500 shrink-0 mt-0.5" />
              <div>
                <strong className="text-stone-900">Strict Safety &amp; Non-Destructive Operation:</strong>
                <p className="mt-0.5">
                  Original camera photos in <code>DCIM/Camera/</code> are strictly safe — files are copied, never moved, renamed, or deleted. Non-camera sources like WhatsApp, Telegram, Screenshots, and Downloads are automatically excluded.
                </p>
              </div>
            </div>

          </div>
        )}

        {activeTab === 'location' && (
          <LocationOrganizerView
            locations={locations}
            schedule={schedule}
            onOpenManageLocations={() => setIsLocationsModalOpen(true)}
            onPhotosOrganized={handlePhotosOrganized}
          />
        )}

        {activeTab === 'festival' && (
          <FestivalScheduleView
            schedule={schedule}
            onUpdateSchedule={setSchedule}
            onSelectFolderToView={handleOpenGalleryFolder}
          />
        )}

        {activeTab === 'gallery' && (
          <GalleryView
            photos={photos}
            initialFolder={galleryInitialFolder}
          />
        )}

      </main>

      {/* Footer */}
      <footer className="border-t border-amber-200/60 bg-white/60 py-6 text-center text-xs text-stone-500 mt-12">
        <p className="font-telugu">
          శ్రీ దుర్గాదేవి అమ్మవారి దసరా ఉత్సవములు 2026 &amp; లొకేషన్ ఫోటో ఆర్గనైజర్ • Visakhapatnam
        </p>
        <p className="mt-1 text-[11px]">
          Target Storage: <code>Pictures/Dussehra_2026/[Devi_Alankaram]/[Home | Temple | Carshed | YYYY-MM-DD]/</code> • Offline EXIF GPS Engine
        </p>
      </footer>

      {/* Modals */}
      <ManageLocationsModal
        isOpen={isLocationsModalOpen}
        onClose={() => setIsLocationsModalOpen(false)}
        locations={locations}
        onSaveLocations={setLocations}
      />

      <SettingsModal
        isOpen={isSettingsModalOpen}
        onClose={() => setIsSettingsModalOpen(false)}
        autoOrganizeLocation={autoOrganizeLocation}
        setAutoOrganizeLocation={setAutoOrganizeLocation}
        autoOrganizeFestival={autoOrganizeFestival}
        setAutoOrganizeFestival={setAutoOrganizeFestival}
        includeVideos={includeVideos}
        setIncludeVideos={setIncludeVideos}
        onOpenManageLocations={() => setIsLocationsModalOpen(true)}
      />

      <ApkDownloadModal
        isOpen={isApkModalOpen}
        onClose={() => setIsApkModalOpen(false)}
      />

      <WifiSyncModal
        isOpen={isWifiSyncModalOpen}
        onClose={() => setIsWifiSyncModalOpen(false)}
        onPhotosOrganized={handlePhotosOrganized}
      />

    </div>
  );
};
