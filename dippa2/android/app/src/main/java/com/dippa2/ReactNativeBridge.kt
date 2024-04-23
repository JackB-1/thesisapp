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

// mockdata imports
import java.util.Timer
import kotlin.concurrent.timerTask
import java.util.Date

import SummingModelClass

import androidx.core.content.ContextCompat

class ReactNativeBridge(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    private var mockDataTimer: Timer? = null
    private lateinit var summingModel: SummingModelClass

    @ReactMethod
    fun startMockDataEmitter() {
        // Log.e("ReactNativeBridge", "startMockDataEmitter called")
        mockDataTimer = Timer()
        mockDataTimer?.scheduleAtFixedRate(timerTask {
        val mockData = "MOCKDATA ${Date().time.toString()}"
        // Log.e("ReactNativeBridge", "Emitting mock data: $mockData")
        emitSensorDataToReactNative(mockData, "MockDataEvent")
        }, 0, 1000) // 0 delay, 1 second period
    }

    @ReactMethod
    fun stopMockDataEmitter() {
        Log.e("ReactNativeBridge", "stopMockDataEmitter called")
        mockDataTimer?.cancel()
        mockDataTimer = null
    }


    // every 26th broadcast is used below because with 52 hertz on IMU, the IMU data arrays are 104 length.
    // this means an IMU data array broadcasted contains 2 seconds of IMU data. 
    // for some reason, the broadcast is done 13 times per second, so we need to skip 13 broadcasts per second.
    // the ML model we are using takes 2 second chunks of IMU data, so we need to skip 26 broadcasts to get the new 2 second chunk of data.

    private var receiveCounter = 0


    private val sensorDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            receiveCounter++

            if (receiveCounter % 26 == 0) {
                val action = intent?.action ?: "Unknown"
                Log.e("ReactNativeBridge", "onReceive: action = $action")
                intent?.extras?.let { bundle ->
                    val jsonData = when (action) {
                        Constants.IMU -> {
                            convertIMUDataToJson(intent)
                        }
                        else -> {
                            compileExtrasIntoString(intent)
                        }
                    }
                    try {
                        Log.e("ReactNativeBridge", "Before calling predict method of SummingModelClass")
                        val modelOutput = summingModel.predict(jsonData)
                        // val modelOutput = 1
                        Log.e("ReactNativeBridge", "Model output: $modelOutput")

                        val modelOutputString = modelOutput.toString()


                        // Emit model output to React Native
                        emitSensorDataToReactNative(modelOutputString, if (action in listOf(Constants.IMU)) "IMUDataEvent" else "GenericEvent")
                    } catch (e: Exception) {
                        Log.e("ReactNativeBridge", "Error calling predict method of SummingModelClass", e)
                    }
                    // emitSensorDataToReactNative(jsonData, if (action in listOf(Constants.IMU)) "IMUDataEvent" else "GenericEvent")

                    // Use SummingModelClass to predict
                    //val modelOutput = summingModel.predict(jsonData)
                    
                    // Emit model output to React Native
                    // emitSensorDataToReactNative(modelOutput, if (action in listOf(Constants.IMU)) "IMUDataEvent" else "GenericEvent")
                }
            }

            if (receiveCounter == 26) {
                receiveCounter = 0
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
        Log.e("ReactNativeBridge", "Registering sensorDataReceiver with filter actions: ${filter.actionsIterator().asSequence().toList()}")

        // reactContext.registerReceiver(sensorDataReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        reactContext.registerReceiver(sensorDataReceiver, filter)

        Log.e("ReactNativeBridge", "Before SummingModelClass instantiation")
        try {
            summingModel = SummingModelClass(reactContext)
            Log.e("ReactNativeBridge", "SummingModelClass instantiated successfully")
        } catch (e: Exception) {
            Log.e("ReactNativeBridge", "Error instantiating SummingModelClass", e)
        }
    }

    private fun convertIMUDataToJson(intent: Intent?): String {
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
                }
            }
        }
        Log.e("ReactNativeBridge", "data parsed: ${json.toString()}")
        return json.toString()
    }
    
    private fun compileExtrasIntoString(intent: Intent?): String {
        val dataBuilder = StringBuilder()
        dataBuilder.append("Action: ${intent?.action ?: "Unknown"}")
        
        // Iterate through all extras in the intent
        intent?.extras?.let { bundle ->
            for (key in bundle.keySet()) {
                val value = bundle.get(key)
                dataBuilder.append(", $key: $value")
            }
        }
        
        // Convert StringBuilder content to String
        return dataBuilder.toString()
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

    /* // Make sure to unregister the receiver when the module is destroyed
    override fun onCatalystInstanceDestroy() {
        Log.e("ReactNativeBridge", "onCatalystInstanceDestroy called")
        reactContext.unregisterReceiver(sensorDataReceiver)
        super.onCatalystInstanceDestroy()
    } */

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
