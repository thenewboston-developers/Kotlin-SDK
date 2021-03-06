package com.thenewboston.utils

object ConfirmationValidatorAPIEndpoints {

    const val CONFIRMATION_BLOCKS_ENDPOINT = "/confirmation_blocks"

    const val BANK_CONFIRMATION_SERVICES = "/bank_confirmation_services"

    const val UPGRADE_REQUEST = "/upgrade_request"

    const val PRIMARY_VALIDATOR_UPDATED = "/primary_validator_updated"

    fun validConfirmationBlocksEndpoint(blockID: String) =
        "/confirmation_blocks/$blockID/valid"

    fun queuedConfirmationBlocksEndpoint(blockID: String) =
        "/confirmation_blocks/$blockID/queued"
}
