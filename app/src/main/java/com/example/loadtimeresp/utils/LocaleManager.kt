package com.example.loadtimeresp.utils

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.core.view.ViewCompat
import java.util.*

object LocaleManager {
    
    fun setLocale(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        
        // Set layout direction for RTL languages
        if (languageCode == "ar") {
            config.setLayoutDirection(locale)
        }
        
        return context.createConfigurationContext(config)
    }
    
    fun applyLocale(activity: Activity, languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        
        val config = Configuration(activity.resources.configuration)
        config.setLocale(locale)
        
        // Set layout direction for RTL languages
        if (languageCode == "ar") {
            config.setLayoutDirection(locale)
        }
        
        activity.resources.updateConfiguration(config, activity.resources.displayMetrics)
        
        // Force RTL layout direction for Arabic
        if (languageCode == "ar") {
            activity.window.decorView.layoutDirection = android.view.View.LAYOUT_DIRECTION_RTL
        } else {
            activity.window.decorView.layoutDirection = android.view.View.LAYOUT_DIRECTION_LTR
        }
    }
    
    fun isRTL(languageCode: String): Boolean {
        return languageCode == "ar"
    }
}
