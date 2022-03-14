package eu.rekisoft.android.wifibug

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.*
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.widget.addTextChangedListener
import eu.rekisoft.android.wifibug.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private val wifiManager by lazy {
        applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }
    private lateinit var binding: ActivityMainBinding
    private val tsFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    private val mainHandler = Handler(Looper.getMainLooper())
    private val prefs by lazy { getSharedPreferences("secrets", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this)).apply {
            setContentView(root)
            if (prefs.getBoolean("save", false)) {
                ssid.setText(prefs.getString("ssid", getString(R.string.default_ssid)))
                password.setText(prefs.getString("pwd", getString(R.string.default_password)))
            } else {
                saveData.isChecked = true
            }
            connect.setOnClickListener {
                var isValid = true
                if (ssid.text.isNullOrBlank()) {
                    isValid = false
                    ssidForm.error = "Please enter a valid SSID"
                }
                if (password.text?.length ?: 0 in 1..7) {
                    isValid = false
                    passwordForm.error = "Password must be empty or at least 8 characters"
                }
                if (isValid) {
                    connectToWifi(ssid.text.toString(), password.text.toString())
                }
            }
            ssid.addTextChangedListener {
                ssidForm.error = null
                if (saveData.isChecked) {
                    prefs.edit().putString("ssid", ssid.text.toString()).apply()
                }
            }
            password.addTextChangedListener {
                ssidForm.error = null
                if (saveData.isChecked) {
                    prefs.edit().putString("pwd", password.text.toString()).apply()
                }
            }
            saveData.setOnCheckedChangeListener { _, checked ->
                if (checked) {
                    prefs.edit()
                        .putBoolean("save", true)
                        .putString("ssid", ssid.text.toString())
                        .putString("pwd", password.text.toString())
                        .apply()
                } else {
                    prefs.edit()
                        .putBoolean("save", false)
                        .remove("ssid")
                        .remove("pwd")
                        .apply()
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    fun log(message: String) {
        Log.d("WifiBugLog", message)
        doOnMain {
            binding.log.text = tsFormat.format(Date()) + " $message\n" + binding.log.text
        }
    }

    fun doOnMain(runnable: Runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run()
        } else {
            mainHandler.post(runnable)
        }
    }

    fun connectToWifi(ssid: String, password: String) {
        log("Connecting to $ssid...")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val suggestion = WifiNetworkSuggestion.Builder()
                .setSsid(ssid).apply {
                    if (password.isNotEmpty()) {
                        setWpa2Passphrase(password)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        setMacRandomizationSetting(WifiNetworkSuggestion.RANDOMIZATION_PERSISTENT)
                    }
                    if (binding.useAppInteractionRequired.isChecked) {
                        setIsAppInteractionRequired(true) // Needs location permission
                    }
                }
                .setIsMetered(false)
                .build()

            val status = wifiManager.addNetworkSuggestions(listOf(suggestion))
            val granted = ActivityCompat.checkSelfPermission(this, Manifest.permission.CHANGE_WIFI_STATE) == PackageManager.PERMISSION_GRANTED
            log("CHANGE_WIFI_STATE permission is " + (if(granted) "" else "NOT ") + "granted")
            if (status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
                log("Success!")
            } else {
                val humanReadableError = when(status) {
                    WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_INTERNAL -> "internal"
                    WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_APP_DISALLOWED -> "app disallowed"
                    WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_DUPLICATE -> "add duplicate"
                    WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_EXCEEDS_MAX_PER_APP -> "add exceeds max per app"
                    WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_REMOVE_INVALID -> "remove invalid"
                    WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_NOT_ALLOWED -> "add not allowed"
                    WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_INVALID -> "add invalid"
                    else -> "Unknown state $status"
                }
                log("Failed with error: $humanReadableError")
            }
        } else {
            if (wifiManager.isWifiEnabled) {
                val wifiConfig = WifiConfiguration()
                wifiConfig.SSID = "\"$ssid\""
                wifiConfig.preSharedKey = "\"$password\""
                val networkId = wifiManager.addNetwork(wifiConfig)
                if (networkId != -1) {
                    wifiManager.enableNetwork(networkId, true)
                }
            }
        }
    }
}