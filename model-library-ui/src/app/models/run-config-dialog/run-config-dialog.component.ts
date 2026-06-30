import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { ModelDetail } from '../../services/model.service';
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
    public dialogRef: MatDialogRef<RunConfigDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { model: ModelDetail }
  ) {
    const modelIdSegments = this.getModelIdSegments(data.model.modelId);
    const simulationId = 'sim-001';
    const coordinatorId = modelIdSegments.length > 1 ? modelIdSegments[modelIdSegments.length - 2] : 'demo-coordinator';
    const modelInstanceId = modelIdSegments.length > 0 ? modelIdSegments[modelIdSegments.length - 1] : 'instance-001';
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
        coordinatorId: [coordinatorId]
      }),
      kafka: this.fb.group({
        bootstrapServers: ['localhost:9092'],
        topic: [`simulation.${simulationId}.${coordinatorId}`],
        consumerGroup: [`${modelInstanceId}`],
        securityProtocol: ['PLAINTEXT'],
        saslMechanism: ['']
      })
    });
  }

  private getModelIdSegments(modelId: string | undefined): string[] {
    return (modelId || '')
      .split('.')
      .map((part) => part.trim())
      .filter((part) => part.length > 0);
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
      this.dialogRef.close(rawValue);
    }
  }
}
