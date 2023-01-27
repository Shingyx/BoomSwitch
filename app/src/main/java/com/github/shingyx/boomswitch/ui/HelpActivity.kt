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
        // TODO actually make it a string res
        val helpStringRes = when (speakerModel) {
            SpeakerModel.BOOM_3,
            SpeakerModel.BOOM_2,
            SpeakerModel.BOOM,
            SpeakerModel.MEGABOOM_3,
            SpeakerModel.MEGABOOM -> {
                "supported but may need to update firmware via BOOM app"
            }
            SpeakerModel.WONDERBOOM_3,
            SpeakerModel.WONDERBOOM_2,
            SpeakerModel.WONDERBOOM -> {
                "not supported. speaker doesn't support it. no app available"
            }
            SpeakerModel.HYPERBOOM -> {
                "not tested, use BOOM app if any issues"
            }
            SpeakerModel.BLAST,
            SpeakerModel.MEGABLAST -> {
                "not tested, use BLAST app if any issues"
            }
            SpeakerModel.ROLL_2,
            SpeakerModel.ROLL -> {
                "not tested, use ROLL app if any issues"
            }
            SpeakerModel.SOMETHING_ELSE -> {
                "not supported. not a UE speaker"
            }
        }
        binding.helpText.setText(helpStringRes)
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
