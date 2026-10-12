(() => {
    'use strict';

    const modalElement = document.getElementById('replaceAssetModal');
    if (!modalElement) {
        return;
    }

    const form = document.getElementById('replaceAssetForm');
    const fields = {
        allocationId: document.getElementById('replaceAllocationId'),
        employeeName: document.getElementById('replaceEmployeeName'),
        assetName: document.getElementById('replaceOldAssetName'),
        assetCode: document.getElementById('replaceOldAssetCode'),
        ownershipType: document.getElementById('replaceOldOwnership')
    };

    document.addEventListener('click', (event) => {
        const trigger = event.target.closest('[data-asset-replace]');
        if (!trigger) {
            return;
        }

        fields.allocationId.value = trigger.dataset.allocationId ?? '';
        fields.employeeName.value = trigger.dataset.employeeName ?? '';
        fields.assetName.value = trigger.dataset.assetName ?? '';
        fields.assetCode.value = trigger.dataset.assetCode ?? '';
        fields.ownershipType.value = trigger.dataset.ownershipType ?? '';
        bootstrap.Modal.getOrCreateInstance(modalElement).show();
    });

    modalElement.addEventListener('hidden.bs.modal', () => form.reset());
})();
