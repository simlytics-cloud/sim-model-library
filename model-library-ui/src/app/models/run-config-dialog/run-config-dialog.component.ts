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
            <div formGroupName="timeSemantics">
              <mat-form-field appearance="fill">
                <mat-label>Time Domain</mat-label>
                <mat-select formControlName="timeDomain">
                  <mat-option value="discrete">discrete</mat-option>
                  <mat-option value="continuous">continuous</mat-option>
                </mat-select>
              </mat-form-field>
              <mat-form-field appearance="fill">
                <mat-label>Value Encoding</mat-label>
                <mat-select formControlName="valueEncoding">
                  <mat-option value="float64">float64</mat-option>
                  <mat-option value="int64">int64</mat-option>
                  <mat-option value="decimal-string">decimal-string</mat-option>
                </mat-select>
              </mat-form-field>
              <div formGroupName="unitSeconds">
                <mat-form-field appearance="fill">
                  <mat-label>Unit Seconds Numerator</mat-label>
                  <input matInput type="number" formControlName="numerator">
                </mat-form-field>
                <mat-form-field appearance="fill">
                  <mat-label>Unit Seconds Denominator</mat-label>
                  <input matInput type="number" formControlName="denominator">
                </mat-form-field>
              </div>
              <div formGroupName="quantum" *ngIf="isDiscreteTimeDomain()">
                <mat-form-field appearance="fill">
                  <mat-label>Quantum Numerator</mat-label>
                  <input matInput type="number" formControlName="numerator">
                </mat-form-field>
                <mat-form-field appearance="fill">
                  <mat-label>Quantum Denominator</mat-label>
                  <input matInput type="number" formControlName="denominator">
                </mat-form-field>
              </div>
            </div>
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
            <mat-label>Consumer Group</mat-label>
            <input matInput formControlName="consumerGroup">
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
          timeSemantics: this.fb.group({
            timeDomain: [defaultTimeMode.timeSemantics.timeDomain, Validators.required],
            valueEncoding: [defaultTimeMode.timeSemantics.valueEncoding, Validators.required],
            unitSeconds: this.fb.group({
              numerator: [defaultTimeMode.timeSemantics.unitSeconds.numerator, Validators.required],
              denominator: [defaultTimeMode.timeSemantics.unitSeconds.denominator, [Validators.required, Validators.min(1)]]
            }),
            quantum: this.fb.group({
              numerator: [defaultTimeMode.timeSemantics.quantum?.numerator ?? 1],
              denominator: [defaultTimeMode.timeSemantics.quantum?.denominator ?? 1]
            }),
            originOffset: this.fb.group({
              numerator: [defaultTimeMode.timeSemantics.originOffset.numerator, Validators.required],
              denominator: [defaultTimeMode.timeSemantics.originOffset.denominator, [Validators.required, Validators.min(1)]]
            }),
            conversionPolicy: [defaultTimeMode.timeSemantics.conversionPolicy, Validators.required],
            infinityPolicy: [defaultTimeMode.timeSemantics.infinityPolicy, Validators.required]
          }),
          realTimeFactor: [defaultTimeMode.realTimeFactor]
        })
      }),
      kafka: this.fb.group({
        bootstrapServers: ['localhost:9092'],
        topic: [`simulation.${simulationId}.${coordinatorId}`],
        consumerGroup: [`${modelInstanceId}`],
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
    this.runForm.get('simulation.timeMode.timeSemantics.timeDomain')?.valueChanges.subscribe(() => {
      this.updateQuantumValidation();
    });
    this.updateRealTimeFactorValidation();
    this.updateQuantumValidation();
  }

  private applyKafkaDefaults(defaults: KafkaDefaults): void {
    const simulationId = this.runForm.get('simulation.simulationId')?.value ?? '';
    const coordinatorId = this.runForm.get('simulation.coordinatorId')?.value ?? '';
    const modelInstanceId = this.runForm.get('simulation.modelInstanceId')?.value ?? '';

    this.runForm.patchValue({
      kafka: {
        bootstrapServers: this.renderTemplate(defaults.bootstrapServers, simulationId, coordinatorId, modelInstanceId),
        topic: this.renderTemplate(defaults.topic, simulationId, coordinatorId, modelInstanceId),
        consumerGroup: this.renderTemplate(defaults.consumerGroup, simulationId, coordinatorId, modelInstanceId),
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
    const defaultTimeSemantics = {
      timeDomain: 'continuous' as const,
      valueEncoding: 'float64' as const,
      unitSeconds: {
        numerator: 1,
        denominator: 1
      },
      originOffset: {
        numerator: 0,
        denominator: 1
      },
      conversionPolicy: 'exact' as const,
      infinityPolicy: 'max-finite' as const
    };

    if (!modelTimeMode) {
      return {
        mode: 'virtual-time',
        timeSemantics: defaultTimeSemantics
      };
    }

    return {
      mode: modelTimeMode.mode ?? 'virtual-time',
      timeSemantics: modelTimeMode.timeSemantics ?? defaultTimeSemantics,
      realTimeFactor: modelTimeMode.mode === 'scaled-real-time' ? modelTimeMode.realTimeFactor : undefined
    };
  }

  isScaledTimeMode(): boolean {
    return this.runForm.get('simulation.timeMode.mode')?.value === 'scaled-real-time';
  }

  isDiscreteTimeDomain(): boolean {
    return this.runForm.get('simulation.timeMode.timeSemantics.timeDomain')?.value === 'discrete';
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

  private updateQuantumValidation(): void {
    const quantumNumeratorControl = this.runForm.get('simulation.timeMode.timeSemantics.quantum.numerator');
    const quantumDenominatorControl = this.runForm.get('simulation.timeMode.timeSemantics.quantum.denominator');
    if (!quantumNumeratorControl || !quantumDenominatorControl) {
      return;
    }

    if (this.isDiscreteTimeDomain()) {
      quantumNumeratorControl.setValidators([Validators.required]);
      quantumDenominatorControl.setValidators([Validators.required, Validators.min(1)]);
    } else {
      quantumNumeratorControl.clearValidators();
      quantumDenominatorControl.clearValidators();
    }

    quantumNumeratorControl.updateValueAndValidity();
    quantumDenominatorControl.updateValueAndValidity();
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
      if (rawValue?.simulation?.timeMode?.timeSemantics?.timeDomain !== 'discrete') {
        delete rawValue.simulation.timeMode.timeSemantics.quantum;
      }
      this.dialogRef.close(rawValue);
    }
  }
}
