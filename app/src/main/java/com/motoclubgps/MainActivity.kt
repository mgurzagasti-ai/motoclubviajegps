package com.motoclubgps

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.motoclubgps.ui.MotoClubGpsApp
import com.motoclubgps.ui.screen.EmailConfirmedScreen
import com.motoclubgps.ui.screen.ResetPasswordScreen
import com.motoclubgps.ui.theme.MotoClubGpsTheme
import java.net.URLDecoder

class MainActivity : ComponentActivity() {
    private var showEmailConfirmed by mutableStateOf(false)
    private var recoveryAccessToken by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        setContent {
            MotoClubGpsTheme {
                if (recoveryAccessToken != null) {
                    ResetPasswordScreen(
                        recoveryAccessToken = recoveryAccessToken.orEmpty(),
                        onComplete = { recoveryAccessToken = null },
                    )
                } else if (showEmailConfirmed) {
                    EmailConfirmedScreen(
                        onContinue = { showEmailConfirmed = false },
                    )
                } else {
                    MotoClubGpsApp()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val uri = intent?.data
        showEmailConfirmed = uri?.scheme == "motoclubgps" && uri.host == "auth-confirmed"
        recoveryAccessToken = if (uri?.scheme == "motoclubgps" && uri.host == "reset-password") {
            parseFragment(uri.fragment)["access_token"].orEmpty()
        } else {
            null
        }
    }

    private fun parseFragment(fragment: String?): Map<String, String> {
        return fragment.orEmpty()
            .split("&")
            .mapNotNull { item ->
                val parts = item.split("=", limit = 2)
                if (parts.size != 2) return@mapNotNull null
                URLDecoder.decode(parts[0], Charsets.UTF_8.name()) to
                    URLDecoder.decode(parts[1], Charsets.UTF_8.name())
            }
            .toMap()
    }
}
