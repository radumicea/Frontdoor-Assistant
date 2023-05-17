package iiotca.frontdoorassistant.data

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.authentication
import com.google.android.gms.tasks.Tasks
import com.google.firebase.messaging.FirebaseMessaging
import iiotca.frontdoorassistant.App
import iiotca.frontdoorassistant.BuildConfig
import iiotca.frontdoorassistant.R
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object AuthenticateDataSource {

    private const val protocol = BuildConfig.PROTOCOL
    private const val address = BuildConfig.ADDRESS

    fun login(userName: String, password: String): Result<Nothing?> {
        val firebaseToken = try {
            Tasks.await(FirebaseMessaging.getInstance().token, 5, TimeUnit.SECONDS)
        } catch (e: Exception) {
            null
        }

        val json = JSONObject()
        json.put("userName", userName).put("password", password).put("firebaseToken", firebaseToken)

        val (_, response, result) = Fuel.post("$protocol://$address${BuildConfig.LOGIN_ROUTE}")
            .header(mapOf("Content-Type" to "application/json")).body(json.toString())
            .responseString()

        val (str, error) = result

        return if (error != null) {
            Result.Error(response.statusCode)
        } else {
            val jsonRes = JSONObject(str!!)
            val token = jsonRes.getString("token")
            val refreshToken = jsonRes.getString("refreshToken")

            with(App.getSharedPreferences(App.getString(R.string.preference_file_key)).edit()) {
                putString(App.getString(R.string.preference_token), token)
                putString(App.getString(R.string.preference_refresh_token), refreshToken)
                apply()
            }

            Repository.token = token
            Repository.refreshToken = refreshToken

            Result.Success(null)
        }
    }

    fun changePassword(
        userName: String,
        oldPassword: String,
        newPassword: String
    ): Result<Nothing?> {
        val json = JSONObject()
        json.put("userName", userName).put("oldPassword", oldPassword)
            .put("newPassword", newPassword)

        val (_, response, result) = Fuel.post("$protocol://$address${BuildConfig.CHANGE_PASSWORD_ROUTE}")
            .header(mapOf("Content-Type" to "application/json")).body(json.toString())
            .response()

        val (_, error) = result

        return if (error == null) {
            Result.Success(null)
        } else {
            return Result.Error(response.statusCode)
        }
    }

    fun logout(): Result<Nothing?> {
        val (_, response, result) = Fuel.post("$protocol://$address${BuildConfig.LOGOUT_ROUTE}")
            .authentication().bearer(Repository.token!!).response()

        return if (result.component2() == null || response.statusCode == 401) {
            Result.Success(null)
        } else {
            Result.Error(response.statusCode)
        }
    }
}
