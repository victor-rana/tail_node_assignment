package com.example.tail.node.db

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager

class Pref(private val context: Context?) {
    var preferences: SharedPreferences

    var userName: String?
        get() {
            return preferences.getString(USER_NAME, null)
        }
        set(interval) {
            preferences.edit().putString(USER_NAME, interval)
                .apply()
        }

    var userPhone: String?
        get() {
            return preferences.getString(USER_PHONE, null)
        }
        set(interval) {
            preferences.edit().putString(USER_PHONE, interval)
                .apply()
        }

    var isLoggedIn: Boolean?
        get() {
            return preferences.getBoolean(IS_LOGGED_IN, false)
        }
        set(interval) {
            preferences.edit().putBoolean(IS_LOGGED_IN, interval!!)
                .apply()
        }


    fun clearPref() {
        preferences.edit().clear().apply()
    }

    companion object {
        private var sInstance: Pref? = null
        private val USER_NAME: String = "user_name"
        private val USER_PHONE: String = "user_phone"
        private val IS_LOGGED_IN: String = "is_logged_in"

        @Synchronized
        fun initializeInstance(context: Context?) {
            if (sInstance == null) {
                sInstance = Pref(context)
            }
        }

        @get:Synchronized
        val instance: Pref?
            get() {
                if (sInstance == null) {
                    throw IllegalStateException(
                        Pref::class.java.simpleName +
                                " is not initialized, call initializeInstance(..) method first."
                    )
                }
                return sInstance
            }
    }

    init {
        preferences = PreferenceManager.getDefaultSharedPreferences(context)
    }
}
