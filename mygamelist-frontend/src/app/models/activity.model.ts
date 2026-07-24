export type ActivityType = 'ADDED_TO_LIST' | 'CHANGED_STATUS' | 'RATED' | 'REVIEWED';

export interface ActivityUser {
  id: number;
  name: string;
  profilePicture: string | null;
}

export interface ActivityGame {
  id: number;
  rawgId: number;
  title: string;
  coverUrl: string;
}

export interface ActivityComment {
  id: number;
  text: string;
  createdAt: string;
  user: ActivityUser;
}

export interface GroupedActivity {
  id: number;
  timestamp: string;
  user: ActivityUser;
  game: ActivityGame;
  types: ActivityType[];
  statusDetail: string | null;
  ratingValue: number | null;
  reviewText: string | null;
  likesCount: number;
  likedByMe: boolean;
  comments: ActivityComment[];
  showComments?: boolean; 
}