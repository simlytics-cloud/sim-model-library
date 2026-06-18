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

export interface ModelDetail extends ModelSummary {
  implementationLanguage: string;
  inputPorts: Port[];
  outputPorts: Port[];
  parameters: Parameter[];
  initializationSchema?: any;
  defaultParameterSet?: any;
  messageSchemas?: { [key: string]: any };
  behaviorDescription?: string;
  metadata?: { [key: string]: string };
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

  getRunStatus(runId: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/runs/${runId}`);
  }
}
