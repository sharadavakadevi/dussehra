import React from 'react';
import { X, Download, Smartphone, CheckCircle, Package, FileCode } from 'lucide-react';

interface ApkDownloadModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const ApkDownloadModal: React.FC<ApkDownloadModalProps> = ({
  isOpen,
  onClose,
}) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-2xl space-y-4">
        
        <div className="flex items-center justify-between border-b border-stone-200 pb-3">
          <div className="flex items-center space-x-2.5">
            <div className="p-2 bg-emerald-100 rounded-xl text-emerald-700">
              <Smartphone className="w-6 h-6" />
            </div>
            <div>
              <h3 className="text-lg font-bold text-stone-900 font-serif-temple">
                Download Android App (APK)
              </h3>
              <p className="text-xs text-stone-500">
                Directly install on your Android device (Samsung, Vivo, etc.)
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-lg hover:bg-stone-100 text-stone-400 hover:text-stone-700"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="space-y-3">
          
          {/* APK Card */}
          <div className="border-2 border-emerald-300 rounded-2xl p-4 bg-emerald-50/60 space-y-3">
            <div className="flex items-start justify-between">
              <div>
                <span className="text-[10px] uppercase font-bold tracking-wider text-emerald-800 bg-emerald-200/80 px-2 py-0.5 rounded-full">
                  Compiled Debug APK (v1.0.0)
                </span>
                <h4 className="text-sm font-bold text-stone-900 mt-1">
                  DussehraPhotoOrganizer.apk
                </h4>
                <p className="text-xs text-stone-600 mt-0.5">
                  Full Android application with offline GPS matching, WorkManager auto-organizer, MediaStore observer, and Telugu UI.
                </p>
              </div>
            </div>

            <ul className="text-xs text-stone-600 space-y-1 pt-1">
              <li className="flex items-center space-x-1.5">
                <CheckCircle className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                <span>Compatible with Android 8 through Android 15 (minSdk 26, targetSdk 34)</span>
              </li>
              <li className="flex items-center space-x-1.5">
                <CheckCircle className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                <span>Includes Home, Yeendda Durga Devi Temple, &amp; Carshed Bobleam offline GPS</span>
              </li>
              <li className="flex items-center space-x-1.5">
                <CheckCircle className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                <span>Safe Scoped Storage: Never deletes or modifies original camera files</span>
              </li>
            </ul>

            <a
              href="/DussehraPhotoOrganizer.apk"
              download="DussehraPhotoOrganizer.apk"
              className="w-full py-2.5 px-4 bg-emerald-700 hover:bg-emerald-800 text-white font-bold rounded-xl text-xs flex items-center justify-center space-x-2 shadow transition-all hover:scale-102"
            >
              <Download className="w-4 h-4" />
              <span>Download DussehraPhotoOrganizer.apk (6.5 MB)</span>
            </a>
          </div>

          {/* Project Source Code Zip */}
          <div className="border border-stone-200 rounded-2xl p-4 bg-stone-50 space-y-2.5">
            <div className="flex items-center space-x-2">
              <Package className="w-5 h-5 text-[#D94F00]" />
              <div>
                <h4 className="text-xs font-bold text-stone-900">
                  Full Updated Android Project ZIP
                </h4>
                <p className="text-[11px] text-stone-500">
                  Complete Gradle source code ready for Android Studio or GitHub import.
                </p>
              </div>
            </div>

            <a
              href="/dussehra-photo-organizer-updated.zip"
              download="dussehra-photo-organizer-updated.zip"
              className="w-full py-2 px-3 bg-white hover:bg-stone-100 text-stone-800 font-semibold rounded-xl text-xs flex items-center justify-center space-x-2 border border-stone-300 transition-colors"
            >
              <FileCode className="w-4 h-4 text-stone-500" />
              <span>Download Project Source ZIP (114 KB)</span>
            </a>

            <a
              href="/fix-duplicates.patch"
              download="fix-duplicates.patch"
              className="w-full py-2 px-3 bg-white hover:bg-stone-100 text-stone-700 font-medium rounded-xl text-xs flex items-center justify-center space-x-2 border border-stone-200 transition-colors"
            >
              <FileCode className="w-4 h-4 text-emerald-600" />
              <span>Download Git Patch (fix-duplicates.patch)</span>
            </a>
          </div>

        </div>

        <div className="pt-2 text-right">
          <button
            onClick={onClose}
            className="px-4 py-2 bg-stone-200 hover:bg-stone-300 text-stone-800 rounded-xl text-xs font-bold transition-colors"
          >
            Close
          </button>
        </div>

      </div>
    </div>
  );
};
