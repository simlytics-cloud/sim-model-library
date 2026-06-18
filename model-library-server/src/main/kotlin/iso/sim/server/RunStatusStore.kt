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

import java.util.concurrent.ConcurrentHashMap

interface RunStatusStore {
    fun save(status: RunStatusResponse)
    fun get(runId: String): RunStatusResponse?
    fun getAll(): List<RunStatusResponse>
}

class InMemoryRunStatusStore : RunStatusStore {
    private val runs = ConcurrentHashMap<String, RunStatusResponse>()

    override fun save(status: RunStatusResponse) {
        runs[status.runId] = status
    }

    override fun get(runId: String): RunStatusResponse? {
        return runs[runId]
    }

    override fun getAll(): List<RunStatusResponse> {
        return runs.values.toList()
    }
}
