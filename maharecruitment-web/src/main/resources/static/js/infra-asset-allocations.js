(() => {
    'use strict';

    const replaceModalElement = document.getElementById('replaceAssetModal');
    const replaceForm = document.getElementById('replaceAssetForm');
    const replaceFields = {
        allocationId: document.getElementById('replaceAllocationId'),
        employeeName: document.getElementById('replaceEmployeeName'),
        assetName: document.getElementById('replaceOldAssetName'),
        assetCode: document.getElementById('replaceOldAssetCode'),
        ownershipType: document.getElementById('replaceOldOwnership')
    };
    const returnModalElement = document.getElementById('returnAssetModal');
    const returnForm = document.getElementById('returnAssetForm');
    const returnFields = {
        allocationId: document.getElementById('returnAllocationId'),
        employeeName: document.getElementById('returnEmployeeName'),
        assetName: document.getElementById('returnAssetName'),
        assetCode: document.getElementById('returnAssetCode')
    };

    function initializeSearchableSelects() {
        if (!window.jQuery?.fn?.select2) {
            return;
        }

        window.jQuery('.asset-search-select').each(function () {
            const select = window.jQuery(this);
            if (select.hasClass('select2-hidden-accessible')) {
                return;
            }

            const parentModal = select.closest('.modal');
            const options = {
                theme: 'bootstrap-5',
                width: '100%',
                placeholder: this.dataset.placeholder || 'Search and select',
                allowClear: true
            };
            if (parentModal.length) {
                options.dropdownParent = parentModal;
            }
            select.select2(options);
        });
    }

    initializeSearchableSelects();

    document.addEventListener('click', (event) => {
        const replaceTrigger = event.target.closest('[data-asset-replace]');
        if (replaceTrigger && replaceModalElement) {
            replaceFields.allocationId.value = replaceTrigger.dataset.allocationId ?? '';
            replaceFields.employeeName.value = replaceTrigger.dataset.employeeName ?? '';
            replaceFields.assetName.value = replaceTrigger.dataset.assetName ?? '';
            replaceFields.assetCode.value = replaceTrigger.dataset.assetCode ?? '';
            replaceFields.ownershipType.value = replaceTrigger.dataset.ownershipType ?? '';
            bootstrap.Modal.getOrCreateInstance(replaceModalElement).show();
            return;
        }

        const returnTrigger = event.target.closest('[data-asset-return]');
        if (returnTrigger && returnModalElement) {
            returnFields.allocationId.value = returnTrigger.dataset.allocationId ?? '';
            returnFields.employeeName.value = returnTrigger.dataset.employeeName ?? '';
            returnFields.assetName.value = returnTrigger.dataset.assetName ?? '';
            returnFields.assetCode.value = returnTrigger.dataset.assetCode ?? '';
            bootstrap.Modal.getOrCreateInstance(returnModalElement).show();
        }
    });

    replaceModalElement?.addEventListener('hidden.bs.modal', () => {
        replaceForm?.reset();
        if (window.jQuery?.fn?.select2) {
            window.jQuery('#replacementAssetId').val('').trigger('change.select2');
        }
    });
    returnModalElement?.addEventListener('hidden.bs.modal', () => returnForm?.reset());
})();
