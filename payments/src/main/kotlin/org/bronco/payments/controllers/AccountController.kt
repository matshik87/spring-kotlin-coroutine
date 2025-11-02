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
import org.bronco.payments.validation.customer.ValidUuid
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

//TODO: create account managements by the means of
/*
 - 4. user can create an account for existing customer(process uuid is returned) --- in process
 - 2. user can retrieve an account by uuid - ok
 - 3. user can retrieve an accounts by process id - ok
 - 1. user can retrieve all accounts by customer id - ok
 - 6. closing an account ---
 - 5. modifying an account/s ---
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
            Retrieves accounts by process progress id. It may be a parent process progress id,
             like when creating a customer, or an actual account creation progress id.
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
                    When process and accounts were found then populated response is returned.
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
        val payload = accountService.getAccountsForProcessId(UUID.fromString(processId))
            .map { it.toApiResponse() }
        ResponseEntity.ok(payload)
    }

    /*@Operation(
        method = "POST",
        tags = ["accounts"],
        summary = """
            Successful request with process UUID and process name results with either a response with process details or
            Bad Request is returned. The result response may include entity id, if such id was included. In case of 
            failed processing, a detailed error message is being included. The entity Id may be included, if such was
            persisted during the process.
            """,
        parameters = [
            Parameter(
                name = "processName", `in` = ParameterIn.PATH, description = "Process type name", required = true,
                examples = [ExampleObject(value = "CREATE_CUSTOMER")],
                schema = Schema(type = "string", implementation = ProcessName::class)
            ),
            Parameter(
                name = "processId",
                `in` = ParameterIn.PATH,
                description = "UUID identifying a process",
                required = true,
                examples = [ExampleObject(value = "05b3b0f6-85af-4593-9bd1-eaa182702405")],
                schema = Schema(type = "uuid")
            )
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "A payload describing the state of a process",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(oneOf = [ProcessProgressDetails::class])
                    )
                ]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Process could not be found",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = PaymentsErrorResponse::class)
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
    @PostMapping(path = ["/open"])
    suspend fun retrieveDetailsForProcessId(
        @ValidUuid @PathVariable processId: String
    ): ResponseEntity<List<ProcessProgressDetails>> = coroutineScope {
        val processUuid = UUID.fromString(processId)
        repository.retrieveProcessDetails(UUID.fromString(processId))
            ?.let { response ->
                ResponseEntity.ok(response)
            } ?: throw ResourceNotFoundException(processUuid, ResourceType.PROCESS)
    }*/
}