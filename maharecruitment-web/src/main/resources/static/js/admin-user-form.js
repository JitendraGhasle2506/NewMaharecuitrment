(() => {
	'use strict';

	const initializeAdminUserForm = () => {
		const form = document.getElementById('adminUserForm');
		if (!form) {
			return;
		}

		const rolePicker = document.getElementById('rolePicker');
		const roleEmptyState = document.getElementById('roleEmptyState');
		const selectedRoleCount = document.getElementById('selectedRoleCount');
		const affiliationEmptyState = document.getElementById('affiliationEmptyState');
		const departmentFieldGroup = document.getElementById('departmentFieldGroup');
		const agencyFieldGroup = document.getElementById('agencyFieldGroup');
		const departmentSelect = document.getElementById('departmentRegistrationId');
		const agencySelect = document.getElementById('agencyId');
		const passwordInput = document.getElementById('adminUserPassword');
		const passwordToggle = document.getElementById('passwordToggle');
		const roleCheckboxes = Array.from(form.querySelectorAll('.selected-role-checkbox'));
		const roleById = new Map(roleCheckboxes.map((checkbox) => [checkbox.value, checkbox]));

		if (!rolePicker || !roleEmptyState || !selectedRoleCount || !affiliationEmptyState
				|| !departmentFieldGroup || !agencyFieldGroup || !departmentSelect || !agencySelect) {
			return;
		}

		const departmentRoleName = form.dataset.departmentRoleName || 'ROLE_DEPARTMENT';
		const agencyRoleName = form.dataset.agencyRoleName || 'ROLE_AGENCY';

		const selectedRoles = () => roleCheckboxes.filter((checkbox) => checkbox.checked && !checkbox.disabled);

		const setAffiliationVisibility = (group, select, visible) => {
			group.classList.toggle('d-none', !visible);
			select.disabled = !visible;
		};

		const synchronizeFormState = () => {
			const selected = selectedRoles();
			const selectedIds = new Set(selected.map((checkbox) => checkbox.value));
			const selectedNames = new Set(selected.map((checkbox) => checkbox.dataset.roleName));

			for (const option of rolePicker.options) {
				if (option.value) {
					option.disabled = selectedIds.has(option.value);
				}
			}

			const roleCount = selected.length;
			selectedRoleCount.textContent = `${roleCount} selected`;
			roleEmptyState.classList.toggle('d-none', roleCount > 0);
			rolePicker.setCustomValidity(roleCount > 0 ? '' : 'Select at least one role.');
			rolePicker.setAttribute('aria-invalid', roleCount > 0 ? 'false' : 'true');

			const showDepartment = selectedNames.has(departmentRoleName);
			const showAgency = selectedNames.has(agencyRoleName);
			setAffiliationVisibility(departmentFieldGroup, departmentSelect, showDepartment);
			setAffiliationVisibility(agencyFieldGroup, agencySelect, showAgency);
			affiliationEmptyState.classList.toggle('d-none', showDepartment || showAgency);
		};

		const addRole = (roleId) => {
			const checkbox = roleById.get(roleId);
			if (!checkbox) {
				return;
			}

			checkbox.disabled = false;
			checkbox.checked = true;
			checkbox.closest('.selected-role-item')?.classList.remove('d-none');
		};

		const removeRole = (checkbox) => {
			checkbox.checked = false;
			checkbox.disabled = true;
			checkbox.closest('.selected-role-item')?.classList.add('d-none');
		};

		rolePicker.addEventListener('change', () => {
			if (!rolePicker.value) {
				return;
			}

			addRole(rolePicker.value);
			rolePicker.value = '';
			synchronizeFormState();
		});

		for (const checkbox of roleCheckboxes) {
			checkbox.addEventListener('change', () => {
				if (!checkbox.checked) {
					removeRole(checkbox);
				}
				synchronizeFormState();
			});
		}

		form.addEventListener('click', (event) => {
			const removeButton = event.target.closest('[data-remove-role-id]');
			if (!removeButton || !form.contains(removeButton)) {
				return;
			}

			const checkbox = roleById.get(removeButton.dataset.removeRoleId);
			if (checkbox) {
				removeRole(checkbox);
				synchronizeFormState();
				rolePicker.focus();
			}
		});

		if (passwordInput && passwordToggle) {
			passwordToggle.addEventListener('click', () => {
				const showPassword = passwordInput.type === 'password';
				passwordInput.type = showPassword ? 'text' : 'password';
				passwordToggle.setAttribute('aria-label', showPassword ? 'Hide password' : 'Show password');
				passwordToggle.setAttribute('aria-pressed', String(showPassword));
				const icon = passwordToggle.querySelector('i');
				icon?.classList.toggle('fa-eye', !showPassword);
				icon?.classList.toggle('fa-eye-slash', showPassword);
			});
		}

		synchronizeFormState();
	};

	if (document.readyState === 'loading') {
		document.addEventListener('DOMContentLoaded', initializeAdminUserForm, { once: true });
	} else {
		initializeAdminUserForm();
	}
})();
