(function () {
    "use strict";

    const form = document.getElementById("commonCalendarForm");
    const typeInputs = Array.from(document.querySelectorAll('input[name="entryType"]'));
    const descriptionField = document.getElementById("calendarDescriptionField");
    const dateModeField = document.getElementById("calendarDateModeField");
    const dateModeInputs = Array.from(document.querySelectorAll('input[name="dateMode"]'));
    const entryDate = document.getElementById("entryDate");
    const entryDateLabel = document.getElementById("entryDateLabel");
    const endDateField = document.getElementById("calendarEndDateField");
    const endDate = document.getElementById("endDate");
    const description = document.getElementById("description");
    const descriptionCount = document.getElementById("descriptionCount");
    const impactNote = document.getElementById("calendarImpactNote");
    const photosField = document.getElementById("calendarPhotosField");
    const photoInput = document.getElementById("calendarPhotos");
    const photoPreview = document.getElementById("calendarPhotoPreview");
    const photoFeedback = document.getElementById("calendarPhotoFeedback");
    const searchInput = document.getElementById("calendarEntrySearch");
    const entryRows = Array.from(document.querySelectorAll(".calendar-entry-row"));
    const archiveForms = Array.from(document.querySelectorAll(".archive-entry-form"));
    const workingDayForm = document.getElementById("workingDayForm");
    const workingDate = document.getElementById("workingDate");

    typeInputs.forEach(input => input.addEventListener("change", updateTypeHelp));
    dateModeInputs.forEach(input => input.addEventListener("change", updateDateModeUi));
    entryDate?.addEventListener("change", validateDateRange);
    endDate?.addEventListener("change", validateDateRange);
    description?.addEventListener("input", updateDescriptionCount);
    photoInput?.addEventListener("change", updatePhotoPreview);
    searchInput?.addEventListener("input", filterEntries);
    form?.addEventListener("submit", validateForm);
    workingDayForm?.addEventListener("submit", validateWorkingDayForm);
    workingDate?.addEventListener("change", validateWorkingDate);
    archiveForms.forEach(archiveForm => archiveForm.addEventListener("submit", confirmArchive));

    updateTypeHelp();
    updateDescriptionCount();

    function selectedType() {
        return typeInputs.find(input => input.checked)?.value || "";
    }

    function selectedDateMode() {
        return dateModeInputs.find(input => input.checked)?.value || "SINGLE";
    }

    function updateTypeHelp() {
        const type = selectedType();
        const holiday = type === "HOLIDAY";
        descriptionField?.classList.toggle("d-none", holiday);
        photosField?.classList.toggle("d-none", holiday);
        dateModeField?.classList.toggle("d-none", holiday);
        if (holiday) {
            const singleDate = dateModeInputs.find(input => input.value === "SINGLE");
            if (singleDate) singleDate.checked = true;
        }
        updateDateModeUi();
        if (holiday && photoInput?.files?.length) {
            photoInput.value = "";
            updatePhotoPreview();
        }
        if (impactNote) {
            impactNote.innerHTML = holiday
                ? '<i class="fas fa-exclamation-circle me-1"></i> This date will be treated as <strong>HOLIDAY</strong> in internal attendance.'
                : '<i class="fas fa-info-circle me-1"></i> Festivals and events are informational and do not change attendance.';
        }
    }

    function updateDateModeUi() {
        const range = selectedType() !== "HOLIDAY" && selectedDateMode() === "RANGE";
        endDateField?.classList.toggle("d-none", !range);
        if (entryDateLabel) entryDateLabel.textContent = range ? "From date" : "Date";
        if (endDate) {
            endDate.required = range;
            if (!range) {
                endDate.value = "";
                endDate.setCustomValidity("");
            }
        }
        validateDateRange();
    }

    function validateDateRange() {
        if (!endDate || selectedDateMode() !== "RANGE" || selectedType() === "HOLIDAY") return true;
        const invalid = Boolean(entryDate?.value && endDate.value && endDate.value < entryDate.value);
        endDate.setCustomValidity(invalid ? "To date must be on or after the from date." : "");
        return !invalid;
    }

    function updateDescriptionCount() {
        if (descriptionCount) descriptionCount.textContent = String(description?.value?.length || 0);
    }

    function filterEntries() {
        const query = searchInput.value.trim().toLocaleLowerCase();
        entryRows.forEach(row => {
            row.classList.toggle("d-none", Boolean(query) && !row.textContent.toLocaleLowerCase().includes(query));
        });
    }

    function validateForm(event) {
        if (!form) return;
        form.querySelectorAll(".is-invalid").forEach(element => element.classList.remove("is-invalid"));
        const requiredFields = [entryDate, document.getElementById("title")];
        if (selectedType() !== "HOLIDAY" && selectedDateMode() === "RANGE") requiredFields.push(endDate);
        const invalidField = requiredFields.find(field => !field?.value?.trim());
        if (!selectedType() || invalidField) {
            event.preventDefault();
            invalidField?.classList.add("is-invalid");
            invalidField?.focus();
            return;
        }
        if (!validateDateRange()) {
            event.preventDefault();
            endDate?.reportValidity();
            return;
        }
        if (photoInput && !photoInput.checkValidity()) {
            event.preventDefault();
            photoInput.reportValidity();
        }
    }

    function updatePhotoPreview() {
        if (!photoInput || !photoPreview) return;
        photoPreview.querySelectorAll("img").forEach(image => URL.revokeObjectURL(image.src));
        photoPreview.replaceChildren();

        const files = Array.from(photoInput.files || []);
        const existingCount = Number.parseInt(photoInput.dataset.existingCount || "0", 10);
        const availableSlots = Math.max(0, 10 - existingCount);
        const tooMany = files.length > availableSlots;
        const message = tooMany
            ? `You can add ${availableSlots} more photo${availableSlots === 1 ? "" : "s"}.`
            : "";
        photoInput.setCustomValidity(message);
        if (photoFeedback) photoFeedback.textContent = message;

        files.slice(0, availableSlots).forEach(file => {
            const image = document.createElement("img");
            image.src = URL.createObjectURL(file);
            image.alt = file.name;
            image.title = file.name;
            photoPreview.appendChild(image);
        });
    }

    function validateWorkingDayForm(event) {
        if (!validateWorkingDate()) {
            event.preventDefault();
            workingDate?.reportValidity();
        }
    }

    function validateWorkingDate() {
        if (!workingDate) return true;
        if (!workingDate.value) {
            workingDate.setCustomValidity("");
            return true;
        }
        const day = new Date(`${workingDate.value}T00:00:00`).getDay();
        const isWeekend = day === 0 || day === 6;
        workingDate.setCustomValidity(isWeekend ? "" : "Select a Saturday or Sunday.");
        return isWeekend;
    }

    function confirmArchive(event) {
        const message = event.currentTarget.dataset.confirmMessage || "Archive this entry?";
        if (!window.confirm(message)) event.preventDefault();
    }
})();
