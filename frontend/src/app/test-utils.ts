import { vi } from 'vitest';

export function createSpyObj(baseName: string, methodNames: string[]): any {
    const obj: any = {};
    methodNames.forEach(method => {
        const spy = vi.fn();
        (spy as any).and = {
            returnValue: (val: any) => spy.mockReturnValue(val),
            callFake: (fn: any) => spy.mockImplementation(fn)
        };
        obj[method] = spy;
    });
    return obj;
}
