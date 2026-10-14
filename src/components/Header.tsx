import React from 'react';
import { Camera, MapPin, Calendar, Image as ImageIcon, Settings, Download } from 'lucide-react';

interface HeaderProps {
  activeTab: 'dashboard' | 'location' | 'festival' | 'gallery';
  setActiveTab: (tab: 'dashboard' | 'location' | 'festival' | 'gallery') => void;
  onOpenSettings: () => void;
  onOpenLocations: () => void;
  onOpenDownload: () => void;
}

export const Header: React.FC<HeaderProps> = ({
  activeTab,
  setActiveTab,
  onOpenSettings,
  onOpenLocations,
  onOpenDownload,
}) => {
  return (
    <header className="bg-gradient-to-r from-[#D94F00] via-[#C93B00] to-[#8C0000] text-white shadow-lg sticky top-0 z-30 border-b-2 border-amber-400">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-18">
          
          {/* Brand */}
          <div 
            onClick={() => setActiveTab('dashboard')}
            className="flex items-center space-x-3 cursor-pointer group"
          >
            <div className="w-11 h-11 rounded-xl bg-amber-400/20 border border-amber-300/40 flex items-center justify-center text-2xl shadow-inner group-hover:scale-105 transition-transform">
              🪔
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <h1 className="font-serif-temple text-lg sm:text-xl font-bold tracking-wide text-amber-100">
                  Dussehra Photo Organizer
                </h1>
                <span className="hidden sm:inline-block text-[10px] font-bold uppercase tracking-wider bg-amber-400/30 text-amber-200 px-2 py-0.5 rounded-full border border-amber-300/30">
                  2026 + GPS
                </span>
              </div>
              <p className="font-telugu text-xs text-amber-200/90 truncate max-w-[280px] sm:max-w-none">
                దసరా నవరాత్రి &amp; లొకేషన్ ఫోటో ఆర్గనైజర్
              </p>
            </div>
          </div>

          {/* Navigation Links */}
          <nav className="hidden md:flex items-center space-x-1 lg:space-x-2">
            <button
              onClick={() => setActiveTab('dashboard')}
              className={`px-3 py-2 rounded-lg text-sm font-medium transition-all ${
                activeTab === 'dashboard'
                  ? 'bg-amber-400 text-stone-900 font-bold shadow'
                  : 'text-amber-100 hover:bg-white/10'
              }`}
            >
              Dashboard
            </button>
            <button
              onClick={() => setActiveTab('location')}
              className={`px-3 py-2 rounded-lg text-sm font-medium flex items-center space-x-1.5 transition-all ${
                activeTab === 'location'
                  ? 'bg-amber-400 text-stone-900 font-bold shadow'
                  : 'text-amber-100 hover:bg-white/10'
              }`}
            >
              <MapPin className="w-4 h-4" />
              <span>Location Organizer</span>
            </button>
            <button
              onClick={() => setActiveTab('festival')}
              className={`px-3 py-2 rounded-lg text-sm font-medium flex items-center space-x-1.5 transition-all ${
                activeTab === 'festival'
                  ? 'bg-amber-400 text-stone-900 font-bold shadow'
                  : 'text-amber-100 hover:bg-white/10'
              }`}
            >
              <Calendar className="w-4 h-4" />
              <span>Festival Schedule</span>
            </button>
            <button
              onClick={() => setActiveTab('gallery')}
              className={`px-3 py-2 rounded-lg text-sm font-medium flex items-center space-x-1.5 transition-all ${
                activeTab === 'gallery'
                  ? 'bg-amber-400 text-stone-900 font-bold shadow'
                  : 'text-amber-100 hover:bg-white/10'
              }`}
            >
              <ImageIcon className="w-4 h-4" />
              <span>Gallery</span>
            </button>
          </nav>

          {/* Right Action Buttons */}
          <div className="flex items-center space-x-2">
            <button
              onClick={onOpenLocations}
              className="p-2 text-amber-100 hover:text-white hover:bg-white/10 rounded-lg transition-colors flex items-center space-x-1 text-xs font-semibold"
              title="Manage Registered Locations"
            >
              <MapPin className="w-4 h-4 text-amber-300" />
              <span className="hidden lg:inline">Locations</span>
            </button>

            <button
              onClick={onOpenDownload}
              className="bg-amber-400 hover:bg-amber-300 text-stone-900 font-semibold px-3 py-1.5 rounded-lg text-xs flex items-center space-x-1.5 shadow transition-all hover:scale-102"
              title="Download Android APK & Source"
            >
              <Download className="w-4 h-4" />
              <span>Get APK</span>
            </button>

            <button
              onClick={onOpenSettings}
              className="p-2 text-amber-100 hover:text-white hover:bg-white/10 rounded-lg transition-colors"
              title="Settings"
            >
              <Settings className="w-5 h-5" />
            </button>
          </div>

        </div>
      </div>

      {/* Mobile Submenu Bar */}
      <div className="md:hidden flex items-center justify-around bg-black/20 px-2 py-1.5 border-t border-white/10 text-xs">
        <button
          onClick={() => setActiveTab('dashboard')}
          className={`px-2.5 py-1 rounded-md font-medium ${
            activeTab === 'dashboard' ? 'bg-amber-400 text-stone-900 font-bold' : 'text-amber-100'
          }`}
        >
          Overview
        </button>
        <button
          onClick={() => setActiveTab('location')}
          className={`px-2.5 py-1 rounded-md font-medium flex items-center space-x-1 ${
            activeTab === 'location' ? 'bg-amber-400 text-stone-900 font-bold' : 'text-amber-100'
          }`}
        >
          <MapPin className="w-3.5 h-3.5" />
          <span>Location</span>
        </button>
        <button
          onClick={() => setActiveTab('festival')}
          className={`px-2.5 py-1 rounded-md font-medium flex items-center space-x-1 ${
            activeTab === 'festival' ? 'bg-amber-400 text-stone-900 font-bold' : 'text-amber-100'
          }`}
        >
          <Calendar className="w-3.5 h-3.5" />
          <span>Schedule</span>
        </button>
        <button
          onClick={() => setActiveTab('gallery')}
          className={`px-2.5 py-1 rounded-md font-medium flex items-center space-x-1 ${
            activeTab === 'gallery' ? 'bg-amber-400 text-stone-900 font-bold' : 'text-amber-100'
          }`}
        >
          <ImageIcon className="w-3.5 h-3.5" />
          <span>Gallery</span>
        </button>
      </div>
    </header>
  );
};
