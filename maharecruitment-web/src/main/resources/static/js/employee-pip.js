document.addEventListener('DOMContentLoaded', () => {
    const selector = document.querySelector('.pip-employee-select');
    if (selector && window.jQuery?.fn?.select2) {
        window.jQuery(selector).select2({ theme: 'bootstrap-5', width: '100%', placeholder: 'Search employee name or code' });
    }
    const decision = document.getElementById('decision');
    const exitDate = document.getElementById('exitDate');
    const exitFields = document.getElementById('pipExitFields');
    if (decision && exitDate && exitFields) {
        const update = () => {
            const exiting = decision.value === 'EXIT';
            exitFields.hidden = !exiting;
            exitDate.required = exiting;
            exitDate.disabled = !exiting;
        };
        decision.addEventListener('change', update);
        update();
    }
});
