/*
 * Sim Model Library Copyright (C) 2026 simlytics.cloud LLC and
 * Sim Model Library contributors.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package iso.sim.server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import iso.sim.server.dto.run.CoordinatorHelperCallbackConfigurationDto;
import iso.sim.server.dto.run.RemoteRunnerEventReport;
import iso.sim.server.executor.RunExecutionContext;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Best-effort reporting. A helper callback must never decide whether a remote model runner starts.
 */
public class HttpCoordinatorHelperCallbackReporter implements CoordinatorHelperCallbackReporter {
    public static final String CALLBACK_TOKEN_HEADER = "X-Coordinator-Helper-Token";
    private static final Logger logger = Logger.getLogger(HttpCoordinatorHelperCallbackReporter.class.getName());

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public HttpCoordinatorHelperCallbackReporter(ObjectMapper objectMapper) {
        this(HttpClient.newHttpClient(), objectMapper);
    }

    HttpCoordinatorHelperCallbackReporter(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public void report(RunExecutionContext context, RemoteRunnerEventType eventType, String errorDetail) {
        CoordinatorHelperCallbackConfigurationDto callback = context.getRequest().getCoordinatorHelper();
        if (callback == null) {
            return;
        }
        String modelInstanceId = context.getRequest().getSimulation().getModelInstanceId();
        RemoteRunnerEventReport report = new RemoteRunnerEventReport(
            context.getRunId(),
            modelInstanceId,
            context.getRequest().getSimulation().getCoordinatorId(),
            context.getRunId() + ":" + modelInstanceId + ":" + eventType.wireValue(),
            eventType.wireValue(),
            Instant.now().toString(),
            errorDetail
        );
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(
                trimTrailingSlash(callback.getEndpoint()) + "/v1/coordinated-runs/" + context.getRunId()
                    + "/remote-runners/" + modelInstanceId + "/events"
            ))
                .header("Content-Type", "application/json")
                .header(CALLBACK_TOKEN_HEADER, callback.getToken())
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(report)))
                .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                logger.warning(() -> "Coordinator helper callback returned HTTP " + response.statusCode()
                    + " for runId=" + context.getRunId());
            }
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            logger.log(Level.WARNING, "Could not prepare coordinator helper callback for runId=" + context.getRunId(), ex);
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Could not report remote runner state to coordinator helper for runId=" + context.getRunId(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            logger.log(Level.WARNING, "Coordinator helper callback interrupted for runId=" + context.getRunId(), ex);
        }
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
