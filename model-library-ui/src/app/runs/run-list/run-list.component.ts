import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ModelService, RunStatus } from '../../services/model.service';

@Component({
  selector: 'app-run-list',
  standalone: true,
  imports: [CommonModule, RouterLink, MatTableModule, MatButtonModule, MatIconModule],
  template: `
    <h2>Model Runs</h2>
    
    <table mat-table [dataSource]="runs" class="mat-elevation-z8">
      <ng-container matColumnDef="runId">
        <th mat-header-cell *matHeaderCellDef> Run ID </th>
        <td mat-cell *matCellDef="let run"> {{run.runId}} </td>
      </ng-container>

      <ng-container matColumnDef="modelId">
        <th mat-header-cell *matHeaderCellDef> Model ID </th>
        <td mat-cell *matCellDef="let run"> {{run.modelId}} </td>
      </ng-container>

      <ng-container matColumnDef="status">
        <th mat-header-cell *matHeaderCellDef> Status </th>
        <td mat-cell *matCellDef="let run"> 
          <span [class]="'status-badge ' + run.status">{{run.status}}</span>
        </td>
      </ng-container>

      <ng-container matColumnDef="acceptedAt">
        <th mat-header-cell *matHeaderCellDef> Accepted At </th>
        <td mat-cell *matCellDef="let run"> {{run.acceptedAt | date:'medium'}} </td>
      </ng-container>

      <ng-container matColumnDef="currentSimulationTime">
        <th mat-header-cell *matHeaderCellDef> Current Simulation Time </th>
        <td mat-cell *matCellDef="let run">
          {{run.currentSimulationTime?.value ?? '-'}}
          <span *ngIf="run.currentSimulationTime?.timeType"> ({{run.currentSimulationTime?.timeType}})</span>
        </td>
      </ng-container>

      <ng-container matColumnDef="actions">
        <th mat-header-cell *matHeaderCellDef> Actions </th>
        <td mat-cell *matCellDef="let run">
          <button mat-icon-button [routerLink]="['/runs', run.runId]" title="View Details">
            <mat-icon>visibility</mat-icon>
          </button>
        </td>
      </ng-container>

      <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
      <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
    </table>
    
    <div *ngIf="runs.length === 0" style="padding: 20px; text-align: center;">
      No runs found.
    </div>
  `,
  styles: [`
    table {
      width: 100%;
    }
    .status-badge {
      padding: 4px 8px;
      border-radius: 4px;
      font-size: 0.85em;
      font-weight: 500;
      text-transform: uppercase;
    }
    .accepted { background-color: #e0e0e0; }
    .starting { background-color: #bbdefb; }
    .ready { background-color: #c8e6c9; }
    .running { background-color: #fff9c4; color: #f57f17; }
    .completed { background-color: #a5d6a7; color: #1b5e20; }
    .failed { background-color: #ffcdd2; color: #b71c1c; }
    .canceled { background-color: #ffccbc; color: #e64a19; }
  `]
})
export class RunListComponent implements OnInit {
  runs: RunStatus[] = [];
  displayedColumns: string[] = ['runId', 'modelId', 'status', 'acceptedAt', 'currentSimulationTime', 'actions'];

  constructor(private modelService: ModelService) {}

  ngOnInit(): void {
    this.modelService.getRuns().subscribe(runs => {
      this.runs = runs.sort((a, b) => b.acceptedAt.localeCompare(a.acceptedAt));
    });
  }
}
