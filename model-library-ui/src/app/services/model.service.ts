/*
 * Sim Model Library Copyright (C) 2026 simlytics.cloud LLC and
 * Sim Model Library contributors.  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License
 *  is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing permissions and limitations under
 *  the License.
 *
 */

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ModelSummary {
  modelId: string;
  name: string;
  description?: string;
}

export interface ModelListResponse {
  models: ModelSummary[];
}

export interface Port {
  name: string;
  direction: 'input' | 'output';
  messageType: string;
  description?: string;
}

export interface Parameter {
  name: string;
  type: string;
  required: boolean;
  description?: string;
  defaultValue?: any;
}

export type TimeMode = 'real-time' | 'scaled-real-time' | 'virtual-time';

export interface TimeModeConfig {
  mode: TimeMode;
  realTimeFactor?: number;
}

export interface ModelDetail extends ModelSummary {
  implementationLanguage: string;
  timeMode?: TimeModeConfig;
  inputPorts: Port[];
  outputPorts: Port[];
  parameters: Parameter[];
  initializationSchema?: any;
  defaultParameterSet?: any;
  messageSchemas?: { [key: string]: any };
  behaviorDescription?: string;
  metadata?: { [key: string]: string };
}

export interface RunStatus {
  runId: string;
  modelId: string;
  simulationId: string;
  modelInstanceId: string;
  coordinatorId: string;
  status: 'accepted' | 'starting' | 'locally-ready' | 'locally-stopped' | 'locally-failed';
  acceptedAt: string;
  readyAt?: string;
  startedAt?: string;
  completedAt?: string;
  message?: string;
  currentSimulationTime?: {
    value?: number;
    sourceMessageType?: string;
    sourceMessageId?: string;
    updatedAt?: string;
  };
}

export interface KafkaDefaults {
  bootstrapServers: string;
  topic: string;
  securityProtocol: string;
  saslMechanism: string;
}

export interface KafkaTransport {
  bootstrapServers: string;
  topic: string;
  securityProtocol?: string;
  saslMechanism?: string;
  properties?: { [key: string]: string };
}

export interface RemoteModelRunRequest {
  runId: string;
  initializationParameters: any;
  simulation: {
    simulationId: string;
    modelInstanceId: string;
    coordinatorId: string;
    timeMode?: TimeModeConfig;
  };
  kafka: KafkaTransport;
}

@Injectable({
  providedIn: 'root'
})
export class ModelService {
  private apiUrl = '/v1';

  constructor(private http: HttpClient) { }

  getModels(): Observable<ModelListResponse> {
    return this.http.get<ModelListResponse>(`${this.apiUrl}/models`);
  }

  getModel(modelId: string): Observable<ModelDetail> {
    return this.http.get<ModelDetail>(`${this.apiUrl}/models/${modelId}`);
  }

  runModel(modelId: string, request: RemoteModelRunRequest): Observable<any> {
    return this.http.put(`${this.apiUrl}/models/${modelId}/run`, request);
  }

  getRuns(): Observable<RunStatus[]> {
    return this.http.get<RunStatus[]>(`${this.apiUrl}/runs`);
  }

  getKafkaDefaults(): Observable<KafkaDefaults> {
    return this.http.get<KafkaDefaults>(`${this.apiUrl}/run-config/defaults`);
  }

  getRunStatus(runId: string): Observable<RunStatus> {
    return this.http.get<RunStatus>(`${this.apiUrl}/runs/${runId}`);
  }

  deleteRun(runId: string): Observable<RunStatus> {
    return this.http.delete<RunStatus>(`${this.apiUrl}/runs/${runId}`);
  }
}
