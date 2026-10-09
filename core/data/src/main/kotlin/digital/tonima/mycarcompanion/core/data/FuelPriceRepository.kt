package digital.tonima.mycarcompanion.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

interface FuelPriceRepository {
    /** The last prices fetched, kept on disk so they are available offline. Null until the first fetch. */
    val snapshot: Flow<FuelPriceSnapshot?>

    /** Fetches fresh prices unless the cache is recent. Network failures are swallowed: stale data is kept. */
    suspend fun refresh(force: Boolean = false)
}

/** Raw access to the prices endpoint, so the repository can be tested without the network. */
interface FuelPriceApi {
    suspend fun fetch(): String
}

@Singleton
class OkHttpFuelPriceApi @Inject constructor(private val client: OkHttpClient) : FuelPriceApi {

    override suspend fun fetch(): String = withContext(Dispatchers.IO) {
        client.newCall(Request.Builder().url(URL).build()).execute().use { response ->
            check(response.isSuccessful) { "HTTP ${response.code}" }
            response.body.string()
        }
    }

    private companion object {
        const val URL = "https://combustivelapi.com.br/api/precos/"
    }
}

@Singleton
class OnlineFuelPriceRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val api: FuelPriceApi,
) : FuelPriceRepository {

    private object Keys {
        val JSON = stringPreferencesKey("fuel_prices_json")
        val FETCHED_AT = longPreferencesKey("fuel_prices_fetched_at")
    }

    override val snapshot: Flow<FuelPriceSnapshot?> = dataStore.data.map { preferences ->
        preferences[Keys.JSON]?.let(FuelPriceParser::parse)
    }

    override suspend fun refresh(force: Boolean) {
        refresh(force, System.currentTimeMillis())
    }

    internal suspend fun refresh(force: Boolean, nowMillis: Long) {
        val fetchedAt = dataStore.data.first()[Keys.FETCHED_AT]
        if (!force && fetchedAt != null && nowMillis - fetchedAt < CACHE_MILLIS) return

        val json = try {
            api.fetch()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            return
        }
        // Only replace the cache with a response that parses, so a bad payload never wipes good data.
        if (FuelPriceParser.parse(json) == null) return
        dataStore.edit {
            it[Keys.JSON] = json
            it[Keys.FETCHED_AT] = nowMillis
        }
    }

    private companion object {
        const val CACHE_MILLIS = 2 * 60 * 60 * 1000L
    }
}
