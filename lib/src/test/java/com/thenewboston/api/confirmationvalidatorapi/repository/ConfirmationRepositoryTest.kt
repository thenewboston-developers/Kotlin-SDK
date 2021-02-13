package com.thenewboston.api.confirmationvalidatorapi.repository

import com.thenewboston.api.confirmationvalidatorapi.datasource.ConfirmationDataSource
import com.thenewboston.common.http.Outcome
import com.thenewboston.data.dto.bankapi.clean.response.Clean
import com.thenewboston.data.dto.common.response.AccountListValidator
import com.thenewboston.utils.Mocks
import io.kotest.matchers.should
import io.kotest.matchers.types.beInstanceOf
import io.ktor.util.*
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@ExperimentalCoroutinesApi
@KtorExperimentalAPI
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConfirmationRepositoryTest {

    @MockK
    lateinit var dataSource: ConfirmationDataSource

    lateinit var repository: ConfirmationRepository

    @BeforeAll
    fun setup() {
        MockKAnnotations.init(this)

        repository = ConfirmationRepository(dataSource)
    }

    @Test
    fun `verify accounts returns success outcome`() = runBlockingTest {
        val pagination = Mocks.paginationOptionsDefault()
        coEvery { dataSource.fetchAccounts(pagination) } returns Outcome.Success(Mocks.accountsFromValidator(pagination))

        val response = repository.accounts(0, 20)
        coVerify { dataSource.fetchAccounts(pagination) }
        response should beInstanceOf<Outcome.Success<AccountListValidator>>()
    }

    @Test
    fun `verify accounts returns error outcome`() = runBlockingTest {
        val pagination = Mocks.paginationOptionsDefault()
        val message = "Error while retrieving accounts"
        coEvery { dataSource.fetchAccounts(pagination) } returns Outcome.Error(message, IOException())

        val response = repository.accounts(0, 20)
        coVerify { dataSource.fetchAccounts(pagination) }
        response should beInstanceOf<Outcome.Error>()
    }

    @Test
    fun `verify send clean returns success outcome`() = runBlockingTest {
        val response = Mocks.cleanSuccess()
        coEvery { dataSource.sendClean(any()) } returns Outcome.Success(response)
        val postRequest = Mocks.postCleanRequest()

        // when
        val result = repository.sendClean(postRequest)

        // then
        coVerify { dataSource.sendClean(postRequest) }
        result should beInstanceOf<Outcome.Success<Clean>>()
    }

    @Test
    fun `verify send clean returns error outcome`() = runBlockingTest {
        val error = Outcome.Error("An error occurred while sending the clean request", IOException())
        val request = Mocks.postCleanRequest()
        coEvery { dataSource.sendClean(request) } returns error

        // when
        val result = repository.sendClean(request)

        // then
        coVerify { dataSource.sendClean(request) }
        result should beInstanceOf<Outcome.Error>()
    }
}
