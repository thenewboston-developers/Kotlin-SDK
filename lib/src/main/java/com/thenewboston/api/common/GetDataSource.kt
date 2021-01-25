package com.thenewboston.api.common

import com.thenewboston.common.http.NetworkClient
import com.thenewboston.common.http.Outcome
import com.thenewboston.data.dto.bankapi.accountdto.response.AccountList
import com.thenewboston.data.dto.bankapi.bankdto.response.BankList
import com.thenewboston.data.dto.bankapi.banktransactiondto.BankTransactionList
import com.thenewboston.data.dto.bankapi.blockdto.BlockList
import com.thenewboston.data.dto.bankapi.clean.response.Clean
import com.thenewboston.data.dto.bankapi.configdto.BankDetails
import com.thenewboston.data.dto.bankapi.crawl.response.Crawl
import com.thenewboston.data.dto.bankapi.invalidblockdto.InvalidBlockList
import com.thenewboston.data.dto.bankapi.validatorconfirmationservicesdto.ConfirmationServicesList
import com.thenewboston.data.dto.common.response.Validator
import com.thenewboston.data.dto.common.response.ValidatorList
import com.thenewboston.utils.BankAPIEndpoints
import com.thenewboston.utils.ErrorMessages
import com.thenewboston.utils.PaginationOptions
import io.ktor.client.request.*
import io.ktor.utils.io.errors.*
import javax.inject.Inject

class GetDataSource @Inject constructor(private val networkClient: NetworkClient) {

    suspend fun banks(pagination: PaginationOptions): Outcome<BankList> {
        val endpoint = BankAPIEndpoints.BANKS_ENDPOINT + pagination.toQuery()
        val result = networkClient.defaultClient.get<BankList>(endpoint)

        return when {
            result.banks.isEmpty() -> Outcome.Error(ErrorMessages.EMPTY_LIST_MESSAGE, IOException())
            else -> Outcome.Success(result)
        }
    }

    suspend fun bankDetail(): Outcome<BankDetails> {
        val result = networkClient.defaultClient.get<BankDetails>(BankAPIEndpoints.CONFIG_ENDPOINT)

        return Outcome.Success(result)
    }

    suspend fun bankTransactions(pagination: PaginationOptions): Outcome<BankTransactionList> {
        val endpoint = BankAPIEndpoints.BANK_TRANSACTIONS_ENDPOINT + pagination.toQuery()
        val result = networkClient.defaultClient.get<BankTransactionList>(endpoint)

        return when {
            result.bankTransactions.isEmpty() ->
                Outcome.Error("Error bank transactions", IOException())
            else -> Outcome.Success(result)
        }
    }

    suspend fun validators(pagination: PaginationOptions): Outcome<ValidatorList> {
        val endpoint = BankAPIEndpoints.VALIDATORS_ENDPOINT + pagination.toQuery()
        val validators = networkClient.defaultClient.get<ValidatorList>(endpoint)

        return when {
            validators.results.isEmpty() -> Outcome.Error(ErrorMessages.EMPTY_LIST_MESSAGE, IOException())
            else -> Outcome.Success(validators)
        }
    }

    suspend fun validator(nodeIdentifier: String): Outcome<Validator> {
        val validatorsEndpoint = BankAPIEndpoints.VALIDATORS_ENDPOINT
        val urlSuffix = "$validatorsEndpoint/$nodeIdentifier"
        val response = networkClient.defaultClient.get<Validator>(urlSuffix)

        return Outcome.Success(response)
    }

    suspend fun accounts(pagination: PaginationOptions): Outcome<AccountList> {
        val endpoint = BankAPIEndpoints.ACCOUNTS_ENDPOINT + pagination.toQuery()
        val accounts = networkClient.defaultClient.get<AccountList>(endpoint)

        return when {
            accounts.results.isEmpty() -> Outcome.Error(
                ErrorMessages.EMPTY_LIST_MESSAGE,
                IOException()
            )
            else -> Outcome.Success(accounts)
        }
    }

    suspend fun blocks(pagination: PaginationOptions): Outcome<BlockList> {
        val endpoint = BankAPIEndpoints.BLOCKS_ENDPOINT + pagination.toQuery()
        val response = networkClient.defaultClient.get<BlockList>(endpoint)

        return when {
            response.blocks.isEmpty() -> Outcome.Error(
                ErrorMessages.EMPTY_LIST_MESSAGE,
                IOException()
            )
            else -> Outcome.Success(response)
        }
    }

    suspend fun invalidBlocks(pagination: PaginationOptions): Outcome<InvalidBlockList> {
        val endpoint = BankAPIEndpoints.INVALID_BLOCKS_ENDPOINT + pagination.toQuery()
        val invalidBlocks = networkClient.defaultClient
            .get<InvalidBlockList>(endpoint)

        return when {
            invalidBlocks.results.isEmpty() -> Outcome.Error(
                "No invalid blocks are available at this time",
                IOException()
            )
            else -> Outcome.Success(invalidBlocks)
        }
    }

    suspend fun validatorConfirmationServices(pagination: PaginationOptions): Outcome<ConfirmationServicesList> {
        val endpoint = BankAPIEndpoints.VALIDATOR_CONFIRMATION_SERVICES_ENDPOINT + pagination.toQuery()
        val response = networkClient.defaultClient.get<ConfirmationServicesList>(endpoint)

        return when {
            response.services.isEmpty() -> {
                val message = ErrorMessages.EMPTY_LIST_MESSAGE
                Outcome.Error(message, IOException())
            }
            else -> Outcome.Success(response)
        }
    }

    suspend fun clean(): Outcome<Clean> {
        val response = networkClient.defaultClient.get<Clean>(BankAPIEndpoints.CLEAN_ENDPOINT)

        return when {
            response.cleanStatus.isEmpty() -> Outcome.Error(
                "The network clean process is not successful",
                IOException()
            )
            else -> Outcome.Success(response)
        }
    }

    suspend fun crawl(): Outcome<Crawl> {
        val response = networkClient.defaultClient.get<Crawl>(BankAPIEndpoints.CRAWL_ENDPOINT)

        return when {
            response.crawlStatus.isEmpty() -> Outcome.Error(
                "The network crawling process is not successful",
                IOException()
            )
            else -> Outcome.Success(response)
        }
    }
}
