type HapticStyle = 'light' | 'medium' | 'heavy';

type TelegramWebApp = {
  initData?: string;
  ready: () => void;
  expand: () => void;
  HapticFeedback?: {
    impactOccurred: (style: HapticStyle) => void;
    notificationOccurred: (type: 'success' | 'warning' | 'error') => void;
  };
};

declare global {
  interface Window {
    Telegram?: {
      WebApp?: TelegramWebApp;
    };
  }
}

export function telegramApp() {
  return window.Telegram?.WebApp;
}

export function prepareTelegramApp() {
  const app = telegramApp();
  app?.ready();
  app?.expand();
}

export function haptic(style: HapticStyle = 'light') {
  telegramApp()?.HapticFeedback?.impactOccurred(style);
}
