import com.gandalf.connect.Connect
import com.gandalf.connect.api.ApiService
import com.gandalf.connect.types.*
import com.gandalf.connect.lib.*
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import io.reactivex.rxjava3.core.Single

class ConnectTest {
    private val apiService = mockk<ApiService>()
    private lateinit var connect: Connect

    private val publicKey = "public_key"
    private val services: InputData = mutableMapOf(
        "uber" to Service(traits = listOf("rating"), activities = listOf("trip"))
    )
    private val redirectURL = "https://example.com/callback"

    private val input = ConnectInput(
        publicKey,
        redirectURL,
        services
    )

    @BeforeEach
    fun setup() {
        coEvery { apiService.verifyPublicKey(any()) } returns Single.fromCallable{ true }
        coEvery { apiService.getSupportedServicesAndTraits() } returns Single.fromCallable { 
            SupportedServicesAndTraits(
                services = mutableListOf("gandalf", "uber"),
                traits = mutableListOf("rating"),
                activities = mutableListOf("trip")
            )
        }

        connect = Connect(input)
        connect.setApiService(apiService)
    }

    @Nested
    inner class Constructor {       
        @Test
        fun `test connect constructor for correct initialization`() {
            assertEquals(publicKey, connect.publicKey, "Public key should match the input.")
            assertEquals(redirectURL, connect.redirectURL, "Redirect URL should match the input.")
        }

        @Test
        fun `test strip redirect URL of trailling slash`() {
            input.redirectURL = "https://example.com/callback/"
            val connect = Connect(input)
            assertEquals(redirectURL, connect.redirectURL, "Redirect URL should match the input.")
        }
    }

    @Nested
    inner class AllValidations {    
        var exception: Exception? = null

        @Test
        fun `run all validations`() = runBlocking {
            val generatedURL = connect.generateURL()
            assertEquals(connect.verificationComplete, true, "All validation shoud be complete")
        }

        @Test
        fun `test invalid public key`() = runBlocking {
            coEvery { apiService.verifyPublicKey(any()) } returns Single.fromCallable{ false }
            val connect = Connect(input)
            connect.setApiService(apiService)

            try {
                connect.generateURL()
            } catch (e: GandalfError) {
                exception = e
            }

            assertNotNull(exception, "GandalfError should be thrown")
            assertEquals("Public key does not exist", exception?.message)
            assertEquals(false, connect.verificationComplete, "Validations should fail")
        }

        @Test
        fun `test invalid redirect url key`() = runBlocking {
            connect.redirectURL = "not a valid URL"

            try {
                connect.generateURL()
            } catch (e: GandalfError) {
                exception = e
            }

            assertNotNull(exception, "GandalfError should be thrown")
            assertEquals("Invalid redirectURL", exception?.message)
            assertEquals(false, connect.verificationComplete, "Validations should fail")
        }

        @Test
        fun `test invalid service`() = runBlocking {
            val fakeService = "fake_service"
            connect.data = mutableMapOf(
                fakeService to Service(traits = listOf("rating"), activities = listOf("trip"))
            )

            try {
                connect.generateURL()
            } catch (e: GandalfError) {
                exception = e
            }

            assertNotNull(exception, "GandalfError should be thrown")
            assertEquals("These services [ ${fakeService} ] are unsupported", exception?.message)
            assertEquals(false, connect.verificationComplete, "Validations should fail")
        }

        @Test
        fun `test invalid trait`() = runBlocking {
            val fakeTrait = "fake_trait"
            connect.data = mutableMapOf(
                "uber" to Service(traits = listOf(fakeTrait), activities = listOf("trip"))
            )

            try {
                connect.generateURL()
            } catch (e: GandalfError) {
                exception = e
            }

            assertNotNull(exception, "GandalfError should be thrown")
            assertEquals("These traits [ ${fakeTrait} ] are unsupported", exception?.message)
            assertEquals(false, connect.verificationComplete, "Validations should fail")
        }

        @Test
        fun `test invalid activities`() = runBlocking {
            val fakeActivity = "fake_activity"
            connect.data = mutableMapOf(
                "uber" to Service(traits = listOf("rating"), activities = listOf(fakeActivity))
            )

            try {
                connect.generateURL()
            } catch (e: GandalfError) {
                exception = e
            }

            assertNotNull(exception, "GandalfError should be thrown")
            assertEquals("These activities [ ${fakeActivity} ] are unsupported", exception?.message)
            assertEquals(false, connect.verificationComplete, "Validations should fail")
        }

        @Test
        fun `test no trait or activity passed`() = runBlocking {
            connect.data = mutableMapOf(
                "uber" to Service(traits = listOf(), activities = listOf())
            )

            try {
                connect.generateURL()
            } catch (e: GandalfError) {
                exception = e
            }

            assertNotNull(exception, "GandalfError should be thrown")
            assertEquals("At least one trait or activity is required", exception?.message)
            assertEquals(false, connect.verificationComplete, "Validations should fail")
        }

        @Test
        fun `test more than one non Gandalf service passed`() = runBlocking {
            connect.data = mutableMapOf(
                "uber" to Service(traits = listOf("rating"), activities = listOf("play")),
                "booking" to Service(traits = listOf("genius_level"), activities = listOf("shop")),
            )

            try {
                connect.generateURL()
            } catch (e: GandalfError) {
                exception = e
            }

            assertNotNull(exception, "GandalfError should be thrown")
            assertEquals("Only one non Gandalf service is supported per Connect URL", exception?.message)
            assertEquals(false, connect.verificationComplete, "Validations should fail")
        }
    }

    @Nested
    inner class GenerateURL {       
        @Test
        fun `test generate URL`() = runBlocking {
            connect.data = mutableMapOf(
                "uber" to Service(traits = listOf("rating"), activities = listOf("trip")),
            )

            val connectURL = connect.generateURL()

            assertNotNull(connectURL, "A ConnectURL should be generated")
        }
    }


    @Nested
    inner class DataKeyExtraction {       
        @Test
        fun `getDataKeyFromURL should extract data key correctly`() {
            val dataKey = Connect.getDataKeyFromURL("https://example.com/callback?dataKey=12345")
            assertEquals("12345", dataKey)
        }

        @Test
        fun `getDataKeyFromURL should fail if data key is missing`() {
            val exception = assertThrows(GandalfError::class.java) {
                Connect.getDataKeyFromURL("https://example.com/callback")
            }
            assertEquals("Datakey not found in the URL https://example.com/callback", exception.message)
        }
    }
}
