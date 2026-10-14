import ExifReader from 'exifreader';

export interface ParsedExifResult {
  captureDateFormatted: string | null; // dd-MM-yyyy
  isoDate: string | null; // yyyy-MM-dd
  timestamp: number;
  latitude: number | null;
  longitude: number | null;
  cameraModel: string | null;
  isAuthenticCamera: boolean;
  rejectReason?: string;
}

export function isCameraPhotoFilter(fileName: string, relativePath: string = ''): { valid: boolean; reason?: string } {
  const nameLower = fileName.toLowerCase();
  const pathLower = relativePath.toLowerCase();

  // Strict Exclusion of non-camera sources
  if (pathLower.includes('whatsapp') || (nameLower.startsWith('img-') && nameLower.includes('-wa'))) {
    return { valid: false, reason: 'Excluded: WhatsApp media' };
  }
  if (pathLower.includes('telegram') || pathLower.includes('messenger') || pathLower.includes('viber') || pathLower.includes('signal')) {
    return { valid: false, reason: 'Excluded: Messaging application media' };
  }
  if (pathLower.includes('screenshot') || nameLower.includes('screenshot') || nameLower.startsWith('screencapture')) {
    return { valid: false, reason: 'Excluded: Device screenshot' };
  }
  if (pathLower.includes('download') || pathLower.includes('downloaded')) {
    return { valid: false, reason: 'Excluded: Web or app download' };
  }
  if (pathLower.includes('facebook') || pathLower.includes('instagram') || pathLower.includes('twitter') || pathLower.includes('reddit')) {
    return { valid: false, reason: 'Excluded: Social media download' };
  }

  // Camera paths & name patterns
  const matchesCameraPattern =
    nameLower.startsWith('img_20') ||
    nameLower.startsWith('sam_') ||
    nameLower.startsWith('vivo_') ||
    nameLower.startsWith('cam_') ||
    nameLower.startsWith('dsc_') ||
    /^202[0-9]{5}_[0-9]{6}/.test(nameLower) ||
    nameLower.endsWith('.jpg') ||
    nameLower.endsWith('.jpeg') ||
    nameLower.endsWith('.heic');

  if (!matchesCameraPattern) {
    return { valid: false, reason: 'Unrecognized camera filename format' };
  }

  return { valid: true };
}

export async function parseFileMetadata(file: File): Promise<ParsedExifResult> {
  const filter = isCameraPhotoFilter(file.name);
  if (!filter.valid) {
    return {
      captureDateFormatted: null,
      isoDate: null,
      timestamp: 0,
      latitude: null,
      longitude: null,
      cameraModel: null,
      isAuthenticCamera: false,
      rejectReason: filter.reason
    };
  }

  try {
    const arrayBuffer = await file.arrayBuffer();
    const tags = ExifReader.load(arrayBuffer);

    let dateStr = tags['DateTimeOriginal']?.description || tags['CreateDate']?.description || tags['DateTime']?.description;
    let captureDateFormatted: string | null = null;
    let isoDate: string | null = null;
    let timestamp = 0;

    if (dateStr) {
      // EXIF date format is usually YYYY:MM:DD HH:MM:SS
      const parts = dateStr.trim().split(' ')[0].split(':');
      if (parts.length === 3) {
        const year = parts[0];
        const month = parts[1].padStart(2, '0');
        const day = parts[2].padStart(2, '0');
        captureDateFormatted = `${day}-${month}-${year}`;
        isoDate = `${year}-${month}-${day}`;
        timestamp = new Date(`${year}-${month}-${day}T12:00:00Z`).getTime();
      }
    }

    let latitude: number | null = null;
    let longitude: number | null = null;

    if (tags['GPSLatitude'] && tags['GPSLongitude']) {
      latitude = Number(tags['GPSLatitude'].description);
      longitude = Number(tags['GPSLongitude'].description);

      const latRef = String(tags['GPSLatitudeRef']?.description || tags['GPSLatitudeRef']?.value || '');
      const lonRef = String(tags['GPSLongitudeRef']?.description || tags['GPSLongitudeRef']?.value || '');
      if (latRef.startsWith('S') && latitude > 0) {
        latitude = -latitude;
      }
      if (lonRef.startsWith('W') && longitude > 0) {
        longitude = -longitude;
      }
    }

    const cameraModel = tags['Model']?.description || tags['Make']?.description || null;

    return {
      captureDateFormatted,
      isoDate,
      timestamp,
      latitude,
      longitude,
      cameraModel,
      isAuthenticCamera: true
    };
  } catch {
    // If EXIF reading fails, return safe default without guessing
    return {
      captureDateFormatted: null,
      isoDate: null,
      timestamp: 0,
      latitude: null,
      longitude: null,
      cameraModel: null,
      isAuthenticCamera: true,
      rejectReason: 'Missing EXIF capture metadata'
    };
  }
}
