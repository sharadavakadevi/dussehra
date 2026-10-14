import { LocationConfigItem } from '../types';

/**
 * Exact user-supplied Google Maps GPS coordinates and non-overlapping 40m initial matching radius.
 * Note: Home & Yeendda Durga Devi Temple are ~106 meters apart.
 * 40m radius for each provides a ~26-meter non-overlapping separation zone.
 */
export const DEFAULT_LOCATIONS: LocationConfigItem[] = [
  {
    id: 'loc_home',
    name: 'Home',
    teluguName: 'ఇల్లు (Home)',
    address: '1-72, Yendada, Endada, Andhra Pradesh 530045, India',
    folderName: 'Home',
    latitude: 17.782889,
    longitude: 83.361833,
    radiusMeters: 40,
    enabled: true
  },
  {
    id: 'loc_temple',
    name: 'Sri Lakshmi Villa / Yeendda Durga Devi Temple',
    teluguName: 'ఎండడ దుర్గాదేవి ఆలయం',
    address: 'Sri Lakshmi Villa, 8, Endada Rushikonda Rd, Sandipini Nagar, Mubarak Colony, Yendada, Endada, Andhra Pradesh 530045, India',
    folderName: 'Yeendda_Durga_Devi_Temple',
    latitude: 17.781944,
    longitude: 83.361917,
    radiusMeters: 40,
    enabled: true
  },
  {
    id: 'loc_carshed',
    name: 'Carshed Bobleam',
    teluguName: 'కార్ షెడ్ బొబ్బిలి / పి.ఎం పాలెం',
    address: 'NH 16 Service Rd, Anand Nagar, Pothinamallayya Palem, Pothinamallayyapalem, Andhra Pradesh 530041, India',
    folderName: 'Carshed_Bobleam',
    latitude: 17.802778,
    longitude: 83.352667,
    radiusMeters: 40,
    enabled: true
  }
];
