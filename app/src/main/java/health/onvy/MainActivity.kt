package health.onvy

import android.icu.util.Calendar
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import co.tryterra.terra.Terra
import co.tryterra.terra.TerraManager
import co.tryterra.terra.enums.Connections
import co.tryterra.terra.enums.CustomPermissions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private var terraManager: TerraManager? = null
    private lateinit var initButton: Button
    private lateinit var initConnectionButton: Button
    private lateinit var getDailyDataButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initButton = findViewById(R.id.initButton)
        initConnectionButton = findViewById(R.id.initConnectionButton)
        getDailyDataButton = findViewById(R.id.getDailyDataButton)

        initButton.setOnClickListener {
            initTerra()
        }

        initConnectionButton.setOnClickListener {
            initConnection()
        }

        getDailyDataButton.setOnClickListener {
            getDailyData()
        }
    }

    private fun initTerra() {
        Terra.instance(BuildConfig.DEV_ID, BuildConfig.REF_ID, this) {terraManager, terraError ->
            if(terraError != null) {
                Log.e(TAG, "Error: ${terraError.message}")
            } else {
                this.terraManager = terraManager
                Log.d(TAG, "Terra initialized successfully")
            }
        }
    }

    private fun initConnection() {
        if(terraManager == null) {
            Log.d(TAG, "TerraManager is null. Cannot init connection.")
            return
        }

        getToken { token ->
            val customPerm = HashSet<CustomPermissions>();
            customPerm.add(CustomPermissions.STEPS)
            customPerm.add(CustomPermissions.ACTIVE_DURATIONS)
            customPerm.add(CustomPermissions.ACTIVITY_SUMMARY)
            customPerm.add(CustomPermissions.WORKOUT_TYPE)
            customPerm.add(CustomPermissions.LOCATION)
            customPerm.add(CustomPermissions.CALORIES)
            customPerm.add(CustomPermissions.HEART_RATE)
            customPerm.add(CustomPermissions.HEART_RATE_VARIABILITY)
            customPerm.add(CustomPermissions.HEIGHT)
            customPerm.add(CustomPermissions.FLIGHTS_CLIMBED)
            customPerm.add(CustomPermissions.EXERCISE_DISTANCE)
            customPerm.add(CustomPermissions.GENDER)
            customPerm.add(CustomPermissions.DATE_OF_BIRTH)
            customPerm.add(CustomPermissions.BASAL_ENERGY_BURNED)
            customPerm.add(CustomPermissions.RESTING_HEART_RATE)
            customPerm.add(CustomPermissions.BLOOD_GLUCOSE)
            customPerm.add(CustomPermissions.BODY_TEMPERATURE)
            customPerm.add(CustomPermissions.MINDFULNESS)
            customPerm.add(CustomPermissions.SLEEP_ANALYSIS)
            customPerm.add(CustomPermissions.RESTING_HEART_RATE)

            terraManager!!.initConnection(Connections.SAMSUNG, token, this, customPerm, false, null) { sucess, terraError ->
                if (sucess) {
                    Log.d(TAG, "Connection initialized successfully")
                } else {
                    Log.e(TAG, "Error: ${terraError?.message}")
                }
            }
        }
    }

    private fun getDailyData() {
        if(terraManager == null) {
            Log.d(TAG, "TerraManager is null. Cannot get data.")
            return
        }

        val oneDayAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.time
        val oneDayLater = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }.time

        terraManager?.let {
            it.getDaily(Connections.SAMSUNG, oneDayAgo, oneDayLater, true) { success, payload, error ->
                Log.d(TAG, "Success: $success")
                Log.d(TAG, "Payload: ${payload?.data?.get(0)?.distance_data?.summary?.steps}")
            }
        }

    }

    private fun getToken(cb: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {

            val client = OkHttpClient()

            val content = ByteArray(0)
            val request = Request.Builder()
                .url("https://api.tryterra.co/v2/auth/generateAuthToken")
                .post(content.toRequestBody(null, content.size))
                .addHeader("dev-id", BuildConfig.DEV_ID)
                .addHeader("x-api-key", BuildConfig.API_KEY)
                .build()

            client.newCall(request).execute().use { response ->
                Log.d(TAG, "Response code: ${response.code}")
                if(!response.isSuccessful) {
                    Log.e(TAG, "Error: ${response.code}")
                } else {
                    val responseBody = response.body?.string()
                    if(responseBody == null) {
                        Log.e(TAG, "Error: Response body is null")
                    } else {
                        val jsonObject = JSONObject(responseBody)
                        withContext(Dispatchers.Main) {
                            cb(jsonObject.getString("token"))
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val TAG = "MainActivity_TAG"
    }
}