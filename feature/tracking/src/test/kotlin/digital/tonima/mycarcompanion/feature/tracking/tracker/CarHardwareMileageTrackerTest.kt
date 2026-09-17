package digital.tonima.mycarcompanion.feature.tracking.tracker

import androidx.car.app.hardware.CarHardwareManager
import androidx.car.app.hardware.common.CarValue
import androidx.car.app.hardware.common.OnCarDataAvailableListener
import androidx.car.app.hardware.info.CarInfo
import androidx.car.app.hardware.info.Mileage
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.Executor

class CarHardwareMileageTrackerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val carHardwareManager = mockk<CarHardwareManager>()
    private val carInfo = mockk<CarInfo>(relaxed = true)
    private val executor = Executor { it.run() }
    private val tracker = CarHardwareMileageTracker()

    private fun mileageOf(meters: Float, status: Int = CarValue.STATUS_SUCCESS): Mileage {
        val odometer = mockk<CarValue<Float>>()
        every { odometer.status } returns status
        every { odometer.value } returns meters
        val mileage = mockk<Mileage>()
        every { mileage.odometerMeters } returns odometer
        return mileage
    }

    @Test
    fun `tracker emits distance in km between two odometer readings`() = runTest(testDispatcher) {
        every { carHardwareManager.carInfo } returns carInfo
        val listenerSlot = slot<OnCarDataAvailableListener<Mileage>>()
        every { carInfo.addMileageListener(executor, capture(listenerSlot)) } returns Unit

        val flow = tracker.startTracking(carHardwareManager, executor)

        val values = mutableListOf<Double>()
        val job = launch {
            flow.collect { values.add(it) }
        }

        testDispatcher.scheduler.advanceUntilIdle()

        // First reading only establishes the baseline, nothing should be emitted yet.
        listenerSlot.captured.onCarDataAvailable(mileageOf(10_000f))
        // Second reading, 1500 meters further -> should emit 1.5 km.
        listenerSlot.captured.onCarDataAvailable(mileageOf(11_500f))

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, values.size)
        assertEquals(1.5, values[0], 0.001)

        job.cancel()
    }

    @Test
    fun `tracker ignores readings with an unsuccessful status`() = runTest(testDispatcher) {
        every { carHardwareManager.carInfo } returns carInfo
        val listenerSlot = slot<OnCarDataAvailableListener<Mileage>>()
        every { carInfo.addMileageListener(executor, capture(listenerSlot)) } returns Unit

        val flow = tracker.startTracking(carHardwareManager, executor)

        val values = mutableListOf<Double>()
        val job = launch {
            flow.collect { values.add(it) }
        }

        testDispatcher.scheduler.advanceUntilIdle()

        listenerSlot.captured.onCarDataAvailable(mileageOf(10_000f))
        listenerSlot.captured.onCarDataAvailable(mileageOf(11_500f, status = CarValue.STATUS_UNAVAILABLE))

        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(values.isEmpty())

        job.cancel()
    }

    @Test
    fun `tracker does not emit a negative distance when odometer value decreases`() = runTest(testDispatcher) {
        every { carHardwareManager.carInfo } returns carInfo
        val listenerSlot = slot<OnCarDataAvailableListener<Mileage>>()
        every { carInfo.addMileageListener(executor, capture(listenerSlot)) } returns Unit

        val flow = tracker.startTracking(carHardwareManager, executor)

        val values = mutableListOf<Double>()
        val job = launch {
            flow.collect { values.add(it) }
        }

        testDispatcher.scheduler.advanceUntilIdle()

        listenerSlot.captured.onCarDataAvailable(mileageOf(10_000f))
        listenerSlot.captured.onCarDataAvailable(mileageOf(9_000f))

        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(values.isEmpty())

        job.cancel()
    }
}
