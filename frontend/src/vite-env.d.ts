/// <reference types="vite/client" />

interface Window {
  YT?: {
    Player: new (elementId: string, options: YoutubePlayerOptions) => YoutubePlayer;
    PlayerState: {
      PLAYING: number;
      PAUSED: number;
      ENDED: number;
    };
  };
  onYouTubeIframeAPIReady?: () => void;
}

interface YoutubePlayerOptions {
  videoId: string;
  width?: string;
  height?: string;
  playerVars?: Record<string, number | string>;
  events?: {
    onReady?: () => void;
    onStateChange?: (event: { data: number }) => void;
  };
}

interface YoutubePlayer {
  cueVideoById(options: { videoId: string; startSeconds?: number; endSeconds?: number }): void;
  loadVideoById(options: { videoId: string; startSeconds?: number; endSeconds?: number }): void;
  seekTo(seconds: number, allowSeekAhead: boolean): void;
  getCurrentTime(): number;
  playVideo(): void;
  pauseVideo(): void;
  setVolume(volume: number): void;
}
