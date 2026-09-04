// Patches Zone.js so Angular's fakeAsync/tick/flush cooperate with Vitest's fake timers.
// The application polyfills (zone.js + zone.js/testing) and the TestBed are already
// initialized by the time this file runs.
import 'zone.js/plugins/vitest-patch';
