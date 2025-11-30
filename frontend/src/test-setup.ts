import 'zone.js';
import 'zone.js/testing';
import { getTestBed } from '@angular/core/testing';
import { BrowserDynamicTestingModule, platformBrowserDynamicTesting } from '@angular/platform-browser-dynamic/testing';
import { vi } from 'vitest';

getTestBed().initTestEnvironment(
    BrowserDynamicTestingModule,
    platformBrowserDynamicTesting()
);

// Jasmine compatibility
const jasmineMock = {
    createSpyObj: (baseName: string, methodNames: string[]) => {
        const obj: any = {};
        methodNames.forEach(method => {
            obj[method] = vi.fn();
        });
        return obj;
    },
    createSpy: (name: string) => vi.fn(),
};

(globalThis as any).jasmine = jasmineMock;
(window as any).jasmine = jasmineMock;
