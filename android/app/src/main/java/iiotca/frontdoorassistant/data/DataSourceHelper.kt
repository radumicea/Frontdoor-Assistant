package iiotca.frontdoorassistant.data

import com.github.kittinunf.fuel.Fuel
import com.google.android.gms.tasks.Tasks
import com.google.firebase.messaging.FirebaseMessaging
import iiotca.frontdoorassistant.App
import iiotca.frontdoorassistant.BuildConfig
import iiotca.frontdoorassistant.R
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object DataSourceHelper {

    private const val protocol = BuildConfig.PROTOCOL
    private const val address = BuildConfig.ADDRESS

    fun refreshToken(token: String, refreshToken: String): Result<Nothing?> {
        val statusCode = refreshTokenInternal(token, refreshToken)
        return if (statusCode == 0) {
            Result.Success(null)
        } else {
            Result.Error(statusCode)
        }
    }

    fun <RT : Any?> handleError(code: Int, func: () -> Result<RT>): Result<RT> {
        return if (code == 401) {
            val token = Repository.token ?: App.getString(R.string.preference_token)
            val refreshToken =
                Repository.refreshToken ?: App.getString(R.string.preference_refresh_token)
            val statusCode = refreshTokenInternal(token, refreshToken)
            if (statusCode != 0) {
                Result.Error(statusCode)
            } else {
                func()
            }
        } else {
            Result.Error(code)
        }
    }

    fun <RT : Any?, PT> handleError(
        code: Int,
        arg: PT,
        func: (input: PT) -> Result<RT>
    ): Result<RT> {
        return if (code == 401) {
            val token = Repository.token ?: App.getString(R.string.preference_token)
            val refreshToken =
                Repository.refreshToken ?: App.getString(R.string.preference_refresh_token)
            val statusCode = refreshTokenInternal(token, refreshToken)
            if (statusCode != 0) {
                Result.Error(statusCode)
            } else {
                func(arg)
            }
        } else {
            Result.Error(code)
        }
    }

    private fun refreshTokenInternal(token: String, refreshToken: String): Int {
        val firebaseToken = try {
            Tasks.await(FirebaseMessaging.getInstance().token, 5, TimeUnit.SECONDS)
        } catch (e: Exception) {
            null
        }

        val json = JSONObject()
        json.put("token", token).put("refreshToken", refreshToken)
            .put("firebaseToken", firebaseToken)

        val (_, response, result) = Fuel.post("$protocol://$address${BuildConfig.REFRESH_TOKEN_ROUTE}")
            .header(mapOf("Content-Type" to "application/json")).body(json.toString())
            .responseString()

        val (str, error) = result

        if (error != null) {
            return response.statusCode
        } else {
            val jsonRes = JSONObject(str!!)
            Repository.token = jsonRes.getString("token")
            Repository.refreshToken = jsonRes.getString("refreshToken")

            with(App.getSharedPreferences(App.getString(R.string.preference_file_key)).edit()) {
                putString(
                    App.getString(R.string.preference_token),
                    Repository.token
                )
                putString(
                    App.getString(R.string.preference_refresh_token),
                    Repository.refreshToken
                )
                apply()
            }
        }

        return 0
    }
}