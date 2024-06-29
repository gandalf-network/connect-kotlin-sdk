import com.gandalf.connect.Connect
import com.gandalf.connect.api.ApiService
import com.gandalf.connect.types.*
import com.gandalf.connect.lib.*
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
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
                services = mutableListOf("gandalf", "uber", "booking"),
                traits = mutableListOf("rating", "genius_level"),
                activities = mutableListOf("trip", "shop")
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
        fun `test strip redirect URL of trailing slash`() {
            val newInput = input.copy(redirectURL = "https://example.com/callback/")
            val connectWithSlash = Connect(newInput)
            assertEquals(redirectURL, connectWithSlash.redirectURL, "Redirect URL should be stripped of trailing slash.")
        }
    }

    @Nested
    inner class AllValidations {    
        var exception: Exception? = null

        @Test
        fun `run all validations`() = runBlocking {
            val generatedURL = connect.generateURL()
            assertEquals(true, connect.verificationComplete, "All validations should be complete")
        }

        @Test
        fun `test invalid public key`() = runBlocking {
            coEvery { apiService.verifyPublicKey(any()) } returns Single.fromCallable{ false }

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
        fun `test invalid redirect url`() = runBlocking {
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
        fun `test single non Gandalf service passed`() = runBlocking {
            connect.data = mutableMapOf(
                "uber" to Service(traits = listOf("rating"), activities = listOf("trip"))
            )

            val generatedURL = connect.generateURL()
            assertNotNull(generatedURL, "A ConnectURL should be generated")
            assertEquals(true, connect.verificationComplete, "All validations should be complete")
        }

        @Test
        fun `test multiple non Gandalf services passed`() = runBlocking {
            connect.data = mutableMapOf(
                "uber" to Service(traits = listOf("rating"), activities = listOf("trip")),
                "booking" to Service(traits = listOf("genius_level"), activities = listOf("shop")),
            )

            val generatedURL = connect.generateURL()
            assertNotNull(generatedURL, "A ConnectURL should be generated")
            assertEquals(true, connect.verificationComplete, "All validations should be complete")
        }

        @Test
        fun `test multiple Gandalf services failing if not required`() = runBlocking {
            connect.data = mutableMapOf(
                "uber" to Service(traits = listOf("rating"), activities = listOf("trip"), required = false),
                "booking" to Service(traits = listOf("genius_level"), activities = listOf("shop"), required = false),
            )

            try {
                connect.generateURL()
            } catch (e: GandalfError) {
                exception = e
            }

            assertNotNull(exception, "GandalfError should be thrown")
            assertEquals("At least one service must have the 'required' property set to true", exception?.message)
            assertEquals(false, connect.verificationComplete, "Validations should fail")
        }

        @Test
        fun `test mixed Gandalf and non Gandalf services passed`() = runBlocking {
            connect.data = mutableMapOf(
                "gandalf" to Service(traits = listOf("rating"), activities = listOf("trip")),
                "uber" to Service(traits = listOf("rating"), activities = listOf("trip")),
            )

            val generatedURL = connect.generateURL()
            assertNotNull(generatedURL, "A ConnectURL should be generated")
            assertEquals(true, connect.verificationComplete, "All validations should be complete")
        }
    }
}