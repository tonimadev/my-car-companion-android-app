package digital.tonima.mycarcompanion.core.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/** A brand, model or year option from the FIPE table. */
data class FipeItem(val code: String, val name: String)

interface FipeRepository {
    suspend fun brands(): Result<List<FipeItem>>
    suspend fun models(brandCode: String): Result<List<FipeItem>>
    suspend fun years(brandCode: String, modelCode: String): Result<List<FipeItem>>
}

/** Raw access to the FIPE API (path relative to the cars endpoint), so parsing and caching can be tested offline. */
interface FipeApi {
    suspend fun get(path: String): String
}

@Singleton
class OkHttpFipeApi @Inject constructor(private val client: OkHttpClient) : FipeApi {
    override suspend fun get(path: String): String = withContext(Dispatchers.IO) {
        client.newCall(Request.Builder().url(BASE_URL + path).build()).execute().use { response ->
            check(response.isSuccessful) { "HTTP ${response.code}" }
            response.body?.string() ?: error("Empty response body")
        }
    }

    private companion object {
        const val BASE_URL = "https://parallelum.com.br/fipe/api/v1/carros/"
    }
}

@Singleton
class OnlineFipeRepository @Inject constructor(private val api: FipeApi) : FipeRepository {

    private val moshi = Moshi.Builder().build()
    private val itemAdapter = moshi.adapter<List<Map<String, Any?>>>(
        Types.newParameterizedType(
            List::class.java,
            Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        )
    )
    private val modelsAdapter = moshi.adapter<Map<String, Any?>>(
        Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    )

    // The tables change monthly at most, so keeping them for the process lifetime is enough.
    private val cache = mutableMapOf<String, List<FipeItem>>()
    private val lock = Mutex()

    override suspend fun brands(): Result<List<FipeItem>> =
        load("marcas") { parseItems(it) }

    override suspend fun models(brandCode: String): Result<List<FipeItem>> =
        load("marcas/$brandCode/modelos") { json ->
            val items = modelsAdapter.fromJson(json)?.get("modelos") as? List<*> ?: return@load null
            items.mapNotNull(::toItem)
        }

    override suspend fun years(brandCode: String, modelCode: String): Result<List<FipeItem>> =
        load("marcas/$brandCode/modelos/$modelCode/anos") { parseItems(it) }

    private suspend fun load(path: String, parse: (String) -> List<FipeItem>?): Result<List<FipeItem>> =
        lock.withLock {
            cache[path]?.let { return@withLock Result.success(it) }
            try {
                val items = parse(api.get(path))?.takeIf { it.isNotEmpty() }
                    ?: return@withLock Result.failure(IllegalStateException("Unexpected FIPE response"))
                cache[path] = items
                Result.success(items)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun parseItems(json: String): List<FipeItem>? =
        itemAdapter.fromJson(json)?.mapNotNull(::toItem)

    private fun toItem(raw: Any?): FipeItem? {
        val map = raw as? Map<*, *> ?: return null
        // Model codes are numbers in the JSON and Moshi reads numbers as Double ("4501.0"), so normalize them.
        val code = when (val raw = map["codigo"]) {
            is Number -> raw.toLong().toString()
            null -> return null
            else -> raw.toString()
        }
        val name = map["nome"] as? String ?: return null
        return FipeItem(code, name)
    }
}

/**
 * The FIPE year code is "2019-1" (year-fuel). Returns the model year, or null for codes such as
 * "32000-1" (zero km) that are not a real year.
 */
fun FipeItem.modelYear(): Int? = code.substringBefore('-').toIntOrNull()?.takeIf { it in 1950..2100 }
