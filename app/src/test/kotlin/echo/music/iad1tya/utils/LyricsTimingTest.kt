package echo.music.iad1tya.utils

import echo.music.iad1tya.lyrics.LyricsEntry
import echo.music.iad1tya.lyrics.LyricsUtils
import echo.music.iad1tya.ui.player.findSyncedLineIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsTimingTest {
  @Test
  fun zeroTimestampLyricsRemainReadableWithoutFollowingPlayback() {
    val lyrics = "[00:00.00]First line\n[00:00.00]Second line"

    assertFalse(LyricsUtils.hasSyncedLyrics(lyrics))
    assertEquals(listOf("First line", "Second line"), LyricsUtils.parseLyrics(lyrics).map { it.text })
  }

  @Test
  fun metadataTagsDoNotTurnPlainLyricsIntoSyncedLyrics() {
    assertFalse(LyricsUtils.hasSyncedLyrics("[ar:Artist]\nFirst line\nSecond line"))
  }

  @Test
  fun realTimingIsRecognizedAfterWhitespaceAndMetadata() {
    assertTrue(LyricsUtils.hasSyncedLyrics("\n[ar:Artist]\n[00:00.00]First line\n[00:12.50]Second line"))
  }

  @Test
  fun compactPlayerSelectsLatestLineAtOrBeforePlaybackPosition() {
    val lines =
      listOf(
        LyricsEntry(time = 1_000L, text = "First"),
        LyricsEntry(time = 2_000L, text = "Second"),
        LyricsEntry(time = 3_000L, text = "Third")
      )

    assertEquals(0, findSyncedLineIndex(lines, 0L))
    assertEquals(0, findSyncedLineIndex(lines, 1_999L))
    assertEquals(1, findSyncedLineIndex(lines, 2_000L))
    assertEquals(2, findSyncedLineIndex(lines, 9_000L))
  }

  @Test
  fun compactPlayerUsesLastLineWhenTimestampsAreEqual() {
    val lines =
      listOf(
        LyricsEntry(time = 1_000L, text = "First"),
        LyricsEntry(time = 1_000L, text = "Second"),
        LyricsEntry(time = 2_000L, text = "Third")
      )

    assertEquals(1, findSyncedLineIndex(lines, 1_000L))
  }

  @Test
  fun compactPlayerReturnsNoIndexForEmptyLyrics() {
    assertEquals(-1, findSyncedLineIndex(emptyList(), 1_000L))
  }
}
