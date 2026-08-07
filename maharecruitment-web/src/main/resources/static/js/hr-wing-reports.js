(function () {
    'use strict';

    const search = document.querySelector('[data-wing-search]');
    if (!search) {
        return;
    }

    const cards = Array.from(document.querySelectorAll('[data-wing-row]'));
    const count = document.querySelector('[data-wing-count]');
    const noResults = document.querySelector('[data-wing-no-results]');

    search.addEventListener('input', () => {
        const query = search.value.trim().toLowerCase();
        let visibleWings = 0;

        cards.forEach((card) => {
            const matches = !query || (card.dataset.wingRow || '').includes(query);
            card.classList.toggle('d-none', !matches);
            visibleWings += matches ? 1 : 0;
        });

        if (count) {
            count.textContent = `${visibleWings} ${visibleWings === 1 ? 'wing' : 'wings'}`;
        }
        if (noResults) {
            noResults.classList.toggle('d-none', visibleWings > 0);
        }
    });
})();
