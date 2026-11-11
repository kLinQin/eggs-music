package com.eggs.musicplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eggs.musicplayer.ui.theme.*
import kotlinx.coroutines.launch

sealed class TipBlock {
    data class Heading(val text: String) : TipBlock()
    data class Paragraph(val text: String) : TipBlock()
    data class Bullet(val text: String) : TipBlock()
    data class Hint(val text: String, val variant: String = "info") : TipBlock()
}

data class TipSection(val id: String, val title: String, val blocks: List<TipBlock>)

private val TIPS: List<TipSection> = listOf(
    TipSection(
        "welcome", "Selamat Datang",
        listOf(
            TipBlock.Heading("Apa itu Egg Music?"),
            TipBlock.Paragraph("Egg Music adalah music player lokal. Semua file musik dibaca langsung dari folder Anda — tidak di-upload ke server mana pun."),
            TipBlock.Heading("Fitur utama"),
            TipBlock.Bullet("Import folder (rekursif) atau file satu-satu"),
            TipBlock.Bullet("Playlist per folder, bisa rename & hapus"),
            TipBlock.Bullet("Player lengkap: play/pause, next/prev, seek, volume, shuffle, repeat"),
            TipBlock.Bullet("Equalizer 5-band (native) dengan preset, tersimpan antar restart"),
            TipBlock.Bullet("Sleep timer, delete track, cover playlist"),
            TipBlock.Bullet("Lirik otomatis: LRCLIB, lyrics.ovh, KuGou"),
            TipBlock.Bullet("Lirik lokal: file .lrc/.txt di folder lagu, atau tag embedded di audio"),
            TipBlock.Bullet("Lirik manual + terjemahan (Google/MyMemory)"),
            TipBlock.Bullet("Deteksi instrumental, tandai \"tidak ada lirik\", priority source"),
            TipBlock.Bullet("Cover art dari tag embedded / cover.jpg di folder"),
            TipBlock.Bullet("Media Session: kontrol dari notifikasi & lockscreen"),
            TipBlock.Hint("Semua data tersimpan lokal di HP. File audio asli tetap di folder Anda.", "info")
        )
    ),
    TipSection(
        "import", "Import Musik",
        listOf(
            TipBlock.Heading("Menambahkan lagu"),
            TipBlock.Paragraph("Ada 2 cara menambahkan musik:"),
            TipBlock.Heading("Import folder"),
            TipBlock.Bullet("Tap tombol Folder di header"),
            TipBlock.Bullet("Pilih folder musik — semua audio di dalamnya (termasuk subfolder) di-import"),
            TipBlock.Bullet("Playlist baru dibuat otomatis dengan nama folder"),
            TipBlock.Heading("Tambah lagu satu-satu"),
            TipBlock.Bullet("Tap tombol Lagu di header"),
            TipBlock.Bullet("Pilih 1 atau lebih file audio — masuk ke playlist Library"),
            TipBlock.Heading("Format yang didukung"),
            TipBlock.Bullet("MP3, FLAC, M4A/AAC, OGG/Opus, WAV"),
            TipBlock.Hint("Metadata (judul, artist, album, cover, durasi) dibaca otomatis. Folder besar butuh beberapa detik.", "info")
        )
    ),
    TipSection(
        "playlist", "Playlist",
        listOf(
            TipBlock.Heading("Mengelola playlist"),
            TipBlock.Paragraph("Sidebar kiri (tablet/landscape) atau tab Playlist (HP) menampilkan daftar playlist."),
            TipBlock.Heading("Library"),
            TipBlock.Paragraph("Library adalah playlist default yang tidak bisa dihapus. Semua lagu yang tidak masuk playlist lain ada di sini."),
            TipBlock.Heading("Rename / hapus"),
            TipBlock.Bullet("Tap nama playlist → tombol pencil / trash muncul"),
            TipBlock.Bullet("Hapus playlist tidak menghapus lagu dari Library"),
            TipBlock.Hint("Lagu di playlist yang dihapus dipindah ke Library, bukan dihapus.", "warning")
        )
    ),
    TipSection(
        "player", "Player",
        listOf(
            TipBlock.Heading("Kontrol pemutaran"),
            TipBlock.Paragraph("Player bar ada di bawah. Di HP 2 baris: info lagu + volume di atas, kontrol utama + seek di bawah."),
            TipBlock.Heading("Kontrol dasar"),
            TipBlock.Bullet("Play/Pause — tombol tengah"),
            TipBlock.Bullet("Prev / Next — pindah lagu"),
            TipBlock.Bullet("Seek bar — lompat ke posisi mana pun"),
            TipBlock.Bullet("Volume — slider kecil di kanan atas; tap ikon speaker untuk mute"),
            TipBlock.Heading("Mute"),
            TipBlock.Bullet("Tap ikon speaker → mute"),
            TipBlock.Bullet("Geser slider volume saat mute → auto-unmute")
        )
    ),
    TipSection(
        "shuffle-repeat", "Shuffle & Repeat",
        listOf(
            TipBlock.Heading("Mode pemutaran"),
            TipBlock.Heading("Shuffle"),
            TipBlock.Bullet("Tap ikon shuffle → hijau. Lagu berikutnya diacak"),
            TipBlock.Bullet("Tap lagi → kembali urut"),
            TipBlock.Heading("Repeat"),
            TipBlock.Bullet("Off (abu) — lagu terakhir habis → berhenti"),
            TipBlock.Bullet("All (hijau) — lagu terakhir → kembali ke pertama, loop"),
            TipBlock.Bullet("One (hijau + 1) — lagu yang sama diulang"),
            TipBlock.Hint("Shuffle + Repeat All = acak terus tanpa berhenti. Favorit untuk playlist besar.", "info")
        )
    ),
    TipSection(
        "equalizer", "Equalizer",
        listOf(
            TipBlock.Heading("Mengatur suara"),
            TipBlock.Paragraph("Tap ikon EQ (slider) di player bar untuk buka equalizer."),
            TipBlock.Heading("5-band EQ"),
            TipBlock.Paragraph("Setiap band mengatur frekuensi berbeda. Jumlah band tergantung perangkat (biasanya 5)."),
            TipBlock.Heading("Preset"),
            TipBlock.Bullet("Flat — semua 0 (netral)"),
            TipBlock.Bullet("Bass Boost — bass ditonjolkan"),
            TipBlock.Bullet("Vocal — vokal lebih jelas"),
            TipBlock.Bullet("Rock — bass + treble"),
            TipBlock.Bullet("Pop — mid + treble"),
            TipBlock.Bullet("Electronic — bass + treble ekstrem"),
            TipBlock.Hint("Boost terlalu tinggi bisa pecah suara (clipping). Turunkan gain atau pilih Flat.", "warning")
        )
    ),
    TipSection(
        "lyrics", "Lirik",
        listOf(
            TipBlock.Heading("Cara kerja"),
            TipBlock.Paragraph("Saat lagu diputar, Egg Music mencari lirik berurutan: local .lrc/.txt → tag embedded → online. Urutan local vs embedded bisa diatur di Settings."),
            TipBlock.Heading("Sumber lokal (offline)"),
            TipBlock.Bullet(".lrc/.txt di folder lagu dengan nama sama (mis. song.mp3 + song.lrc)"),
            TipBlock.Bullet("Tag lirik embedded di file audio (USLT/lyrics tag)"),
            TipBlock.Bullet("Manual — tempel/ketik sendiri, tersimpan per track"),
            TipBlock.Heading("Sumber online"),
            TipBlock.Bullet("LRCLIB — lirik sinkron + plain, dengan fuzzy match fallback"),
            TipBlock.Bullet("lyrics.ovh — plain text saja, katalog luas"),
            TipBlock.Bullet("KuGou — katalog Asia, lirik sinkron"),
            TipBlock.Paragraph("Kalau satu provider gagal, otomatis coba yang lain. Track yang tidak ditemukan di-cache 6 jam agar tidak request berulang."),
            TipBlock.Heading("Fitur lanjutan"),
            TipBlock.Bullet("Instrumental — judul dengan (Instrumental)/Off Vocal otomatis ditandai"),
            TipBlock.Bullet("Tandai tidak ada lirik — tombol block; un-ignore lewat \"Batal tandai\""),
            TipBlock.Bullet("Priority — pilih local dulu atau embedded dulu (Settings)"),
            TipBlock.Bullet("Stretch timestamp — sesuaikan [length:] tag ke durasi aktual (nightcore/sped-up)"),
            TipBlock.Bullet("Hide bar — sembunyikan bar Sumber/Translate di panel lirik"),
            TipBlock.Heading("Terjemahan"),
            TipBlock.Bullet("Plain — lirik asli"),
            TipBlock.Bullet("Indo — terjemahan Indonesia di bawah tiap baris"),
            TipBlock.Bullet("Inggris — terjemahan Inggris"),
            TipBlock.Bullet("Google — kualitas terbaik, rate limit ketat"),
            TipBlock.Bullet("MyMemory — gratis, stabil, fallback otomatis"),
            TipBlock.Hint("Lirik tidak selalu ada — tergantung katalog. Lagu indie/cover sering tidak ketemu.", "info")
        )
    ),
    TipSection(
        "mobile", "Mobile",
        listOf(
            TipBlock.Heading("Pakai Egg Music di HP"),
            TipBlock.Heading("Layout mobile"),
            TipBlock.Bullet("Player bar 2 baris (info+volume atas, kontrol+seek bawah)"),
            TipBlock.Bullet("Tab di main view: Playlist / Lagu / Lirik / Tips"),
            TipBlock.Bullet("Sidebar playlist digantikan tab Playlist"),
            TipBlock.Heading("Background play"),
            TipBlock.Paragraph("Pakai Media Session — musik tetap main walau pindah app atau layar mati. Kontrol di notifikasi / lockscreen.")
        )
    ),
    TipSection(
        "troubleshooting", "Troubleshooting",
        listOf(
            TipBlock.Heading("Masalah umum"),
            TipBlock.Heading("Musik berhenti saat pindah aplikasi"),
            TipBlock.Bullet("Penyebab: pengaturan hemat baterai HP mematikan audio background"),
            TipBlock.Bullet("Solusi: izinkan app berjalan di background / nonaktifkan hemat baterai untuk Egg Music"),
            TipBlock.Heading("Lirik tidak muncul"),
            TipBlock.Bullet("Penyebab: lagu tidak ada di database, atau sumber belum dipilih"),
            TipBlock.Bullet("Solusi: pilih sumber, atau cari manual dengan artist + judul, atau tempel lirik manual"),
            TipBlock.Heading("Terjemahan lambat / error"),
            TipBlock.Bullet("Penyebab: rate limit provider"),
            TipBlock.Bullet("Solusi: otomatis fallback. Coba lagi beberapa menit"),
            TipBlock.Heading("Cover art tidak muncul"),
            TipBlock.Bullet("Penyebab: file tidak punya artwork tertanam & folder tidak punya cover.jpg"),
            TipBlock.Bullet("Solusi: tambahkan cover.jpg / folder.jpg di folder musik, lalu scan ulang")
        )
    ),
    TipSection(
        "faq", "FAQ",
        listOf(
            TipBlock.Heading("Pertanyaan umum"),
            TipBlock.Heading("Apakah file musik di-upload ke server?"),
            TipBlock.Paragraph("Tidak. Semua file dibaca langsung dari folder HP. Tidak ada upload."),
            TipBlock.Heading("Berapa banyak lagu yang bisa disimpan?"),
            TipBlock.Paragraph("Tergantung storage HP. Karena tidak di-copy, praktis sebatas ruang folder musik Anda."),
            TipBlock.Heading("Bisakah pindah device?"),
            TipBlock.Paragraph("File audio harus di-import ulang dari sumber aslinya. Playlist, lirik manual, dan cache lirik tersimpan di database Room lokal — belum ada fitur export/import otomatis."),
            TipBlock.Heading("Equalizer disimpan?"),
            TipBlock.Paragraph("Ya. Preset dan level EQ tersimpan antar restart.")
        )
    ),
    TipSection(
        "about", "Tentang",
        listOf(
            TipBlock.Heading("Tentang Egg Music"),
            TipBlock.Paragraph("Music player lokal untuk Android. Dibangun dengan Jetpack Compose, Media3 ExoPlayer, Room, dan Coil."),
            TipBlock.Heading("Fitur"),
            TipBlock.Bullet("Import folder rekursif dengan metadata otomatis"),
            TipBlock.Bullet("Playlist multi-list"),
            TipBlock.Bullet("Player lengkap"),
            TipBlock.Bullet("Shuffle & repeat"),
            TipBlock.Bullet("Equalizer 5-band native"),
            TipBlock.Bullet("Lirik LRCLIB + lyrics.ovh + manual + terjemahan"),
            TipBlock.Bullet("Media Session (lockscreen + notifikasi)"),
            TipBlock.Bullet("Cover art dari ID3 / folder"),
            TipBlock.Hint("Selamat mendengarkan!", "success")
        )
    )
)

