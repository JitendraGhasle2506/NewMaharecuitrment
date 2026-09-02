(() => {
    'use strict';

    const normalize = (value) => (value || '').toLocaleLowerCase().trim();

    document.addEventListener('DOMContentLoaded', () => {
        const searchInput = document.querySelector('[data-team-search]');
        const rowsContainer = document.querySelector('[data-team-rows]');
        const emptyState = document.querySelector('[data-search-empty]');

        if (searchInput && rowsContainer) {
            const rows = Array.from(rowsContainer.querySelectorAll('tr[data-search-text]'));
            searchInput.addEventListener('input', () => {
                const query = normalize(searchInput.value);
                let visibleRows = 0;

                rows.forEach((row) => {
                    const matches = !query || normalize(row.dataset.searchText).includes(query);
                    row.hidden = !matches;
                    visibleRows += matches ? 1 : 0;
                });

                if (emptyState) {
                    emptyState.hidden = visibleRows !== 0;
                }
            });
        }

        const periodForm = document.querySelector('[data-period-form]');
        periodForm?.querySelectorAll('select').forEach((select) => {
            select.addEventListener('change', () => periodForm.requestSubmit());
        });
    });
})();
