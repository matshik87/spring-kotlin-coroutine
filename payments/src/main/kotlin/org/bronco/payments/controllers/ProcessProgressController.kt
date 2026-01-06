package org.bronco.payments.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.coroutineScope
import org.bronco.payments.controllers.api.PaymentsErrorResponse
import org.bronco.payments.model.ResourceNotFoundException
import org.bronco.payments.model.ResourceType
import org.bronco.payments.repositories.progress.impl.ProcessProgressRepository
import org.bronco.payments.services.processes.model.ProcessProgressDetails
import org.bronco.payments.services.processes.model.ProcessType
import org.bronco.payments.validation.customer.IsProcessTypeValid
import org.bronco.payments.validation.customer.ValidUuid
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

//TODO: sth is wrong on swagger with response payload schema - it's not displayed;/
@Tag(name = "process", description = "Operations regards processes progress")
@RestController
@RequestMapping(path = ["processProgress"])
open class ProcessProgressController(
    private val repository: ProcessProgressRepository
) {
    @Operation(
        method = "GET",
        tags = ["process"],
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
                schema = Schema(type = "string", implementation = ProcessType::class)
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
                        schema = Schema(implementation = ProcessProgressDetails::class)
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
    @GetMapping(path = ["/process/{processId}/{processType}"])
    suspend fun retrieveProcessDetails(
        @IsProcessTypeValid @PathVariable processType: String,
        @ValidUuid @PathVariable processId: String
    ): ResponseEntity<ProcessProgressDetails> = coroutineScope {
        val processUuid = UUID.fromString(processId)
        repository.retrieveProcessDetailsByName(processUuid, ProcessType.valueOf(processType))
            ?.let { response ->
                ResponseEntity.ok(response)
            } ?: throw ResourceNotFoundException(processUuid, ResourceType.PROCESS)
    }

    @Operation(
        method = "GET",
        tags = ["process"],
        summary = """
            Successful request with process UUID results in either details for the process id or and exception with not found process(404).
            Bad Request is returned.
            The result response may include entity id, if such id was included.
            In case of a failed processing, a detailed error message is being included.
            """,
        parameters = [
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
                        schema = Schema(implementation = ProcessProgressDetails::class)
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
    @GetMapping(path = ["/process/{processId}"])
    suspend fun retrieveDetailsForProcessId(
        @ValidUuid @PathVariable processId: String
    ): ResponseEntity<ProcessProgressDetails> = coroutineScope {
        val processUuid = UUID.fromString(processId)
        repository.retrieveProcessDetails(processUuid).firstOrNull()
            ?.let { response ->
                ResponseEntity.ok(response)
            } ?: throw ResourceNotFoundException(processUuid, ResourceType.PROCESS)
    }

    @Operation(
        method = "GET",
        tags = ["process"],
        summary = """
            Successful request with parent process UUID results in either all details about processes done for this id or an empty list in case of no such parent id.
            The result response may include entity id, if such id was included.
            In case of a failed processing, a detailed error message is being included.
            """,
        parameters = [
            Parameter(
                name = "parentProcessId",
                `in` = ParameterIn.PATH,
                description = "UUID identifying a parent process",
                required = true,
                examples = [ExampleObject(value = "05b3b0f6-85af-4593-9bd1-eaa182702405")],
                schema = Schema(type = "uuid")
            )
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "A payload describing all processes for a parent process UUID",
                content = [
                    Content(
                        mediaType = "application/json",
                        array = ArraySchema(schema = Schema(implementation = ProcessProgressDetails::class))
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
    @GetMapping(path = ["/parentProcessId/{parentProcessId}"])
    suspend fun retrieveDetailsForParentProcessId(
        @ValidUuid @PathVariable parentProcessId: String
    ): ResponseEntity<List<ProcessProgressDetails>> = coroutineScope {
        ResponseEntity.ok(
            repository.findProcessDetailsForProcessTypesByIds(
                emptyList(),
                parentProcessId = UUID.fromString(parentProcessId)
            )
        )
    }

    @Operation(
        method = "GET",
        tags = ["process"],
        summary = """
            Successful request with parent process UUID results in either all details about processes done for this id or an empty list in case of no such parent id.
            The result response may include entity id, if such id was included.
            In case of a failed processing, a detailed error message is being included.
            """,
        parameters = [
            Parameter(
                name = "processType", `in` = ParameterIn.PATH, description = "Process type name", required = true,
                examples = [ExampleObject(value = "CREATE_CUSTOMER")],
                schema = Schema(type = "string", implementation = ProcessType::class)
            ),
            Parameter(
                name = "parentProcessId",
                `in` = ParameterIn.PATH,
                description = "UUID identifying a parent process",
                required = true,
                examples = [ExampleObject(value = "05b3b0f6-85af-4593-9bd1-eaa182702405")],
                schema = Schema(type = "uuid")
            )
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "A payload describing all processes for a parent process UUID",
                content = [
                    Content(
                        mediaType = "application/json",
                        array = ArraySchema(schema = Schema(implementation = ProcessProgressDetails::class))
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
    @GetMapping(path = ["/parentProcessId/{parentProcessId}/{processType}"])
    suspend fun retrieveDetailsForParentProcessIdAndProcessType(
        @ValidUuid @PathVariable parentProcessId: String,
        @IsProcessTypeValid @PathVariable processType: String
    ): ResponseEntity<List<ProcessProgressDetails>> = coroutineScope {
        ResponseEntity.ok(
            repository.findProcessDetailsForProcessTypesByIds(
                listOf(ProcessType.fromString(processType)),
                parentProcessId = UUID.fromString(parentProcessId)
            )
        )
    }
}