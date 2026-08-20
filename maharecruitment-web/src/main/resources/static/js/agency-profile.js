(function () {
    "use strict";

    const AUTO_MASK_DELAY_MS = 30000;
    const fieldLabels = {
        PAN: "PAN number",
        GST: "GST number",
        CERTIFICATE: "Certificate of Incorporation number",
        TAN: "TAN number"
    };

    document.addEventListener("DOMContentLoaded", function () {
        const profileForm = document.getElementById("agencyProfileForm");
        const revealUrl = profileForm?.dataset.sensitiveRevealUrl?.replace(/\/+$/, "");
        if (!profileForm || !revealUrl) return;

        const buttons = Array.from(profileForm.querySelectorAll("[data-sensitive-toggle]"));
        const maskTimers = new WeakMap();

        const getElements = function (button) {
            const field = button.dataset.sensitiveToggle;
            return {
                field: field,
                label: fieldLabels[field] || "identifier",
                display: profileForm.querySelector(`[data-sensitive-display="${field}"]`),
                error: profileForm.querySelector(`[data-sensitive-error="${field}"]`),
                icon: button.querySelector("i")
            };
        };

        const clearMaskTimer = function (button) {
            const timerId = maskTimers.get(button);
            if (timerId) window.clearTimeout(timerId);
            maskTimers.delete(button);
        };

        const maskValue = function (button) {
            const elements = getElements(button);
            clearMaskTimer(button);
            if (elements.display) {
                elements.display.textContent = elements.display.dataset.maskedValue || "-";
            }
            button.setAttribute("aria-pressed", "false");
            button.setAttribute("aria-label", `Show full ${elements.label}`);
            button.title = `Show full ${elements.label}`;
            if (elements.icon) elements.icon.className = "fa-solid fa-eye";
        };

        const revealValue = async function (button) {
            const elements = getElements(button);
            if (!elements.display || !elements.field) return;

            if (button.getAttribute("aria-pressed") === "true") {
                maskValue(button);
                return;
            }

            buttons.filter(function (candidate) { return candidate !== button; }).forEach(maskValue);
            if (elements.error) elements.error.textContent = "";
            button.disabled = true;
            if (elements.icon) elements.icon.className = "fa-solid fa-spinner fa-spin";

            try {
                const response = await fetch(`${revealUrl}/${encodeURIComponent(elements.field)}`, {
                    method: "GET",
                    credentials: "same-origin",
                    cache: "no-store",
                    headers: {
                        "Accept": "application/json",
                        "X-App-Loader": "off"
                    }
                });
                if (!response.ok) throw new Error("Sensitive identifier request failed");

                const result = await response.json();
                if (!result || typeof result.value !== "string" || !result.value.trim()) {
                    throw new Error("Sensitive identifier response was empty");
                }

                elements.display.textContent = result.value;
                button.setAttribute("aria-pressed", "true");
                button.setAttribute("aria-label", `Hide full ${elements.label}`);
                button.title = `Hide full ${elements.label}`;
                if (elements.icon) elements.icon.className = "fa-solid fa-eye-slash";
                maskTimers.set(button, window.setTimeout(function () {
                    maskValue(button);
                }, AUTO_MASK_DELAY_MS));
            } catch (error) {
                maskValue(button);
                if (elements.error) elements.error.textContent = `Unable to show the full ${elements.label}. Please try again.`;
            } finally {
                button.disabled = false;
            }
        };

        buttons.forEach(function (button) {
            maskValue(button);
            button.addEventListener("click", function () {
                revealValue(button);
            });
        });

        const maskAllValues = function () {
            buttons.forEach(maskValue);
        };
        window.addEventListener("pagehide", maskAllValues);
        document.addEventListener("visibilitychange", function () {
            if (document.hidden) maskAllValues();
        });
    });
}());
