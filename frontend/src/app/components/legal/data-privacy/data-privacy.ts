import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';

@Component({
    selector: 'app-data-privacy',
    standalone: true,
    imports: [CommonModule, RouterModule, TranslateModule],
    templateUrl: './data-privacy.html'
})
export class DataPrivacyComponent { }
