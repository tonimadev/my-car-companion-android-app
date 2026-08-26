package digital.tonima.mycarcompanion.core.designsystem.component

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import kotlin.math.roundToInt

@Composable
fun AdBannerView(
    modifier: Modifier = Modifier,
    adId: String = "ca-app-pub-3940256099942544/6300978111", // ID padrão de teste do AdMob
    isProUser: Boolean = false,
    loadAd: Boolean = true,
) {
    if (isProUser) return

    val isInspectionMode = LocalInspectionMode.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        val adWidth = maxWidth.value.roundToInt()

        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { context ->
                AdView(context).apply {
                    val finalSize = if (adWidth > 0) {
                        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)
                    } else {
                        AdSize.BANNER
                    }
                    setAdSize(finalSize)
                    adUnitId = adId

                    if (!isInspectionMode && loadAd) {
                        loadAd(AdRequest.Builder().build())
                    }
                }
            },
            onRelease = { adView ->
                adView.destroy()
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AdBannerViewPreview() {
    AdBannerView(
        adId = "ca-app-pub-3940256099942544/6300978111",
        isProUser = false,
        loadAd = false,
    )
}