@Composable
fun TipsScreen() {
    var activeId by remember { mutableStateOf(TIPS.first().id) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val sectionStart = remember {
        val map = mutableMapOf<String, Int>()
        var idx = 0
        for (sec in TIPS) {
            map[sec.id] = idx
            idx += 1 + sec.blocks.size + 1
        }
        map
    }

    LaunchedEffect(listState.firstVisibleItemIndex) {
        val first = listState.firstVisibleItemIndex
        var current = TIPS.first().id
        for (sec in TIPS) {
            val start = sectionStart[sec.id] ?: continue
            if (start <= first) current = sec.id else break
        }
        if (current != activeId) activeId = current
    }

    Column(Modifier.fillMaxSize().background(BgElev)) {
        LazyRow(
            modifier = Modifier.fillMaxWidth().background(BgPanel).padding(vertical = 6.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(TIPS) { sec ->
                val active = activeId == sec.id
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (active) Accent else BgActive)
                        .clickable {
                            activeId = sec.id
                            scope.launch {
                                sectionStart[sec.id]?.let { listState.animateScrollToItem(it) }
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(sec.title, color = if (active) Color.Black else TextMute, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        HorizontalDivider(thickness = 1.dp, color = Border)
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            TIPS.forEach { sec ->
                item(key = "h_" + sec.id) {
                    Text(
                        sec.title,
                        color = TextMain,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
                items(sec.blocks.size) { i ->
                    TipBlockView(sec.blocks[i])
                }
                item(key = "sp_" + sec.id) {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(thickness = 1.dp, color = Border)
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun TipBlockView(block: TipBlock) {
    when (block) {
        is TipBlock.Heading -> Text(
            block.text,
            color = TextMain,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        is TipBlock.Paragraph -> Text(
            block.text,
            color = TextMute,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        is TipBlock.Bullet -> Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)
        ) {
            Text("•", color = Accent, fontSize = 13.sp, modifier = Modifier.width(14.dp))
            Text(block.text, color = TextMute, fontSize = 13.sp, lineHeight = 19.sp)
        }
        is TipBlock.Hint -> {
            val bg = when (block.variant) {
                "warning" -> Color(0xFF3B2A1E)
                "success" -> Color(0xFF1E3B25)
                else -> Color(0xFF1E2A3B)
            }
            val border = when (block.variant) {
                "warning" -> Color(0xFFE5A50A)
                "success" -> Color(0xFF4CAF50)
                else -> Color(0xFF4A9EE0)
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(bg)
                    .padding(10.dp)
            ) {
                Text(block.text, color = border, fontSize = 12.sp, lineHeight = 18.sp)
            }
        }
    }
}
