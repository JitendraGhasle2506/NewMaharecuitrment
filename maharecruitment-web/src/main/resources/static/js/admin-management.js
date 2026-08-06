(() => {
	'use strict';

	const initializeManagementUi = () => {
		for (const select of document.querySelectorAll('[data-auto-submit]')) {
			select.addEventListener('change', () => {
				const form = select.closest('form');
				if (form) {
					form.requestSubmit();
				}
			});
		}

		for (const form of document.querySelectorAll('.js-confirm-action')) {
			form.addEventListener('submit', (event) => {
				const message = form.dataset.confirmMessage || 'Continue with this action?';
				if (!window.confirm(message)) {
					event.preventDefault();
				}
			});
		}

		const roleNameSelect = document.getElementById('name');
		const rolePreviewName = document.getElementById('rolePreviewName');
		const rolePreviewCode = document.getElementById('rolePreviewCode');
		if (!roleNameSelect || !rolePreviewName || !rolePreviewCode) {
			return;
		}

		const updateRolePreview = () => {
			const roleName = roleNameSelect.value;
			if (!roleName) {
				rolePreviewName.textContent = 'Choose a role';
				rolePreviewCode.textContent = 'No role selected';
				return;
			}

			const friendlyName = roleName
				.replace(/^ROLE_/, '')
				.split('_')
				.map((part) => part.charAt(0) + part.slice(1).toLowerCase())
				.join(' ');
			rolePreviewName.textContent = friendlyName;
			rolePreviewCode.textContent = roleName;
		};

		roleNameSelect.addEventListener('change', updateRolePreview);
		updateRolePreview();
	};

	if (document.readyState === 'loading') {
		document.addEventListener('DOMContentLoaded', initializeManagementUi, { once: true });
	} else {
		initializeManagementUi();
	}
})();
