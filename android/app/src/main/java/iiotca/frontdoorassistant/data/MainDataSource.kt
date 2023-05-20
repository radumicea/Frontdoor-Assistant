package iiotca.frontdoorassistant.data

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.authentication
import iiotca.frontdoorassistant.BuildConfig
import iiotca.frontdoorassistant.data.dto.Location
import org.json.JSONObject

object MainDataSource {
    private const val protocol = BuildConfig.PROTOCOL
    private const val address = BuildConfig.ADDRESS

    fun setLocation(location: Location): Result<Nothing?> {
        val json = JSONObject()
        json.put("latitude", location.latitude).put("longitude", location.longitude)

        val (_, response, result) = Fuel.post("$protocol://$address${BuildConfig.SET_LOCATION_ROUTE}")
            .authentication().bearer(Repository.token)
            .header(mapOf("Content-Type" to "application/json")).body(json.toString()).response()

        if (result.component2() == null) {
            return Result.Success(null)
        }

        return DataSourceHelper.handleError(response.statusCode, location, ::setLocation)
    }
}