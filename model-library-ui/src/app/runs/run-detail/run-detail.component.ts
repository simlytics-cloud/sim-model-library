import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { ModelService, RunStatus } from '../../services/model.service';

@Component({
  selector: 'app-run-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, MatCardModule, MatButtonModule, MatIconModule, MatDividerModule],
  template: `
    <div *ngIf="run" class="detail-container">
      <div class="header">
        <button mat-icon-button routerLink="/runs">
          <mat-icon>arrow_back</mat-icon>
        </button>
        <h2>Run Details: {{run.runId}}</h2>
      </div>

      <mat-card>
        <mat-card-content>
          <div class="info-grid">
            <div class="info-item">
              <label>Run ID:</label>
              <span>{{run.runId}}</span>
            </div>
            <div class="info-item">
              <label>Model ID:</label>
              <a [routerLink]="['/models', run.modelId]">{{run.modelId}}</a>
            </div>
            <div class="info-item">
              <label>Status:</label>
              <span [class]="'status-badge ' + run.status">{{run.status}}</span>
            </div>
            <div class="info-item" *ngIf="run.message">
              <label>Message:</label>
              <span>{{run.message}}</span>
            </div>
            <div class="info-item" *ngIf="run.currentSimulationTime?.value !== undefined && run.currentSimulationTime?.value !== null">
              <label>Current Simulation Time:</label>
              <span>
                {{run.currentSimulationTime?.value}}
                <span *ngIf="run.currentSimulationTime?.timeType"> ({{run.currentSimulationTime?.timeType}})</span>
              </span>
            </div>
          </div>

          <mat-divider></mat-divider>

          <h3>Timestamps</h3>
          <div class="info-grid">
            <div class="info-item">
              <label>Accepted At:</label>
              <span>{{run.acceptedAt | date:'medium'}}</span>
            </div>
            <div class="info-item" *ngIf="run.readyAt">
              <label>Ready At:</label>
              <span>{{run.readyAt | date:'medium'}}</span>
            </div>
            <div class="info-item" *ngIf="run.startedAt">
              <label>Started At:</label>
              <span>{{run.startedAt | date:'medium'}}</span>
            </div>
            <div class="info-item" *ngIf="run.completedAt">
              <label>Completed At:</label>
              <span>{{run.completedAt | date:'medium'}}</span>
            </div>
          </div>
        </mat-card-content>
        <mat-card-actions>
           <button mat-button color="primary" (click)="refresh()">REFRESH</button>
        </mat-card-actions>
      </mat-card>
    </div>

    <div *ngIf="!run && !error" style="text-align: center; padding: 50px;">
      Loading run details...
    </div>

    <div *ngIf="error" class="error-container">
      <mat-icon color="warn">error</mat-icon>
      <p>{{error}}</p>
      <button mat-raised-button routerLink="/runs">Back to Runs</button>
    </div>
  `,
  styles: [`
    .detail-container {
      max-width: 800px;
      margin: 0 auto;
    }
    .header {
      display: flex;
      align-items: center;
      margin-bottom: 20px;
    }
    .header h2 {
      margin: 0 0 0 10px;
    }
    .info-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 15px;
      margin: 15px 0;
    }
    .info-item {
      display: flex;
      flex-direction: column;
    }
    .info-item label {
      font-weight: bold;
      font-size: 0.9em;
      color: #666;
    }
    .info-item span, .info-item a {
      font-size: 1.1em;
    }
    mat-divider {
      margin: 20px 0;
    }
    .status-badge {
      display: inline-block;
      padding: 4px 8px;
      border-radius: 4px;
      font-size: 0.85em;
      font-weight: 500;
      text-transform: uppercase;
      width: fit-content;
    }
    .accepted { background-color: #e0e0e0; }
    .starting { background-color: #bbdefb; }
    .ready { background-color: #c8e6c9; }
    .running { background-color: #fff9c4; color: #f57f17; }
    .completed { background-color: #a5d6a7; color: #1b5e20; }
    .failed { background-color: #ffcdd2; color: #b71c1c; }
    .canceled { background-color: #ffccbc; color: #e64a19; }
    .error-container {
      text-align: center;
      padding: 50px;
    }
  `]
})
export class RunDetailComponent implements OnInit {
  run?: RunStatus;
  error?: string;

  constructor(
    private route: ActivatedRoute,
    private modelService: ModelService
  ) {}

  ngOnInit(): void {
    this.loadRun();
  }

  loadRun(): void {
    const runId = this.route.snapshot.paramMap.get('runId');
    if (runId) {
      this.modelService.getRunStatus(runId).subscribe({
        next: (run) => this.run = run,
        error: (err) => {
          console.error('Error loading run', err);
          this.error = 'Failed to load run details. It may not exist.';
        }
      });
    }
  }

  refresh(): void {
    this.loadRun();
  }
}
