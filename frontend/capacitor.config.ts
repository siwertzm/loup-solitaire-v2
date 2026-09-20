import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.loupsolitaire.app',
  appName: 'Loup Solitaire',
  webDir: 'dist/loup-solitaire-front/browser',
  backgroundColor: '#2c3323',
  plugins: {
    SystemBars: {
      style: 'DARK',
      initialViewportFitValueHint: 'cover'
    }
  }
};

export default config;