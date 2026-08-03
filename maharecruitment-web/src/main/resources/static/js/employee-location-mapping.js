(function () {
    "use strict";

    document.addEventListener("DOMContentLoaded", function () {
        const form = document.getElementById("employeeLocationMappingForm");
        const picker = document.getElementById("employeeLocationPicker");
        const inputContainer = document.getElementById("selectedEmployeeLocationInputs");
        const selectedBox = document.getElementById("selectedEmployeeLocationBox");
        const selectedCount = document.getElementById("selectedEmployeeLocationCount");
        const clearButton = document.getElementById("clearEmployeeLocations");
        const primaryInput = document.getElementById("primaryLocationId");

        if (!form || !picker || !inputContainer || !selectedBox || !primaryInput) {
            return;
        }

        function selectedInputs() {
            return Array.from(inputContainer.querySelectorAll("input[name='selectedLocationIds']"));
        }

        function selectedIds() {
            return selectedInputs()
                .map(function (input) {
                    return input.value;
                })
                .filter(Boolean);
        }

        function primaryLocationId() {
            return primaryInput.value ? String(primaryInput.value) : "";
        }

        function setPrimary(locationId) {
            const normalizedId = locationId ? String(locationId) : "";
            primaryInput.value = normalizedId;
            selectedInputs().forEach(function (input) {
                input.dataset.primary = String(input.value === normalizedId);
            });
        }

        function optionFor(locationId) {
            return Array.from(picker.options).find(function (option) {
                return option.value === String(locationId);
            });
        }

        function labelFor(locationId, input) {
            const option = optionFor(locationId);
            if (input && input.dataset.label) {
                return input.dataset.label;
            }
            if (option && option.dataset.label) {
                return option.dataset.label;
            }
            if (option && option.textContent.trim()) {
                return option.textContent.trim();
            }
            return "Location #" + locationId;
        }

        function resetPicker() {
            picker.value = "";
            if (window.jQuery && window.jQuery.fn && window.jQuery.fn.select2) {
                window.jQuery(picker).val(null).trigger("change.select2");
            }
        }

        function syncValidity() {
            const ids = selectedIds();
            const hasLocation = ids.length > 0;
            const primaryId = primaryLocationId();
            const hasValidPrimary = primaryId !== "" && ids.includes(primaryId);
            let validationMessage = "";
            if (!hasLocation) {
                validationMessage = "Select at least one employee location.";
            } else if (!hasValidPrimary) {
                validationMessage = "Select a primary location before saving.";
            }
            picker.setCustomValidity(validationMessage);
            selectedBox.classList.toggle("is-invalid", Boolean(validationMessage));
            return !validationMessage;
        }

        function addLocation(locationId) {
            if (!locationId) {
                return;
            }
            const normalizedId = String(locationId);
            if (selectedIds().includes(normalizedId)) {
                resetPicker();
                syncValidity();
                return;
            }

            const hiddenInput = document.createElement("input");
            hiddenInput.type = "hidden";
            hiddenInput.name = "selectedLocationIds";
            hiddenInput.value = normalizedId;
            hiddenInput.dataset.label = labelFor(normalizedId);
            hiddenInput.dataset.primary = "false";
            inputContainer.appendChild(hiddenInput);
            if (selectedInputs().length === 1 && !primaryLocationId()) {
                setPrimary(normalizedId);
            }

            resetPicker();
            renderSelectedLocations();
        }

        function removeLocation(locationId) {
            const normalizedId = String(locationId);
            const removedPrimary = primaryLocationId() === normalizedId;
            selectedInputs().forEach(function (input) {
                if (input.value === normalizedId) {
                    input.remove();
                }
            });
            if (removedPrimary) {
                setPrimary("");
            }
            renderSelectedLocations();
        }

        function updateCount(count) {
            if (selectedCount) {
                selectedCount.textContent = count + (count === 1 ? " mapped" : " mapped");
            }
            if (clearButton) {
                clearButton.disabled = count === 0;
            }
        }

        function renderSelectedLocations() {
            selectedBox.innerHTML = "";
            const inputs = selectedInputs();
            updateCount(inputs.length);

            if (inputs.length === 0) {
                const emptyState = document.createElement("div");
                emptyState.className = "employee-location-empty";
                const icon = document.createElement("i");
                icon.className = "fa-regular fa-map";
                const copy = document.createElement("span");
                copy.textContent = "No location selected.";
                emptyState.appendChild(icon);
                emptyState.appendChild(copy);
                selectedBox.appendChild(emptyState);
                syncValidity();
                return;
            }

            inputs.forEach(function (input) {
                const isPrimary = input.value === primaryLocationId();
                const item = document.createElement("div");
                item.className = "employee-location-selected-item";
                item.classList.toggle("is-primary", isPrimary);

                const content = document.createElement("div");
                content.className = "employee-location-selected-content";

                const label = document.createElement("div");
                label.className = "employee-location-selected-label";
                label.textContent = labelFor(input.value, input);

                const indicator = document.createElement("span");
                indicator.className = isPrimary
                    ? "employee-location-type-badge is-primary"
                    : "employee-location-type-badge is-secondary";
                indicator.textContent = isPrimary ? "Primary" : "Secondary";

                content.appendChild(label);
                content.appendChild(indicator);

                const actions = document.createElement("div");
                actions.className = "employee-location-selected-actions";

                if (!isPrimary) {
                    const primaryButton = document.createElement("button");
                    primaryButton.type = "button";
                    primaryButton.className = "employee-location-primary-btn";
                    primaryButton.textContent = "Set as Primary";
                    primaryButton.addEventListener("click", function () {
                        setPrimary(input.value);
                        renderSelectedLocations();
                    });
                    actions.appendChild(primaryButton);
                }

                const removeButton = document.createElement("button");
                removeButton.type = "button";
                removeButton.className = "employee-location-remove-btn";
                removeButton.title = "Remove location";
                removeButton.setAttribute("aria-label", "Remove " + label.textContent);
                const removeIcon = document.createElement("i");
                removeIcon.className = "fa-solid fa-xmark";
                removeButton.appendChild(removeIcon);
                removeButton.addEventListener("click", function () {
                    removeLocation(input.value);
                });

                actions.appendChild(removeButton);
                item.appendChild(content);
                item.appendChild(actions);
                selectedBox.appendChild(item);
            });
            syncValidity();
        }

        function initializePicker() {
            if (!window.jQuery || !window.jQuery.fn || !window.jQuery.fn.select2) {
                picker.addEventListener("change", function () {
                    addLocation(picker.value);
                });
                return;
            }

            const $picker = window.jQuery(picker);
            $picker.select2({
                theme: "bootstrap-5",
                width: "100%",
                placeholder: picker.dataset.placeholder || "Search and select location",
                allowClear: true
            });
            $picker.on("select2:select", function (event) {
                addLocation(event.params.data.id);
            });
        }

        form.addEventListener("submit", function (event) {
            if (!syncValidity()) {
                event.preventDefault();
                picker.reportValidity();
            }
        });

        if (clearButton) {
            clearButton.addEventListener("click", function () {
                selectedInputs().forEach(function (input) {
                    input.remove();
                });
                setPrimary("");
                renderSelectedLocations();
            });
        }

        initializePicker();
        renderSelectedLocations();
    });
})();
