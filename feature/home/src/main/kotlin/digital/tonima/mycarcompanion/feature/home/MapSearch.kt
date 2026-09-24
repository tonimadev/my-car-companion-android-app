package digital.tonima.mycarcompanion.feature.home

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Opens a map search for [query]. Devices without an app that handles `geo:` (e.g. no Google
 * Maps, Android Automotive emulators) fall back to the Google Maps web URL, and when nothing can
 * open it either, a message is shown instead of crashing.
 */
internal fun openMapSearch(context: Context, query: String) {
    val encoded = Uri.encode(query)
    val candidates = listOf(
        "geo:0,0?q=$encoded",
        "https://www.google.com/maps/search/?api=1&query=$encoded"
    )
    for (uri in candidates) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
            return
        } catch (_: ActivityNotFoundException) {
            // Try the next candidate.
        }
    }
    Toast.makeText(context, R.string.no_map_app, Toast.LENGTH_SHORT).show()
}
