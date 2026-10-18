package app.etsprivate.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

object TokenStore {
    var revision by androidx.compose.runtime.mutableIntStateOf(0)
        private set

    private fun prefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        "oauth_tokens",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun load(context: Context, account: String): TokenSet? {
        app.etsprivate.demo.Demo.source?.let { return TokenSet("demo", "demo", null, it.email) }
        val p = prefs(context)
        val accessToken = p.getString("$account.access", null) ?: return null
        return TokenSet(
            accessToken = accessToken,
            refreshToken = p.getString("$account.refresh", null),
            idToken = null,
            email = p.getString("$account.email", null),
        )
    }

    fun save(context: Context, account: String, tokens: TokenSet) {
        prefs(context).edit()
            .putString("$account.access", tokens.accessToken)
            .apply {
                tokens.refreshToken?.let { putString("$account.refresh", it) }
                tokens.email?.let { putString("$account.email", it) }
            }
            .apply()
        revision++
    }

    fun changed() {
        revision++
    }

    fun clear(context: Context, account: String) {
        app.etsprivate.demo.Demo.stop(context)
        prefs(context).edit()
            .remove("$account.access")
            .remove("$account.refresh")
            .remove("$account.email")
            .apply()
        revision++
    }
}
