import { Component } from '@angular/core';
import { RouterOutlet, RouterLink } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, MatToolbarModule, MatButtonModule, MatIconModule],
  template: `
    <mat-toolbar color="primary">
      <span routerLink="/">Model Library</span>
      <span class="spacer"></span>
      <button mat-button routerLink="/models">Models</button>
      <button mat-button routerLink="/runs">Runs</button>
    </mat-toolbar>

    <div class="container" style="padding: 20px;">
      <router-outlet></router-outlet>
    </div>
  `,
  styles: [`
    .spacer {
      flex: 1 1 auto;
    }
    span[routerLink] {
      cursor: pointer;
    }
  `]
})
export class AppComponent {
  title = 'model-library-ui';
}
