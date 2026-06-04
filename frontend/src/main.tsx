import React, { useEffect, useMemo, useRef, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { Clapperboard, Pause, Play, RotateCw, SkipForward, Trophy, Volume2 } from 'lucide-react';
import './styles.css';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';
const DAILY_MOVIE_COUNT = 5;
const MAX_ATTEMPTS = 3;

type Challenge = {
  challengeId: string;
  movieId: number;
  youtubeKey: string;
  fragmentStart: number;
  stages: number[];
  currentStage: number;
};

type AnswerResponse = {
  correct: boolean;
  movieTitle: string;
  posterUrl: string | null;
  tmdbRating: number | null;
  score: number;
};

type LeaderboardEntry = {
  username: string;
  score: number;
  attempts: number;
};

type SyncResponse = {
  moviesSynced: number;
  videosSynced: number;
};

type OutcomeModal = {
  kind: 'correct' | 'failed';
  movieTitle: string;
  posterUrl: string | null;
  tmdbRating: number | null;
};

function loadYouTubeApi() {
  if (window.YT?.Player) {
    return Promise.resolve();
  }

  return new Promise<void>((resolve) => {
    const existing = document.querySelector<HTMLScriptElement>('script[src="https://www.youtube.com/iframe_api"]');
    window.onYouTubeIframeAPIReady = () => resolve();
    if (!existing) {
      const script = document.createElement('script');
      script.src = 'https://www.youtube.com/iframe_api';
      document.head.appendChild(script);
    }
  });
}

function App() {
  const [challenge, setChallenge] = useState<Challenge | null>(null);
  const [leaderboard, setLeaderboard] = useState<LeaderboardEntry[]>([]);
  const [movieIndex, setMovieIndex] = useState(0);
  const [currentStage, setCurrentStage] = useState(1);
  const [skippedStages, setSkippedStages] = useState<Set<number>>(new Set());
  const [attemptsUsed, setAttemptsUsed] = useState(0);
  const [outcomeModal, setOutcomeModal] = useState<OutcomeModal | null>(null);
  const [answer, setAnswer] = useState('');
  const [username, setUsername] = useState(localStorage.getItem('cineguess:username') ?? 'guest');
  const [result, setResult] = useState<AnswerResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [syncing, setSyncing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [syncMessage, setSyncMessage] = useState<string | null>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [videoRevealed, setVideoRevealed] = useState(false);
  const [playbackElapsed, setPlaybackElapsed] = useState(0);
  const [volume, setVolume] = useState(65);
  const playerRef = useRef<YoutubePlayer | null>(null);
  const timerRef = useRef<number | null>(null);
  const progressTimerRef = useRef<number | null>(null);
  const autoAdvanceTimerRef = useRef<number | null>(null);
  const challengeRef = useRef<Challenge | null>(null);
  const currentStageRef = useRef(currentStage);
  const playbackElapsedRef = useRef(playbackElapsed);
  const playbackBaseElapsedRef = useRef(0);
  const playbackStartedAtRef = useRef<number | null>(null);

  const scorePreview = useMemo(() => {
    if (currentStage === 1) return 1000;
    if (currentStage === 4) return 750;
    if (currentStage === 15) return 500;
    if (currentStage === 30) return 300;
    if (currentStage === 60) return 100;
    return 0;
  }, [currentStage]);

  const timelineProgress = Math.min((playbackElapsed / 60) * 100, 100);
  const targetProgress = Math.min((currentStage / 60) * 100, 100);

  useEffect(() => {
    challengeRef.current = challenge;
  }, [challenge]);

  useEffect(() => {
    currentStageRef.current = currentStage;
  }, [currentStage]);

  useEffect(() => {
    playbackElapsedRef.current = playbackElapsed;
  }, [playbackElapsed]);

  useEffect(() => {
    fetchChallenge(0);
    fetchLeaderboard();
  }, []);

  useEffect(() => {
    if (!challenge) return;

    loadYouTubeApi().then(() => {
      if (!window.YT?.Player) return;
      if (!playerRef.current) {
        playerRef.current = new window.YT.Player('youtube-player', {
          videoId: challenge.youtubeKey,
          width: '100%',
          height: '100%',
          playerVars: {
            controls: 0,
            disablekb: 1,
            fs: 0,
            iv_load_policy: 3,
            modestbranding: 1,
            rel: 0,
            playsinline: 1
          },
          events: {
            onReady: () => playerRef.current?.setVolume(volume),
            onStateChange: (event) => setIsPlaying(event.data === window.YT?.PlayerState.PLAYING)
          }
        });
      } else {
        playerRef.current.loadVideoById({
          videoId: challenge.youtubeKey,
          startSeconds: challenge.fragmentStart
        });
        window.setTimeout(() => {
          playerRef.current?.pauseVideo();
          playerRef.current?.seekTo(challenge.fragmentStart, true);
          setIsPlaying(false);
        }, 250);
      }
    });
  }, [challenge]);

  useEffect(() => {
    playerRef.current?.setVolume(volume);
  }, [volume]);

  useEffect(() => () => {
    clearPlaybackTimers();
    clearAutoAdvanceTimer();
  }, []);

  async function fetchChallenge(slot = movieIndex) {
    setLoading(true);
    setError(null);
    clearAutoAdvanceTimer();
    clearPlaybackTimers();
    try {
      const response = await fetch(`${API_BASE_URL}/api/challenges/daily/${slot}`);
      if (!response.ok) throw new Error(await readError(response));
      const data: Challenge = await response.json();
      setMovieIndex(slot);
      setChallenge(data);
      setCurrentStage(data.currentStage);
      setSkippedStages(new Set());
      setAttemptsUsed(0);
      setOutcomeModal(null);
      setPlaybackElapsed(0);
      playbackElapsedRef.current = 0;
      playbackBaseElapsedRef.current = 0;
      playbackStartedAtRef.current = null;
      setResult(null);
      setAnswer('');
      setVideoRevealed(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not load daily challenge');
    } finally {
      setLoading(false);
    }
  }

  async function readError(response: Response) {
    const fallback = `Request failed with ${response.status}`;
    try {
      const text = await response.text();
      if (!text) return fallback;
      try {
        const data = JSON.parse(text);
        return data.reason ?? data.message ?? data.error ?? text;
      } catch {
        return text;
      }
    } catch {
      return fallback;
    }
  }

  async function syncPopularMovies() {
    setSyncing(true);
    setError(null);
    setSyncMessage(null);
    try {
      const response = await fetch(`${API_BASE_URL}/api/movies/popular/sync`);
      if (!response.ok) throw new Error(await readError(response));
      const data: SyncResponse = await response.json();
      setSyncMessage(`Synced ${data.moviesSynced} movies and ${data.videosSynced} videos`);
      await fetchChallenge(movieIndex);
      await fetchLeaderboard();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not sync popular movies');
    } finally {
      setSyncing(false);
    }
  }

  async function fetchLeaderboard() {
    try {
      const response = await fetch(`${API_BASE_URL}/api/leaderboard/daily`);
      if (response.ok) setLeaderboard(await response.json());
    } catch {
      setLeaderboard([]);
    }
  }

  function playFragment() {
    if (!challenge || !playerRef.current) return;
    clearPlaybackTimers();
    setVideoRevealed(true);
    const start = challenge.fragmentStart;
    const elapsedAtStart = playbackElapsedRef.current;
    const shouldResume = elapsedAtStart > 0 && elapsedAtStart < currentStage;
    if (!shouldResume) {
      setPlaybackElapsed(0);
      playbackElapsedRef.current = 0;
      playerRef.current.seekTo(start, true);
    }
    playerRef.current.playVideo();
    startProgressTimer(shouldResume ? elapsedAtStart : 0);
    const remainingMs = Math.max((currentStage - (shouldResume ? elapsedAtStart : 0)) * 1000, 0);
    timerRef.current = window.setTimeout(() => {
      clearPlaybackTimers();
      playbackElapsedRef.current = currentStage;
      setPlaybackElapsed(currentStage);
      playerRef.current?.pauseVideo();
      setIsPlaying(false);
    }, remainingMs);
  }

  function pauseFragment() {
    updatePlaybackElapsedFromClock();
    clearPlaybackTimers();
    playerRef.current?.pauseVideo();
    setIsPlaying(false);
  }

  async function submitAnswer(event: React.FormEvent) {
    event.preventDefault();
    if (!challenge || !answer.trim()) return;
    localStorage.setItem('cineguess:username', username.trim() || 'guest');
    const response = await fetch(`${API_BASE_URL}/api/challenges/${challenge.challengeId}/answer`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Username': username.trim() || 'guest'
      },
      body: JSON.stringify({ answer, stageSeconds: currentStage })
    });
    if (!response.ok) {
      setError(await readError(response));
      return;
    }
    const data: AnswerResponse = await response.json();
    fetchLeaderboard();
    if (data.correct) {
      setResult(data);
      setOutcomeModal({
        kind: 'correct',
        movieTitle: data.movieTitle,
        posterUrl: data.posterUrl,
        tmdbRating: data.tmdbRating
      });
      return;
    }

    const nextAttemptsUsed = attemptsUsed + 1;
    setAttemptsUsed(nextAttemptsUsed);
    setAnswer('');
    if (nextAttemptsUsed >= MAX_ATTEMPTS) {
      setResult(data);
      setOutcomeModal({
        kind: 'failed',
        movieTitle: data.movieTitle,
        posterUrl: data.posterUrl,
        tmdbRating: data.tmdbRating
      });
      return;
    }
    setResult({
      correct: false,
      movieTitle: '',
      posterUrl: null,
      tmdbRating: null,
      score: 0
    });
    advanceStageAfterMiss();
  }

  function goToNextMovie() {
    setOutcomeModal(null);
    if (movieIndex < DAILY_MOVIE_COUNT - 1) {
      fetchChallenge(movieIndex + 1);
      return;
    }
    setResult(null);
  }

  function nextStage() {
    if (!challenge) return;
    advanceStageAfterMiss();
  }

  function advanceStageAfterMiss() {
    if (!challenge) return;
    clearPlaybackTimers();
    playerRef.current?.pauseVideo();
    setIsPlaying(false);
    setPlaybackElapsed(0);
    playbackElapsedRef.current = 0;
    playbackBaseElapsedRef.current = 0;
    playbackStartedAtRef.current = null;
    setVideoRevealed(false);
    const activeStage = currentStageRef.current;
    const index = challenge.stages.indexOf(activeStage);
    const next = challenge.stages[Math.min(index + 1, challenge.stages.length - 1)];
    if (next !== activeStage) {
      setSkippedStages((previous) => new Set(previous).add(activeStage));
    }
    currentStageRef.current = next;
    setCurrentStage(next);
  }

  function clearPlaybackTimers() {
    if (timerRef.current) {
      window.clearTimeout(timerRef.current);
      timerRef.current = null;
    }
    if (progressTimerRef.current) {
      window.clearInterval(progressTimerRef.current);
      progressTimerRef.current = null;
    }
    playbackStartedAtRef.current = null;
  }

  function clearAutoAdvanceTimer() {
    if (autoAdvanceTimerRef.current) {
      window.clearTimeout(autoAdvanceTimerRef.current);
      autoAdvanceTimerRef.current = null;
    }
  }

  function startProgressTimer(baseElapsed: number) {
    if (progressTimerRef.current) return;
    playbackBaseElapsedRef.current = baseElapsed;
    playbackStartedAtRef.current = Date.now();
    updatePlaybackElapsedFromClock();
    progressTimerRef.current = window.setInterval(updatePlaybackElapsedFromClock, 100);
  }

  function updatePlaybackElapsedFromClock() {
    if (playbackStartedAtRef.current == null) return;
    const elapsed = playbackBaseElapsedRef.current + (Date.now() - playbackStartedAtRef.current) / 1000;
    const nextElapsed = Math.min(elapsed, currentStageRef.current);
    playbackElapsedRef.current = nextElapsed;
    setPlaybackElapsed(nextElapsed);
  }

  function formatTime(seconds: number) {
    const safeSeconds = Math.max(0, seconds);
    const minutes = Math.floor(safeSeconds / 60);
    const wholeSeconds = Math.floor(safeSeconds % 60);
    const tenths = Math.floor((safeSeconds % 1) * 10);
    return `${minutes}:${String(wholeSeconds).padStart(2, '0')}.${tenths}`;
  }

  return (
    <main className="app-screen">
      <section className="app-shell">
        <div className="video-shell">
          <div className={isPlaying ? 'video-frame playing' : 'video-frame paused'}>
            <div id="youtube-player" className="h-full w-full" />
            <div className="spoiler-mask" />
            <div className="youtube-mask top" />
            <div className="youtube-mask bottom" />
            <div className={videoRevealed ? 'video-privacy-cover hidden' : 'video-privacy-cover'} />
            <div className="video-click-shield" aria-hidden="true" />
            {loading && <div className="video-overlay">Loading daily movie</div>}
            {error && (
              <div className="video-overlay error-state">
                <Clapperboard size={34} />
                <strong>No daily movie yet</strong>
                <span>{error}</span>
                <button className="sync-button" onClick={syncPopularMovies} disabled={syncing}>
                  <RotateCw size={17} />
                  <span>{syncing ? 'Syncing' : 'Sync movies'}</span>
                </button>
              </div>
            )}
          </div>
        </div>

        <aside className="game-panel">
          <div className="game-topbar">
            <button className="icon-button" onClick={() => fetchChallenge(movieIndex)} aria-label="Reload challenge" title="Reload challenge">
              <RotateCw size={18} />
            </button>
            <label className="volume-pill" title="Volume">
              <Volume2 size={16} />
              <input min="0" max="100" type="range" value={volume} onChange={(event) => setVolume(Number(event.target.value))} />
            </label>
          </div>

          <div className="brand-row">
            <div className="brand-mark">CINEGUESS</div>
            <div className="score-box">
              <span>{scorePreview}</span>
              <small>score</small>
            </div>
          </div>

          <div className="meta-row">
            <span>Clip</span>
            <b>{challenge ? `${movieIndex + 1} / ${DAILY_MOVIE_COUNT}` : `0 / ${DAILY_MOVIE_COUNT}`}</b>
            <span>Window</span>
            <b>{currentStage}s</b>
          </div>

          <div className="stage-list">
            {challenge?.stages.map((stage) => {
              const skipped = skippedStages.has(stage);
              const active = stage === currentStage;
              const className = ['stage-card', active ? 'active' : '', skipped ? 'skipped' : '']
                .filter(Boolean)
                .join(' ');
              return (
                <button
                  key={stage}
                  className={className}
                  disabled={!active && !skipped}
                  onClick={() => active && setCurrentStage(stage)}
                >
                  {skipped ? 'Пропущено' : active ? '...' : ''}
                </button>
              );
            })}
            {!challenge && [1, 4, 15, 30, 60].map((stage, index) => (
              <div key={stage} className={index === 0 ? 'stage-card active' : 'stage-card'}>
                {index === 0 ? '...' : ''}
              </div>
            ))}
          </div>

          <div className="playback-area">
            <div className="progress-rail">
              <div className="time-chip" style={{ left: `clamp(32px, ${targetProgress}%, calc(100% - 32px))` }}>
                {currentStage}s
              </div>
              <div className="progress-track">
                <div className="progress-fill" style={{ width: `${timelineProgress}%` }} />
                {challenge?.stages.map((stage) => (
                  <span
                    key={stage}
                    className={stage <= currentStage ? 'progress-tick reached' : 'progress-tick'}
                    style={{ left: `${(stage / 60) * 100}%` }}
                  />
                ))}
                <span className="progress-target" style={{ left: `${targetProgress}%` }} />
              </div>
            </div>
            <div className="time-row">
              <span>{formatTime(playbackElapsed)}</span>
              <span>/</span>
              <span>{formatTime(currentStage)}</span>
            </div>
          </div>

          <div className="controls">
            <button className="play-button" onClick={isPlaying ? pauseFragment : playFragment} disabled={!challenge} aria-label={isPlaying ? 'Pause' : 'Play'}>
              {isPlaying ? <Pause size={28} /> : <Play size={32} fill="currentColor" />}
            </button>
            <button className="skip-button" onClick={nextStage} disabled={!challenge || currentStage === 60} title="Next stage">
              <SkipForward size={20} />
            </button>
          </div>

          <form className="answer-form" onSubmit={submitAnswer}>
            <input value={username} onChange={(event) => setUsername(event.target.value)} placeholder="username" />
            <input value={answer} onChange={(event) => setAnswer(event.target.value)} placeholder="movie title" />
            <button className="submit-button" disabled={!challenge || !answer.trim() || Boolean(outcomeModal)}>Submit</button>
          </form>
          <div className="attempts-counter">
            <span>Attempts left</span>
            <b>{Math.max(MAX_ATTEMPTS - attemptsUsed, 0)} / {MAX_ATTEMPTS}</b>
          </div>

          {syncMessage && <div className="sync-message">{syncMessage}</div>}

          {result && (
            <div className={result.correct ? 'result success' : 'result miss'}>
              <strong>{result.correct ? 'Correct' : 'Incorrect'}</strong>
              <span>{result.correct || result.movieTitle ? result.movieTitle : 'Wrong title'}</span>
              {(result.correct || result.score > 0) && <b>{result.score} pts</b>}
            </div>
          )}

          <div className="leaderboard">
            <div className="leaderboard-title">
              <Trophy size={17} />
              <span>Daily Top</span>
            </div>
            {leaderboard.length === 0 ? (
              <div className="empty-row">No attempts</div>
            ) : (
              leaderboard.map((entry, index) => (
                <div className="leaderboard-row" key={entry.username}>
                  <span>{index + 1}</span>
                  <strong>{entry.username}</strong>
                  <b>{entry.score}</b>
                </div>
              ))
            )}
          </div>
        </aside>
      </section>

      {outcomeModal && (
        <div className="modal-backdrop">
          <div className={`outcome-modal ${outcomeModal.kind}`}>
            <div className="modal-poster">
              {outcomeModal.posterUrl ? (
                <img src={outcomeModal.posterUrl} alt="" />
              ) : (
                <Clapperboard size={42} />
              )}
            </div>
            <div className="modal-copy">
              <span>{outcomeModal.kind === 'correct' ? 'Correct' : 'Attempts ended'}</span>
              <h2>{outcomeModal.kind === 'correct' ? 'You guessed it' : 'You did not guess'}</h2>
              <strong>{outcomeModal.movieTitle}</strong>
              <p>TMDB rating: {outcomeModal.tmdbRating == null ? 'N/A' : outcomeModal.tmdbRating.toFixed(1)}</p>
              <button className="modal-next-button" onClick={goToNextMovie}>
                {movieIndex < DAILY_MOVIE_COUNT - 1 ? 'Next movie' : 'Finish'}
              </button>
            </div>
          </div>
        </div>
      )}
    </main>
  );
}

createRoot(document.getElementById('root')!).render(<App />);
