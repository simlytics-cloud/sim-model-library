import { Routes } from '@angular/router';
import { ModelListComponent } from './models/model-list/model-list.component';
import { ModelDetailComponent } from './models/model-detail/model-detail.component';

export const routes: Routes = [
  { path: '', redirectTo: '/models', pathMatch: 'full' },
  { path: 'models', component: ModelListComponent },
  { path: 'models/:modelId', component: ModelDetailComponent },
];
