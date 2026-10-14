import React from 'react';
import { FestivalScheduleItem } from '../types';
import { Calendar, Sparkles, Folder, Clock } from 'lucide-react';

interface TodayAlankaramCardProps {
  todayItem: FestivalScheduleItem | null;
  currentDateFormatted: string;
}

export const TodayAlankaramCard: React.FC<TodayAlankaramCardProps> = ({
  todayItem,
  currentDateFormatted,
}) => {
  return (
    <div className="bg-gradient-to-br from-[#FFF5EA] to-[#FFE8D6] rounded-2xl border-2 border-amber-300 p-6 shadow-md relative overflow-hidden">
      {/* Background Decorative Diya / Mandala */}
      <div className="absolute -right-8 -bottom-8 opacity-10 text-9xl pointer-events-none select-none">
        🪔
      </div>

      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-amber-200 pb-4 mb-4">
        <div className="flex items-center space-x-2 text-red-800 font-bold text-sm">
          <Calendar className="w-4 h-4 text-[#D94F00]" />
          <span>ఈ రోజు తేది / Today\'s Date:</span>
          <span className="bg-amber-100 text-red-900 px-2.5 py-0.5 rounded-full border border-amber-300 font-mono">
            {currentDateFormatted}
          </span>
        </div>
        <div className="flex items-center space-x-1.5 text-xs font-semibold text-amber-900 bg-amber-400/20 px-3 py-1 rounded-full border border-amber-300 w-fit">
          <Sparkles className="w-3.5 h-3.5 text-amber-600" />
          <span>విశాఖపట్నం శ్రీ దుర్గాదేవి దసరా ఉత్సవములు 2026</span>
        </div>
      </div>

      {todayItem ? (
        <div className="space-y-3">
          <div>
            <span className="text-xs font-bold uppercase tracking-wider text-[#D94F00]">
              నేటి అమ్మవారి అలంకారం ({todayItem.dayTithi})
            </span>
            <h2 className="font-telugu text-2xl sm:text-3xl font-bold text-[#8C0000] mt-1">
              {todayItem.teluguName}
            </h2>
            <h3 className="text-lg font-serif-temple font-semibold text-stone-700">
              {todayItem.englishName}
            </h3>
          </div>

          <div className="bg-white/80 backdrop-blur rounded-xl p-3.5 border border-amber-200 text-sm text-stone-800 flex items-start space-x-2.5">
            <Clock className="w-4 h-4 text-amber-600 mt-0.5 shrink-0" />
            <div>
              <span className="font-semibold text-stone-900">ప్రత్యేక పూజా కార్యక్రమము: </span>
              <span className="font-telugu">{todayItem.specialProgram}</span>
            </div>
          </div>

          <div className="flex items-center space-x-2 text-xs font-mono bg-red-950/5 text-red-900 px-3 py-2 rounded-lg border border-red-900/10">
            <Folder className="w-4 h-4 text-[#D94F00] shrink-0" />
            <span>Target Folder (No Date): </span>
            <strong className="text-[#8C0000]">Pictures/Dussehra_2026/{todayItem.folderName}/</strong>
          </div>
        </div>
      ) : (
        <div className="py-4">
          <h2 className="font-telugu text-xl font-bold text-[#8C0000]">
            శ్రీ దుర్గాదేవి అమ్మవారి దసరా ఉత్సవములు 2026
          </h2>
          <p className="text-stone-600 text-sm mt-1">
            పండుగ తేదిలు: అక్టోబర్ 11, 2026 నుండి అక్టోబర్ 21, 2026 వరకు (11 అమ్మవారి అలంకారాలు సిద్ధంగా ఉన్నాయి).
          </p>
          <div className="flex items-center space-x-2 text-xs font-mono bg-amber-500/10 text-stone-800 px-3 py-2 rounded-lg border border-amber-200 mt-3">
            <Folder className="w-4 h-4 text-[#D94F00]" />
            <span>Main Festival Storage: <strong>Pictures/Dussehra_2026/</strong> (11 Subfolders ready)</span>
          </div>
        </div>
      )}
    </div>
  );
};
