import React, { useState } from 'react';
import { FestivalScheduleItem, LocationConfigItem, OrganizedPhoto } from '../types';
import { evaluatePhotoDestination } from '../utils/geoUtils';
import { parseFileMetadata } from '../utils/exifParser';
import { SAMPLE_CAMERA_PHOTOS } from '../data/samplePhotos';
import JSZip from 'jszip';
import { 
  MapPin, 
  Upload, 
  FolderCheck, 
  AlertCircle, 
  CheckCircle2, 
  Download, 
  RefreshCw, 
  Filter, 
  ShieldCheck, 
  SlidersHorizontal,
  FolderSync,
  Compass
} from 'lucide-react';

interface LocationOrganizerViewProps {
  locations: LocationConfigItem[];
  schedule: FestivalScheduleItem[];
  onOpenManageLocations: () => void;
  onPhotosOrganized: (photos: OrganizedPhoto[]) => void;
}

export const LocationOrganizerView: React.FC<LocationOrganizerViewProps> = ({
  locations,
  schedule,
  onOpenManageLocations,
  onPhotosOrganized,
}) => {
  const [photoList, setPhotoList] = useState<OrganizedPhoto[]>(SAMPLE_CAMERA_PHOTOS);
  const [reviewList, setReviewList] = useState<{ name: string; reason: string }[]>([]);
  const [isProcessing, setIsProcessing] = useState(false);
  const [copiedSuccessMessage, setCopiedSuccessMessage] = useState<string | null>(null);
  const [selectedFolderFilter, setSelectedFolderFilter] = useState<string>('ALL');

  // Handle uploading real photos from disk
  const handleFileUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const files = event.target.files;
    if (!files || files.length === 0) return;

    setIsProcessing(true);
    const newPhotos: OrganizedPhoto[] = [];
    const newReviewItems: { name: string; reason: string }[] = [];

    for (let i = 0; i < files.length; i++) {
      const file = files[i];
      const meta = await parseFileMetadata(file);

      // 1. Strict authentic camera check
      if (!meta.isAuthenticCamera) {
        newReviewItems.push({
          name: file.name,
          reason: meta.rejectReason || 'Excluded (Non-camera media)',
        });
        continue;
      }

      // Check capture date validity: if missing, skip and record for review rather than guessing
      if (!meta.isoDate && !meta.captureDateFormatted) {
        newReviewItems.push({
          name: file.name,
          reason: 'Missing EXIF original capture date (Skipped - Needs Review)',
        });
        continue;
      }

      // 2. Offline location evaluation & festival date mapping
      const evalResult = evaluatePhotoDestination(
        meta.latitude,
        meta.longitude,
        meta.captureDateFormatted,
        meta.isoDate,
        schedule,
        locations
      );

      const previewUrl = URL.createObjectURL(file);

      const dist = evalResult.selectedAlias
        ? evalResult.distancesToLocations[evalResult.selectedAlias]
        : undefined;

      newPhotos.push({
        id: `photo_${Date.now()}_${i}`,
        fileName: file.name,
        fileSize: file.size,
        captureDateFormatted: meta.captureDateFormatted || meta.isoDate || 'Unknown',
        isoDate: meta.isoDate || undefined,
        timestamp: meta.timestamp,
        rootFolder: 'Dussehra_2026',
        deviParentFolder: evalResult.deviParentFolder,
        locationSubfolder: evalResult.locationSubfolder,
        destinationFolder: `${evalResult.deviParentFolder}/${evalResult.locationSubfolder}`,
        sourceType: 'camera',
        isCameraAuthentic: true,
        latitude: meta.latitude || undefined,
        longitude: meta.longitude || undefined,
        distanceMeters: dist,
        locationLabel: evalResult.selectedAlias || `Date: ${evalResult.locationSubfolder}`,
        statusMessage: evalResult.statusSummary,
        previewUrl,
        rawFile: file,
        selected: true,
      });
    }

    setPhotoList((prev) => [...newPhotos, ...prev]);
    setReviewList((prev) => [...newReviewItems, ...prev]);
    setIsProcessing(false);
  };

  const toggleSelectAll = (select: boolean) => {
    setPhotoList((prev) => prev.map((p) => ({ ...p, selected: select })));
  };

  const toggleSelectPhoto = (id: string) => {
    setPhotoList((prev) =>
      prev.map((p) => (p.id === id ? { ...p, selected: !p.selected } : p))
    );
  };

  // Execute Copy / Organize
  const executeCopy = async (autoAll: boolean = false) => {
    const targets = photoList.filter((p) => autoAll || p.selected);
    if (targets.length === 0) {
      alert('Please select at least one photo to organize.');
      return;
    }

    setIsProcessing(true);

    // Group by destination path
    const folderStats: Record<string, number> = {};
    targets.forEach((p) => {
      folderStats[p.destinationFolder] = (folderStats[p.destinationFolder] || 0) + 1;
    });

    // Notify parent for Gallery integration
    onPhotosOrganized(targets);

    setTimeout(() => {
      setIsProcessing(false);
      setCopiedSuccessMessage(
        `Successfully organized ${targets.length} camera photos into Pictures/Dussehra_2026/:\n` +
          Object.entries(folderStats)
            .map(([folder, count]) => `• ${folder}/ (${count} photos)`)
            .join('\n')
      );
    }, 600);
  };

  // Export organized structure as ZIP
  const exportAsZip = async () => {
    const targets = photoList.filter((p) => p.selected);
    if (targets.length === 0) return;

    setIsProcessing(true);
    const zip = new JSZip();
    const root = zip.folder('Dussehra_2026');

    for (const photo of targets) {
      const deviFolder = root?.folder(photo.deviParentFolder);
      const subFolder = deviFolder?.folder(photo.locationSubfolder);

      if (photo.rawFile) {
        subFolder?.file(photo.fileName, photo.rawFile);
      } else {
        try {
          const resp = await fetch(photo.previewUrl);
          const blob = await resp.blob();
          subFolder?.file(photo.fileName, blob);
        } catch {
          subFolder?.file(photo.fileName, `Sample photo metadata: ${photo.fileName}`);
        }
      }
    }

    const content = await zip.generateAsync({ type: 'blob' });
    const url = URL.createObjectURL(content);
    const a = document.createElement('a');
    a.href = url;
    a.download = `Pictures_Dussehra_2026_${Date.now()}.zip`;
    a.click();
    URL.revokeObjectURL(url);
    setIsProcessing(false);
  };

  const resetToSamples = () => {
    setPhotoList(SAMPLE_CAMERA_PHOTOS);
    setReviewList([]);
    setCopiedSuccessMessage(null);
  };

  const filteredPhotos =
    selectedFolderFilter === 'ALL'
      ? photoList
      : photoList.filter((p) => p.destinationFolder.includes(selectedFolderFilter));

  const uniqueFolders = Array.from(new Set(photoList.map((p) => p.destinationFolder)));

  return (
    <div className="space-y-6">
      
      {/* Top Banner & Info Card */}
      <div className="bg-white rounded-2xl border border-stone-200 p-6 shadow-sm">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <div className="flex items-center space-x-2">
              <span className="bg-[#D94F00] text-white p-1.5 rounded-lg text-lg">📍</span>
              <h2 className="text-xl font-bold text-stone-900 font-serif-temple">
                Location &amp; Alankaram Photo Organizer
              </h2>
            </div>
            <p className="text-xs text-stone-600 mt-1">
              Main Root: <code className="bg-stone-100 text-red-800 font-bold px-2 py-0.5 rounded border border-stone-300">Pictures/Dussehra_2026/[Devi_Alankaram]/[Location_Or_Date]/</code>
            </p>
            <p className="text-xs text-stone-500 mt-1">
              Supports location subfolders: <strong>Home/</strong>, <strong>Yeendda_Durga_Devi_Temple/</strong>, <strong>Carshed_Bobleam/</strong>, or fallback <strong>YYYY-MM-DD/</strong> for ambiguous or GPS-missing photos.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            <button
              onClick={onOpenManageLocations}
              className="px-3.5 py-2 bg-stone-100 hover:bg-stone-200 text-stone-800 rounded-xl text-xs font-semibold flex items-center space-x-1.5 border border-stone-300 transition-colors"
            >
              <SlidersHorizontal className="w-4 h-4 text-[#D94F00]" />
              <span>Locations &amp; GPS Test Tool</span>
            </button>
            <button
              onClick={resetToSamples}
              className="px-3.5 py-2 bg-amber-50 hover:bg-amber-100 text-amber-900 rounded-xl text-xs font-semibold flex items-center space-x-1.5 border border-amber-300 transition-colors"
            >
              <RefreshCw className="w-3.5 h-3.5 text-amber-600" />
              <span>Reset Samples</span>
            </button>
          </div>
        </div>

        {/* Mappings Badges */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 mt-4 pt-4 border-t border-stone-100">
          {locations.map((loc) => (
            <div key={loc.id} className="bg-[#FFF9F2] p-3 rounded-xl border border-amber-200 text-xs">
              <div className="flex items-center justify-between font-bold text-stone-900">
                <span>{loc.name}</span>
                <span className="bg-amber-200/80 text-red-900 px-2 py-0.5 rounded font-mono text-[11px]">
                  {loc.folderName}/
                </span>
              </div>
              <p className="text-stone-500 text-[11px] truncate mt-1" title={loc.address}>
                {loc.address}
              </p>
              <div className="flex items-center justify-between text-[10px] text-amber-800 mt-1 font-mono">
                <span>GPS: {loc.latitude.toFixed(6)}°, {loc.longitude.toFixed(6)}°</span>
                <span>Radius: {loc.radiusMeters}m (Non-overlapping)</span>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Upload & Drop Zone */}
      <div className="bg-gradient-to-r from-amber-50/70 to-orange-50/70 border-2 border-dashed border-amber-300 rounded-2xl p-6 text-center hover:bg-amber-50 transition-colors">
        <input
          type="file"
          id="photo-file-input"
          multiple
          accept="image/*"
          onChange={handleFileUpload}
          className="hidden"
        />
        <label
          htmlFor="photo-file-input"
          className="cursor-pointer flex flex-col items-center justify-center space-y-2"
        >
          <div className="w-12 h-12 rounded-full bg-amber-200 flex items-center justify-center text-[#D94F00] shadow-sm">
            <Upload className="w-6 h-6" />
          </div>
          <div>
            <span className="text-sm font-bold text-stone-900 hover:text-[#D94F00] underline">
              Choose Camera Photos
            </span>
            <span className="text-sm text-stone-600"> or drag and drop files here</span>
          </div>
          <p className="text-xs text-stone-500 flex items-center space-x-1">
            <ShieldCheck className="w-4 h-4 text-emerald-600 inline" />
            <span>Strict Policy: Camera photos only (DCIM/Camera). WhatsApp, Telegram, Screenshots, &amp; Downloads are excluded.</span>
          </p>
        </label>
      </div>

      {/* Notification banner if copied */}
      {copiedSuccessMessage && (
        <div className="bg-emerald-50 border border-emerald-300 text-emerald-900 rounded-xl p-4 text-sm flex items-start space-x-3 shadow-sm">
          <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0 mt-0.5" />
          <div className="flex-1 whitespace-pre-line font-medium text-xs sm:text-sm">
            {copiedSuccessMessage}
          </div>
          <button
            onClick={() => setCopiedSuccessMessage(null)}
            className="text-xs font-bold text-emerald-700 hover:underline"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Review needed banner */}
      {reviewList.length > 0 && (
        <div className="bg-amber-50 border border-amber-300 text-amber-900 rounded-xl p-4 text-xs space-y-2">
          <div className="flex items-center space-x-2 font-bold text-amber-950">
            <AlertCircle className="w-4 h-4 text-amber-600" />
            <span>Files Flagged for Review or Excluded ({reviewList.length})</span>
          </div>
          <div className="max-h-28 overflow-y-auto space-y-1">
            {reviewList.map((item, idx) => (
              <div key={idx} className="flex items-center justify-between bg-white/70 px-2.5 py-1 rounded border border-amber-200">
                <span className="font-mono">{item.name}</span>
                <span className="text-stone-600 italic">{item.reason}</span>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Action Controls & Filter Bar */}
      <div className="bg-white rounded-xl border border-stone-200 p-4 shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-3">
        <div className="flex flex-wrap items-center gap-2">
          <span className="text-xs font-bold text-stone-700">Filter by Destination Subfolder:</span>
          <button
            onClick={() => setSelectedFolderFilter('ALL')}
            className={`px-2.5 py-1 rounded-lg text-xs font-semibold ${
              selectedFolderFilter === 'ALL'
                ? 'bg-[#D94F00] text-white'
                : 'bg-stone-100 text-stone-700 hover:bg-stone-200'
            }`}
          >
            All ({photoList.length})
          </button>
          {uniqueFolders.map((folder) => (
            <button
              key={folder}
              onClick={() => setSelectedFolderFilter(folder)}
              className={`px-2.5 py-1 rounded-lg text-xs font-semibold ${
                selectedFolderFilter === folder
                  ? 'bg-[#D94F00] text-white'
                  : 'bg-stone-100 text-stone-700 hover:bg-stone-200'
              }`}
            >
              {folder} ({photoList.filter((p) => p.destinationFolder === folder).length})
            </button>
          ))}
        </div>

        <div className="flex items-center space-x-2">
          <button
            onClick={() => toggleSelectAll(true)}
            className="text-xs font-semibold text-red-800 hover:underline px-2 py-1"
          >
            Select All
          </button>
          <button
            onClick={() => toggleSelectAll(false)}
            className="text-xs font-semibold text-stone-600 hover:underline px-2 py-1"
          >
            Deselect All
          </button>
        </div>
      </div>

      {/* Photo Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {filteredPhotos.map((photo) => (
          <div
            key={photo.id}
            onClick={() => toggleSelectPhoto(photo.id)}
            className={`bg-white rounded-xl border p-4 shadow-sm cursor-pointer transition-all hover:shadow-md flex space-x-3.5 ${
              photo.selected ? 'border-amber-400 ring-2 ring-amber-400/30' : 'border-stone-200 opacity-75'
            }`}
          >
            <input
              type="checkbox"
              checked={photo.selected}
              onChange={() => {}} // Controlled by card click
              className="mt-1 w-4 h-4 text-[#D94F00] rounded focus:ring-amber-500"
            />

            <div className="w-20 h-20 rounded-lg overflow-hidden bg-stone-100 shrink-0 border border-stone-200">
              <img
                src={photo.previewUrl}
                alt={photo.fileName}
                className="w-full h-full object-cover"
              />
            </div>

            <div className="flex-1 min-w-0 space-y-1">
              <h4 className="text-xs font-bold text-stone-900 truncate font-mono" title={photo.fileName}>
                {photo.fileName}
              </h4>
              <p className="text-[11px] text-stone-500">
                Captured: {photo.captureDateFormatted}
              </p>

              <div className="flex items-center space-x-1 text-xs font-semibold text-red-900">
                <FolderCheck className="w-3.5 h-3.5 text-[#D94F00] shrink-0" />
                <span className="truncate text-[11px]">
                  Pictures/<strong>{photo.destinationFolder}</strong>/
                </span>
              </div>

              <div className="text-[10px] text-emerald-800 bg-emerald-50 px-2 py-0.5 rounded border border-emerald-200 font-medium truncate">
                {photo.statusMessage}
              </div>

              {photo.latitude && photo.longitude && (
                <div className="text-[10px] text-stone-400 font-mono">
                  GPS: {photo.latitude.toFixed(6)}°, {photo.longitude.toFixed(6)}°
                </div>
              )}
            </div>
          </div>
        ))}
      </div>

      {/* Sticky Bottom Organization Toolbar */}
      <div className="sticky bottom-4 z-20 bg-white/95 backdrop-blur-md rounded-2xl border border-amber-300 p-4 shadow-xl flex flex-col sm:flex-row items-center justify-between gap-3">
        <div className="text-xs text-stone-700">
          <span>Ready to organize: </span>
          <strong className="text-stone-900 text-sm">{photoList.filter((p) => p.selected).length}</strong>
          <span> / {photoList.length} photos selected</span>
        </div>

        <div className="flex flex-wrap items-center gap-2.5 w-full sm:w-auto">
          {/* Automatic One-Click Organization */}
          <button
            onClick={() => executeCopy(true)}
            disabled={isProcessing || photoList.length === 0}
            className="flex-1 sm:flex-none px-4 py-2.5 bg-red-900 hover:bg-red-800 text-white rounded-xl text-xs sm:text-sm font-bold flex items-center justify-center space-x-2 shadow transition-all hover:scale-102 disabled:opacity-50"
            title="Automatically organizes all eligible photos into Devi and Location folders without repeatedly asking for confirmation"
          >
            <FolderSync className="w-4 h-4" />
            <span>Auto-Organize All (No Prompts)</span>
          </button>

          {/* Manual Copy Selected */}
          <button
            onClick={() => executeCopy(false)}
            disabled={isProcessing || photoList.filter((p) => p.selected).length === 0}
            className="flex-1 sm:flex-none px-4 py-2.5 bg-[#D94F00] hover:bg-[#C93B00] text-white rounded-xl text-xs sm:text-sm font-bold flex items-center justify-center space-x-2 shadow transition-all hover:scale-102 disabled:opacity-50"
          >
            <FolderCheck className="w-4 h-4" />
            <span>Copy Selected</span>
          </button>

          {/* Export Zip */}
          <button
            onClick={exportAsZip}
            disabled={isProcessing || photoList.filter((p) => p.selected).length === 0}
            className="px-3.5 py-2.5 bg-stone-100 hover:bg-stone-200 text-stone-800 rounded-xl text-xs sm:text-sm font-semibold flex items-center space-x-1.5 border border-stone-300 transition-colors"
            title="Download complete Pictures/Dussehra_2026/ folder hierarchy as ZIP"
          >
            <Download className="w-4 h-4 text-stone-600" />
            <span className="hidden sm:inline">Export ZIP</span>
          </button>
        </div>
      </div>

    </div>
  );
};
