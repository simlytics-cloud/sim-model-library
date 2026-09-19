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

import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { KafkaDefaults, ModelDetail, ModelService, TimeModeConfig } from '../../services/model.service';
import { MaterialDesignFrameworkModule } from '@ajsf/material';

@Component({
  selector: 'app-run-config-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MaterialDesignFrameworkModule
  ],
  template: `
    <h2 mat-dialog-title>Configure Run: {{ data.model.name }}</h2>
    <mat-dialog-content>
      <form [formGroup]="runForm" class="run-form">
        <mat-form-field appearance="fill">
          <mat-label>Run ID</mat-label>
          <input matInput formControlName="runId" placeholder="e.g. run-001">
        </mat-form-field>

        <div *ngIf="hasInitializationSchema; else missingSchemaError">
          <h3>Initialization Parameters</h3>
          <json-schema-form
            [schema]="initializationSchema"
            [data]="data.model.defaultParameterSet"
            framework="material-design"
            (onChanges)="onInitializationParametersChange($event)">
          </json-schema-form>
        </div>
        <ng-template #missingSchemaError>
          <h3>Initialization Parameters</h3>
          <p class="schema-error">
            This model cannot be configured because a valid initialization schema is missing.
            Please update the model catalog.
          </p>
        </ng-template>

        <h3>Simulation Context</h3>
        <div formGroupName="simulation">
          <mat-form-field appearance="fill">
            <mat-label>Simulation ID</mat-label>
            <input matInput formControlName="simulationId">
          </mat-form-field>
          <mat-form-field appearance="fill">
            <mat-label>Model Instance ID</mat-label>
            <input matInput formControlName="modelInstanceId">
          </mat-form-field>
          <mat-form-field appearance="fill">
            <mat-label>Coordinator ID</mat-label>
            <input matInput formControlName="coordinatorId">
          </mat-form-field>
          <div formGroupName="timeMode">
            <mat-form-field appearance="fill">
              <mat-label>Time Mode</mat-label>
              <mat-select formControlName="mode">
                <mat-option value="real-time">real-time</mat-option>
                <mat-option value="scaled-real-time">scaled-real-time</mat-option>
                <mat-option value="virtual-time">virtual-time</mat-option>
              </mat-select>
            </mat-form-field>
            <mat-form-field appearance="fill" *ngIf="isScaledTimeMode()">
              <mat-label>Real Time Factor</mat-label>
              <input matInput type="number" formControlName="realTimeFactor">
              <mat-error *ngIf="runForm.get('simulation.timeMode.realTimeFactor')?.hasError('required')">
                Real Time Factor is required for scaled-real-time.
              </mat-error>
              <mat-error *ngIf="runForm.get('simulation.timeMode.realTimeFactor')?.hasError('min')">
                Real Time Factor must be greater than 0.
              </mat-error>
            </mat-form-field>
          </div>
        </div>

        <h3>Kafka Configuration</h3>
        <div formGroupName="kafka">
          <mat-form-field appearance="fill">
            <mat-label>Bootstrap Servers</mat-label>
            <input matInput formControlName="bootstrapServers">
          </mat-form-field>
          <mat-form-field appearance="fill">
            <mat-label>Topic</mat-label>
            <input matInput formControlName="topic">
          </mat-form-field>
          <mat-form-field appearance="fill">
            <mat-label>Security Protocol</mat-label>
            <input matInput formControlName="securityProtocol">
          </mat-form-field>
          <mat-form-field appearance="fill">
            <mat-label>SASL Mechanism</mat-label>
            <input matInput formControlName="saslMechanism">
          </mat-form-field>
        </div>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="onCancel()">Cancel</button>
      <button mat-raised-button color="primary" [disabled]="!runForm.valid || !hasInitializationSchema" (click)="onRun()">Start Run</button>
    </mat-dialog-actions>
  `,
  styles: [`
    .run-form {
      display: flex;
      flex-direction: column;
      gap: 10px;
      min-width: 400px;
      padding-top: 10px;
    }
    mat-form-field {
      width: 100%;
    }
    h3 {
      margin-top: 15px;
      margin-bottom: 5px;
      border-bottom: 1px solid #ccc;
    }
    .schema-error {
      margin: 0;
      color: #c62828;
    }
  `]
})
export class RunConfigDialogComponent {
  runForm: FormGroup;
  hasInitializationSchema: boolean;
  initializationSchema: any;

  constructor(
    private fb: FormBuilder,
    private modelService: ModelService,
    public dialogRef: MatDialogRef<RunConfigDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { model: ModelDetail }
  ) {
    const modelIdSegments = this.getModelIdSegments(data.model.modelId);
    const simulationId = 'sim-001';
    const coordinatorId = modelIdSegments.length > 1 ? modelIdSegments[modelIdSegments.length - 2] : 'demo-coordinator';
    const modelInstanceId = modelIdSegments.length > 0 ? modelIdSegments[modelIdSegments.length - 1] : 'instance-001';
    const defaultTimeMode = this.resolveDefaultTimeMode(data.model.timeMode);
    const defaultParams = data.model.defaultParameterSet;
    this.initializationSchema = this.buildInitializationSchema(
      data.model.initializationSchema,
      data.model.messageSchemas
    );
    this.hasInitializationSchema = !!this.initializationSchema;
    this.runForm = this.fb.group({
      runId: [`run-${Math.floor(Math.random() * 1000)}`, Validators.required],
      initializationParameters: this.fb.group(defaultParams || {}),
      simulation: this.fb.group({
        simulationId: [simulationId],
        modelInstanceId: [modelInstanceId],
        coordinatorId: [coordinatorId],
        timeMode: this.fb.group({
          mode: [defaultTimeMode.mode, Validators.required],
          realTimeFactor: [defaultTimeMode.realTimeFactor]
        })
      }),
      kafka: this.fb.group({
        bootstrapServers: ['localhost:9092'],
        topic: ['devs-sim'],
        securityProtocol: ['PLAINTEXT'],
        saslMechanism: ['']
      })
    });

    this.modelService.getKafkaDefaults().subscribe({
      next: (defaults) => this.applyKafkaDefaults(defaults),
      error: () => {
        // Keep constructor defaults if server defaults are unavailable.
      }
    });

    this.runForm.get('simulation.timeMode.mode')?.valueChanges.subscribe(() => {
      this.updateRealTimeFactorValidation();
    });
    this.updateRealTimeFactorValidation();
  }

