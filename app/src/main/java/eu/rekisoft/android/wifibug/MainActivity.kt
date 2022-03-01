package eu.rekisoft.android.wifibug

import android.annotation.SuppressLint
import android.content.Context
import android.net.*
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import eu.rekisoft.android.wifibug.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private val connectivityManager by lazy {
        applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
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
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                log("onAvailable($network)")
                // To make sure that requests don't go over mobile data
                connectivityManager.bindProcessToNetwork(network)
            }

            override fun onLosing(network: Network, maxMsToLive: Int) {
                log("onAvailable($network, $maxMsToLive)")
            }

            override fun onLost(network: Network) {
                println("onLost($network)")
                // This is to stop the looping request for OnePlus & Xiaomi models
                connectivityManager.bindProcessToNetwork(null)
                connectivityManager.unregisterNetworkCallback(this)
            }

            override fun onUnavailable() {
                log("onUnavailable()")
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                log("onCapabilitiesChanged($network, $networkCapabilities)")
            }

            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                log("onLinkPropertiesChanged($network, $linkProperties)")
            }

            override fun onBlockedStatusChanged(network: Network, blocked: Boolean) {
                log("onBlockedStatusChanged($network, $blocked)")
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val wifiNetworkSpecifier = WifiNetworkSpecifier.Builder().apply {
                setSsid(ssid)
                if (password.isNotEmpty()) {
                    setWpa2Passphrase(password)
                }
            }.build()

            val networkRequest = NetworkRequest.Builder().apply {
                addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                if (binding.useCapabilities.isChecked) {
                    addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                }
                setNetworkSpecifier(wifiNetworkSpecifier)
            }.build()

            connectivityManager.requestNetwork(networkRequest, networkCallback)
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