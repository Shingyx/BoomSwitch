package com.github.shingyx.boomswitch.ui

import androidx.annotation.StringRes
import com.github.shingyx.boomswitch.R

enum class SpeakerModel(@StringRes val modelStringResId: Int) {
  BOOM_4(R.string.speaker_boom_4),
  BOOM_3(R.string.speaker_boom_3),
  BOOM_2(R.string.speaker_boom_2),
  BOOM(R.string.speaker_boom),
  MEGABOOM_4(R.string.speaker_megaboom_4),
  MEGABOOM_3(R.string.speaker_megaboom_3),
  MEGABOOM(R.string.speaker_megaboom),
  WONDERBOOM_PLAY(R.string.speaker_wonderboom_play),
  WONDERBOOM_4(R.string.speaker_wonderboom_4),
  WONDERBOOM_3(R.string.speaker_wonderboom_3),
  WONDERBOOM_2(R.string.speaker_wonderboom_2),
  WONDERBOOM(R.string.speaker_wonderboom),
  EVERBOOM(R.string.speaker_everboom),
  EPICBOOM(R.string.speaker_epicboom),
  HYPERBOOM(R.string.speaker_hyperboom),
  BLAST(R.string.speaker_blast),
  MEGABLAST(R.string.speaker_megablast),
  MINIROLL(R.string.speaker_miniroll),
  ROLL_2(R.string.speaker_roll_2),
  ROLL(R.string.speaker_roll),
  SOMETHING_ELSE(R.string.speaker_something_else),
}
