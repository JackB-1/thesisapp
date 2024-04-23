import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.Tensor
import org.pytorch.Module
import org.json.JSONObject

import android.util.Log
import com.facebook.react.bridge.ReactApplicationContext
import java.io.File
import java.io.FileOutputStream

public class SummingModelClass(private val reactContext: ReactApplicationContext) {
    private val model: Module

    init {
        val modelPath = "summingModel.ptl"
        Log.e("SummingModelClass", "Attempting to load model from path: $modelPath")
        try {
            val modelPath = copyModelFromAssets()
            model = LiteModuleLoader.load(modelPath)
            Log.e("SummingModelClass", "Model loaded successfully.")
        } catch (e: Exception) {
            Log.e("SummingModelClass", "Failed to load model.", e)
            throw e
        }
    }

    fun predict(inputJson: String): Int {
        try {
            // Preparing Input
            val inputTensor = jsonToTensor(inputJson)
            // Run Inference
            val outputTensor = model.forward(IValue.from(inputTensor)).toTensor()
            // Processing results
            val maxIndex = outputTensor.dataAsLongArray.first().toInt()
            return maxIndex
        } catch (e: Exception) {
            Log.e("SummingModelClass", "Error during prediction", e)
            throw e // Rethrow or handle as appropriate
        }
    }

    private fun jsonToTensor(inputJson: String): Tensor {
        val jsonObject = JSONObject(inputJson)
        val arrays = listOf("acc_x", "acc_y", "acc_z", "gyro_x", "gyro_y", "gyro_z")
        val allData = arrays.flatMap { key ->
            val jsonArray = jsonObject.getJSONArray(key)
            List(jsonArray.length()) { i -> jsonArray.getDouble(i).toFloat() }
        }
        val tensorData = allData.toFloatArray()
        // Adjusting the shape of the tensor to match the input shape expected by the model
        return Tensor.fromBlob(tensorData, longArrayOf(1, tensorData.size.toLong()))
    }

    private fun copyModelFromAssets(): String {
        val assetManager = reactContext.assets
        val inputStream = assetManager.open("summingModel.ptl")
        val outputFile = File(reactContext.filesDir, "summingModel.ptl")
        val outputStream = FileOutputStream(outputFile)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()
        
        // Log the absolute path of the copied model file
        Log.e("SummingModelClass", "Model file copied to: ${outputFile.absolutePath}")
        
        return outputFile.absolutePath
    }
}