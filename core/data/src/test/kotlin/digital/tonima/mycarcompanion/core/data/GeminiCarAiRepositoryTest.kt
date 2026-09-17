package digital.tonima.mycarcompanion.core.data

import com.google.firebase.ai.Chat
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.type.GenerateContentResponse
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiCarAiRepositoryTest {

    private val generativeModel = mockk<GenerativeModel>()
    private val repository = GeminiCarAiRepository(generativeModel)

    @Test
    fun `diagnose returns the model response text`() = runTest {
        val chat = mockk<Chat>()
        val response = mockk<GenerateContentResponse>()

        every { generativeModel.startChat(any()) } returns chat
        coEvery { chat.sendMessage("Barulho ao frear") } returns response
        every { response.text } returns "Possível causa: pastilhas de freio desgastadas."

        val result = repository.diagnose(
            vehicleContext = "Veículo: Civic 2020",
            history = emptyList(),
            message = "Barulho ao frear"
        )

        assertTrue(result.isSuccess)
        assertEquals("Possível causa: pastilhas de freio desgastadas.", result.getOrNull())
    }

    @Test
    fun `diagnose returns failure when model throws`() = runTest {
        every { generativeModel.startChat(any()) } throws IllegalStateException("offline")

        val result = repository.diagnose(
            vehicleContext = "Veículo: Civic 2020",
            history = emptyList(),
            message = "Barulho ao frear"
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun `generateMaintenanceInsight returns the model response text`() = runTest {
        val response = mockk<GenerateContentResponse>()
        coEvery { generativeModel.generateContent(any<String>()) } returns response
        every { response.text } returns "Priorize a troca de óleo nos próximos 500 km."

        val result = repository.generateMaintenanceInsight("Veículo: Civic 2020")

        assertTrue(result.isSuccess)
        assertEquals("Priorize a troca de óleo nos próximos 500 km.", result.getOrNull())
    }

    @Test
    fun `generateMaintenanceInsight returns failure when response text is null`() = runTest {
        val response = mockk<GenerateContentResponse>()
        coEvery { generativeModel.generateContent(any<String>()) } returns response
        every { response.text } returns null

        val result = repository.generateMaintenanceInsight("Veículo: Civic 2020")

        assertTrue(result.isFailure)
    }
}
