package com.dippa2

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter
// new
import com.facebook.react.bridge.Arguments
import org.json.JSONObject
import org.json.JSONArray

import android.util.Log
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.dippa2.movesenseLog.util.Constants

// for emitting sensorNames list periodically
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

import SummingModelClass

import androidx.core.content.ContextCompat

class ReactNativeBridge(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    private lateinit var summingModel: SummingModelClass
    private val sensorNames = mutableSetOf<String>()
    private val receiveCounters = mutableMapOf<String, Int>()
    private val sensorDataMap = mutableMapOf<String, JSONObject>()
    private val lastUpdateTime = mutableMapOf<String, Long>()

    private val scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()


    // every 26th broadcast is used below because with 52 hertz on IMU, the IMU data arrays are 104 length.
    // this means an IMU data array broadcasted contains 2 seconds of IMU data. 
    // for some reason, the broadcast is done 13 times per second, so we need to skip (n(seconds)*13 -1) broadcasts between each reading.
    // the ML model we are using takes 2 second chunks of IMU data, so we need to skip (2*13-1)=25 broadcasts to get the new 2 second chunk of data.

    private var receiveCounter = 0


    private val sensorDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val sensorName = intent?.getStringExtra(Constants.SENSOR_NAME)
            if (sensorName != null) {
                if (sensorNames.add(sensorName)) {
                    // Reset all counters and clear previous data when a new sensor is added
                    // emitSensorDataToReactNative(JSONObject().put("sensorNames", JSONArray(sensorNames)).toString(), "sensorsUpdated")
                    receiveCounters.clear()
                    sensorDataMap.clear()
                }

                val counter = receiveCounters.getOrDefault(sensorName, 0) + 1
                receiveCounters[sensorName] = counter

                // Collect data for this sensor
                if (counter == 26) {
                    val jsonData = when (intent?.action) {
                        Constants.IMU -> convertIMUDataToJson(intent)
                        else -> JSONObject()
                    }
                    sensorDataMap[sensorName] = jsonData
                    lastUpdateTime[sensorName] = System.currentTimeMillis()
                }

                // Check if all sensors have reached or exceeded the 26th broadcast
                if (receiveCounters.values.all { it >= 26 }) {
                    val combinedData = JSONObject()
                    // Iterate over each sensor's data in sensorDataMap
                    for ((_, value) in sensorDataMap) {
                        // Assuming 'value' is a JSONObject containing the sensor data like {"timestap": ..., "acc_x": ...}
                        // Merge this sensor's data into combinedData
                        value.keys().forEach { key ->
                            if (combinedData.has(key)) {
                                // If the key already exists, append the new value to the existing array
                                val existingArray = combinedData.getJSONArray(key)
                                // Flatten the array if the value is also an array
                                if (value.get(key) is JSONArray) {
                                    val innerArray = value.getJSONArray(key)
                                    for (i in 0 until innerArray.length()) {
                                        existingArray.put(innerArray.get(i))
                                    }
                                } else {
                                    existingArray.put(value.get(key))
                                }
                                combinedData.put(key, existingArray)
                            } else {
                                // If the key does not exist, create a new array and add the value
                                val newArray = JSONArray()
                                // Check if the value is an array and add its elements
                                if (value.get(key) is JSONArray) {
                                    val innerArray = value.getJSONArray(key)
                                    for (i in 0 until innerArray.length()) {
                                        newArray.put(innerArray.get(i))
                                    }
                                } else {
                                    newArray.put(value.get(key))
                                }
                                combinedData.put(key, newArray)
                            }
                        }
                    }
                    try {
                        val modelInput = combinedData.toString()
                        Log.e("ReactNativeBridge", "modelInput: $modelInput")
                        val timestamp = combinedData.getJSONArray("timestamp").getLong(0)
                        val modelOutput = summingModel.predict(modelInput)
                        val outputJson = JSONObject().apply {
                            put("timestamp", timestamp)
                            put("modelOutput", modelOutput)
                        }
                        emitSensorDataToReactNative(outputJson.toString(), "IMUDataEvent")
                    } catch (e: Exception) {
                        Log.e("ReactNativeBridge", "Error in model prediction", e)
                    }

                    // Reset counters and clear data after processing
                    receiveCounters.keys.forEach { key ->
                        receiveCounters[key] = 0
                    }
                    sensorDataMap.clear()
                }
            }
        }
    }
    
    init {
        Log.e("ReactNativeBridge", "Initializing ReactNativeBridge")
        // IntentFilter without any actions to listen to all intents
        val filter = IntentFilter().apply {
            addAction(Constants.IMU)
            // addAction(Constants.ECG)
            // Add other actions as needed
        }
        // reactContext.registerReceiver(sensorDataReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        reactContext.registerReceiver(sensorDataReceiver, filter)

        Log.e("ReactNativeBridge", "Before SummingModelClass instantiation")
        try {
            summingModel = SummingModelClass(reactContext)
            Log.e("ReactNativeBridge", "SummingModelClass instantiated successfully")
        } catch (e: Exception) {
            Log.e("ReactNativeBridge", "Error instantiating SummingModelClass", e)
        }

        val timeout = 30000 // 8 seconds, if sensor has not broadcasted for this long, it is considered inactive
        scheduledExecutorService.scheduleAtFixedRate({
            try {
                val currentTime = System.currentTimeMillis()
                val inactiveSensors = lastUpdateTime.filter {
                    (currentTime - it.value) > timeout
                }.keys
                // Remove inactive sensors
                sensorNames.removeAll(inactiveSensors)
                lastUpdateTime.keys.removeAll(inactiveSensors)
                emitSensorDataToReactNative(JSONObject().put("sensorNames", JSONArray(sensorNames)).toString(), "sensorsUpdated")
            } catch (e: Exception) {
                Log.e("ReactNativeBridge", "Error emitting sensor names", e)
            }
        }, 0, 10, TimeUnit.SECONDS)
    }

    private fun convertIMUDataToJson(intent: Intent?): JSONObject { // String instead of JSONObject
        val json = JSONObject()
        // Add the current timestamp
        json.put("timestamp", System.currentTimeMillis())
    
        intent?.extras?.let { bundle ->
            // Mapping of original keys to new keys
            val keyMap = mapOf(
                Constants.UPDATE_GRAPH_X to "acc_x",
                Constants.UPDATE_GRAPH_Y to "acc_y",
                Constants.UPDATE_GRAPH_Z to "acc_z",
                Constants.UPDATE_GRAPH_GYR_X to "gyro_x",
                Constants.UPDATE_GRAPH_GYR_Y to "gyro_y",
                Constants.UPDATE_GRAPH_GYR_Z to "gyro_z"
            )
    
            keyMap.forEach { (originalKey, newKey) ->
                val value = bundle.get(originalKey)
                if (value is FloatArray) {
                    // Convert FloatArray to JSONArray
                    val jsonArray = JSONArray()
                    value.forEach { jsonArray.put(it) }
                    json.put(newKey, jsonArray)
                } else {
                    Log.e("ReactNativeBridge", "Missing data for key: $originalKey")
                }
            }
        }
    
        if (!json.has("acc_x") || !json.has("acc_y") || !json.has("acc_z") ||
            !json.has("gyro_x") || !json.has("gyro_y") || !json.has("gyro_z")) {
            Log.e("ReactNativeBridge", "Incomplete sensor data: ${json.toString()}")
        } else {
            Log.d("ReactNativeBridge", "Complete sensor data: ${json.toString()}")
        }
        return json
    }

    private fun emitSensorDataToReactNative(sensorData: String?, eventName: String) {
        sensorData?.let {
            // Log.e("ReactNativeBridge", "Preparing to emit $eventName data: $sensorData")
            reactContext
                .getJSModule(RCTDeviceEventEmitter::class.java)
                .emit(eventName, sensorData)
        }
    }

    override fun getName(): String {
        Log.e("ReactNativeBridge", "getName called")
        return "ReactNativeBridge"
    }

    @ReactMethod
    fun startMoveSenseLog() {
        Log.e("ReactNativeBridge", "Starting MoveSenseLog")
        val intent = Intent(reactApplicationContext, com.dippa2.movesenseLog.MoveSenseLog::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK // Required when starting an Activity from a non-Activity context.
        reactApplicationContext.startActivity(intent)
    }

    @ReactMethod
    fun addListener(eventName: String) {
        // This function is a no-op in native code.
        // Events will be sent to JavaScript regardless of this function.
        Log.d("ReactNativeBridge", "addListener called for: $eventName")
    }

    @ReactMethod
    fun removeListeners(eventName: String) {
        // This function is a no-op in native code.
        // Actual listener removal is handled in JavaScript.
        Log.d("ReactNativeBridge", "removeListeners called for: $eventName")
    }
}