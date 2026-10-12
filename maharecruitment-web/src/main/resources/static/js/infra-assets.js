(function () {
    'use strict';

    const viewIds = [
        'listView',
        'addCategoryView',
        'addAssetView',
        'allocateAssetView',
        'returnAssetView'
    ];
    const views = new Map(
        viewIds
            .map((id) => [id, document.getElementById(id)])
            .filter(([, element]) => element)
    );

    if (!views.has('listView')) {
        return;
    }

    function showView(viewId) {
        const requestedView = views.get(viewId);
        if (!requestedView) {
            return;
        }
        views.forEach((view) => view.classList.add('d-none'));
        requestedView.classList.remove('d-none');

        const firstControl = requestedView.querySelector('input:not([type="hidden"]), select, textarea');
        firstControl?.focus({ preventScroll: true });
        requestedView.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }

    document.addEventListener('click', (event) => {
        const viewButton = event.target.closest('[data-asset-view-target]');
        if (viewButton) {
            showView(viewButton.dataset.assetViewTarget);
            return;
        }

        const returnButton = event.target.closest('[data-asset-return]');
        if (!returnButton) {
            return;
        }

        const allocationInput = document.getElementById('returnAllocationId');
        const assetInput = document.getElementById('returnAssetName');
        const employeeInput = document.getElementById('returnEmpName');
        if (!allocationInput || !assetInput || !employeeInput) {
            return;
        }
        allocationInput.value = returnButton.dataset.allocationId || '';
        assetInput.value = returnButton.dataset.assetName || '';
        employeeInput.value = returnButton.dataset.employeeName || '';
        showView('returnAssetView');
    });
})();
