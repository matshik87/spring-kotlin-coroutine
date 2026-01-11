package org.bronco.payments.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.coroutineScope
import org.bronco.payments.controllers.api.AccountResponse
import org.bronco.payments.controllers.api.PaymentsErrorResponse
import org.bronco.payments.model.ResourceNotFoundException
import org.bronco.payments.model.ResourceType
import org.bronco.payments.repositories.account.model.toApiResponse
import org.bronco.payments.services.account.CustomerAccountService
import org.bronco.payments.services.account.model.BatchAccountCreation
import org.bronco.payments.services.account.model.BatchAccountCreationResponse
import org.bronco.payments.services.account.model.CreateCustomerAccountCommand
import org.bronco.payments.services.processes.model.ProcessProgressDetails
import org.bronco.payments.validation.customer.ValidUuid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.util.*
import io.swagger.v3.oas.annotations.parameters.RequestBody as ApiRequestBody

/*
 - 4. user can create an account(main process uuid is returned) --- ok, add tests
 - 2. user can retrieve an account by uuid - ok
 - 3. user can retrieve an accounts by process id - ok
 - 1. user can retrieve all accounts by customer id - ok
 - 6. closing an account ---
 - 5. modifying an account/s(activation etc) ---
 */
@Tag(name = "accounts", description = "Operations manages users' accounts")
@RestController
@RequestMapping(path = ["accounts"])
open class AccountController(
    private val accountService: CustomerAccountService,
) {
    @Operation(
        method = "GET",
        tags = ["accounts"],
        summary = """
            Retrieval of customer's accounts by customer id(UUID)
            """,
        parameters = [
            Parameter(
                name = "id", `in` = ParameterIn.PATH, description = "customer id", required = true,
                schema = Schema(type = "uuid")
            ),
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Accounts were found for customer",
                content = [
                    Content(
                        mediaType = "application/json",
                        array = ArraySchema(schema = Schema(implementation = AccountResponse::class))
                    )
                ]
            ),
            ApiResponse(
                responseCode = "404",
                description = "No account was found",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = PaymentsErrorResponse::class)
                    )
                ]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Request is invalid. Most probably user does not exist",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = PaymentsErrorResponse::class)
                    )
                ]
            )
        ]
    )
    @Validated
    @GetMapping(path = ["/customer/{id}"])
    suspend fun retrieveAccountsByCustomerId(
        @ValidUuid @PathVariable("id") customerId: String,
    ): ResponseEntity<List<AccountResponse>> = coroutineScope {
        val customerUuid = UUID.fromString(customerId)
        val payload = accountService.getAccountsByCustomerId(customerUuid)
            .ifEmpty {
                throw ResourceNotFoundException(customerUuid, ResourceType.CUSTOMER_ACCOUNT)
            }
            .map { accountData -> accountData.toApiResponse() }
        ResponseEntity.ok(payload)
    }

    @Operation(
        method = "GET",
        tags = ["accounts"],
        summary = """
            Retrieval an account by id
            """,
        parameters = [
            Parameter(
                name = "id", `in` = ParameterIn.PATH, description = "customer id", required = true,
                schema = Schema(type = "uuid")
            ),
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Customer account was found",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = AccountResponse::class)
                    )
                ]
            ),
            ApiResponse(
                responseCode = "404",
                description = "No account was found",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = PaymentsErrorResponse::class)
                    )
                ]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Request is invalid. Most probably user does not exist",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = PaymentsErrorResponse::class)
                    )
                ]
            )
        ]
    )
    @Validated
    @GetMapping(path = ["/{id}"])
    suspend fun retrieveAccountById(
        @ValidUuid @PathVariable("id") accountId: String,
    ): ResponseEntity<AccountResponse> = coroutineScope {
        val accountUuid = UUID.fromString(accountId)
        val payload = accountService.getById(accountUuid)?.toApiResponse()
        ResponseEntity.ok(payload ?: throw ResourceNotFoundException(accountUuid, ResourceType.ACCOUNT))
    }

    @Operation(
        method = "GET",
        tags = ["accounts"],
        summary = """
            Retrieves an account by process progress id. Otherwise, an empty list is returned.
            """,
        parameters = [
            Parameter(
                name = "id", `in` = ParameterIn.PATH, description = "process progress id", required = true,
                schema = Schema(type = "uuid")
            ),
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = """
                    When an account for specified process id was found, then returned list includes that account.
                    Otherwise, an empty response is returned.
                    """,
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = AccountResponse::class)
                    )
                ]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Request is invalid. Most probably invalid uuid",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = PaymentsErrorResponse::class)
                    )
                ]
            )
        ]
    )
    @Validated
    @GetMapping(path = ["/process/{id}"])
    suspend fun retrieveAccountsByProcessId(
        @ValidUuid @PathVariable("id") processId: String,
    ): ResponseEntity<List<AccountResponse>> = coroutineScope {
        val payload = accountService.getAccountsByProcessId(UUID.fromString(processId))
            .map { it.toApiResponse() }
        ResponseEntity.ok(payload)
    }

    @Operation(
        method = "GET",
        tags = ["accounts"],
        summary = """
            Retrieves accounts by parent process progress id. Otherwise, an empty list is returned.
            """,
        parameters = [
            Parameter(
                name = "id", `in` = ParameterIn.PATH, description = "process progress id", required = true,
                schema = Schema(type = "uuid")
            ),
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = """
                    When an account for specified parent process id was found, then returned list includes those account.
                    Otherwise, an empty response is returned.
                    """,
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = AccountResponse::class)
                    )
                ]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Request is invalid. Most probably invalid uuid",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = PaymentsErrorResponse::class)
                    )
                ]
            )
        ]
    )
    @Validated
    @GetMapping(path = ["/process/parent/{id}"])
    suspend fun retrieveAccountsByParentProcessId(
        @ValidUuid @PathVariable("id") parentProcessId: String,
    ): ResponseEntity<List<AccountResponse>> = coroutineScope {
        val payload = accountService.getAccountsByParentProcessId(UUID.fromString(parentProcessId))
            .map { it.toApiResponse() }
        ResponseEntity.ok(payload)
    }

    @Operation(
        method = "PUT",
        tags = ["accounts"],
        summary = """
            This endpoint schedules an account creation for specified payload.
             The returned process details describes the result.
            """,
        requestBody = ApiRequestBody(
            required = true,
            description = "Payload describing an account",
            content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = CreateCustomerAccountCommand::class)
            )]
        ),
        responses = [
            ApiResponse(
                responseCode = "202",
                description = "A payload describing process details for account creation",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ProcessProgressDetails::class)
                    )
                ]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Request was invalid. The details are provided in the response.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = PaymentsErrorResponse::class)
                    )
                ]
            )
        ]
    )
    @Validated
    @PutMapping(path = ["/create"])
    @ResponseStatus(HttpStatus.ACCEPTED)
    suspend fun scheduleAccountCreation(
        @RequestBody payload: CreateCustomerAccountCommand
    ): ProcessProgressDetails = coroutineScope {
        accountService.scheduleNewAccountCreation(UUID.randomUUID(), payload)
    }

    @Operation(
        method = "PUT",
        tags = ["accounts"],
        summary = """
            This endpoint schedules batch account creation as per provided payload, that describes all accounts.
             The returned process details describes the result.
            """,
        requestBody = ApiRequestBody(
            required = true,
            description = "Batch definition of account creation.",
            content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = BatchAccountCreation::class)
            )]
        ),
        responses = [
            ApiResponse(
                responseCode = "202",
                description = "A payload describing process details for batch account creation",
                content = [
                    Content(
                        mediaType = "application/json",

                        schema = Schema(implementation = BatchAccountCreationResponse::class)
                    )
                ]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Request was invalid. The details are provided in the response.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = PaymentsErrorResponse::class)
                    )
                ]
            )
        ]
    )
    @Validated
    @PutMapping(path = ["/create/batch"])
    @ResponseStatus(HttpStatus.ACCEPTED)
    suspend fun scheduleNewAccountsCreation(
        @RequestBody batch: BatchAccountCreation
    ): BatchAccountCreationResponse = coroutineScope {
        BatchAccountCreationResponse(batch.requests.groupBy { it.customerId }
            .map { (customerId, requests) ->
                customerId to accountService.scheduleNewAccountCreation(UUID.randomUUID(), customerId, requests)
            }.toMap())
    }
}