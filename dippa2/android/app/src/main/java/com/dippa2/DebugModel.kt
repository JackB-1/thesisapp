/* import android.util.Log
import org.pytorch.LiteModuleLoader
import org.pytorch.Module

object DebugModelLoading {

    private const val TAG = "DebugModelLoading"

    @JvmStatic
    fun main(args: Array<String>) {
        try {
            Log.e(TAG, "Attempting to load model.")
            val model: Module = LiteModuleLoader.load("summingModel.pt")
            Log.e(TAG, "Model loaded successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load model.", e)
        }
    }
}
 */