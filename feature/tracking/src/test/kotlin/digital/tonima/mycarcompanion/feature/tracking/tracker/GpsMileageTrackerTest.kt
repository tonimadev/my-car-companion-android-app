package digital.tonima.mycarcompanion.feature.tracking.tracker

import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GpsMileageTrackerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fusedLocationClient = mockk<FusedLocationProviderClient>(relaxed = true)
    private val tracker = GpsMileageTracker(fusedLocationClient)

    @Before
    fun setup() {
        mockkStatic(Looper::class)
        every { Looper.getMainLooper() } returns mockk(relaxed = true)
    }

    @Test
    fun `tracker emits distance between two locations`() = runTest(testDispatcher) {
        val callbackSlot = slot<LocationCallback>()
        every { 
            fusedLocationClient.requestLocationUpdates(any(), capture(callbackSlot), any()) 
        } returns mockk()

        val flow = tracker.startTracking()
        
        val values = mutableListOf<Double>()
        val job = launch {
            flow.collect { values.add(it) }
        }
        
        testDispatcher.scheduler.advanceUntilIdle()
        
        val loc1 = mockk<Location>()
        val loc2 = mockk<Location>()
        every { loc1.distanceTo(loc2) } returns 1000f // 1000 meters
        
        // Trigger first location
        callbackSlot.captured.onLocationResult(LocationResult.create(listOf(loc1)))
        
        // Trigger second location - should emit 1.0 KM
        callbackSlot.captured.onLocationResult(LocationResult.create(listOf(loc2)))
        
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertEquals(1, values.size)
        assertEquals(1.0, values[0], 0.001)
        
        job.cancel()
    }
}
