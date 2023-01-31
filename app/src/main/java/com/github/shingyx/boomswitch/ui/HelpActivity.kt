package com.github.shingyx.boomswitch.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.shingyx.boomswitch.R
import com.github.shingyx.boomswitch.databinding.ActivityHelpBinding

class HelpActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHelpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHelpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.run {
            setTitle(R.string.help)
            setDisplayHomeAsUpEnabled(true)
        }

        val adapter = SpeakerModelAdapter(this)
        binding.selectSpeakerModel.setAdapter(adapter)
        binding.selectSpeakerModel.onItemClickListener = adapterOnItemClick { position ->
            val speakerModel = adapter.getItem(position)
            binding.selectSpeakerModel.setText(speakerModel.modelStringRes)
            updateHelpText(speakerModel)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun updateHelpText(speakerModel: SpeakerModel) {
        val helpStringRes = when (speakerModel) {
            SpeakerModel.BOOM_3,
            SpeakerModel.BOOM_2,
            SpeakerModel.BOOM,
            SpeakerModel.MEGABOOM_3,
            SpeakerModel.MEGABOOM -> {
                R.string.help_text_supported
            }
            SpeakerModel.WONDERBOOM_3,
            SpeakerModel.WONDERBOOM_2,
            SpeakerModel.WONDERBOOM -> {
                R.string.help_text_not_supported_wonderboom
            }
            SpeakerModel.HYPERBOOM -> {
                R.string.help_text_not_supported_hyperboom
            }
            SpeakerModel.BLAST,
            SpeakerModel.MEGABLAST -> {
                R.string.help_text_not_supported_blast
            }
            SpeakerModel.ROLL_2,
            SpeakerModel.ROLL -> {
                R.string.help_text_not_supported_roll
            }
            SpeakerModel.SOMETHING_ELSE -> {
                R.string.help_text_not_supported_non_ue
            }
        }
        binding.helpText.text = getString(helpStringRes, getString(speakerModel.modelStringRes))
    }

    private fun sendFeedback() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf("shingyx.dev@gmail.com"))
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.send_feedback_email_subject))
            putExtra(Intent.EXTRA_TEXT, getString(R.string.send_feedback_email_body))
        }
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, R.string.error_no_email_client, Toast.LENGTH_LONG).show()
        }
    }
}
