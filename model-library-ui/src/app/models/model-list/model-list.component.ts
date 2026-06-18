import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { ModelService, ModelSummary } from '../../services/model.service';

@Component({
  selector: 'app-model-list',
  standalone: true,
  imports: [CommonModule, RouterLink, MatCardModule, MatButtonModule, MatListModule, MatIconModule],
  template: `
    <div class="model-list-container">
      <h1>Available Models</h1>
      <div class="model-grid">
        @for (model of models; track model.modelId) {
          <mat-card class="model-card">
            <mat-card-header>
              <mat-card-title>{{ model.name }}</mat-card-title>
              <mat-card-subtitle>{{ model.modelId }}</mat-card-subtitle>
            </mat-card-header>
            <mat-card-content>
              <p>{{ model.description || 'No description available.' }}</p>
            </mat-card-content>
            <mat-card-actions>
              <button mat-flat-button color="primary" [routerLink]="['/models', model.modelId]">View Details</button>
            </mat-card-actions>
          </mat-card>
        } @empty {
          <p>No models found in the catalog.</p>
        }
      </div>
    </div>
  `,
  styles: [`
    .model-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
      gap: 20px;
      margin-top: 20px;
    }
    .model-card {
      height: 100%;
      display: flex;
      flex-direction: column;
    }
    mat-card-content {
      flex-grow: 1;
    }
  `]
})
export class ModelListComponent implements OnInit {
  models: ModelSummary[] = [];

  constructor(private modelService: ModelService) {}

  ngOnInit(): void {
    this.modelService.getModels().subscribe(response => {
      this.models = response.models;
    });
  }
}
