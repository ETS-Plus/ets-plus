package app.etsprivate.auth

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom

data class TokenSet(
    val accessToken: String,
    val refreshToken: String?,
    val idToken: String?,
    val email: String?,
)

data class OAuthConfig(
    val clientId: String,
    val redirectUri: String,
    val redirectPrefix: String,
    val tenant: String,
    val scopes: List<String>,
)

class OAuthException(message: String, val error: String? = null) : Exception(message) {
    val requiresReauth: Boolean get() = error == "invalid_grant"
}

object MicrosoftOAuth {
    val MAIL = OAuthConfig(
        clientId = "e6f8716e-299d-4ed9-bbf3-453f192f44e5",
        redirectUri = "msauth://net.thunderbird.android/S9nqeF27sTJcEfaInpC+DHzHuCY=",
        redirectPrefix = "msauth://net.thunderbird.android",
        tenant = "common",
        scopes = listOf(
            "openid", "profile", "email", "offline_access",
            "https://outlook.office.com/IMAP.AccessAsUser.All",
            "https://outlook.office.com/SMTP.Send",
        ),
    )

    val ETS = OAuthConfig(
        clientId = "d4e3e5f6-4be1-4d08-a16f-a94949791431",
        redirectUri = "msauth://ca.etsmtl.applets.etsmobile/pSxfUKGVg0pBh+aW9lzfW1VGvyQ=",
        redirectPrefix = "msauth://ca.etsmtl.applets.etsmobile",
        tenant = "70aae3b7-9f3b-484d-8f95-49e8fbb783c0",
        scopes = listOf("openid", "profile", "offline_access", "api://etsmobileapi/access_as_user"),
    )

    private fun authEndpoint(tenant: String) = "https://login.microsoftonline.com/$tenant/oauth2/v2.0/authorize"
    private fun tokenEndpoint(tenant: String) = "https://login.microsoftonline.com/$tenant/oauth2/v2.0/token"

    private val http = OkHttpClient()

    fun newCodeVerifier(): String {
        val bytes = ByteArray(64)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    private fun codeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    fun authorizeUrl(config: OAuthConfig, codeVerifier: String, loginHint: String? = null): String {
        val params = buildList {
            add("client_id" to config.clientId)
            add("response_type" to "code")
            add("redirect_uri" to config.redirectUri)
            add("response_mode" to "query")
            add("scope" to config.scopes.joinToString(" "))
            add("code_challenge" to codeChallenge(codeVerifier))
            add("code_challenge_method" to "S256")
            add("prompt" to "select_account")
            if (loginHint != null) add("login_hint" to loginHint)
        }
        return authEndpoint(config.tenant) + "?" + params.joinToString("&") { (k, v) ->
            "$k=${java.net.URLEncoder.encode(v, "UTF-8")}"
        }
    }

    suspend fun exchangeCode(config: OAuthConfig, code: String, codeVerifier: String): TokenSet =
        withContext(Dispatchers.IO) {
            val form = FormBody.Builder()
                .add("client_id", config.clientId)
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("redirect_uri", config.redirectUri)
                .add("code_verifier", codeVerifier)
                .build()
            tokenRequest(config, form)
        }

    suspend fun refresh(config: OAuthConfig, refreshToken: String): TokenSet = withContext(Dispatchers.IO) {
        val form = FormBody.Builder()
            .add("client_id", config.clientId)
            .add("grant_type", "refresh_token")
            .add("refresh_token", refreshToken)
            .add("scope", config.scopes.joinToString(" "))
            .build()
        tokenRequest(config, form).let { fresh ->

            if (fresh.refreshToken == null) fresh.copy(refreshToken = refreshToken) else fresh
        }
    }

    private fun tokenRequest(config: OAuthConfig, form: FormBody): TokenSet {
        val request = Request.Builder().url(tokenEndpoint(config.tenant)).post(form).build()
        val response = http.newCall(request).execute()
        val body = response.body.string()
        response.close()
        val json = JSONObject(body)
        if (!response.isSuccessful) {
            throw OAuthException(json.optString("error_description", body).take(400), json.optString("error").ifBlank { null })
        }
        val accessToken = json.optString("access_token", "")
        if (accessToken.isEmpty()) throw OAuthException("No access_token in response: ${body.take(400)}")
        val idToken = json.optString("id_token", "").ifBlank { null }
        return TokenSet(
            accessToken = accessToken,
            refreshToken = json.optString("refresh_token", "").ifBlank { null },
            idToken = idToken,
            email = idToken?.let { emailFromIdToken(it) },
        )
    }

    fun emailFromIdToken(idToken: String): String? {
        val parts = idToken.split(".")
        if (parts.size < 2) return null
        return try {
            val payload = Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
            val json = JSONObject(String(payload, Charsets.UTF_8))
            json.optString("email").ifBlank { json.optString("preferred_username").ifBlank { null } }
        } catch (e: Exception) {
            null
        }
    }
}
