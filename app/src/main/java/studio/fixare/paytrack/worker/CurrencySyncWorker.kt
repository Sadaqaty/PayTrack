package studio.fixare.paytrack.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import studio.fixare.paytrack.PayTrackApplication
import studio.fixare.paytrack.data.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class CurrencySyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val rates = fetchExchangeRates()
            if (rates.isNotEmpty()) {
                val repository = (applicationContext as PayTrackApplication).userPreferencesRepository
                repository.setExchangeRates(rates)
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private suspend fun fetchExchangeRates(): Map<String, Double> = withContext(Dispatchers.IO) {
        val url = URL("https://open.er-api.com/v6/latest/USD")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        
        try {
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                parseRates(response)
            } else {
                emptyMap()
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseRates(jsonString: String): Map<String, Double> {
        val rates = mutableMapOf<String, Double>()
        try {
            val jsonObject = JSONObject(jsonString)
            if (jsonObject.getString("result") == "success") {
                val ratesObject = jsonObject.getJSONObject("rates")
                val keys = ratesObject.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    rates[key] = ratesObject.getDouble(key)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return rates
    }
}
