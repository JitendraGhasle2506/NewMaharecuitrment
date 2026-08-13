(() => {
    'use strict';

    const CONTROL_SELECTOR = 'form, input, textarea, select';

    const disableAutocomplete = (root) => {
        if (!(root instanceof Element) && root !== document) {
            return;
        }

        if (root instanceof Element && root.matches(CONTROL_SELECTOR)) {
            root.setAttribute('autocomplete', 'off');
        }

        root.querySelectorAll(CONTROL_SELECTOR).forEach((control) => {
            control.setAttribute('autocomplete', 'off');
        });
    };

    disableAutocomplete(document);

    const observer = new MutationObserver((mutations) => {
        mutations.forEach((mutation) => {
            mutation.addedNodes.forEach(disableAutocomplete);
        });
    });

    observer.observe(document.documentElement, {
        childList: true,
        subtree: true
    });
})();
