import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';

@Component({
    selector: 'app-landing-page',
    standalone: true,
    imports: [CommonModule, RouterModule, TranslateModule],
    templateUrl: './landing-page.html'
})
export class LandingPageComponent { }
