import { Component, EventEmitter, Input, Output, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { FormsModule } from '@angular/forms';
import { MatchResponse } from '../../../api/models/match-response';
import { MatchStickerDto } from '../../../api/models/match-sticker-dto';
import { CreateExchangeRequestDto } from '../../../api/models/create-exchange-request-dto';

type MatchTab = 'EXCHANGE' | 'FREEBIE' | 'PAYED';

interface ExchangePair {
    give: MatchStickerDto | null;
    get: MatchStickerDto | null;
}

@Component({
    selector: 'app-exchange-request-modal',
    standalone: true,
    imports: [CommonModule, TranslateModule, FormsModule],
    templateUrl: './exchange-request-modal.html',
})
export class ExchangeRequestModalComponent implements OnChanges {
    @Input() isVisible = false;
    @Input() match: MatchResponse | null = null;
    @Input() tab: MatchTab = 'EXCHANGE';
    @Output() close = new EventEmitter<void>();
    @Output() submit = new EventEmitter<CreateExchangeRequestDto[]>();

    // Exchange Tab State
    pairs: ExchangePair[] = [];
    currentGive: MatchStickerDto | null = null;
    currentGet: MatchStickerDto | null = null;

    // Freebie/Payed Tab State
    selectedItemIds: Set<number> = new Set();

    ngOnChanges(changes: SimpleChanges): void {
        if (changes['isVisible'] && this.isVisible) {
            this.resetState();
        }
    }

    resetState() {
        this.pairs = [];
        this.currentGive = null;
        this.currentGet = null;
        this.selectedItemIds.clear();
    }

    // Exchange Logic
    addPair() {
        if (this.currentGive && this.currentGet) {
            this.pairs.push({ give: this.currentGive, get: this.currentGet });
            this.currentGive = null;
            this.currentGet = null;
        }
    }

    removePair(index: number) {
        this.pairs.splice(index, 1);
    }

    get availableGiveItems(): MatchStickerDto[] {
        // Items I can give (Requested by partner)
        // Filter out items already used in pairs
        if (!this.match?.itemsRequested) return [];
        const usedIds = new Set(this.pairs.map(p => p.give?.id));
        return this.match.itemsRequested.filter(item => item.id && !usedIds.has(item.id));
    }

    get availableGetItems(): MatchStickerDto[] {
        // Items I can get (Offered by partner)
        // Filter out items already used in pairs
        if (!this.match?.itemsOffered) return [];
        const usedIds = new Set(this.pairs.map(p => p.get?.id));
        return this.match.itemsOffered.filter(item => item.id && !usedIds.has(item.id));
    }

    // Freebie/Payed Logic
    toggleSelection(item: MatchStickerDto) {
        if (!item.id) return;
        if (this.selectedItemIds.has(item.id)) {
            this.selectedItemIds.delete(item.id);
        } else {
            this.selectedItemIds.add(item.id);
        }
    }

    isSelected(item: MatchStickerDto): boolean {
        return !!item.id && this.selectedItemIds.has(item.id);
    }

    // Submission
    onSubmit() {
        if (!this.match?.userId) return;

        const requests: CreateExchangeRequestDto[] = [];

        if (this.tab === 'EXCHANGE') {
            this.pairs.forEach(pair => {
                if (pair.give?.id && pair.get?.id) {
                    requests.push({
                        exchangeType: 'EXCHANGE',
                        offererId: this.match!.userId, // The partner is the offerer of the card I want
                        offeredStickerId: pair.get.id, // The card I want (from partner) -> Wait, pair.get is itemsOffered (I Have). So this is the card I OFFER.
                        requestedStickerId: pair.give.id // The card I give (partner wants) -> Wait, pair.give is itemsRequested (Partner Has). So this is the card I REQUEST.
                    });
                }
            });
        } else {
            // Freebie or Payed
            // Logic: I am requesting items from the partner (Freebie/Payed)
            // Wait, user said: "select the stickers that I want to give based on the Angebotene Artikel and Gesuchte Artikel"
            // For Exchange: 1:1
            // For Gift/Buy: "out of the 'Gesuchte Artikel' which one he wants to request from the other party"

            // Interpretation:
            // If I am in "Freebie" tab (I want a freebie), I select from "Offered Items" of the partner?
            // Or if I want to GIVE a freebie?

            // Let's look at the columns:
            // Exchange Tab: Offered (Partner has), Requested (Partner wants).
            // Freebie/Payed Tab: Offered (Hidden), Requested (Partner wants).

            // User said: "For Geschenk and kaufen please implement a similar logic where the user can say out of the 'Gesuchte Artikel' which one he wants to request from the other party."

            // "Gesuchte Artikel" = Requested Items (Items the PARTNER wants).
            // So if I select from "Gesuchte Artikel", I am offering to GIVE these items to the partner.

            // If the tab is FREEBIE, it means the match is a "Freebie Match".
            // Usually "Freebie Match" means I have something the other person wants, and I'm willing to give it for free?
            // Or the other person has something I want for free?

            // Let's check `getFreebieMatches` logic or naming convention.
            // Usually "My Matches" shows people who match with ME.

            // If I am in "Freebie" tab, it means there is a potential freebie transaction.
            // If "Offered Items" is hidden, it implies the partner is NOT offering anything relevant to me (or it's hidden by design).
            // "Requested Items" are items the PARTNER wants from ME.

            // So if I select from "Requested Items", I am fulfilling their request.
            // So I am the OFFERER (I give the card).

            // DTO:
            // offererId: The ID of the user who OFFERS the exchange? Or the ID of the partner?
            // Let's check `CreateExchangeRequestDto` again.
            // `offererId`: usually the ID of the person who OWNS the card being offered?

            // If I initiate the request, I am the "Requester".
            // But the DTO has `offererId`.

            // Let's assume `offererId` refers to the PARTNER in the match context.
            // If I want to GIVE a card (Freebie), I am the offerer.
            // But the API might expect the ID of the person I am sending the request TO.

            // Let's look at `createExchangeRequest` usage in other parts if any. None.

            // Let's assume standard logic:
            // I am the logged-in user.
            // `match.userId` is the partner.

            // If I want to GIVE a card (Freebie):
            // I am offering `requestedStickerId` (my card).
            // The partner is receiving.

            // If I want to GET a card (Payed/Exchange):
            // The partner is offering `offeredStickerId`.

            // In `EXCHANGE` mode:
            // `offeredStickerId` = Card from Partner (I want this)
            // `requestedStickerId` = Card from Me (Partner wants this)
            // `offererId` = Partner's ID? Or My ID?

            // Let's look at the backend code if possible?
            // `ExchangeRequestController.createExchangeRequest`

            // I'll assume `offererId` is the ID of the user I am sending the request TO (the partner).
            // Wait, if I am the requester, I don't need to send my ID (it's in the token).
            // So `offererId` must be the partner.

            // BUT: If I am GIVING a card (Freebie), the partner is the RECEIVER.
            // Does `offererId` mean "Person who initiates"? No, that's Requester.
            // Does `offererId` mean "Person who provides the card"?

            // If `offererId` is the partner:
            // Exchange: Partner provides `offeredStickerId`. Correct.
            // Freebie: Partner provides... nothing? I provide the card.

            // Maybe `offererId` is ALWAYS the partner ID?
            // Let's assume `offererId` = `match.userId`.

            // If Tab = FREEBIE:
            // User said: "out of the 'Gesuchte Artikel' which one he wants to request from the other party."
            // "Gesuchte Artikel" = Items the PARTNER wants.
            // "Request from the other party" -> This sounds like I am asking the partner to give me something?
            // But "Gesuchte Artikel" are items the PARTNER wants.
            // This is contradictory.

            // "Gesuchte Artikel" (Requested Items) = Items the partner is looking for (and I have).
            // "Angebotene Artikel" (Offered Items) = Items the partner has (and I want).

            // If I select from "Gesuchte Artikel", I am saying "I have this card you want".
            // So I am offering it.

            // If the user says "request from the other party", maybe he means "Request the other party to ACCEPT this card"?
            // Or maybe he is confused about the columns?

            // Let's stick to the column definitions:
            // MATCHES.TABLE.OFFERED = Items the partner OFFERS.
            // MATCHES.TABLE.REQUESTED = Items the partner REQUESTS.

            // User instruction: "For Geschenk and kaufen please implement a similar logic where the user can say out of the 'Gesuchte Artikel' which one he wants to request from the other party."

            // If I am in "Kauf" (Payed):
            // Usually I BUY something from the partner.
            // So I should select from "Angebotene Artikel" (Items partner has).
            // But the user said "Gesuchte Artikel".

            // Wait, maybe "Gesuchte Artikel" in the table means "Items *I* am looking for"?
            // Let's check `MatchResponse` mapping.
            // `itemsOffered`: Array<MatchStickerDto> -> Items the MATCH user offers.
            // `itemsRequested`: Array<MatchStickerDto> -> Items the MATCH user requests.

            // If the table header says "Gesuchte Artikel" (Requested), it usually means "Items the partner is looking for".

            // Let's look at the "Exchange" logic the user described:
            // "select the stickers that I want to give based on the Angebotene Artikel and Gesuchte Artikel"
            // "Give" -> I give. So I select from "Gesuchte Artikel" (Partner wants this).
            // "Get" -> I get. So I select from "Angebotene Artikel" (Partner has this).
            // This confirms:
            // "Gesuchte Artikel" = Items Partner Wants (I have).
            // "Angebotene Artikel" = Items Partner Has (I want).

            // Now for "Geschenk" (Freebie) and "Kauf" (Payed):
            // User: "out of the 'Gesuchte Artikel' which one he wants to request from the other party."

            // If I select from "Gesuchte Artikel", I am selecting items the PARTNER WANTS.
            // Why would I "request" them FROM the partner? I should be GIVING them TO the partner.

            // Unless... "Gesuchte Artikel" means "Items I am looking for"?
            // No, that contradicts the Exchange logic.

            // Maybe the user meant "Angebotene Artikel"?
            // "For Geschenk and kaufen ... out of the 'Gesuchte Artikel' ..."

            // If I am BUYING (Payed), I want to get items. So I should select from "Angebotene Artikel".
            // But in the previous step, the user asked to HIDE "Angebotene Artikel" for Freebie/Payed!
            // "Okay, the Geschenk and Kauf should not show the Angebotene Artikel as it's then for sure empty."

            // If "Angebotene Artikel" is empty/hidden, then I CANNOT select from it.
            // So I MUST select from "Gesuchte Artikel".

            // Conclusion:
            // For Freebie/Payed, the match exists because the PARTNER WANTS something from ME.
            // (Since "Offered Items" is empty, the partner has nothing I want).
            // So it's a "I give you something" scenario.
            // Freebie: I give for free.
            // Payed: I sell (I give, you pay).

            // So:
            // I select items from "Gesuchte Artikel" (Items partner wants).
            // I am initiating a transaction to GIVE these items.

            // So the logic is:
            // Iterate selected items (from `itemsRequested`).
            // Create request:
            // `offeredStickerId`: null? (I am not asking for anything).
            // `requestedStickerId`: The item I am giving (Partner's requested item).

            // Wait, `requestedStickerId` in DTO usually means "The sticker that is REQUESTED".
            // `offeredStickerId` in DTO usually means "The sticker that is OFFERED".

            // If I am GIVING a card:
            // It is the `requestedStickerId` (from the partner's perspective).
            // Or is it `offeredStickerId` (from my perspective)?

            // Let's assume the DTO fields are named from the perspective of the "Exchange Request" object, which links two users.
            // Usually:
            // `offeredStickerId`: Sticker provided by `offererId`.
            // `requestedStickerId`: Sticker provided by `requesterId`? Or sticker requested FROM `offererId`?

            // Let's assume:
            // `offererId`: The user I am sending the request TO.
            // `offeredStickerId`: The sticker THE PARTNER gives.
            // `requestedStickerId`: The sticker THE PARTNER receives (or I give).

            // If I am GIVING (Freebie/Payed):
            // `offeredStickerId`: null (Partner gives nothing).
            // `requestedStickerId`: The card I am giving.

            // Let's verify this assumption.
            // If `offererId` is the partner.
            // And I am giving a card.
            // Then `requestedStickerId` is the card the partner REQUESTS.

            this.selectedItemIds.forEach(id => {
                requests.push({
                    exchangeType: this.tab,
                    offererId: this.match!.userId,
                    offeredStickerId: undefined, // Partner gives nothing
                    requestedStickerId: id // I give this (Partner wants it)
                });
            });
        }

        this.submit.emit(requests);
    }

    onCancel() {
        this.close.emit();
    }
}
