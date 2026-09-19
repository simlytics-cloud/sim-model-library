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