  private applyKafkaDefaults(defaults: KafkaDefaults): void {
    const simulationId = this.runForm.get('simulation.simulationId')?.value ?? '';
    const coordinatorId = this.runForm.get('simulation.coordinatorId')?.value ?? '';
    const modelInstanceId = this.runForm.get('simulation.modelInstanceId')?.value ?? '';

    this.runForm.patchValue({
      kafka: {
        bootstrapServers: this.renderTemplate(defaults.bootstrapServers, simulationId, coordinatorId, modelInstanceId),
        topic: this.renderTemplate(defaults.topic, simulationId, coordinatorId, modelInstanceId),
        securityProtocol: this.renderTemplate(defaults.securityProtocol, simulationId, coordinatorId, modelInstanceId),
        saslMechanism: this.renderTemplate(defaults.saslMechanism, simulationId, coordinatorId, modelInstanceId)
      }
    });
  }

  private renderTemplate(template: string, simulationId: string, coordinatorId: string, modelInstanceId: string): string {
    return (template || '')
      .replaceAll('${simulationId}', simulationId)
      .replaceAll('${coordinatorId}', coordinatorId)
      .replaceAll('${modelInstanceId}', modelInstanceId);
  }

  private getModelIdSegments(modelId: string | undefined): string[] {
    return (modelId || '')
      .split('.')
      .map((part) => part.trim())
      .filter((part) => part.length > 0);
  }

  private resolveDefaultTimeMode(modelTimeMode?: TimeModeConfig): TimeModeConfig {
    if (!modelTimeMode) {
      return {
        mode: 'virtual-time'
      };
    }

    return {
      mode: modelTimeMode.mode ?? 'virtual-time',
      realTimeFactor: modelTimeMode.mode === 'scaled-real-time' ? modelTimeMode.realTimeFactor : undefined
    };
  }

  isScaledTimeMode(): boolean {
    return this.runForm.get('simulation.timeMode.mode')?.value === 'scaled-real-time';
  }

  private updateRealTimeFactorValidation(): void {
    const realTimeFactorControl = this.runForm.get('simulation.timeMode.realTimeFactor');
    if (!realTimeFactorControl) {
      return;
    }

    if (this.isScaledTimeMode()) {
      realTimeFactorControl.setValidators([Validators.required, Validators.min(Number.EPSILON)]);
    } else {
      realTimeFactorControl.clearValidators();
    }

    realTimeFactorControl.updateValueAndValidity();
  }

  private buildInitializationSchema(initializationSchema: any, messageSchemas?: { [key: string]: any }): any {
    if (!initializationSchema || typeof initializationSchema !== 'object') {
      return null;
    }

    const schema = JSON.parse(JSON.stringify(initializationSchema));
    this.removeSchemaDialectKeywords(schema);

    const defs = messageSchemas && Object.keys(messageSchemas).length > 0
      ? this.removeSchemaDialectKeywords(JSON.parse(JSON.stringify(messageSchemas)))
      : undefined;

    if (defs) {
      schema.definitions = {
        ...(schema.definitions || {}),
        ...defs
      };
    }

    this.rewriteSchemaRefs(schema);

    return schema;
  }

  private rewriteSchemaRefs(node: any): void {
    if (!node || typeof node !== 'object') {
      return;
    }

    if (typeof node.$ref === 'string') {
      if (node.$ref.startsWith('#/components/schemas/')) {
        node.$ref = node.$ref.replace('#/components/schemas/', '#/definitions/');
      } else if (node.$ref.startsWith('#/$defs/')) {
        node.$ref = node.$ref.replace('#/$defs/', '#/definitions/');
      }
    }

    Object.values(node).forEach((value) => this.rewriteSchemaRefs(value));
  }

  private removeSchemaDialectKeywords(node: any): any {
    if (!node || typeof node !== 'object') {
      return node;
    }

    delete node.$schema;

    Object.values(node).forEach((value) => this.removeSchemaDialectKeywords(value));

    return node;
  }

  onInitializationParametersChange(event: any): void {
    this.runForm.patchValue({
      initializationParameters: event
    });
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  onRun(): void {
    if (this.runForm.valid && this.hasInitializationSchema) {
      const rawValue = this.runForm.value;
      if (rawValue?.simulation?.timeMode?.mode !== 'scaled-real-time') {
        delete rawValue.simulation.timeMode.realTimeFactor;
      }
      this.dialogRef.close(rawValue);
    }
  }
}
