package com.thenewboston.api.bankapi.datasource

import com.thenewboston.common.http.NetworkClient
import com.thenewboston.common.http.Outcome
import com.thenewboston.common.http.makeApiCall
import com.thenewboston.data.dto.bankapi.accountdto.response.Account
import com.thenewboston.data.dto.bankapi.accountdto.response.AccountList
import com.thenewboston.data.dto.bankapi.bankdto.response.BankList
import com.thenewboston.data.dto.bankapi.banktransactiondto.BankTransactionList
import com.thenewboston.data.dto.bankapi.blockdto.Block
import com.thenewboston.data.dto.bankapi.blockdto.BlockList
import com.thenewboston.data.dto.bankapi.blockdto.request.PostBlockRequest
import com.thenewboston.data.dto.bankapi.clean.request.PostCleanRequest
import com.thenewboston.data.dto.bankapi.clean.response.Clean
import com.thenewboston.data.dto.bankapi.common.request.UpdateTrustRequest
import com.thenewboston.data.dto.bankapi.common.response.Bank
import com.thenewboston.data.dto.bankapi.configdto.BankDetails
import com.thenewboston.data.dto.bankapi.connectionrequestsdto.ConnectionRequest
import com.thenewboston.data.dto.bankapi.crawl.response.Crawl
import com.thenewboston.data.dto.bankapi.invalidblockdto.InvalidBlock
import com.thenewboston.data.dto.bankapi.invalidblockdto.InvalidBlockList
import com.thenewboston.data.dto.bankapi.invalidblockdto.request.PostInvalidBlockRequest
import com.thenewboston.data.dto.bankapi.upgradenoticedto.UpgradeNoticeRequest
import com.thenewboston.data.dto.bankapi.validatorconfirmationservicesdto.ConfirmationServices
import com.thenewboston.data.dto.bankapi.validatorconfirmationservicesdto.ConfirmationServicesList
import com.thenewboston.data.dto.bankapi.validatorconfirmationservicesdto.request.PostConfirmationServicesRequest
import com.thenewboston.data.dto.bankapi.validatordto.Validator
import com.thenewboston.data.dto.bankapi.validatordto.ValidatorList
import com.thenewboston.utils.BankAPIEndpoints
import com.thenewboston.utils.ErrorMessages
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.utils.io.errors.*
import javax.inject.Inject

@KtorExperimentalAPI
class BankDataSource @Inject constructor(private val networkClient: NetworkClient) {

    suspend fun fetchBanks() = makeApiCall(
        call = { banks() },
        errorMessage = "Failed to retrieve banks"
    )

    private suspend fun banks(): Outcome<BankList> {
        val result = networkClient.defaultClient.get<BankList>(BankAPIEndpoints.BANKS_ENDPOINT)

        return when {
            result.banks.isNullOrEmpty() -> Outcome.Error(ErrorMessages.EMPTY_LIST_MESSAGE, IOException())
            else -> Outcome.Success(result)
        }
    }

    suspend fun fetchBankDetails() = makeApiCall(
        call = { bankDetail() },
        errorMessage = "Failed to retrieve bank details"
    )

    private suspend fun bankDetail(): Outcome<BankDetails> {
        val result = networkClient.defaultClient.get<BankDetails>(BankAPIEndpoints.CONFIG_ENDPOINT)

        return Outcome.Success(result)
    }

    suspend fun fetchBankTransactions() = makeApiCall(
        call = { bankTransactions() },
        errorMessage = "Failed to retrieve bank transactions"
    )

    private suspend fun bankTransactions(): Outcome<BankTransactionList> {
        val endpoint = BankAPIEndpoints.BANK_TRANSACTIONS_ENDPOINT
        val result = networkClient.defaultClient.get<BankTransactionList>(endpoint)

        return when {
            result.bankTransactions.isNullOrEmpty() ->
                Outcome.Error("Error bank transactions", IOException())
            else -> Outcome.Success(result)
        }
    }

    suspend fun fetchValidators(): Outcome<ValidatorList> = makeApiCall(
        call = { doFetchValidators() },
        errorMessage = "Could not fetch list of validators"
    )

    private suspend fun doFetchValidators(): Outcome<ValidatorList> {
        val endpoint = BankAPIEndpoints.VALIDATORS_ENDPOINT
        val validators = networkClient.defaultClient.get<ValidatorList>(endpoint)

        return when {
            validators.results.isNullOrEmpty() -> Outcome.Error(ErrorMessages.EMPTY_LIST_MESSAGE, IOException())
            else -> Outcome.Success(validators)
        }
    }

    suspend fun fetchValidator(nodeIdentifier: String): Outcome<Validator> = makeApiCall(
        call = { doFetchValidator(nodeIdentifier) },
        errorMessage = "Could not fetch validator with NID $nodeIdentifier"
    )

