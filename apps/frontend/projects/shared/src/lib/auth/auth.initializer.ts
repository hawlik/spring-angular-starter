import { APP_INITIALIZER, Provider, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { AuthService } from './auth.service';

export function provideAuthInitializer(): Provider {
  return {
    provide: APP_INITIALIZER,
    multi: true,
    useFactory: () => {
      const authService = inject(AuthService);
      return () => firstValueFrom(authService.init());
    },
  };
}
