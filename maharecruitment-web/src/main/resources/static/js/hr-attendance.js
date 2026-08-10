(function () {
    'use strict';

    const search = document.querySelector('[data-attendance-search]');
    if (!search) {
        return;
    }

    const rows = Array.from(document.querySelectorAll('[data-attendance-row]'));
    const count = document.querySelector('[data-attendance-count]');
    const noResults = document.querySelector('[data-attendance-no-results]');

    search.addEventListener('input', () => {
        const query = search.value.trim().toLowerCase();
        let visibleRows = 0;

        rows.forEach((row) => {
            const matches = !query || (row.dataset.attendanceRow || '').includes(query);
            row.classList.toggle('d-none', !matches);
            visibleRows += matches ? 1 : 0;
        });

        if (count) {
            const singular = count.dataset.countSingular || 'item';
            const plural = count.dataset.countPlural || 'items';
            count.textContent = `${visibleRows} ${visibleRows === 1 ? singular : plural}`;
        }
        if (noResults) {
            noResults.classList.toggle('d-none', visibleRows > 0);
        }
    });
})();
