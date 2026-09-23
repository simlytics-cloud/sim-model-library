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

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
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
        <h2>Remote Runner Details: {{run.runId}}</h2>
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
              <label>Runner Status:</label>
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
           <button mat-button color="primary" (click)="refresh()" [disabled]="deleting">REFRESH</button>
           <button mat-button color="warn" (click)="deleteRun()" [disabled]="deleting">
             <mat-icon>delete</mat-icon>
             STOP REMOTE RUNNER
           </button>
        </mat-card-actions>
      </mat-card>
    </div>

    <div *ngIf="!run && !error" style="text-align: center; padding: 50px;">
      Loading remote runner details...
    </div>

    <div *ngIf="error" class="error-container">
      <mat-icon color="warn">error</mat-icon>
      <p>{{error}}</p>
      <button mat-raised-button routerLink="/runs">Back to Local Instances</button>
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
    .locally-ready { background-color: #c8e6c9; }
    .locally-stopped { background-color: #ffccbc; color: #e64a19; }
    .locally-failed { background-color: #ffcdd2; color: #b71c1c; }
    .error-container {
      text-align: center;
      padding: 50px;
    }
  `]
})
export class RunDetailComponent implements OnInit {
  run?: RunStatus;
  error?: string;
  deleting = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
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
          this.error = 'Failed to load remote runner details. It may not exist.';
        }
      });
    }
  }

  refresh(): void {
    this.loadRun();
  }

  deleteRun(): void {
    if (!this.run || this.deleting) {
      return;
    }
    if (!window.confirm(`Stop remote runner '${this.run.runId}'? This does not cancel the coordinated run.`)) {
      return;
    }

    const runId = this.run.runId;
    this.deleting = true;
    this.modelService.deleteRun(runId).subscribe({
      next: () => {
        this.router.navigate(['/runs']);
      },
      error: (err) => {
        console.error('Error stopping remote runner', err);
        this.error = 'Failed to stop remote runner.';
      },
      complete: () => {
        this.deleting = false;
      }
    });
  }
}
