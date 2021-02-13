package com.thenewboston.api.confirmationvalidatorapi.datasource

import com.thenewboston.api.common.GetDataSource
import com.thenewboston.api.common.PostDataSource
import com.thenewboston.common.http.Outcome
import com.thenewboston.utils.Mocks
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.kotest.matchers.string.beEmpty
import io.kotest.matchers.types.beInstanceOf
import io.ktor.util.*
import io.ktor.utils.io.errors.*
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.*

@ExperimentalCoroutinesApi
@KtorExperimentalAPI
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConfirmationDataSourceTest {

    @MockK
    lateinit var getDataSource: GetDataSource

    @MockK
    lateinit var postDataSource: PostDataSource

    @MockK
    lateinit var confirmationDataSource: ConfirmationDataSource

    @BeforeAll
    fun setup() {
        MockKAnnotations.init(this)

        confirmationDataSource = ConfirmationDataSource(getDataSource, postDataSource)
    }

    @Nested
    @DisplayName("Given successful request...")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GivenSucceedingRequest {

        private val paginationTwenty = Mocks.paginationOptionsTwenty()
        private val paginationThirty = Mocks.paginationOptionsThirty()

        @Nested
        @DisplayName("When performing a GET request...")
        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        inner class WhenGetRequest {

            @Test
            fun `should fetch list of 20 accounts successfully from primary validator`() = runBlockingTest {
                val value = Mocks.accountsFromValidator(paginationTwenty)
                coEvery { getDataSource.accountsFromValidator(paginationTwenty) } returns Outcome.Success(value)

                val response = confirmationDataSource.fetchAccounts(paginationTwenty)

                check(response is Outcome.Success)
                response.value.results.shouldNotBeEmpty()
                response.value.count shouldBeGreaterThan 20
                response.value.results.size shouldBeLessThanOrEqual 20
            }

            @Test
            fun `should fetch list of 30 accounts successfully`() = runBlockingTest {
                val value = Mocks.accountsFromValidator(paginationThirty)

                coEvery { getDataSource.accountsFromValidator(paginationThirty) } returns Outcome.Success(value)
                val response = confirmationDataSource.fetchAccounts(paginationThirty)

                check(response is Outcome.Success)
                response.value.results.shouldNotBeEmpty()
                response.value.count shouldBeGreaterThan 0
                response.value.results.size shouldBeLessThanOrEqual 30
            }
        }

        @Nested
        @DisplayName("When performing POST request...")
        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        inner class WhenPostRequest {

            @Test
            fun `should return success with clean status `() = runBlockingTest {
                // given
                val request = Mocks.postCleanRequest()
                val value = Mocks.postClean(request.data.clean)
                coEvery { postDataSource.doSendClean(request) } returns Outcome.Success(value)

                // when
                val response = confirmationDataSource.sendClean(request)

                // then
                check(response is Outcome.Success)
                response.value.cleanStatus shouldNot beEmpty()
                response.value.cleanStatus shouldBe request.data.clean
            }
        }
    }

    @Nested
    @DisplayName("Given request that should fail")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GivenFailingRequest {

        private val pagination = Mocks.paginationOptionsDefault()

        @Nested
        @DisplayName("When performing a GET request...")
        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        inner class WhenGetRequest {

            @Test
            fun `should return error outcome for list of accounts from primary validator IOException`() = runBlockingTest {
                val message = "Failed to retrieve accounts from primary validator"
                coEvery { getDataSource.accountsFromValidator(pagination) } returns Outcome.Error(message, IOException())

                val response = confirmationDataSource.fetchAccounts(pagination)

                check(response is Outcome.Error)
                response.cause should beInstanceOf<IOException>()
                response.message shouldBe message
            }
        }

        @Nested
        @DisplayName("When sending POST request...")
        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        inner class WhenPostRequest {

            @Test
            fun `should return error outcome when sending clean`() = runBlockingTest {
                // given
                val request = Mocks.postCleanRequest()
                val message = "An error occurred while sending the clean request"
                coEvery { postDataSource.doSendClean(request) } returns Outcome.Error(message, IOException())

                // when
                val response = confirmationDataSource.sendClean(request)

                // then
                check(response is Outcome.Error)
                response.cause should beInstanceOf<IOException>()
                response.message shouldBe message
            }
        }
    }
}
