import React, { useState } from 'react';
import { LocationConfigItem } from '../types';
import { DEFAULT_LOCATIONS } from '../data/defaultLocations';
import { DEFAULT_FESTIVAL_SCHEDULE } from '../data/defaultSchedule';
import { evaluatePhotoDestination, PhotoDestinationEvaluation } from '../utils/geoUtils';
import { MapPin, X, RotateCcw, Save, ShieldCheck, Compass, CheckCircle2, AlertTriangle, HelpCircle } from 'lucide-react';

interface ManageLocationsModalProps {
  isOpen: boolean;
  onClose: () => void;
  locations: LocationConfigItem[];
  onSaveLocations: (locations: LocationConfigItem[]) => void;
}

export const ManageLocationsModal: React.FC<ManageLocationsModalProps> = ({
  isOpen,
  onClose,
  locations,
  onSaveLocations,
}) => {
  const [localList, setLocalList] = useState<LocationConfigItem[]>(locations);
  const [editingId, setEditingId] = useState<string | null>(null);

  // Test Tool State
  const [showTestPanel, setShowTestPanel] = useState(false);
  const [testLat, setTestLat] = useState<string>('17.782889');
  const [testLon, setTestLon] = useState<string>('83.361833');
  const [testDate, setTestDate] = useState<string>('12-10-2026');
  const [testIsoDate, setTestIsoDate] = useState<string>('2026-10-12');
  const [testResult, setTestResult] = useState<PhotoDestinationEvaluation | null>(null);

  if (!isOpen) return null;

  const handleUpdate = (id: string, updates: Partial<LocationConfigItem>) => {
    setLocalList((prev) =>
      prev.map((item) => (item.id === id ? { ...item, ...updates } : item))
    );
  };

  const handleSave = () => {
    onSaveLocations(localList);
    onClose();
  };

  const handleResetDefaults = () => {
    if (confirm('Reset all 3 locations to exact verified coordinates and 40m radius?')) {
      setLocalList(DEFAULT_LOCATIONS);
      onSaveLocations(DEFAULT_LOCATIONS);
    }
  };

  const runTestEvaluation = (lat: number | null, lon: number | null, date: string, iso: string) => {
    const res = evaluatePhotoDestination(lat, lon, date, iso, DEFAULT_FESTIVAL_SCHEDULE, localList);
    setTestResult(res);
  };

  const applyTestPreset = (type: 'home' | 'temple' | 'carshed' | 'beach' | 'midpoint' | 'no_gps') => {
    if (type === 'home') {
      setTestLat('17.782889');
      setTestLon('83.361833');
      setTestDate('12-10-2026');
      setTestIsoDate('2026-10-12');
      runTestEvaluation(17.782889, 83.361833, '12-10-2026', '2026-10-12');
    } else if (type === 'temple') {
      setTestLat('17.781944');
      setTestLon('83.361917');
      setTestDate('13-10-2026');
      setTestIsoDate('2026-10-13');
      runTestEvaluation(17.781944, 83.361917, '13-10-2026', '2026-10-13');
    } else if (type === 'carshed') {
      setTestLat('17.802778');
      setTestLon('83.352667');
      setTestDate('14-10-2026');
      setTestIsoDate('2026-10-14');
      runTestEvaluation(17.802778, 83.352667, '14-10-2026', '2026-10-14');
    } else if (type === 'beach') {
      setTestLat('17.712500');
      setTestLon('83.321000');
      setTestDate('15-10-2026');
      setTestIsoDate('2026-10-15');
      runTestEvaluation(17.7125, 83.321, '15-10-2026', '2026-10-15');
    } else if (type === 'midpoint') {
      // Exactly midpoint between Home and Temple (~53m from each)
      setTestLat('17.782416');
      setTestLon('83.361875');
      setTestDate('16-10-2026');
      setTestIsoDate('2026-10-16');
      runTestEvaluation(17.782416, 83.361875, '16-10-2026', '2026-10-16');
    } else if (type === 'no_gps') {
      setTestLat('');
      setTestLon('');
      setTestDate('17-10-2026');
      setTestIsoDate('2026-10-17');
      runTestEvaluation(null, null, '17-10-2026', '2026-10-17');
    }
  };

  const handleCustomTestSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const lat = testLat.trim() ? parseFloat(testLat.trim()) : null;
    const lon = testLon.trim() ? parseFloat(testLon.trim()) : null;
    runTestEvaluation(lat, lon, testDate.trim(), testIsoDate.trim());
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl max-w-3xl w-full p-6 shadow-2xl space-y-4 max-h-[92vh] overflow-y-auto">
        
        {/* Header */}
        <div className="flex items-center justify-between border-b border-stone-200 pb-3">
          <div className="flex items-center space-x-2">
            <div className="p-2 bg-amber-100 rounded-lg text-[#D94F00]">
              <MapPin className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-lg font-bold text-stone-900 font-serif-temple">
                Manage Registered GPS Locations &amp; Radii
              </h3>
              <p className="text-xs text-stone-500">
                Exact user-verified coordinates &bull; Non-overlapping 40m radii &bull; Offline EXIF matching
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

        {/* Action Toggles */}
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <button
              onClick={() => setShowTestPanel(!showTestPanel)}
              className={`px-3 py-1.5 rounded-lg text-xs font-bold flex items-center space-x-1.5 transition-colors border ${
                showTestPanel
                  ? 'bg-[#D94F00] text-white border-[#D94F00]'
                  : 'bg-amber-50 text-[#D94F00] border-amber-300 hover:bg-amber-100'
              }`}
            >
              <Compass className="w-4 h-4" />
              <span>{showTestPanel ? 'Hide Test Tool' : 'Run GPS Matching Test'}</span>
            </button>
          </div>

          <div className="text-xs text-stone-500">
            Home &amp; Temple separation: <strong>~106m</strong> (Safe 26m buffer at 40m radius)
          </div>
        </div>

        {/* Interactive GPS Test Tool Panel */}
        {showTestPanel && (
          <div className="bg-amber-50/70 border-2 border-amber-300 rounded-xl p-4 space-y-3">
            <div className="flex items-center justify-between">
              <h4 className="text-xs font-bold text-stone-900 flex items-center space-x-1.5">
                <Compass className="w-4 h-4 text-[#D94F00]" />
                <span>Test Function: Photo Coordinates, Distances &amp; Destination Path</span>
              </h4>
              <span className="text-[10px] text-stone-500 font-mono">Offline Evaluator</span>
            </div>

            {/* Test Presets */}
            <div className="flex flex-wrap gap-1.5 pt-1">
              <button
                type="button"
                onClick={() => applyTestPreset('home')}
                className="px-2.5 py-1 bg-white hover:bg-amber-100 text-stone-800 rounded-md text-[11px] font-semibold border border-amber-300"
              >
                📍 Home Pin
              </button>
              <button
                type="button"
                onClick={() => applyTestPreset('temple')}
                className="px-2.5 py-1 bg-white hover:bg-amber-100 text-stone-800 rounded-md text-[11px] font-semibold border border-amber-300"
              >
                📍 Temple Pin
              </button>
              <button
                type="button"
                onClick={() => applyTestPreset('carshed')}
                className="px-2.5 py-1 bg-white hover:bg-amber-100 text-stone-800 rounded-md text-[11px] font-semibold border border-amber-300"
              >
                📍 Carshed Pin
              </button>
              <button
                type="button"
                onClick={() => applyTestPreset('beach')}
                className="px-2.5 py-1 bg-white hover:bg-stone-100 text-stone-700 rounded-md text-[11px] font-semibold border border-stone-300"
              >
                🌊 RK Beach (Outside)
              </button>
              <button
                type="button"
                onClick={() => applyTestPreset('midpoint')}
                className="px-2.5 py-1 bg-white hover:bg-amber-100 text-amber-900 rounded-md text-[11px] font-semibold border border-amber-300"
              >
                ⚠️ Midpoint (~53m from both)
              </button>
              <button
                type="button"
                onClick={() => applyTestPreset('no_gps')}
                className="px-2.5 py-1 bg-white hover:bg-stone-100 text-stone-700 rounded-md text-[11px] font-semibold border border-stone-300"
              >
                📅 No GPS in Photo
              </button>
            </div>

            {/* Custom Input Form */}
            <form onSubmit={handleCustomTestSubmit} className="grid grid-cols-1 sm:grid-cols-4 gap-2 pt-2">
              <div>
                <label className="text-[10px] font-bold text-stone-600 block">Latitude (°N)</label>
                <input
                  type="text"
                  placeholder="e.g. 17.782889"
                  value={testLat}
                  onChange={(e) => setTestLat(e.target.value)}
                  className="w-full mt-0.5 px-2 py-1 text-xs border rounded bg-white font-mono"
                />
              </div>
              <div>
                <label className="text-[10px] font-bold text-stone-600 block">Longitude (°E)</label>
                <input
                  type="text"
                  placeholder="e.g. 83.361833"
                  value={testLon}
                  onChange={(e) => setTestLon(e.target.value)}
                  className="w-full mt-0.5 px-2 py-1 text-xs border rounded bg-white font-mono"
                />
              </div>
              <div>
                <label className="text-[10px] font-bold text-stone-600 block">Capture Date (dd-MM-yyyy)</label>
                <input
                  type="text"
                  placeholder="12-10-2026"
                  value={testDate}
                  onChange={(e) => {
                    setTestDate(e.target.value);
                    if (e.target.value.match(/^\d{2}-\d{2}-\d{4}$/)) {
                      const [d, m, y] = e.target.value.split('-');
                      setTestIsoDate(`${y}-${m}-${d}`);
                    }
                  }}
                  className="w-full mt-0.5 px-2 py-1 text-xs border rounded bg-white font-mono"
                />
              </div>
              <div className="flex items-end">
                <button
                  type="submit"
                  className="w-full py-1.5 bg-[#D94F00] hover:bg-[#C93B00] text-white text-xs font-bold rounded shadow-xs"
                >
                  Evaluate
                </button>
              </div>
            </form>

            {/* Test Result Display */}
            {testResult && (
              <div className="bg-white rounded-lg p-3 border border-amber-200 text-xs space-y-2 mt-2">
                <div className="font-bold text-stone-900 border-b pb-1 flex items-center justify-between">
                  <span>GPS Matching Test Output</span>
                  <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                    testResult.matchType === 'SINGLE_MATCH'
                      ? 'bg-emerald-100 text-emerald-800'
                      : testResult.matchType === 'AMBIGUOUS_MULTIPLE_MATCH'
                      ? 'bg-amber-100 text-amber-900'
                      : 'bg-stone-100 text-stone-700'
                  }`}>
                    {testResult.matchType}
                  </span>
                </div>

                <div className="space-y-1 text-[11px]">
                  <div>
                    <strong>1. Photo EXIF Coordinates:</strong>{' '}
                    <span className="font-mono text-stone-700">
                      {testResult.photoLatitude !== null && testResult.photoLongitude !== null
                        ? `${testResult.photoLatitude.toFixed(6)}° N, ${testResult.photoLongitude.toFixed(6)}° E`
                        : '(No GPS metadata present in photo)'}
                    </span>
                  </div>

                  <div>
                    <strong>2. Distances to Registered Locations:</strong>
                    {Object.keys(testResult.distancesToLocations).length > 0 ? (
                      <div className="grid grid-cols-1 sm:grid-cols-3 gap-1.5 mt-1">
                        {localList.map((loc) => {
                          const dist = testResult.distancesToLocations[loc.folderName];
                          const inside = dist !== undefined && dist <= loc.radiusMeters;
                          return (
                            <div
                              key={loc.id}
                              className={`p-1.5 rounded border text-[10px] ${
                                inside
                                  ? 'bg-emerald-50 border-emerald-300 text-emerald-900 font-bold'
                                  : 'bg-stone-50 border-stone-200 text-stone-600'
                              }`}
                            >
                              <div>{loc.name}: <strong>{dist}m</strong></div>
                              <div>Radius: {loc.radiusMeters}m &bull; {inside ? 'MATCH ✓' : 'Outside ✗'}</div>
                            </div>
                          );
                        })}
                      </div>
                    ) : (
                      <span className="text-stone-500 italic ml-1">None (GPS missing)</span>
                    )}
                  </div>

                  <div className="pt-1">
                    <strong>3. Match Decision:</strong>{' '}
                    <span className="text-red-900 font-bold">
                      {testResult.selectedAlias
                        ? `Selected Alias: ${testResult.selectedAlias}`
                        : `Fallback to Date Folder: ${testResult.locationSubfolder} (${testResult.matchType})`}
                    </span>
                  </div>

                  <div>
                    <strong>4. Devi Parent Folder:</strong>{' '}
                    <span className="font-mono text-[#D94F00] font-bold">
                      {testResult.deviParentFolder}/
                    </span>{' '}
                    <span className="text-stone-500">(Mapped from festival date: {testDate})</span>
                  </div>

                  <div className="bg-stone-100 p-2 rounded border border-stone-300">
                    <strong>5. Final Destination Path:</strong>{' '}
                    <code className="text-red-900 font-bold font-mono">
                      Pictures/{testResult.destinationRelativePath}/
                    </code>
                  </div>
                </div>
              </div>
            )}
          </div>
        )}

        {/* Location Cards */}
        <div className="space-y-3">
          {localList.map((loc) => (
            <div
              key={loc.id}
              className="border border-stone-200 rounded-xl p-4 bg-stone-50/50 space-y-3"
            >
              <div className="flex items-center justify-between">
                <div>
                  <h4 className="text-sm font-bold text-stone-900">
                    {loc.name} <span className="font-telugu text-stone-500 font-normal">({loc.teluguName})</span>
                  </h4>
                  <span className="text-xs text-[#8C0000] font-mono font-bold">
                    Folder Alias: {loc.folderName}/
                  </span>
                </div>
                <button
                  onClick={() => setEditingId(editingId === loc.id ? null : loc.id)}
                  className="text-xs font-semibold text-[#D94F00] hover:underline"
                >
                  {editingId === loc.id ? 'Done' : 'Edit Coordinates'}
                </button>
              </div>

              <p className="text-xs text-stone-600 bg-white p-2 rounded border border-stone-200">
                <strong>Address:</strong> {loc.address}
              </p>

              {/* Editable Fields */}
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5 pt-1">
                <div>
                  <label className="text-[11px] font-bold text-stone-600">Latitude (°N)</label>
                  <input
                    type="number"
                    step="0.000001"
                    disabled={editingId !== loc.id}
                    value={loc.latitude}
                    onChange={(e) =>
                      handleUpdate(loc.id, { latitude: parseFloat(e.target.value) || 0 })
                    }
                    className="w-full mt-0.5 px-2.5 py-1.5 border rounded-lg text-xs font-mono bg-white disabled:bg-stone-100 disabled:text-stone-500"
                  />
                </div>

                <div>
                  <label className="text-[11px] font-bold text-stone-600">Longitude (°E)</label>
                  <input
                    type="number"
                    step="0.000001"
                    disabled={editingId !== loc.id}
                    value={loc.longitude}
                    onChange={(e) =>
                      handleUpdate(loc.id, { longitude: parseFloat(e.target.value) || 0 })
                    }
                    className="w-full mt-0.5 px-2.5 py-1.5 border rounded-lg text-xs font-mono bg-white disabled:bg-stone-100 disabled:text-stone-500"
                  />
                </div>

                <div>
                  <label className="text-[11px] font-bold text-stone-600">Radius (Meters)</label>
                  <input
                    type="number"
                    step="1"
                    disabled={editingId !== loc.id}
                    value={loc.radiusMeters}
                    onChange={(e) =>
                      handleUpdate(loc.id, { radiusMeters: parseFloat(e.target.value) || 0 })
                    }
                    className="w-full mt-0.5 px-2.5 py-1.5 border rounded-lg text-xs font-mono bg-white disabled:bg-stone-100 disabled:text-stone-500"
                  />
                </div>
              </div>
            </div>
          ))}
        </div>

        {/* Footer */}
        <div className="flex items-center justify-between pt-3 border-t border-stone-200">
          <button
            onClick={handleResetDefaults}
            className="text-xs text-stone-600 hover:text-red-800 flex items-center space-x-1 font-semibold"
          >
            <RotateCcw className="w-3.5 h-3.5" />
            <span>Reset Verified Coordinates (40m)</span>
          </button>

          <div className="flex items-center space-x-2">
            <button
              onClick={onClose}
              className="px-4 py-2 text-stone-600 hover:bg-stone-100 rounded-xl text-xs font-semibold"
            >
              Cancel
            </button>
            <button
              onClick={handleSave}
              className="px-4 py-2 bg-[#D94F00] hover:bg-[#C93B00] text-white rounded-xl text-xs font-bold flex items-center space-x-1.5 shadow"
            >
              <Save className="w-4 h-4" />
              <span>Save &amp; Apply</span>
            </button>
          </div>
        </div>

      </div>
    </div>
  );
};
