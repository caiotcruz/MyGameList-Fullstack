export interface Activity {
  id: number;
  user: {
    name: string;
  };
  game: {
    title: string;
    coverUrl: string;
  };
  type: 'ADDED_TO_LIST' | 'CHANGED_STATUS' | 'RATED' | 'REVIEWED';
  detail: string;
  timestamp: string;
}