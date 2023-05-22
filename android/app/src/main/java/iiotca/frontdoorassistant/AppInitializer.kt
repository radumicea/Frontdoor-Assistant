package iiotca.frontdoorassistant

import android.content.Context
import androidx.startup.Initializer
import com.github.kittinunf.fuel.core.FuelManager

class AppInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        FuelManager.instance.baseHeaders = mapOf("Api-Key" to BuildConfig.API_KEY)
        FuelManager.instance.timeoutInMillisecond = 15_000
        FuelManager.instance.timeoutReadInMillisecond = 60_000
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}