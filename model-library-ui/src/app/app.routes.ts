import { Routes } from '@angular/router';
import { ModelListComponent } from './models/model-list/model-list.component';
import { ModelDetailComponent } from './models/model-detail/model-detail.component';
import { RunListComponent } from './runs/run-list/run-list.component';
import { RunDetailComponent } from './runs/run-detail/run-detail.component';

export const routes: Routes = [
  { path: '', redirectTo: '/models', pathMatch: 'full' },
  { path: 'models', component: ModelListComponent },
  { path: 'models/:modelId', component: ModelDetailComponent },
  { path: 'runs', component: RunListComponent },
  { path: 'runs/:runId', component: RunDetailComponent },
];
