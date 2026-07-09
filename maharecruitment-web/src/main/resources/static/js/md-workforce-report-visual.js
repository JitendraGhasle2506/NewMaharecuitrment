(() => {
    const page = document.querySelector('.md-workforce-report');
    if (!page) {
        return;
    }

    const searchInput = document.getElementById('mdWorkforceSearch');
    const expandAllButton = document.getElementById('mdReportExpandAll');
    const collapseAllButton = document.getElementById('mdReportCollapseAll');
    const visibleEmployeeCount = document.getElementById('mdReportVisibleEmployees');
    const selectedCount = document.getElementById('mdReportSelectedCount');
    const selectionWing = document.getElementById('mdReportSelectionWing');
    const selectionTitle = document.getElementById('mdReportSelectionTitle');
    const selectionMeta = document.getElementById('mdReportSelectionMeta');
    const selectionEmpty = document.getElementById('mdReportSelectionEmpty');
    const noResults = document.getElementById('mdReportNoResults');

    const wings = Array.from(page.querySelectorAll('.md-visual-wing'));
    const cellButtons = Array.from(page.querySelectorAll('.md-visual-cell'));
    const employeeGroups = Array.from(page.querySelectorAll('.md-visual-employee-group'));

    expandAllButton?.addEventListener('click', () => {
        wings.forEach((wing) => {
            wing.open = true;
        });
    });

    collapseAllButton?.addEventListener('click', () => {
        searchInput.value = '';
        resetVisibility();
        wings.forEach((wing) => {
            wing.open = false;
        });
    });

    searchInput?.addEventListener('input', applySearch);

    cellButtons.forEach((button) => {
        button.addEventListener('click', () => selectCell(button.dataset.cellId));
    });

    selectCell(firstVisibleCellId());

    function selectCell(cellId) {
        const activeButton = cellButtons.find((button) => String(button.dataset.cellId) === String(cellId) && !button.hidden);
        if (!activeButton) {
            clearSelection();
            return;
        }

        cellButtons.forEach((button) => {
            button.classList.toggle('is-active', button === activeButton);
        });

        const activeGroup = employeeGroups.find((group) => String(group.dataset.cellId) === String(cellId));
        employeeGroups.forEach((group) => {
            group.hidden = group !== activeGroup;
        });

        const parentWing = activeButton.closest('.md-visual-wing');
        if (parentWing) {
            parentWing.open = true;
            wings.forEach((wing) => {
                wing.classList.toggle('is-active', wing === parentWing);
            });
        }

        const count = activeGroup ? visibleEmployeesInGroup(activeGroup) : Number(activeButton.dataset.employeeCount || 0);
        selectionWing.textContent = activeButton.dataset.wingName || 'Wing';
        selectionTitle.textContent = activeButton.dataset.cellName || 'Cell';
        selectionMeta.textContent = `${count} ${count === 1 ? 'employee' : 'employees'} mapped`;
        selectedCount.textContent = String(count);
        visibleEmployeeCount.textContent = String(count);
        if (selectionEmpty) {
            selectionEmpty.hidden = true;
        }
    }

    function clearSelection() {
        cellButtons.forEach((button) => button.classList.remove('is-active'));
        wings.forEach((wing) => wing.classList.remove('is-active'));
        employeeGroups.forEach((group) => {
            group.hidden = true;
        });
        selectionWing.textContent = 'Select Wing / Cell';
        selectionTitle.textContent = 'Employees';
        selectionMeta.textContent = 'Select a cell to view employees';
        selectedCount.textContent = '0';
        visibleEmployeeCount.textContent = '0';
        if (selectionEmpty) {
            selectionEmpty.hidden = false;
        }
    }

    function applySearch() {
        const search = normalize(searchInput.value);
        if (!search) {
            resetVisibility();
            const activeCell = page.querySelector('.md-visual-cell.is-active:not([hidden])');
            selectCell(activeCell?.dataset.cellId || firstVisibleCellId());
            return;
        }

        let firstMatch = null;
        let matchingWingCount = 0;
        let matchingEmployeeCount = 0;

        wings.forEach((wing) => {
            const wingMatched = matches(wing, search);
            let visibleCells = 0;

            cellButtons
                .filter((button) => button.dataset.wingId === wing.dataset.wingId)
                .forEach((button) => {
                    const cellMatched = wingMatched || matches(button, search);
                    const group = groupForCell(button.dataset.cellId);
                    let visibleEmployees = 0;

                    if (group) {
                        Array.from(group.querySelectorAll('.md-visual-employee')).forEach((employee) => {
                            const employeeMatched = cellMatched || matches(employee, search);
                            employee.hidden = !employeeMatched;
                            if (employeeMatched) {
                                visibleEmployees += 1;
                                matchingEmployeeCount += 1;
                            }
                        });
                    }

                    const showCell = cellMatched || visibleEmployees > 0;
                    button.hidden = !showCell;
                    if (showCell) {
                        visibleCells += 1;
                        firstMatch = firstMatch || button;
                    }
                });

            const showWing = wingMatched || visibleCells > 0;
            wing.hidden = !showWing;
            wing.open = showWing;
            if (showWing) {
                matchingWingCount += 1;
            }
        });

        noResults?.classList.toggle('d-none', matchingWingCount > 0);
        visibleEmployeeCount.textContent = String(matchingEmployeeCount);

        const activeCell = page.querySelector('.md-visual-cell.is-active:not([hidden])');
        selectCell(activeCell?.dataset.cellId || firstMatch?.dataset.cellId);
    }

    function resetVisibility() {
        wings.forEach((wing) => {
            wing.hidden = false;
        });
        cellButtons.forEach((button) => {
            button.hidden = false;
        });
        employeeGroups.forEach((group) => {
            Array.from(group.querySelectorAll('.md-visual-employee')).forEach((employee) => {
                employee.hidden = false;
            });
        });
        noResults?.classList.add('d-none');
    }

    function firstVisibleCellId() {
        return cellButtons.find((button) => !button.hidden)?.dataset.cellId || null;
    }

    function groupForCell(cellId) {
        return employeeGroups.find((group) => String(group.dataset.cellId) === String(cellId));
    }

    function visibleEmployeesInGroup(group) {
        return Array.from(group.querySelectorAll('.md-visual-employee'))
            .filter((employee) => !employee.hidden)
            .length;
    }

    function matches(element, search) {
        return normalize(element.dataset.search).includes(search);
    }

    function normalize(value) {
        return String(value || '').trim().toLowerCase();
    }
})();