    private suspend fun doFetchValidator(nodeIdentifier: String): Outcome<Validator> {
        val validatorsEndpoint = BankAPIEndpoints.VALIDATORS_ENDPOINT
        val urlSuffix = "$validatorsEndpoint/$nodeIdentifier"
        val response = networkClient.defaultClient.get<Validator>(urlSuffix)

        return Outcome.Success(response)
    }

    suspend fun fetchAccounts(): Outcome<AccountList> = makeApiCall(
        call = { accounts() },
        errorMessage = "Could not fetch list of accounts"
    )

    private suspend fun accounts(): Outcome<AccountList> {
        val urlString = BankAPIEndpoints.ACCOUNTS_ENDPOINT
        val accounts = networkClient.defaultClient.get<AccountList>(urlString)

        return when {
            accounts.results.isNullOrEmpty() -> Outcome.Error(
                ErrorMessages.EMPTY_LIST_MESSAGE,
                IOException()
            )
            else -> Outcome.Success(accounts)
        }
    }

    suspend fun fetchBlocks(): Outcome<BlockList> = makeApiCall(
        call = { blocks() },
        errorMessage = "Could not fetch list of blocks"
    )

    private suspend fun blocks(): Outcome<BlockList> {
        val response = networkClient.defaultClient.get<BlockList>(BankAPIEndpoints.BLOCKS_ENDPOINT)

        return when {
            response.blocks.isNullOrEmpty() -> Outcome.Error(
                ErrorMessages.EMPTY_LIST_MESSAGE,
                IOException()
            )
            else -> Outcome.Success(response)
        }
    }

    suspend fun updateAccountTrust(accountNumber: String, request: UpdateTrustRequest): Outcome<Account> = makeApiCall(
        call = { doUpdateAccount(accountNumber, request) },
        errorMessage = "Could not update trust level of given account"
    )

    private suspend fun doUpdateAccount(accountNumber: String, request: UpdateTrustRequest): Outcome<Account> {
        val patchAccountUrl = "${BankAPIEndpoints.ACCOUNTS_ENDPOINT}/$accountNumber"

        val updatedAccount = networkClient.defaultClient.patch<Account> {
            url(patchAccountUrl)
            contentType(ContentType.Application.Json)
            body = request
        }

        return when {
            updatedAccount.id.isBlank() -> Outcome.Error(
                "Received unexpected response when updating trust level of account $accountNumber",
                IOException()
            )
            else -> Outcome.Success(updatedAccount)
        }
    }

    suspend fun updateBankTrust(request: UpdateTrustRequest): Outcome<Bank> = makeApiCall(
        call = { doUpdateBankTrust(request) },
        errorMessage = "Could not send bank trust for ${request.nodeIdentifier}"
    )

    private suspend fun doUpdateBankTrust(request: UpdateTrustRequest): Outcome<Bank> {
        val url = "${BankAPIEndpoints.BANKS_ENDPOINT}/${request.nodeIdentifier}"

        val response = networkClient.defaultClient.patch<Bank> {
            url(url)
            body = request
        }

        return when {
            response.accountNumber.isBlank() -> {
                val message = "Received invalid request when updating trust level of bank with" +
                    " ${request.nodeIdentifier}"
                Outcome.Error(message, IOException())
            }
            else -> {
                Outcome.Success(response)
            }
        }
    }

    suspend fun fetchInvalidBlocks(): Outcome<InvalidBlockList> = makeApiCall(
        call = { getInvalidBlocks() },
        errorMessage = "Could not fetch list of invalid blocks"
    )

    private suspend fun getInvalidBlocks(): Outcome<InvalidBlockList> {
        val invalidBlocks = networkClient.defaultClient
            .get<InvalidBlockList>(BankAPIEndpoints.INVALID_BLOCKS_ENDPOINT)

        return when {
            invalidBlocks.results.isNullOrEmpty() -> Outcome.Error(
                "No invalid blocks are available at this time",
                IOException()
            )
            else -> Outcome.Success(invalidBlocks)
        }
    }

    suspend fun sendInvalidBlock(request: PostInvalidBlockRequest): Outcome<InvalidBlock> = makeApiCall(
        call = { doSendInvalidBlock(request) },
        errorMessage = "An error occurred while sending invalid block"
    )

    private suspend fun doSendInvalidBlock(request: PostInvalidBlockRequest): Outcome<InvalidBlock> {
        val response = networkClient.defaultClient.patch<InvalidBlock> {
            url(BankAPIEndpoints.INVALID_BLOCKS_ENDPOINT)
            body = request
        }

        return when {
            response.blockIdentifier.isBlank() -> {
                val blockIdentifier = request.message.blockIdentifier
                val message = "Received invalid response when sending invalid block with identifier $blockIdentifier"
                Outcome.Error(message, IOException())
            }
            else -> Outcome.Success(response)
        }
    }

    suspend fun sendBlock(request: PostBlockRequest): Outcome<Block> = makeApiCall(
        call = { doSendBlock(request) },
        errorMessage = "An error occurred while sending the block"
    )

