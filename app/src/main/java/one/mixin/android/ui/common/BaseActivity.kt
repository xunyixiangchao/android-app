package one.mixin.android.ui.common

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import com.uber.autodispose.android.lifecycle.scope
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import java.util.Locale
import javax.inject.Inject
import one.mixin.android.Constants.Account.PREF_LANGUAGE
import one.mixin.android.Constants.Account.PREF_SET_LANGUAGE
import one.mixin.android.Constants.Theme.THEME_AUTO_ID
import one.mixin.android.Constants.Theme.THEME_CURRENT_ID
import one.mixin.android.Constants.Theme.THEME_DEFAULT_ID
import one.mixin.android.Constants.Theme.THEME_NIGHT_ID
import one.mixin.android.R
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.util.SystemUIManager
import org.jetbrains.anko.configuration

@SuppressLint("Registered")
open class BaseActivity : AppCompatActivity(), HasAndroidInjector {

    protected val stopScope = scope(Lifecycle.Event.ON_STOP)

    @Inject
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Any>

    override fun androidInjector() = dispatchingAndroidInjector

    override fun attachBaseContext(context: Context) {
        val setLanguage = context.defaultSharedPreferences.getBoolean(PREF_SET_LANGUAGE, false)
        if (setLanguage) {
            val conf = context.resources.configuration
            val defaultLang = Locale.getDefault().language
            val language = context.defaultSharedPreferences.getString(PREF_LANGUAGE, defaultLang)
                ?: defaultLang
            conf.setLocale(Locale(language))
            super.attachBaseContext(context.createConfigurationContext(conf))
        } else {
            super.attachBaseContext(context)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isNightMode()) {
            setTheme(getNightThemeId())
            SystemUIManager.lightUI(window, false)
        } else {
            setTheme(getDefaultThemeId())
            SystemUIManager.lightUI(window, true)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.navigationBarColor = colorFromAttribute(R.attr.bg_white)
        }
    }

    protected fun isNightMode(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val currentId = defaultSharedPreferences.getInt(
                THEME_CURRENT_ID,
                THEME_AUTO_ID
            )
            return if (currentId == THEME_AUTO_ID) {
                configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
            } else {
                currentId == THEME_NIGHT_ID
            }
        } else {
            defaultSharedPreferences.getInt(
                THEME_CURRENT_ID,
                THEME_DEFAULT_ID
            ) == THEME_NIGHT_ID
        }
    }

    open fun getNightThemeId(): Int {
        return R.style.AppTheme_Night_NoActionBar
    }

    open fun getDefaultThemeId(): Int {
        return R.style.AppTheme_NoActionBar
    }

    override fun onBackPressed() {
        val fragments = supportFragmentManager.fragments
        if (fragments.size > 0) {
            // Make sure there is a BaseFragment handle this event.
            fragments.indices.reversed()
                .map { fragments[it] }
                .filter { it != null && it is BaseFragment && it.onBackPressed() }
                .forEach { _ -> return }
        }
        super.onBackPressed()
    }
}
