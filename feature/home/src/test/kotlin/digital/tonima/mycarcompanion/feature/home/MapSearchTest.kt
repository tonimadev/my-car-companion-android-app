package digital.tonima.mycarcompanion.feature.home

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import android.widget.Toast
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.After
import org.junit.Before
import org.junit.Test

class MapSearchTest {

    private val context = mockk<Context>()
    private val toast = mockk<Toast>(relaxed = true)

    @Before
    fun setUp() {
        mockkStatic(Uri::class, Toast::class)
        every { Uri.encode(any()) } answers { firstArg<String>().replace(" ", "%20") }
        every { Uri.parse(any()) } returns mockk()
        every { Toast.makeText(any<Context>(), any<Int>(), any()) } returns toast
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `opens geo intent when a map app is available`() {
        every { context.startActivity(any()) } just runs

        openMapSearch(context, "gas station")

        verify(exactly = 1) { context.startActivity(any()) }
        verify { Uri.parse("geo:0,0?q=gas%20station") }
        verify(exactly = 0) { Toast.makeText(any<Context>(), any<Int>(), any()) }
    }

    @Test
    fun `falls back to web maps when no app handles geo`() {
        var calls = 0
        every { context.startActivity(any()) } answers {
            if (calls++ == 0) throw ActivityNotFoundException()
        }

        openMapSearch(context, "gas station")

        verifyOrder {
            Uri.parse("geo:0,0?q=gas%20station")
            Uri.parse("https://www.google.com/maps/search/?api=1&query=gas%20station")
        }
        verify(exactly = 2) { context.startActivity(any()) }
        verify(exactly = 0) { Toast.makeText(any<Context>(), any<Int>(), any()) }
    }

    @Test
    fun `shows message instead of crashing when nothing can open maps`() {
        every { context.startActivity(any()) } throws ActivityNotFoundException()

        openMapSearch(context, "gas station")

        verify { Toast.makeText(context, R.string.no_map_app, Toast.LENGTH_SHORT) }
        verify { toast.show() }
    }
}
