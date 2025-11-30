import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'app-footer',
    standalone: true,
    imports: [CommonModule, RouterModule, TranslateModule],
    templateUrl: './footer.html'
})
export class FooterComponent {
    constructor(private translate: TranslateService) { }

    get currentLang(): string {
        return this.translate.currentLang;
    }

    switchLanguage(lang: string) {
        this.translate.use(lang);
    }
}
