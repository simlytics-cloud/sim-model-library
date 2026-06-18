/*
 *
 *  Foundry for Executable Digital Engineering
 *  Copyright (C) 2026 simlytics.cloud LLC
 *  All rights reserved.
 *
 *  This software was developed under U.S. Government contract(s).
 *  Rights in this software are defined by the applicable contract and
 *  associated DFARS clauses. Unless otherwise specified, this software
 *  is intended to be delivered with Government Purpose Rights (GPR).
 *
 *  This repository is private. No rights are granted except as provided by:
 *  (1) applicable U.S. Government contract(s), or
 *  (2) a separate written license agreement with simlytics.cloud LLC.
 *
 *  Authorized licensees may use and modify this software only in accordance
 *  with the terms of such agreement.
 *
 *  As a condition of any license:
 *  - All modifications and derivative works must be provided back to
 *     simlytics.cloud LLC.
 *  - Redistribution, sublicensing, or third-party sharing is prohibited
 *    without prior written authorization.
 *
 *  Contributions are subject to a Contributor Agreement. By contributing,
 *  you grant simlytics.cloud LLC all rights necessary to use, modify, and
 *  redistribute your contributions without restriction, and you retain
 *  no independent distribution rights.
 *
 */

package iso.sim.server

import java.time.Instant
import java.util.UUID

class RunService(
    private val modelCatalogService: ModelCatalogService,
    private val runExecutor: RunExecutor = StubRunExecutor(),
    private val runStatusStore: RunStatusStore = InMemoryRunStatusStore()
) {
    fun startRun(modelId: String, request: StartModelRunRequest): StartModelRunResponse {
        modelCatalogService.getModel(modelId)
        validateRequest(request)

        val runId = request.runId?.takeIf { it.isNotBlank() } ?: "run-${UUID.randomUUID()}"
        val acceptedAt = Instant.now().toString()
        val statusUrl = "/v1/runs/$runId"
        val status = RunStatusResponse(
            runId = runId,
            modelId = modelId,
            status = "accepted",
            acceptedAt = acceptedAt,
            message = "Run request accepted; backend start is stubbed for Phase 1E"
        )
        runStatusStore.save(status)
        runExecutor.start(
            RunExecutionContext(
                runId = runId,
                modelId = modelId,
                request = request
            )
        )

        return StartModelRunResponse(
            runId = runId,
            modelId = modelId,
            status = status.status,
            statusUrl = statusUrl,
            acceptedAt = acceptedAt,
            message = status.message
        )
    }

    fun getRunStatus(runId: String): RunStatusResponse {
        return runStatusStore.get(runId) ?: throw RunNotFoundException(runId)
    }

    fun getRunStatusStore(): RunStatusStore {
        return runStatusStore
    }

    private fun validateRequest(request: StartModelRunRequest) {
        if (request.initializationParameters == null) {
            throw InvalidRunRequestException("'initializationParameters' is required")
        }
        val kafka = request.kafka ?: throw InvalidRunRequestException("'kafka' is required")
        if (kafka.bootstrapServers.isNullOrBlank()) {
            throw InvalidRunRequestException("'kafka.bootstrapServers' is required")
        }
        if (kafka.topic.isNullOrBlank()) {
            throw InvalidRunRequestException("'kafka.topic' is required")
        }
    }
}

class InvalidRunRequestException(message: String) : RuntimeException(message)

class RunNotFoundException(runId: String) : RuntimeException("Run '$runId' was not found")
