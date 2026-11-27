package echo.music.iad1tya.ui.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dagger.hilt.android.EntryPointAccessors
import echo.music.iad1tya.LocalDatabase
import echo.music.iad1tya.LocalPlayerConnection
import echo.music.iad1tya.db.entities.LyricsEntity
import echo.music.iad1tya.lyrics.LyricsEntry
import echo.music.iad1tya.lyrics.LyricsUtils
import echo.music.iad1tya.models.MediaMetadata
import echo.music.iad1tya.ui.component.LyricsLineV2
import echo.music.iad1tya.ui.component.shimmer.ShimmerHost
import echo.music.iad1tya.ui.component.shimmer.TextPlaceholder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

@Composable
fun PlayerSyncedLyricsView(
  mediaMetadata: MediaMetadata?,
  positionProvider: () -> Long,
  modifier: Modifier = Modifier
) {
  val playerConnection = LocalPlayerConnection.current ?: return
  val currentLyrics by playerConnection.currentLyrics.collectAsState(initial = null)
  val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState(initial = false)
  val context = androidx.compose.ui.platform.LocalContext.current
  val database = LocalDatabase.current
  val latestPositionProvider by rememberUpdatedState(positionProvider)

  // Fetch only when this track has no cached lyrics. Keeping the work in this effect means a
  // song change cancels the old request instead of allowing it to update a stale player view.
  LaunchedEffect(mediaMetadata?.id, currentLyrics?.id) {
    val metadata = mediaMetadata ?: return@LaunchedEffect
    if (currentLyrics?.id == metadata.id) return@LaunchedEffect

    delay(350L)
    try {
      val existing = withContext(Dispatchers.IO) {
        database.lyrics(metadata.id).firstOrNull()
      }
      if (existing != null) return@LaunchedEffect

      val entryPoint =
        EntryPointAccessors.fromApplication(
          context.applicationContext,
          echo.music.iad1tya.di.LyricsHelperEntryPoint::class.java
        )
      val lyricsHelper = entryPoint.lyricsHelper()
      val fetchedLyricsWithProvider = withContext(Dispatchers.IO) {
        lyricsHelper.getLyrics(metadata)
      }
      withContext(Dispatchers.IO) {
        database.query {
          upsert(
            LyricsEntity(
              metadata.id,
              fetchedLyricsWithProvider.lyrics?.takeIf { it.isNotBlank() } ?: LyricsEntity.LYRICS_NOT_FOUND,
              fetchedLyricsWithProvider.providerName
            )
          )
        }
      }
    } catch (e: CancellationException) {
      throw e
    } catch (_: Exception) {
      // Lyrics are optional; do not interrupt playback if a provider is unavailable.
    }
  }

  val trackLyrics =
    currentLyrics?.takeIf { it.id == mediaMetadata?.id }?.lyrics?.trim()

  // The compact player only shows timed lyrics. Plain text remains available in the full
  // lyrics screen, but should not masquerade as a synchronized line.
  val lines =
    remember(mediaMetadata?.id, trackLyrics) {
      trackLyrics
        ?.takeIf { lyrics ->
          lyrics.isNotEmpty() &&
            lyrics != LyricsEntity.LYRICS_NOT_FOUND &&
            LyricsUtils.hasSyncedLyrics(lyrics)
        }
        ?.let { lyrics ->
          LyricsUtils.parseLyrics(lyrics).filter { it.text.isNotBlank() }
        } ?: emptyList()
    }

  var currentPosition by remember(mediaMetadata?.id) { mutableLongStateOf(0L) }
  LaunchedEffect(mediaMetadata?.id, isPlaying, lines.isNotEmpty()) {
    currentPosition = latestPositionProvider()
    if (mediaMetadata == null || !isPlaying || lines.isEmpty()) return@LaunchedEffect

    // 20 Hz is sufficient for word karaoke progress and avoids recomposing on every display
    // frame (which can be 60–120 times per second on modern phones).
    while (isActive) {
      currentPosition = latestPositionProvider()
      delay(POSITION_UPDATE_INTERVAL_MS)
    }
  }

  val currentLineIndex = remember(currentPosition, lines) {
    findSyncedLineIndex(lines, currentPosition)
  }
  val currentLine = lines.getOrNull(currentLineIndex)

  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .padding(horizontal = echo.music.iad1tya.constants.PlayerHorizontalPadding),
    contentAlignment = Alignment.CenterStart
  ) {
    when {
      mediaMetadata == null -> Unit
      currentLyrics?.id != mediaMetadata.id -> {
        ShimmerHost(modifier = Modifier.fillMaxWidth()) {
          TextPlaceholder(
            height = 20.dp,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth(0.6f)
          )
        }
      }
      currentLine != null -> {
        LyricsLineV2(
          entry = currentLine,
          isActive = true,
          isPast = false,
          effectivePlaybackPosition = currentPosition,
          expressiveAccent = MaterialTheme.colorScheme.primary,
          inactiveAlpha = 0.82f,
          baseFontSize = 18f,
          lineHeight = 24f,
          showTranslated = false,
          agentAlignment = Alignment.Start,
          agentTextAlign = TextAlign.Start
        )
      }
    }
  }
}

private const val POSITION_UPDATE_INTERVAL_MS = 50L

/**
 * Returns the latest lyric line whose timestamp is not after [positionMs].
 * Binary search keeps lookup inexpensive for long lyrics and selects the last line when
 * multiple entries share the same timestamp.
 */
internal fun findSyncedLineIndex(lines: List<LyricsEntry>, positionMs: Long): Int {
  if (lines.isEmpty()) return -1

  var low = 0
  var high = lines.lastIndex
  var result = -1

  while (low <= high) {
    val middle = low + (high - low) / 2
    if (lines[middle].time <= positionMs) {
      result = middle
      low = middle + 1
    } else {
      high = middle - 1
    }
  }

  return if (result >= 0) result else 0
}
