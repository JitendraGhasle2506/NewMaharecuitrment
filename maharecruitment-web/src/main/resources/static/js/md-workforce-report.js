(() => {
    const page = document.querySelector('.md-workforce-report');
    if (!page) {
        return;
    }

    const searchInput = document.getElementById('mdWorkforceSearch');
    const expandAllButton = document.getElementById('mdReportExpandAll');
    const collapseAllButton = document.getElementById('mdReportCollapseAll');
    const visibleEmployeeCount = document.getElementById('mdReportVisibleEmployees');
    const noResults = document.getElementById('mdReportNoResults');
    const wings = Array.from(page.querySelectorAll('.md-report-wing'));
    const cells = Array.from(page.querySelectorAll('.md-report-cell'));
    const employees = Array.from(page.querySelectorAll('.md-report-employee'));

    expandAllButton?.addEventListener('click', () => setAllOpen(true));
    collapseAllButton?.addEventListener('click', () => {
        searchInput.value = '';
        resetVisibility();
        setAllOpen(false);
    });
    searchInput?.addEventListener('input', applySearch);

    function setAllOpen(open) {
        wings.forEach((wing) => {
            wing.open = open;
        });
        cells.forEach((cell) => {
            cell.open = open;
        });
    }

    function applySearch() {
        const search = normalize(searchInput.value);
        if (!search) {
            resetVisibility();
            return;
        }

        let visibleWings = 0;
        let visibleEmployees = 0;

        wings.forEach((wing) => {
            const wingMatched = matches(wing, search);
            let matchingCells = 0;

            Array.from(wing.querySelectorAll('.md-report-cell')).forEach((cell) => {
                const cellMatched = wingMatched || matches(cell, search);
                let matchingEmployees = 0;

                Array.from(cell.querySelectorAll('.md-report-employee')).forEach((employee) => {
                    const employeeMatched = cellMatched || matches(employee, search);
                    employee.hidden = !employeeMatched;
                    if (employeeMatched) {
                        matchingEmployees += 1;
                        visibleEmployees += 1;
                    }
                });

                const showCell = cellMatched || matchingEmployees > 0;
                cell.hidden = !showCell;
                cell.open = showCell;
                if (showCell) {
                    matchingCells += 1;
                }
            });

            const showWing = wingMatched || matchingCells > 0;
            wing.hidden = !showWing;
            wing.open = showWing;
            if (showWing) {
                visibleWings += 1;
            }
        });

        visibleEmployeeCount.textContent = String(visibleEmployees);
        noResults?.classList.toggle('d-none', visibleWings > 0);
    }

    function resetVisibility() {
        wings.forEach((wing) => {
            wing.hidden = false;
        });
        cells.forEach((cell) => {
            cell.hidden = false;
        });
        employees.forEach((employee) => {
            employee.hidden = false;
        });
        const totalEmployees = employees.length;
        visibleEmployeeCount.textContent = String(totalEmployees);
        noResults?.classList.add('d-none');
    }

    function matches(element, search) {
        return normalize(element.dataset.search).includes(search);
    }

    function normalize(value) {
        return String(value || '').trim().toLowerCase();
    }
})();
