import { FestivalScheduleItem, LocationConfigItem } from '../types';

/**
 * Calculates offline Haversine distance in meters between two GPS coordinates.
 */
export function calculateDistanceMeters(
  lat1: number,
  lon1: number,
  lat2: number,
  lon2: number
): number {
  const R = 6371e3; // Earth radius in meters
  const phi1 = (lat1 * Math.PI) / 180;
  const phi2 = (lat2 * Math.PI) / 180;
  const deltaPhi = ((lat2 - lat1) * Math.PI) / 180;
  const deltaLambda = ((lon2 - lon1) * Math.PI) / 180;

  const a =
    Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2) +
    Math.cos(phi1) * Math.cos(phi2) * Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

  return R * c;
}

export type MatchType = 'SINGLE_MATCH' | 'AMBIGUOUS_MULTIPLE_MATCH' | 'UNMATCHED_GPS' | 'NO_GPS';

export interface PhotoDestinationEvaluation {
  photoLatitude: number | null;
  photoLongitude: number | null;
  distancesToLocations: Record<string, number>; // folderName -> distance in meters
  matchedLocations: LocationConfigItem[];
  matchType: MatchType;
  selectedAlias: string | null; // "Home" | "Yeendda_Durga_Devi_Temple" | "Carshed_Bobleam" | null
  deviParentFolder: string; // e.g. "Sri_Gayatri_Devi" or "Other_Dates"
  locationSubfolder: string; // e.g. "Home" or "2026-10-12"
  destinationRelativePath: string; // e.g. "Dussehra_2026/Sri_Gayatri_Devi/Home"
  statusSummary: string;
}

/**
 * Evaluates photo coordinates and festival date offline.
 * - Computes distance to every registered location.
 * - Checks if distances <= radius.
 * - If multiple locations match: marks as ambiguous, does NOT guess, uses date-based fallback.
 * - Maps capture date to Devi Alankaram folder from festival schedule.
 * - Constructs final path under Pictures/Dussehra_2026/[Devi_Folder]/[Location_Or_Date]/
 */
export function evaluatePhotoDestination(
  latitude: number | null,
  longitude: number | null,
  captureDateFormatted: string | null, // dd-MM-yyyy
  isoDate: string | null, // yyyy-MM-dd
  schedule: FestivalScheduleItem[],
  locations: LocationConfigItem[]
): PhotoDestinationEvaluation {
  const distances: Record<string, number> = {};
  const matchedLocations: LocationConfigItem[] = [];

  const hasGps = latitude !== null && longitude !== null;

  if (hasGps) {
    for (const loc of locations) {
      if (!loc.enabled) continue;
      const dist = calculateDistanceMeters(latitude, longitude, loc.latitude, loc.longitude);
      distances[loc.folderName] = Math.round(dist * 10) / 10;

      if (dist <= loc.radiusMeters) {
        matchedLocations.push(loc);
      }
    }
  }

  // 1. Determine Devi Parent Folder from Festival Schedule
  let deviParentFolder = 'Other_Dates';
  if (captureDateFormatted) {
    const matched = schedule.find((item) => item.date === captureDateFormatted);
    if (matched) {
      deviParentFolder = matched.folderName;
    }
  }

  // 2. Determine Location Subfolder & Match Type
  let locationSubfolder: string;
  let selectedAlias: string | null = null;
  let matchType: MatchType;
  let statusSummary: string;

  const fallbackDate = isoDate || (captureDateFormatted ? captureDateFormatted : 'Unknown_Date');

  if (!hasGps) {
    matchType = 'NO_GPS';
    locationSubfolder = fallbackDate;
    statusSummary = `📅 No GPS -> Fallback: ${deviParentFolder}/${locationSubfolder}/`;
  } else if (matchedLocations.length === 1) {
    const single = matchedLocations[0];
    selectedAlias = single.folderName;
    locationSubfolder = single.folderName;
    matchType = 'SINGLE_MATCH';
    const dist = distances[single.folderName];
    statusSummary = `📍 ${single.name} (${dist}m <= ${single.radiusMeters}m) -> ${deviParentFolder}/${locationSubfolder}/`;
  } else if (matchedLocations.length > 1) {
    // Ambiguous overlap: multiple locations matched!
    matchType = 'AMBIGUOUS_MULTIPLE_MATCH';
    locationSubfolder = fallbackDate;
    statusSummary = `⚠️ Multiple Locations Matched (${matchedLocations.length}) -> Fallback: ${deviParentFolder}/${locationSubfolder}/`;
  } else {
    // Unmatched GPS
    matchType = 'UNMATCHED_GPS';
    locationSubfolder = fallbackDate;
    statusSummary = `📅 Unmatched GPS -> Fallback: ${deviParentFolder}/${locationSubfolder}/`;
  }

  const destinationRelativePath = `Dussehra_2026/${deviParentFolder}/${locationSubfolder}`;

  return {
    photoLatitude: latitude,
    photoLongitude: longitude,
    distancesToLocations: distances,
    matchedLocations,
    matchType,
    selectedAlias,
    deviParentFolder,
    locationSubfolder,
    destinationRelativePath,
    statusSummary
  };
}
