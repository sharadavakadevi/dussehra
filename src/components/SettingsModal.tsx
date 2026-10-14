import React from 'react';
import { X, Sliders, ShieldCheck, HardDrive, Smartphone } from 'lucide-react';

interface SettingsModalProps {
  isOpen: boolean;
  onClose: () => void;
  autoOrganizeLocation: boolean;
  setAutoOrganizeLocation: (val: boolean) => void;
  autoOrganizeFestival: boolean;
  setAutoOrganizeFestival: (val: boolean) => void;
  includeVideos: boolean;
  setIncludeVideos: (val: boolean) => void;
  onOpenManageLocations: () => void;
}

export const SettingsModal: React.FC<SettingsModalProps> = ({
  isOpen,
  onClose,
  autoOrganizeLocation,
  setAutoOrganizeLocation,
  autoOrganizeFestival,
  setAutoOrganizeFestival,
  includeVideos,
  setIncludeVideos,
  onOpenManageLocations,
}) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl space-y-4">
        
        <div className="flex items-center justify-between border-b border-stone-200 pb-3">
          <div className="flex items-center space-x-2">
            <Sliders className="w-5 h-5 text-[#D94F00]" />
            <h3 className="text-lg font-bold text-stone-900 font-serif-temple">
              App Preferences
            </h3>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-lg hover:bg-stone-100 text-stone-400 hover:text-stone-700"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="space-y-4 text-xs">
          
          {/* Location Auto Organize */}
          <div className="flex items-start justify-between p-3.5 bg-stone-50 rounded-xl border border-stone-200">
            <div className="pr-4 space-y-1">
              <span className="font-bold text-stone-900 text-sm">Location Auto-Organize</span>
              <p className="text-stone-500 text-[11px]">
                Automatically sorts new camera photos into Home, Temple, Carshed, or Date folders without confirmation prompts.
              </p>
            </div>
            <input
              type="checkbox"
              checked={autoOrganizeLocation}
              onChange={(e) => setAutoOrganizeLocation(e.target.checked)}
              className="mt-1 w-5 h-5 text-[#D94F00] rounded focus:ring-amber-500 cursor-pointer"
            />
          </div>

          {/* Festival Auto Organize */}
          <div className="flex items-start justify-between p-3.5 bg-stone-50 rounded-xl border border-stone-200">
            <div className="pr-4 space-y-1">
              <span className="font-bold text-stone-900 text-sm">Dussehra Festival Auto-Organize</span>
              <p className="text-stone-500 text-[11px]">
                Sorts photos taken on festival dates into Goddess Alankaram folders.
              </p>
            </div>
            <input
              type="checkbox"
              checked={autoOrganizeFestival}
              onChange={(e) => setAutoOrganizeFestival(e.target.checked)}
              className="mt-1 w-5 h-5 text-[#D94F00] rounded focus:ring-amber-500 cursor-pointer"
            />
          </div>

          {/* Include Videos */}
          <div className="flex items-start justify-between p-3.5 bg-stone-50 rounded-xl border border-stone-200">
            <div className="pr-4 space-y-1">
              <span className="font-bold text-stone-900 text-sm">Include Camera Videos</span>
              <p className="text-stone-500 text-[11px]">
                By default, camera photos only are processed. Enable to also organize videos.
              </p>
            </div>
            <input
              type="checkbox"
              checked={includeVideos}
              onChange={(e) => setIncludeVideos(e.target.checked)}
              className="mt-1 w-5 h-5 text-[#D94F00] rounded focus:ring-amber-500 cursor-pointer"
            />
          </div>

          {/* Manage Locations Shortcut */}
          <button
            onClick={() => {
              onClose();
              onOpenManageLocations();
            }}
            className="w-full py-2.5 px-3 bg-amber-50 hover:bg-amber-100 text-amber-900 font-bold rounded-xl border border-amber-300 text-center transition-colors"
          >
            Manage Registered GPS Locations &rarr;
          </button>

          {/* Storage Information */}
          <div className="p-3 bg-[#FFF9F2] rounded-xl border border-amber-200 text-stone-600 text-[11px] space-y-1">
            <div className="flex items-center space-x-1 font-bold text-red-900">
              <HardDrive className="w-3.5 h-3.5" />
              <span>Public Storage Targets:</span>
            </div>
            <p className="font-mono text-stone-800">1. Pictures/Location_Organized/</p>
            <p className="font-mono text-stone-800">2. Pictures/Dussehra_2026/</p>
            <p className="text-stone-500 pt-1">
              • Compatible with Android 10, 11, 12, 13, 14, and 15 devices.<br/>
              • Never moves or deletes original camera photos.
            </p>
          </div>

        </div>

        <div className="pt-2 text-right">
          <button
            onClick={onClose}
            className="px-4 py-2 bg-stone-900 text-white rounded-xl text-xs font-bold hover:bg-stone-800"
          >
            Done
          </button>
        </div>

      </div>
    </div>
  );
};
