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

    replaceModalElement?.addEventListener('hidden.bs.modal', () => replaceForm?.reset());
    returnModalElement?.addEventListener('hidden.bs.modal', () => returnForm?.reset());
})();
