package fi.thl.koronahaavi.service

import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey
import fi.thl.koronahaavi.data.AppStateRepository
import fi.thl.koronahaavi.data.SettingsRepository
import fi.thl.koronahaavi.utils.TestData
import io.mockk.*
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.HttpException
import retrofit2.Response
import java.time.Instant
import kotlin.random.Random


class DiagnosisKeyServiceTest {
    private lateinit var diagnosisKeyService: DiagnosisKeyService
    private lateinit var backendService: BackendService
    private lateinit var appStateRepository: AppStateRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var systemOperations: SystemOperations

    private val batches = listOf("a", "b", "c")

    @get:Rule
    val folder = TemporaryFolder()

    @Before
    fun init() {
        backendService = mockk(relaxed = true)
        appStateRepository = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)
        systemOperations = mockk(relaxed = true)

        coEvery { appStateRepository.getDiagnosisKeyBatchId() } returns "batch_id"
        coEvery { backendService.listDiagnosisKeyBatches(any()) } returns DiagnosisKeyBatches(batches)
        coEvery { backendService.getDiagnosisKeyFile(any()) } returns "test".toResponseBody()
        every { systemOperations.createFileInPersistedStorage(any()) } returns folder.newFile()
        coEvery { backendService.getConfiguration() } returns TestData.exposureConfiguration()
        every { settingsRepository.appConfiguration } returns TestData.appConfig

        diagnosisKeyService = DiagnosisKeyService(backendService, appStateRepository, settingsRepository, systemOperations)
    }

    @Test
    fun sendSuccess() {
        runBlocking {
            val sentList = slot<DiagnosisKeyList>()
            val result = diagnosisKeyService.sendExposureKeys("1234", fakeKeyList(), listOf(), true)
            assertEquals(SendKeysResult.Success, result)

            coVerify { backendService.sendKeys(any(), capture(sentList)) }
            assertEquals(TestData.appConfig.diagnosisKeysPerSubmit, sentList.captured.keys.size)
        }
    }

    @Test
    fun sendAuthFailure() {
        coEvery { backendService.sendKeys(any(), any()) } throws
                HttpException(Response.error<String>(403, "".toResponseBody() ))

        runBlocking {
            val result = diagnosisKeyService.sendExposureKeys("1234", fakeKeyList(), listOf(), true)
            assertEquals(SendKeysResult.Unauthorized, result)
        }
    }

    @Test
    fun sendOtherFailure() {
        coEvery { backendService.sendKeys(any(), any()) } throws Exception()

        runBlocking {
            val result = diagnosisKeyService.sendExposureKeys("1234", fakeKeyList(), listOf(), true)
            assertEquals(SendKeysResult.Failed, result)
        }
    }

    @Test
    fun sendVisitedCountries() {
        every { settingsRepository.getExposureConfiguration() } returns TestData.exposureConfiguration().copy(
            participatingCountries = listOf("aa", "bb", "cc")
        )

        runBlocking {
            val sentData = slot<DiagnosisKeyList>()

            diagnosisKeyService.sendExposureKeys("1234", fakeKeyList(), listOf("bb"), true)
            coVerify { backendService.sendKeys(any(), capture(sentData)) }

            assertEquals(BackendService.NumericBoolean.FALSE, sentData.captured.visitedCountries["aa"])
            assertEquals(BackendService.NumericBoolean.TRUE, sentData.captured.visitedCountries["bb"])
            assertEquals(BackendService.NumericBoolean.FALSE, sentData.captured.visitedCountries["cc"])
        }
    }

    @Test
    fun normalDownload() {
        runBlocking {
            val result = diagnosisKeyService.downloadDiagnosisKeyFiles()
            assertEquals(batches.last(), result.lastSuccessfulBatchId)
        }
    }

    @Test
    fun fileDownloadFailure() {
        // last file fails, but operation succeeds with status indicating last successful file

        coEvery { backendService.getDiagnosisKeyFile(batches.last()) } throws Exception()
        runBlocking {
            val result = diagnosisKeyService.downloadDiagnosisKeyFiles()
            assertEquals(batches[batches.lastIndex-1], result.lastSuccessfulBatchId)
        }
    }

    @Test(expected = Exception::class)
    fun initialBatchIdFailure() {
        // there is no stored initial batch id, and network request fails

        coEvery { appStateRepository.getDiagnosisKeyBatchId() } throws Exception()
        runBlocking {
            diagnosisKeyService.downloadDiagnosisKeyFiles()
        }
    }

    @Test(expected = Exception::class)
    fun listFailure() {
        coEvery { backendService.listDiagnosisKeyBatches(any()) } throws Exception()
        runBlocking {
            diagnosisKeyService.downloadDiagnosisKeyFiles()
        }
    }

    @Test
    fun invalidStoredBatchIdFailure() {
        // assume corrupted batch id, and error returned from list endpoint
        coEvery { backendService.listDiagnosisKeyBatches(any()) } throws
                HttpException(Response.error<String>(400, "".toResponseBody() ))

        runBlocking {
            var exceptionThrown = false
            try {
                diagnosisKeyService.downloadDiagnosisKeyFiles()
            }
            catch (e: Exception) {
                exceptionThrown = true
            }

            assertTrue(exceptionThrown)
            verify { appStateRepository.resetDiagnosisKeyBatchId() }
        }
    }

    @Test
    fun countryCodesFiltered() {
        coEvery { backendService.getConfiguration() } returns TestData.exposureConfiguration().copy(
            participatingCountries = listOf("DE", "IE", "", "X", "test", "FI", " ", "&&", "3", "dk", "IT")
        )

        val expectedCountries = listOf("DE", "IE", "IT")

        runBlocking {
            val result = diagnosisKeyService.downloadDiagnosisKeyFiles()

            assertEquals(expectedCountries, result.exposureConfig.participatingCountries)

            val savedConfig = slot<ExposureConfigurationData>()
            verify { settingsRepository.updateExposureConfiguration(capture(savedConfig)) }
            assertEquals(expectedCountries, savedConfig.captured.participatingCountries)
        }
    }

    private fun fakeKeyList() = listOf(fakeExposureKey(), fakeExposureKey())

    private fun fakeExposureKey() = TemporaryExposureKey.TemporaryExposureKeyBuilder()
        .setKeyData(Random.nextBytes(16))
        .setRollingPeriod(144)
        .setRollingStartIntervalNumber((Instant.now().epochSecond / 600).toInt())
        .build()
}

