(function () {
    'use strict';

    const search = document.querySelector('[data-cell-search]');
    if (!search) {
        return;
    }

    const rows = Array.from(document.querySelectorAll('[data-cell-row]'));
    const count = document.querySelector('[data-cell-count]');
    const noResults = document.querySelector('[data-cell-no-results]');

    search.addEventListener('input', () => {
        const query = search.value.trim().toLowerCase();
        let visibleCells = 0;

        rows.forEach((row) => {
            const matches = !query || (row.dataset.cellRow || '').includes(query);
            row.classList.toggle('d-none', !matches);
            visibleCells += matches ? 1 : 0;
        });

        if (count) {
            count.textContent = `${visibleCells} ${visibleCells === 1 ? 'cell' : 'cells'}`;
        }
        if (noResults) {
            noResults.classList.toggle('d-none', visibleCells > 0);
        }
    });
})();
