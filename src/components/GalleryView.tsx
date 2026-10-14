import React, { useState } from 'react';
import { OrganizedPhoto } from '../types';
import { Folder, MapPin, Calendar, Image as ImageIcon, X, ExternalLink } from 'lucide-react';

interface GalleryViewProps {
  photos: OrganizedPhoto[];
  initialFolder?: string | null;
}

export const GalleryView: React.FC<GalleryViewProps> = ({
  photos,
  initialFolder = null,
}) => {
  const [selectedFolder, setSelectedFolder] = useState<string | null>(initialFolder);
  const [previewPhoto, setPreviewPhoto] = useState<OrganizedPhoto | null>(null);

  const folders = Array.from(new Set(photos.map((p) => p.destinationFolder)));

  const filteredPhotos = selectedFolder
    ? photos.filter((p) => p.destinationFolder === selectedFolder)
    : photos;

  return (
    <div className="space-y-6">
      
      {/* Header & Filter Chips */}
      <div className="bg-white rounded-2xl border border-stone-200 p-5 shadow-sm space-y-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <ImageIcon className="w-5 h-5 text-[#D94F00]" />
            <h2 className="text-lg font-bold text-stone-900 font-serif-temple">
              Organized Photo Gallery
            </h2>
          </div>
          <span className="text-xs text-stone-500 font-mono">
            Total Photos: {photos.length}
          </span>
        </div>

        {/* Chips */}
        <div className="flex flex-wrap gap-2 pt-2 border-t border-stone-100">
          <button
            onClick={() => setSelectedFolder(null)}
            className={`px-3 py-1.5 rounded-full text-xs font-semibold transition-all ${
              selectedFolder === null
                ? 'bg-amber-500 text-stone-950 font-bold shadow-sm'
                : 'bg-stone-100 text-stone-700 hover:bg-stone-200'
            }`}
          >
            All Folders ({photos.length})
          </button>

          {folders.map((folder) => {
            const isLocation =
              folder === 'Home' ||
              folder === 'Yeendda_Durga_Devi_Temple' ||
              folder === 'Carshed_Bobleam' ||
              /^\d{4}-\d{2}-\d{2}$/.test(folder);

            const count = photos.filter((p) => p.destinationFolder === folder).length;

            return (
              <button
                key={folder}
                onClick={() => setSelectedFolder(folder)}
                className={`px-3 py-1.5 rounded-full text-xs font-medium transition-all flex items-center space-x-1.5 ${
                  selectedFolder === folder
                    ? 'bg-[#D94F00] text-white font-bold shadow-sm'
                    : 'bg-stone-100 text-stone-700 hover:bg-stone-200'
                }`}
              >
                {isLocation ? (
                  <MapPin className="w-3 h-3 text-amber-500" />
                ) : (
                  <span className="text-xs">🌸</span>
                )}
                <span>{folder}</span>
                <span className="text-[10px] opacity-75">({count})</span>
              </button>
            );
          })}
        </div>
      </div>

      {/* Grid or Empty */}
      {filteredPhotos.length === 0 ? (
        <div className="bg-white rounded-2xl border border-stone-200 p-12 text-center space-y-2">
          <div className="w-16 h-16 rounded-full bg-amber-100 text-[#D94F00] flex items-center justify-center mx-auto text-2xl">
            📷
          </div>
          <h3 className="text-base font-bold text-stone-800">No photos in this folder yet</h3>
          <p className="text-xs text-stone-500 max-w-sm mx-auto">
            Use the Location Organizer or Festival Scanner to process camera photos and copy them into this folder.
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-3.5">
          {filteredPhotos.map((photo) => (
            <div
              key={photo.id}
              onClick={() => setPreviewPhoto(photo)}
              className="bg-white rounded-xl border border-stone-200 overflow-hidden shadow-xs hover:shadow-md cursor-pointer group transition-all"
            >
              <div className="aspect-square bg-stone-100 relative overflow-hidden">
                <img
                  src={photo.previewUrl}
                  alt={photo.fileName}
                  className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                />
                <div className="absolute inset-0 bg-gradient-to-t from-black/70 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity p-2 flex flex-col justify-end text-white text-[10px]">
                  <span className="font-mono truncate">{photo.fileName}</span>
                  <span className="text-amber-300">{photo.destinationFolder}</span>
                </div>
              </div>
              <div className="p-2 text-[11px]">
                <p className="font-mono text-stone-900 truncate font-semibold" title={photo.fileName}>
                  {photo.fileName}
                </p>
                <div className="flex items-center justify-between text-stone-500 text-[10px] mt-0.5">
                  <span className="truncate">{photo.destinationFolder}</span>
                  <span>{photo.captureDateFormatted}</span>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Fullscreen Preview Modal */}
      {previewPhoto && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-stone-900 text-white rounded-2xl max-w-2xl w-full overflow-hidden shadow-2xl border border-stone-700">
            <div className="p-4 border-b border-stone-800 flex items-center justify-between">
              <div className="truncate pr-4">
                <h3 className="font-mono text-sm font-bold truncate">
                  {previewPhoto.fileName}
                </h3>
                <p className="text-xs text-amber-400">
                  Target: Pictures/{previewPhoto.rootFolder}/{previewPhoto.destinationFolder}/
                </p>
              </div>
              <button
                onClick={() => setPreviewPhoto(null)}
                className="p-1 rounded-lg hover:bg-stone-800 text-stone-400 hover:text-white"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="max-h-[60vh] bg-black flex items-center justify-center p-2">
              <img
                src={previewPhoto.previewUrl}
                alt={previewPhoto.fileName}
                className="max-h-[58vh] max-w-full object-contain rounded"
              />
            </div>

            <div className="p-4 bg-stone-950 space-y-2 text-xs border-t border-stone-800">
              <div className="grid grid-cols-2 gap-2 text-stone-300">
                <div>
                  <span className="text-stone-500">Capture Date: </span>
                  <strong className="text-white">{previewPhoto.captureDateFormatted}</strong>
                </div>
                <div>
                  <span className="text-stone-500">Status: </span>
                  <span className="text-emerald-400">{previewPhoto.statusMessage}</span>
                </div>
                {previewPhoto.latitude && previewPhoto.longitude && (
                  <div className="col-span-2 text-amber-200 font-mono">
                    GPS Coordinates: {previewPhoto.latitude.toFixed(6)}° N, {previewPhoto.longitude.toFixed(6)}° E
                    {previewPhoto.distanceMeters && previewPhoto.distanceMeters >= 0 && (
                      <span className="ml-2 text-stone-400">(Distance: {previewPhoto.distanceMeters}m)</span>
                    )}
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      )}

    </div>
  );
};
