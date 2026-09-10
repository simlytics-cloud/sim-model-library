import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';
import { ModelService, ModelDetail } from '../../services/model.service';
import { RunConfigDialogComponent } from '../run-config-dialog/run-config-dialog.component';

@Component({
  selector: 'app-model-detail',
  standalone: true,
  imports: [
    CommonModule, 
    RouterLink, 
    MatCardModule, 
    MatButtonModule, 
    MatTabsModule, 
    MatTableModule, 
    MatIconModule, 
    MatChipsModule,
    MatDialogModule,
    MatSnackBarModule
  ],
  template: `
    <div *ngIf="model" class="model-detail-container">
      <div style="margin-bottom: 20px;">
        <button mat-icon-button routerLink="/models">
          <mat-icon>arrow_back</mat-icon>
        </button>
        <span style="font-size: 24px; vertical-align: middle; margin-left: 10px;">{{ model.name }}</span>
      </div>

      <div class="detail-layout">
        <mat-card class="info-card">
          <mat-card-header>
            <mat-card-title>Model Information</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <p><strong>ID:</strong> {{ model.modelId }}</p>
            <p><strong>Language:</strong> {{ model.implementationLanguage }}</p>
            <p><strong>Description:</strong> {{ model.description }}</p>
            <p *ngIf="model.behaviorDescription"><strong>Behavior:</strong> {{ model.behaviorDescription }}</p>
            <div *ngIf="model.timeMode">
              <p><strong>Time Mode:</strong> {{ model.timeMode.mode }}</p>
              <p *ngIf="model.timeMode.mode === 'scaled-real-time' && model.timeMode.realTimeFactor != null">
                <strong>Real Time Factor:</strong> {{ model.timeMode.realTimeFactor }}
              </p>
            </div>
            
            <div *ngIf="model.metadata" style="margin-top: 20px;">
              <strong>Metadata:</strong>
              <mat-chip-set>
                @for (entry of model.metadata | keyvalue; track entry.key) {
                  <mat-chip>{{ entry.key }}: {{ entry.value }}</mat-chip>
                }
              </mat-chip-set>
            </div>
          </mat-card-content>
          <mat-card-actions>
            <button mat-raised-button color="accent" (click)="configureRun()">Configure Run</button>
          </mat-card-actions>
        </mat-card>

        <mat-card class="ports-card">
          <mat-tab-group>
            <mat-tab label="Input Ports">
              <table mat-table [dataSource]="model.inputPorts" class="full-width">
                <ng-container matColumnDef="name">
                  <th mat-header-cell *matHeaderCellDef> Name </th>
                  <td mat-cell *matCellDef="let port"> {{port.name}} </td>
                </ng-container>
                <ng-container matColumnDef="type">
                  <th mat-header-cell *matHeaderCellDef> Message Type </th>
                  <td mat-cell *matCellDef="let port"> {{port.messageType}} </td>
                </ng-container>
                <tr mat-header-row *matHeaderRowDef="['name', 'type']"></tr>
                <tr mat-row *matRowDef="let row; columns: ['name', 'type'];"></tr>
              </table>
            </mat-tab>
            <mat-tab label="Output Ports">
              <table mat-table [dataSource]="model.outputPorts" class="full-width">
                <ng-container matColumnDef="name">
                  <th mat-header-cell *matHeaderCellDef> Name </th>
                  <td mat-cell *matCellDef="let port"> {{port.name}} </td>
                </ng-container>
                <ng-container matColumnDef="type">
                  <th mat-header-cell *matHeaderCellDef> Message Type </th>
                  <td mat-cell *matCellDef="let port"> {{port.messageType}} </td>
                </ng-container>
                <tr mat-header-row *matHeaderRowDef="['name', 'type']"></tr>
                <tr mat-row *matRowDef="let row; columns: ['name', 'type'];"></tr>
              </table>
            </mat-tab>
            <mat-tab label="Parameters">
              <table mat-table [dataSource]="model.parameters" class="full-width">
                <ng-container matColumnDef="name">
                  <th mat-header-cell *matHeaderCellDef> Name </th>
                  <td mat-cell *matCellDef="let p"> {{p.name}} </td>
                </ng-container>
                <ng-container matColumnDef="type">
                  <th mat-header-cell *matHeaderCellDef> Type </th>
                  <td mat-cell *matCellDef="let p"> {{p.type}} </td>
                </ng-container>
                <tr mat-header-row *matHeaderRowDef="['name', 'type']"></tr>
                <tr mat-row *matRowDef="let row; columns: ['name', 'type'];"></tr>
              </table>
            </mat-tab>
          </mat-tab-group>
        </mat-card>
      </div>
    </div>
  `,
  styles: [`
    .detail-layout {
      display: grid;
      grid-template-columns: 1fr 2fr;
      gap: 20px;
    }
    .full-width {
      width: 100%;
    }
    mat-card {
      margin-bottom: 20px;
    }
  `]
})
export class ModelDetailComponent implements OnInit {
  model?: ModelDetail;

  constructor(
    private route: ActivatedRoute,
    private modelService: ModelService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    const modelId = this.route.snapshot.paramMap.get('modelId');
    if (modelId) {
      this.modelService.getModel(modelId).subscribe(model => {
        this.model = model;
      });
    }
  }

  configureRun(): void {
    if (!this.model) return;

    const dialogRef = this.dialog.open(RunConfigDialogComponent, {
      width: '800px',
      data: { model: this.model }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.modelService.runModel(this.model!.modelId, result).subscribe({
          next: (response) => {
            this.snackBar.open(`Run started successfully: ${response.runId}`, 'Close', {
              duration: 5000
            });
          },
          error: (err) => {
            console.error('Failed to start run', err);
            this.snackBar.open('Failed to start run: ' + (err.error?.message || err.message), 'Close', {
              duration: 5000
            });
          }
        });
      }
    });
  }
}
