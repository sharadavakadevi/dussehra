export interface FestivalScheduleItem {
  date: string; // dd-MM-yyyy e.g. 11-10-2026
  dayTithi: string; // e.g. ఆదివారం శుద్ధ పాడ్యమి
  teluguName: string; // e.g. శ్రీ బాలాత్రిపురసుందరి దేవి
  englishName: string; // e.g. Sri Bala Tripura Sundari Devi
  folderName: string; // strictly without date: e.g. Sri_Bala_Tripura_Sundari_Devi
  specialProgram: string;
}

export interface LocationConfigItem {
  id: string;
  name: string;
  teluguName: string;
  address: string;
  folderName: string;
  latitude: number;
  longitude: number;
  radiusMeters: number;
  enabled: boolean;
}

export interface OrganizedPhoto {
  id: string;
  fileName: string;
  fileSize: number;
  captureDateFormatted: string; // dd-MM-yyyy or yyyy-MM-dd
  isoDate?: string; // yyyy-MM-dd
  timestamp: number;
  rootFolder: 'Dussehra_2026';
  deviParentFolder: string; // e.g. "Sri_Gayatri_Devi" or "Other_Dates"
  locationSubfolder: string; // e.g. "Home" or "2026-10-12"
  destinationFolder: string; // e.g. "Sri_Gayatri_Devi/Home"
  sourceType: 'camera' | 'external';
  isCameraAuthentic: boolean;
  latitude?: number;
  longitude?: number;
  distanceMeters?: number;
  locationLabel?: string;
  statusMessage: string;
  previewUrl: string;
  rawFile?: File;
  selected: boolean;
}
