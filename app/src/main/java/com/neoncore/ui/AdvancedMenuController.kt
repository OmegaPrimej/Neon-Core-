
package com.neoncore.ui

import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import com.neoncore.R
import com.neoncore.alexa.AlexaController
import com.neoncore.voice.Assistant
import com.neoncore.voice.NeonStyle

class AdvancedMenuController(
    private val activity: AppCompatActivity,
    private val alexa: AlexaController
) {

    fun show(anchor: View) {
        val popup = PopupMenu(activity, anchor)
        popup.menuInflater.inflate(R.menu.advanced_menu, popup.menu)
        syncChecks(popup)
        popup.setOnMenuItemClickListener { item -> handle(item) }
        popup.show()
    }

    private fun syncChecks(popup: PopupMenu) {
        val menu = popup.menu
        // Alexa enabled
        menu.findItem(R.id.action_alexa_enable)?.isChecked = alexa.isEnabled
        // Assistant
        menu.findItem(R.id.action_assistant_alexa)?.isChecked = alexa.active == Assistant.ALEXA
        menu.findItem(R.id.action_assistant_google)?.isChecked = alexa.active == Assistant.GOOGLE
        menu.findItem(R.id.action_assistant_custom)?.isChecked = alexa.active == Assistant.NEON
        // Style
        menu.findItem(R.id.action_style_neutral)?.isChecked = alexa.getStyle() == NeonStyle.NEUTRAL
        menu.findItem(R.id.action_style_friendly)?.isChecked = alexa.getStyle() == NeonStyle.FRIENDLY
        menu.findItem(R.id.action_style_warm)?.isChecked = alexa.getStyle() == NeonStyle.WARM
        menu.findItem(R.id.action_style_energetic)?.isChecked = alexa.getStyle() == NeonStyle.ENERGETIC
    }

    private fun handle(item: MenuItem): Boolean {
        return when (item.itemId) {
            // Alexa
            R.id.action_alexa_enable -> {
                item.isChecked = !item.isChecked
                alexa.setEnabled(item.isChecked)
                toast("Alexa ${if (item.isChecked) "enabled" else "disabled"}")
                true
            }
            R.id.action_alexa_login -> { alexa.startLogin(); true }
            R.id.action_alexa_logout -> { alexa.logout(); toast("Logged out"); true }
            R.id.action_alexa_push_to_talk -> { alexa.startPushToTalk(); true }
            R.id.action_alexa_status -> { toast(alexa.statusMessage()); true }

            // Assistant Switching - uses arbitrator
            R.id.action_assistant_alexa -> {
                alexa.setAssistant(Assistant.ALEXA)
                toast("Active -> Alexa")
                true
            }
            R.id.action_assistant_google -> {
                alexa.setAssistant(Assistant.GOOGLE)
                toast("Active -> Google")
                true
            }
            R.id.action_assistant_custom -> {
                alexa.setAssistant(Assistant.NEON)
                toast("Active -> Neon")
                true
            }

            // Voice Style - clean, certifiable
            R.id.action_style_neutral -> { alexa.setStyle(NeonStyle.NEUTRAL); toast("Style -> Neutral"); true }
            R.id.action_style_friendly -> { alexa.setStyle(NeonStyle.FRIENDLY); toast("Style -> Friendly"); true }
            R.id.action_style_warm -> { alexa.setStyle(NeonStyle.WARM); toast("Style -> Warm"); true }
            R.id.action_style_energetic -> { alexa.setStyle(NeonStyle.ENERGETIC); toast("Style -> Energetic"); true }

            // Dev
            R.id.action_view_logs -> { toast(alexa.statusMessage()); true }
            R.id.action_clear_cache -> { activity.cacheDir.deleteRecursively(); toast("Cache cleared"); true }
            R.id.action_reset_settings -> { alexa.reset(); toast("Settings reset"); true }
            R.id.action_about -> { toast("NEON CORE — Arbitration Active | Fire TV Ready"); true }
            R.id.action_exit -> { activity.finishAffinity(); true }

            else -> false
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
    }
}
