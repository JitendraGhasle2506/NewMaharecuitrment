(function () {
    const root = document.querySelector('.employee-dashboard');
    if (!root) {
        return;
    }

    function openDialog(dialog) {
        if (typeof dialog.showModal === 'function') {
            dialog.showModal();
            return;
        }
        dialog.setAttribute('open', '');
    }

    function closeDialog(dialog) {
        if (typeof dialog.close === 'function') {
            dialog.close();
            return;
        }
        dialog.removeAttribute('open');
    }

    root.addEventListener('click', (event) => {
        const opener = event.target.closest('[data-dashboard-dialog-target]');
        if (opener) {
            const dialog = document.getElementById(opener.dataset.dashboardDialogTarget);
            if (dialog && !dialog.open) {
                openDialog(dialog);
            }
            return;
        }

        const closeButton = event.target.closest('[data-dashboard-dialog-close]');
        if (closeButton) {
            const dialog = closeButton.closest('dialog');
            if (dialog) {
                closeDialog(dialog);
            }
        }
    });

    root.querySelectorAll('.dashboard-dialog').forEach((dialog) => {
        dialog.addEventListener('click', (event) => {
            if (event.target === dialog) {
                closeDialog(dialog);
            }
        });
        dialog.addEventListener('close', () => {
            dialog.querySelectorAll('details[open]').forEach((details) => details.removeAttribute('open'));
        });
    });
})();