    private suspend fun doSendBlock(request: PostBlockRequest): Outcome<Block> {
        val response = networkClient.defaultClient.patch<Block> {
            url(BankAPIEndpoints.BLOCKS_ENDPOINT)
            body = request
        }

        return when {
            response.balanceKey.isBlank() -> {
                val balanceKey = request.message.balanceKey
                val message = "Received invalid response when sending block with balance key: $balanceKey"
                Outcome.Error(message, IOException())
            }
            else -> Outcome.Success(response)
        }
    }

    suspend fun fetchValidatorConfirmationServices() = makeApiCall(
        call = { getValidatorConfirmationServices() },
        errorMessage = "An error occurred while fetching validator confirmation services"
    )

    private suspend fun getValidatorConfirmationServices(): Outcome<ConfirmationServicesList> {
        val endpoint = BankAPIEndpoints.VALIDATOR_CONFIRMATION_SERVICES_ENDPOINT
        val response = networkClient.defaultClient.get<ConfirmationServicesList>(endpoint)

        return when {
            response.services.isNullOrEmpty() -> {
                val message = ErrorMessages.EMPTY_LIST_MESSAGE
                Outcome.Error(message, IOException())
            }
            else -> Outcome.Success(response)
        }
    }

    suspend fun sendValidatorConfirmationServices(request: PostConfirmationServicesRequest) = makeApiCall(
        call = { doSendValidatorConfirmationServices(request) },
        errorMessage = "An error occurred while sending validator confirmation services"
    )

    private suspend fun doSendValidatorConfirmationServices(request: PostConfirmationServicesRequest): Outcome<ConfirmationServices> {
        val response = networkClient.defaultClient.post<ConfirmationServices> {
            url(BankAPIEndpoints.VALIDATOR_CONFIRMATION_SERVICES_ENDPOINT)
            body = request
        }

        return when {
            response.id.isBlank() -> {
                val nodeIdentifier = request.nodeIdentifier
                val message =
                    "Received invalid response sending confirmation services with node identifier: $nodeIdentifier"
                return Outcome.Error(message, IOException())
            }
            else -> Outcome.Success(response)
        }
    }

    suspend fun sendUpgradeNotice(request: UpgradeNoticeRequest) = makeApiCall(
        call = { doSendUpgradeNotice(request) },
        errorMessage = "An error occurred while sending upgrade notice"
    )

    private suspend fun doSendUpgradeNotice(request: UpgradeNoticeRequest): Outcome<String> {
        networkClient.defaultClient.post<HttpResponse> {
            url(BankAPIEndpoints.UPGRADE_NOTICE_ENDPOINT)
            body = request
        }

        // Return success as response body is empty
        return Outcome.Success("Successfully sent upgrade notice")
    }

    suspend fun fetchClean() = makeApiCall(
        call = { getClean() },
        errorMessage = "Failed to update the network"
    )

    private suspend fun getClean(): Outcome<Clean> {
        val response = networkClient.defaultClient.get<Clean>(BankAPIEndpoints.CLEAN_ENDPOINT)

        return when {
            response.cleanStatus.isEmpty() -> Outcome.Error(
                "The network clean process is not successful",
                IOException()
            )
            else -> Outcome.Success(response)
        }
    }

    suspend fun fetchCrawl() = makeApiCall(
        call = { getCrawl() },
        errorMessage = "An error occurred while sending crawl request"
    )

    private suspend fun getCrawl(): Outcome<Crawl> {
        val response = networkClient.defaultClient.get<Crawl>(BankAPIEndpoints.CRAWL_ENDPOINT)

        return when {
            response.crawlStatus.isEmpty() -> Outcome.Error(
                "The network crawling process is not successful",
                IOException()
            )
            else -> Outcome.Success(response)
        }
    }

    suspend fun sendClean(request: PostCleanRequest): Outcome<Clean> = makeApiCall(
        call = { doSendClean(request) },
        errorMessage = "An error occurred while sending the clean request"
    )

    private suspend fun doSendClean(request: PostCleanRequest): Outcome<Clean> {
        val response = networkClient.defaultClient.post<Clean> {
            url(BankAPIEndpoints.CLEAN_ENDPOINT)
            body = request
        }

        return when {
            response.cleanStatus.isEmpty() -> {
                val clean = request.data.clean
                val message = "Received invalid response when sending block with clean: $clean"
                Outcome.Error(message, IOException())
            }
            else -> Outcome.Success(response)
        }
    }

    suspend fun sendConnectionRequests(request: ConnectionRequest) = makeApiCall(
        call = { doSendConnectionRequests(request) },
        errorMessage = "An error occurred while sending connection requests"
    )

    private suspend fun doSendConnectionRequests(request: ConnectionRequest): Outcome<String> {
        networkClient.defaultClient.post<HttpResponse> {
            url(BankAPIEndpoints.CONNECTION_REQUESTS_ENDPOINT)
            body = request
        }

        return Outcome.Success("Successfully sent connection requests")
    }
}
