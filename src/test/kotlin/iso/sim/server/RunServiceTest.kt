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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RunServiceTest {
    private val catalogService = ModelCatalogService(CatalogRepository())
    private val recordingExecutor = RecordingRunExecutor()
    private val runService = RunService(catalogService, runExecutor = recordingExecutor)

    @Test
    fun `startRun accepts valid request and status can be fetched`() {
        val request = StartModelRunRequest(
            runId = "run-vehicle-001",
            initializationParameters = mapOf("vehicleId" to 1),
            kafka = KafkaConfigurationDto(
                bootstrapServers = "kafka.example.com:9092",
                topic = "irp-system"
            )
        )

        val response = runService.startRun("irpsystem.irpmodel.Vehicle", request)
        assertEquals("run-vehicle-001", response.runId)
        assertEquals("accepted", response.status)
        assertEquals("/v1/runs/run-vehicle-001", response.statusUrl)
        assertEquals("run-vehicle-001", recordingExecutor.lastContext?.runId)
        assertEquals("irpsystem.irpmodel.Vehicle", recordingExecutor.lastContext?.modelId)

        val status = runService.getRunStatus(response.runId)
        assertEquals("run-vehicle-001", status.runId)
        assertEquals("irpsystem.irpmodel.Vehicle", status.modelId)
        assertEquals("accepted", status.status)
    }

    @Test
    fun `startRun throws when kafka section is missing`() {
        val request = StartModelRunRequest(
            initializationParameters = mapOf("vehicleId" to 1)
        )

        val exception = org.junit.jupiter.api.Assertions.assertThrows(InvalidRunRequestException::class.java) {
            runService.startRun("irpsystem.irpmodel.Vehicle", request)
        }
        assertTrue(exception.message!!.contains("kafka"))
    }

    @Test
    fun `getRunStatus throws for unknown run`() {
        val exception = org.junit.jupiter.api.Assertions.assertThrows(RunNotFoundException::class.java) {
            runService.getRunStatus("missing-run")
        }
        assertTrue(exception.message!!.contains("missing-run"))
    }

    private class RecordingRunExecutor : RunExecutor {
        var lastContext: RunExecutionContext? = null

        override fun start(context: RunExecutionContext) {
            lastContext = context
        }
    }
}
