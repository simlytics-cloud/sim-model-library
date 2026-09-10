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
export type TimeDomain = 'discrete' | 'continuous';
export type TimeValueEncoding = 'int64' | 'decimal-string' | 'float64';
export type TimeConversionPolicy = 'exact' | 'approximate';
export type TimeRoundingMode = 'floor' | 'ceiling' | 'half-up';
export type TimeInfinityPolicy = 'max-finite' | 'string-sentinel';

export interface RationalTime {
  numerator: number;
  denominator: number;
}

export interface TimeSemantics {
  timeDomain: TimeDomain;
  valueEncoding: TimeValueEncoding;
  unitSeconds: RationalTime;
  quantum?: RationalTime;
  originOffset: RationalTime;
  conversionPolicy: TimeConversionPolicy;
  maxAbsErrorSeconds?: number;
  roundingMode?: TimeRoundingMode;
  infinityPolicy: TimeInfinityPolicy;
}

export interface TimeModeConfig {
  mode: TimeMode;
  timeSemantics: TimeSemantics;
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
  status: string;
  acceptedAt: string;
  readyAt?: string;
  startedAt?: string;
  completedAt?: string;
  message?: string;
  currentSimulationTime?: {
    value?: string;
    timeSemantics?: TimeSemantics;
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

  runModel(modelId: string, request: any): Observable<any> {
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
