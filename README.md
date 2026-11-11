# Egg Music

Music player lokal untuk Android. Dibangun dengan Jetpack Compose, Media3 ExoPlayer, dan Room.

## Fitur

### Library
- Import folder rekursif via SAF (Storage Access Framework)
- Import file satu-satu
- Playlist per folder, rename, hapus
- Metadata otomatis: judul, artist, album, durasi
- Cover art dari tag embedded atau `cover.jpg`/`folder.jpg` di folder
- Deteksi duplikat, skip file invalid
- Pencarian judul / artist

### Player
- Play, pause, prev, next, seek, volume, mute
- Shuffle & repeat (off, all, one) — sinkron dengan media notification
- Queue-based playback (Media3 `setMediaItems`)
- Media Session: kontrol dari notification, lockscreen, headset
- Background playback via `MediaSessionService`
- Sleep timer (5, 10, 15, 30, 45, 60 menit)
- Now Playing bottom sheet (tap cover di player bar)
- Persist state: volume, shuffle, repeat

### Equalizer
- 5-band native `AudioEffect` Equalizer
- Preset: Flat, Bass Boost, Vocal, Rock, Pop, Electronic
- Tersimpan antar restart

### Lirik
- **Lokal (offline)**: file `.lrc`/`.txt` sidecar di folder lagu, atau tag embedded di audio
- **Online**: LRCLIB (synced + plain), lyrics.ovh (plain), KuGou (synced)
- **Fallback fuzzy**: LRCLIB search dengan scorer (judul + artist + durasi)
- **Manual**: tempel/ketik sendiri, tersimpan per track
- **Sinkron**: highlight baris aktif + auto-scroll
- **Deteksi instrumental**: judul `(Instrumental)`/`Off Vocal` otomatis ditandai
- **Ignore marker**: tandai track tanpa lirik, un-ignore kapan saja
- **Priority source**: local dulu atau embedded dulu (setting)
- **Stretch timestamp**: sesuaikan `[length:]` tag ke durasi aktual
- **Not-found cache**: 6 jam, hindari request berulang
- **Terjemahan**: Google Translate / MyMemory, mode Indo / Inggris
- **Cari manual**: search LRCLIB dari panel lirik

### UI
- Dark theme, warna accent hijau
- Layout responsif: sidebar di tablet/landscape, tab di HP
- Player bar 2 baris: info + volume, kontrol + seek
- Chip bar 2 grup: sumber lirik + provider terjemahan (bisa disembunyikan)
- Tips screen dengan dokumentasi in-app

## Teknologi

- **Kotlin** + **Jetpack Compose** (Material3)
- **Media3 ExoPlayer** untuk playback + `MediaSessionService`
- **Room** untuk database (tracks, playlists, lyrics cache, manual lyrics, translations)
- **Coil** untuk image loading
- **OkHttp** + **kotlinx.serialization** untuk networking
- **Jaudiotagger** untuk baca tag embedded
- **DocumentFile** (SAF) untuk akses storage tanpa permission

## Build

```bash
./gradlew :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## Requirements

- Android 8.0+ (API 26)
- Storage permission via SAF (tidak butuh READ_EXTERNAL_STORAGE di Android 10+)

## Struktur

```
app/src/main/java/com/eggs/musicplayer/
├── data/          Room DB, DAOs, FolderScanner, Settings
├── lyrics/        Repositories: LRCLIB, lyrics.ovh, KuGou, local, embedded
├── model/         Entities (Track, Playlist, LyricsCache, dll)
├── player/        PlayerController, PlaybackService, EqualizerController
├── ui/            Compose screens & components
│   ├── components/  PlayerBar, LyricsPanel, NowPlayingScreen, dll
│   └── theme/       Colors, Theme
└── MainViewModel.kt
```

## Lisensi

Personal project. Tidak untuk redistribusi komersial.
