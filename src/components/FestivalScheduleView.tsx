import React, { useState } from 'react';
import { FestivalScheduleItem } from '../types';
import { Calendar, Folder, Sparkles, Edit3, Save, RotateCcw } from 'lucide-react';
import { DEFAULT_FESTIVAL_SCHEDULE } from '../data/defaultSchedule';

interface FestivalScheduleViewProps {
  schedule: FestivalScheduleItem[];
  onUpdateSchedule: (newSchedule: FestivalScheduleItem[]) => void;
  onSelectFolderToView?: (folderName: string) => void;
}

export const FestivalScheduleView: React.FC<FestivalScheduleViewProps> = ({
  schedule,
  onUpdateSchedule,
  onSelectFolderToView,
}) => {
  const [editingItem, setEditingItem] = useState<FestivalScheduleItem | null>(null);
  const [editIndex, setEditIndex] = useState<number>(-1);

  const handleStartEdit = (item: FestivalScheduleItem, index: number) => {
    setEditingItem({ ...item });
    setEditIndex(index);
  };

  const handleSaveEdit = () => {
    if (editingItem && editIndex >= 0) {
      const updated = [...schedule];
      updated[editIndex] = editingItem;
      onUpdateSchedule(updated);
      setEditingItem(null);
      setEditIndex(-1);
    }
  };

  const handleResetDefaults = () => {
    if (confirm('Reset festival schedule to default Visakhapatnam temple dates & alankarams?')) {
      onUpdateSchedule(DEFAULT_FESTIVAL_SCHEDULE);
    }
  };

  return (
    <div className="space-y-6">
      
      {/* Temple Header Banner */}
      <div className="bg-gradient-to-r from-[#FFF5EA] via-[#FFEAD5] to-[#FFDFC0] rounded-2xl border-2 border-amber-300 p-6 shadow-sm">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <div className="flex items-center space-x-2 text-xs font-bold text-[#D94F00] uppercase tracking-wider">
              <Sparkles className="w-4 h-4 text-amber-600" />
              <span>విశాఖపట్నం, మధురవాడ, చంద్రంపాలెం జాతర గట్టు</span>
            </div>
            <h2 className="font-telugu text-2xl sm:text-3xl font-bold text-[#8C0000] mt-1">
              శ్రీ దుర్గాదేవి అమ్మవారి దసరా శరన్నవరాత్రి ఉత్సవములు 2026
            </h2>
            <p className="text-sm text-stone-700 mt-1">
              Official 11 Alankaram temple schedule. Subfolder names <strong>STRICTLY DO NOT</strong> include dates.
            </p>
          </div>

          <button
            onClick={handleResetDefaults}
            className="px-3.5 py-2 bg-white hover:bg-stone-50 text-stone-700 rounded-xl text-xs font-semibold flex items-center space-x-1.5 border border-amber-300 shadow-sm transition-colors self-start sm:self-auto"
          >
            <RotateCcw className="w-3.5 h-3.5 text-stone-500" />
            <span>Reset Schedule Defaults</span>
          </button>
        </div>

        <div className="mt-4 p-3 bg-white/70 rounded-xl border border-amber-200 text-xs text-stone-700 font-mono">
          Target Directory: <strong>Pictures/Dussehra_2026/[Devi_Alankaram]/</strong> (No date prefix)
        </div>
      </div>

      {/* Schedule Items List */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {schedule.map((item, index) => (
          <div
            key={item.date}
            className="bg-white rounded-xl border border-amber-200/80 p-5 shadow-sm hover:shadow-md transition-shadow relative group"
          >
            <div className="flex items-start justify-between">
              <div className="flex items-center space-x-2">
                <span className="w-7 h-7 rounded-full bg-[#D94F00]/10 text-[#D94F00] font-bold text-xs flex items-center justify-center">
                  {index + 1}
                </span>
                <span className="font-mono text-xs font-bold text-red-900 bg-red-50 px-2 py-0.5 rounded border border-red-200">
                  {item.date}
                </span>
                <span className="font-telugu text-xs text-stone-500">
                  {item.dayTithi}
                </span>
              </div>

              <button
                onClick={() => handleStartEdit(item, index)}
                className="text-stone-400 hover:text-[#D94F00] p-1 rounded transition-colors"
                title="Edit this alankaram"
              >
                <Edit3 className="w-4 h-4" />
              </button>
            </div>

            <div className="mt-3">
              <h3 className="font-telugu text-lg font-bold text-[#8C0000]">
                {item.teluguName}
              </h3>
              <h4 className="font-serif-temple text-sm font-semibold text-stone-700">
                {item.englishName}
              </h4>
            </div>

            <div className="mt-2.5 text-xs text-stone-600 bg-stone-50 p-2.5 rounded-lg border border-stone-200">
              <span className="font-semibold text-stone-800">పూజ: </span>
              <span className="font-telugu">{item.specialProgram}</span>
            </div>

            <div className="mt-3 flex items-center justify-between text-xs pt-2.5 border-t border-stone-100">
              <div className="flex items-center space-x-1.5 text-stone-600 font-mono text-[11px]">
                <Folder className="w-3.5 h-3.5 text-[#D94F00]" />
                <span className="text-[#8C0000] font-bold truncate">{item.folderName}/</span>
              </div>

              {onSelectFolderToView && (
                <button
                  onClick={() => onSelectFolderToView(item.folderName)}
                  className="text-amber-800 hover:underline text-xs font-semibold"
                >
                  View Photos &rarr;
                </button>
              )}
            </div>
          </div>
        ))}
      </div>

      {/* Edit Modal */}
      {editingItem && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-2xl space-y-4">
            <h3 className="text-lg font-bold text-stone-900 font-serif-temple">
              Edit Festival Alankaram ({editingItem.date})
            </h3>

            <div className="space-y-3 text-xs">
              <div>
                <label className="font-bold text-stone-700">Date (dd-MM-yyyy)</label>
                <input
                  type="text"
                  value={editingItem.date}
                  onChange={(e) => setEditingItem({ ...editingItem, date: e.target.value })}
                  className="w-full mt-1 px-3 py-2 border rounded-lg font-mono text-sm"
                />
              </div>
              <div>
                <label className="font-bold text-stone-700">Tithi / Day (Telugu)</label>
                <input
                  type="text"
                  value={editingItem.dayTithi}
                  onChange={(e) => setEditingItem({ ...editingItem, dayTithi: e.target.value })}
                  className="w-full mt-1 px-3 py-2 border rounded-lg text-sm"
                />
              </div>
              <div>
                <label className="font-bold text-stone-700">Devi Alankaram (Telugu)</label>
                <input
                  type="text"
                  value={editingItem.teluguName}
                  onChange={(e) => setEditingItem({ ...editingItem, teluguName: e.target.value })}
                  className="w-full mt-1 px-3 py-2 border rounded-lg text-sm"
                />
              </div>
              <div>
                <label className="font-bold text-stone-700">English Name</label>
                <input
                  type="text"
                  value={editingItem.englishName}
                  onChange={(e) => setEditingItem({ ...editingItem, englishName: e.target.value })}
                  className="w-full mt-1 px-3 py-2 border rounded-lg text-sm"
                />
              </div>
              <div>
                <label className="font-bold text-stone-700">Target Folder Name (Strictly No Date)</label>
                <input
                  type="text"
                  value={editingItem.folderName}
                  onChange={(e) => setEditingItem({ ...editingItem, folderName: e.target.value })}
                  className="w-full mt-1 px-3 py-2 border rounded-lg font-mono text-sm"
                />
              </div>
              <div>
                <label className="font-bold text-stone-700">Special Pooja Program</label>
                <textarea
                  rows={2}
                  value={editingItem.specialProgram}
                  onChange={(e) => setEditingItem({ ...editingItem, specialProgram: e.target.value })}
                  className="w-full mt-1 px-3 py-2 border rounded-lg text-sm"
                />
              </div>
            </div>

            <div className="flex items-center justify-end space-x-2 pt-2">
              <button
                onClick={() => setEditingItem(null)}
                className="px-4 py-2 text-stone-600 hover:bg-stone-100 rounded-lg text-xs font-semibold"
              >
                Cancel
              </button>
              <button
                onClick={handleSaveEdit}
                className="px-4 py-2 bg-[#D94F00] hover:bg-[#C93B00] text-white rounded-lg text-xs font-semibold flex items-center space-x-1.5 shadow"
              >
                <Save className="w-4 h-4" />
                <span>Save Changes</span>
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
};
