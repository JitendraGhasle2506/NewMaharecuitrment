(function () {
    'use strict';

    const viewIds = [
        'listView',
        'addCategoryView',
        'addAssetView'
    ];
    const views = new Map(
        viewIds
            .map((id) => [id, document.getElementById(id)])
            .filter(([, element]) => element)
    );

    if (!views.has('listView')) {
        return;
    }

    const ownershipType = document.getElementById('assetOwnershipType');
    const rentalFields = document.getElementById('assetRentalFields');
    const rentStartDate = document.getElementById('assetRentStartDate');
    const rentEndDate = document.getElementById('assetRentEndDate');
    const purchaseDateField = document.getElementById('assetPurchaseDateField');
    const purchaseDate = document.getElementById('assetPurchaseDate');
    const warrantyExpiryField = document.getElementById('assetWarrantyExpiryField');
    const warrantyExpiryDate = document.getElementById('assetWarrantyExpiryDate');

    function syncRentalFields() {
        if (!ownershipType || !rentalFields || !rentStartDate || !rentEndDate) {
            return;
        }
        const rented = ownershipType.value === 'RENTED';
        rentalFields.classList.toggle('d-none', !rented);
        rentStartDate.disabled = !rented;
        rentEndDate.disabled = !rented;
        rentStartDate.required = rented;
        purchaseDateField?.classList.toggle('d-none', rented);
        warrantyExpiryField?.classList.toggle('d-none', rented);
        if (purchaseDate) {
            purchaseDate.disabled = rented;
        }
        if (warrantyExpiryDate) {
            warrantyExpiryDate.disabled = rented;
        }
        if (rented) {
            if (purchaseDate) {
                purchaseDate.value = '';
            }
            if (warrantyExpiryDate) {
                warrantyExpiryDate.value = '';
            }
        }
        if (!rented) {
            rentStartDate.value = '';
            rentEndDate.value = '';
            rentEndDate.removeAttribute('min');
        }
    }

    ownershipType?.addEventListener('change', syncRentalFields);
    rentStartDate?.addEventListener('change', () => {
        if (!rentEndDate) {
            return;
        }
        if (rentStartDate.value) {
            rentEndDate.min = rentStartDate.value;
            if (rentEndDate.value && rentEndDate.value < rentStartDate.value) {
                rentEndDate.value = '';
            }
        } else {
            rentEndDate.removeAttribute('min');
        }
    });
    syncRentalFields();

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

    });
})();
