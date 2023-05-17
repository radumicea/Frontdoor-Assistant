package iiotca.frontdoorassistant

import android.content.Context
import androidx.startup.Initializer
import com.github.kittinunf.fuel.core.FuelManager

class AppInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        FuelManager.instance.baseHeaders = mapOf("Api-Key" to BuildConfig.API_KEY)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}