import { defineConfig } from 'vitest/config';
import angular from '@analogjs/vite-plugin-angular';

export default defineConfig({
    plugins: [angular()],
    test: {
        globals: true,
        environment: 'jsdom',
        setupFiles: ['./src/test-setup.ts'],
        include: ['src/**/*.spec.ts'],
        reporters: ['default'],
        cache: {
            dir: './node_modules/.vitest',
        },
        coverage: {
            provider: 'v8',
            reporter: ['text', 'html', 'json-summary'],
            reportsDirectory: './coverage',
            exclude: [
                'node_modules/',
                'src/test-setup.ts',
                '**/*.spec.ts',
                '**/*.config.ts',
            ],
        },
    },
});
