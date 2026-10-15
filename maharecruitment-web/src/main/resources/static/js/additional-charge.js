(() => {
    'use strict';
    const form = document.getElementById('additionalChargeForm');
    if (!form) return;
    if (window.jQuery && window.jQuery.fn.select2) {
        window.jQuery('.ac-searchable').select2({ theme: 'bootstrap-5', width: '100%' });
    }
    const start = document.getElementById('startDate');
    const end = document.getElementById('endDate');
    const validateDates = () => {
        end.min = start.value;
        end.setCustomValidity(end.value && start.value && end.value < start.value
            ? 'End date must be on or after the start date.' : '');
    };
    start.addEventListener('change', validateDates);
    end.addEventListener('change', validateDates);
    validateDates();
    const assignee = document.getElementById('assignee');
    const scope = document.getElementById('scope');
    const replaces = document.getElementById('replaces');
    const teamStatus = document.getElementById('replacementTeamStatus');
    const teamMembers = document.getElementById('replacementTeamMembers');
    let teamRequest = null;
    const previewTeam = async () => {
        if (teamRequest) teamRequest.abort();
        const controller = new AbortController();
        teamRequest = controller;
        for (const option of replaces.options) option.disabled = !!option.value && option.value === assignee.value;
        replaces.setCustomValidity(replaces.value && replaces.value === assignee.value
            ? 'The charge holder and person being replaced must be different people.' : '');
        teamMembers.replaceChildren();
        teamMembers.hidden = true;
        if (!replaces.value) {
            teamStatus.textContent = 'Select a person being replaced to preview their direct reports. A blank cell covers all cells.';
            return;
        }
        teamStatus.textContent = 'Loading direct reports…';
        try {
            const query = new URLSearchParams({ scope: scope.value, replaces: replaces.value });
            const response = await fetch(`${form.dataset.teamApi}?${query}`, { signal: controller.signal, headers: { Accept: 'application/json' } });
            const data = await response.json();
            if (!response.ok) throw new Error(data.message || 'Unable to load the replacement team.');
            if (controller.signal.aborted) return;
            const members = data.filter(member => `E:${member.id}` !== assignee.value);
            teamStatus.textContent = members.length
                ? `${members.length} direct report${members.length === 1 ? '' : 's'} will be mapped ${scope.value ? 'in the selected cell' : 'across all cells'} during the charge period. Their existing team branches follow them.`
                : 'No current direct reports in this scope. The charge can still be recorded.';
            for (const member of members) {
                const item = document.createElement('li');
                item.textContent = member.label;
                teamMembers.append(item);
            }
            teamMembers.hidden = members.length === 0;
        } catch (error) {
            if (controller.signal.aborted) return;
            teamStatus.textContent = error.message || 'Unable to load the replacement team.';
        }
    };
    if (window.jQuery && window.jQuery.fn.select2) {
        window.jQuery([assignee, scope, replaces]).on('change', previewTeam);
    } else {
        [assignee, scope, replaces].forEach(select => select.addEventListener('change', previewTeam));
    }
    previewTeam();
    form.addEventListener('submit', () => { form.querySelector('button[type="submit"]').disabled = true; });
    window.addEventListener('pageshow', () => { form.querySelector('button[type="submit"]').disabled = false; });
    document.querySelectorAll('.ac-revoke').forEach(revoke => {
        revoke.addEventListener('submit', event => {
            if (!window.confirm('Revoke this additional charge? The record will remain in history.')) event.preventDefault();
        });
    });
    const errors = document.getElementById('acErrors');
    if (errors) errors.focus();
})();
