package iiotca.frontdoorassistant.data

import com.github.kittinunf.fuel.Fuel
import com.google.android.gms.tasks.Tasks
import com.google.firebase.messaging.FirebaseMessaging
import iiotca.frontdoorassistant.BuildConfig
import iiotca.frontdoorassistant.R
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object DataSourceHelper {

    private const val protocol = BuildConfig.PROTOCOL
    private const val address = BuildConfig.ADDRESS

    fun refreshToken(): Result<Nothing?> {
        return when (refreshTokenInternal()) {
            200 -> {
                Result.Success(null)
            }

            401 -> {
                Result.Error(R.string.session_expired_cannot_refresh)
            }

            else -> {
                Result.Error(R.string.request_failed)
            }
        }
    }

    fun <RT : Any?> handleError(code: Int, func: () -> Result<RT>): Result<RT> {
        return if (code == 401) {
            when (refreshTokenInternal()) {
                200 -> {
                    func()
                }

                401 -> {
                    Result.Error(R.string.session_expired_cannot_refresh)
                }

                else -> {
                    Result.Error(R.string.request_failed)
                }
            }
        } else {
            Result.Error(R.string.request_failed)
        }
    }

    fun <RT : Any?, PT> handleError(
        code: Int, arg: PT, func: (input: PT) -> Result<RT>
    ): Result<RT> {
        return if (code == 401) {
            when (refreshTokenInternal()) {
                200 -> {
                    func(arg)
                }

                401 -> {
                    Result.Error(R.string.session_expired_cannot_refresh)
                }

                else -> {
                    Result.Error(R.string.request_failed)
                }
            }
        } else {
            Result.Error(R.string.request_failed)
        }
    }

    private fun refreshTokenInternal(): Int {
        val firebaseToken = try {
            Tasks.await(FirebaseMessaging.getInstance().token, 5, TimeUnit.SECONDS)
        } catch (e: Exception) {
            null
        }

        val json = JSONObject()
        json.put("token", Repository.token).put("refreshToken", Repository.refreshToken)
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
        }

        return 200
    }
}