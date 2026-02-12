import {ChangeDetectionStrategy, Component} from '@angular/core';

@Component({
  selector: 'app-dashboard',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="container mx-auto p-4">
      <h2 class="text-2xl font-bold mb-4">Dashboard</h2>
      <p class="text-gray-600">Welcome to the app. Content coming soon.</p>
    </div>
  `,
})
export class DashboardComponent {
}
