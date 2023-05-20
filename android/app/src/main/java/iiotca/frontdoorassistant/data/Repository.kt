package iiotca.frontdoorassistant.data

import iiotca.frontdoorassistant.App
import iiotca.frontdoorassistant.R

object Repository {
    var token = ""
        get() {
            if (field != "") return field

            val pref = App.getSharedPreferences(App.getString(R.string.preference_file_key))
            field = pref.getString(App.getString(R.string.preference_token), "")!!

            return field
        }
        set(value) {
            field = value

            val pref = App.getSharedPreferences(App.getString(R.string.preference_file_key))
            with(pref.edit()) {
                putString(App.getString(R.string.preference_token), value)
                apply()
            }
        }
    var refreshToken = ""
        get() {
            if (field != "") return field

            val pref = App.getSharedPreferences(App.getString(R.string.preference_file_key))
            field = pref.getString(App.getString(R.string.preference_refresh_token), "")!!

            return field
        }
        set(value) {
            field = value

            val pref = App.getSharedPreferences(App.getString(R.string.preference_file_key))
            with(pref.edit()) {
                putString(App.getString(R.string.preference_refresh_token), value)
                apply()
            }
        }
}