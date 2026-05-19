import { telegramApp } from './telegram';

export type PlayerProfile = {
  playerId: string;
  handle: string;
  displayName: string;
  avatarId?: string;
  referralCode?: string;
};

export type AuthState = {
  accessToken: string | null;
  player: PlayerProfile;
  isDemo: boolean;
};

const demoPlayer: PlayerProfile = {
  playerId: 'demo-player',
  handle: 'iq_player',
  displayName: 'Игрок IQ',
  avatarId: 'avatar_player',
  referralCode: 'IQ-PLAYER'
};

export async function authenticate(): Promise<AuthState> {
  const initData = telegramApp()?.initData || '';
  if (!initData) {
    return { accessToken: null, player: demoPlayer, isDemo: true };
  }

  const response = await fetch('/v1/auth/telegram/webapp', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ initData })
  });

  if (!response.ok) {
    throw new Error('Telegram authorization failed');
  }

  const data = await response.json();
  return {
    accessToken: data.accessToken,
    player: {
      playerId: data.playerId,
      handle: data.handle,
      displayName: data.displayName,
      avatarId: data.avatarId,
      referralCode: data.referralCode
    },
    isDemo: false
  };
}

export async function issueWsTicket(accessToken: string) {
  const response = await fetch('/v1/realtime/session', {
    method: 'POST',
    headers: { Authorization: `Bearer ${accessToken}` }
  });

  if (!response.ok) {
    throw new Error('Realtime ticket failed');
  }

  return response.json() as Promise<{ wsTicket: string; expiresInSeconds: number }>;
}
