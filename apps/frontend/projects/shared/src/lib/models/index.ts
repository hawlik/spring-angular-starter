// Shared models/interfaces - to be implemented
export interface User {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  roles: string[];
  avatarUrl: string | null;
}

export interface Group {
  id: number;
  name: string;
  eventId: number;
  eventName: string;
  memberCount: number;
}

export interface Match {
  id: number;
  team1: Team;
  team2: Team;
  matchTime: string;
  status: 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED';
  result?: Result;
  multiplier: number;
}

export interface Team {
  id: number;
  name: string;
  shortName: string;
  flagUrl: string;
}

export interface Result {
  goalsTeam1: number;
  goalsTeam2: number;
}

export interface Bet {
  id: number;
  matchId: number;
  prediction: Result;
  isLocked: boolean;
  pointsEarned?: number;
}
