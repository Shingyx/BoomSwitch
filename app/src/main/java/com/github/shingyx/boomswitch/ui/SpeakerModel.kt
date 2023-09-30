package com.github.shingyx.boomswitch.ui

import androidx.annotation.StringRes
import com.github.shingyx.boomswitch.R

enum class SpeakerModel(@StringRes val modelStringResId: Int) {
    BOOM_3(R.string.speaker_boom_3),
    BOOM_2(R.string.speaker_boom_2),
    BOOM(R.string.speaker_boom),
    MEGABOOM_3(R.string.speaker_megaboom_3),
    MEGABOOM(R.string.speaker_megaboom),
    WONDERBOOM_3(R.string.speaker_wonderboom_3),
    WONDERBOOM_2(R.string.speaker_wonderboom_2),
    WONDERBOOM(R.string.speaker_wonderboom),
    EPICBOOM(R.string.speaker_epicboom),
    HYPERBOOM(R.string.speaker_hyperboom),
    BLAST(R.string.speaker_blast),
    MEGABLAST(R.string.speaker_megablast),
    ROLL_2(R.string.speaker_roll_2),
    ROLL(R.string.speaker_roll),
    SOMETHING_ELSE(R.string.speaker_something_else),
}
