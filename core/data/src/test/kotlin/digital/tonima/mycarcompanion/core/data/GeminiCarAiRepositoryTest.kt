package digital.tonima.mycarcompanion.core.data

import com.google.firebase.ai.Chat
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.type.GenerateContentResponse
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import com.google.firebase.ai.type.Content
import org.junit.Assert.assertThrows
import org.junit.Assert.assertFalse
import java.io.IOException
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

    @Test
    fun `diagnose rethrows coroutine cancellation instead of returning a failure`() {
        every { generativeModel.startChat(any()) } throws CancellationException("cancelled")

        assertThrows(CancellationException::class.java) {
            runBlocking { repository.diagnose("ctx", emptyList(), "hi") }
        }
    }

    @Test
    fun `io failures are classified as network errors`() = runTest {
        every { generativeModel.startChat(any()) } throws IOException("offline")

        val error = repository.diagnose("ctx", emptyList(), "hi").exceptionOrNull()

        assertEquals(AiException.Kind.NETWORK, (error as AiException).kind)
    }

    @Test
    fun `empty response is classified as empty`() = runTest {
        val response = mockk<GenerateContentResponse>()
        coEvery { generativeModel.generateContent(any<String>()) } returns response
        every { response.text } returns null

        val error = repository.generateMaintenanceInsight("ctx").exceptionOrNull()

        assertEquals(AiException.Kind.EMPTY_RESPONSE, (error as AiException).kind)
    }

    @Test
    fun `trimmed history starts with a user turn and keeps alternating roles`() = runTest {
        val chat = mockk<Chat>()
        val response = mockk<GenerateContentResponse>()
        val history = slot<List<Content>>()
        every { generativeModel.startChat(capture(history)) } returns chat
        coEvery { chat.sendMessage(any<String>()) } returns response
        every { response.text } returns "ok"
        // 21 turns: after takeLast(20) the window starts with a MODEL turn, which must be dropped.
        val turns = (0 until 21).map { ChatTurn(if (it % 2 == 0) ChatRole.USER else ChatRole.MODEL, "t$it") }

        repository.diagnose("ctx", turns, "next")

        val roles = history.captured.map { it.role }
        assertEquals(listOf("user", "model", "user"), roles.take(3))
        assertFalse(roles.zipWithNext().any { (a, b) -> a == b })
    }
}
