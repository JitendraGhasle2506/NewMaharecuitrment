(function () {
    function openManagedDocument(path) {
        if (!path) {
            return;
        }

        try {
            var encodedPath = encodeURIComponent(btoa(path));
            window.open(contextPath + "documents/view?path=" + encodedPath, "_blank");
        } catch (error) {
            console.error("Unable to open document.", error);
            alert("Unable to open document.");
        }
    }

    window.openManagedDocument = openManagedDocument;

    var form = document.getElementById("candidateBatchForm");
    var tableBody = document.getElementById("candidateInputTableBody");
    var addRowButton = document.getElementById("addCandidateRowButton");
    var designationSelect = document.getElementById("designationVacancySelect");
    var submitButton = document.getElementById("submitCandidatesButton");
    var designationHelpText = document.getElementById("designationVacancyHelpText");
    var existingSubmittedContacts = loadExistingSubmittedContacts();

    if (!form || !tableBody || !addRowButton || !designationSelect || !submitButton) {
        return;
    }

    function createRow(index) {
        var row = document.createElement("tr");
        row.className = "candidate-input-row";
        row.innerHTML =
            '<td class="text-center candidate-index">' + (index + 1) + "</td>" +
            '<td>' +
            '<input type="text" class="form-control candidate-name-input" name="candidates[' + index + '].candidateName" minlength="2" maxlength="100" required>' +
            '<div class="invalid-feedback">Name must be 2-100 characters, cannot start with space, and cannot contain numbers.</div>' +
            '</td>' +
            '<td>' +
            '<input type="email" class="form-control email-input" name="candidates[' + index + '].email" maxlength="255" required>' +
            '<div class="invalid-feedback">Please enter a valid email address.</div>' +
            "</td>" +
            '<td>' +
            '<input type="text" class="form-control mobile-input" name="candidates[' + index + '].mobile" pattern="[0-9]{10}" maxlength="10" inputmode="numeric" required>' +
            '<div class="invalid-feedback">Mobile number must be 10 digits.</div>' +
            "</td>" +
            '<td><input type="text" class="form-control" name="candidates[' + index + '].candidateEducation" required></td>' +
            '<td><input type="number" class="form-control total-exp-input" name="candidates[' + index + '].totalExp" min="0" step="0.1" required></td>' +
            '<td><input type="number" class="form-control relevant-exp-input" name="candidates[' + index + '].relevantExp" min="0" step="0.1" required></td>' +
            '<td><input type="number" class="form-control current-ctc-input" name="candidates[' + index + '].currentCtc" min="0" max="999999999999.99" step="0.01" inputmode="decimal" required></td>' +
            '<td><select class="form-select resigned-status-input" name="candidates[' + index + '].resigned" required>' +
            '<option value="">Select</option>' +
            '<option value="false">No</option>' +
            '<option value="true">Yes</option>' +
            '</select><div class="invalid-feedback">Select Yes or No.</div></td>' +
            '<td><input type="date" class="form-control last-working-day-input" name="candidates[' + index + '].lastWorkingDay" disabled>' +
            '<div class="invalid-feedback">Last working day is required when resigned.</div></td>' +
            '<td><select class="form-select" name="candidates[' + index + '].joiningTime" required>' +
            '<option value="">Select</option>' +
            '<option value="Immediate">Immediate</option>' +
            '<option value="15 days">15 Days</option>' +
            '<option value="30 days">30 Days</option>' +
            '<option value="45 days">45 Days</option>' +
            '<option value="60 days">60 Days</option>' +
            "</select></td>" +
            '<td><input type="file" class="form-control resume-file-input" name="candidates[' + index + '].resumeFile" accept=".pdf" required></td>' +
            '<td class="text-center"><button type="button" class="btn btn-outline-danger btn-sm remove-candidate-row">Remove</button></td>';

        return row;
    }

    function clearRowValues(row) {
        row.querySelectorAll("input, select").forEach(function (field) {
            if (field.type === "file") {
                field.value = "";
                field.classList.remove("is-invalid");
                field.setCustomValidity("");
                return;
            }
            field.value = "";
            field.classList.remove("is-invalid");
            field.setCustomValidity("");
        });
    }

    function resequenceRows() {
        var rows = tableBody.querySelectorAll(".candidate-input-row");
        rows.forEach(function (row, index) {
            row.querySelector(".candidate-index").textContent = index + 1;
            row.querySelectorAll("input, select").forEach(function (field) {
                var currentName = field.getAttribute("name");
                if (!currentName) {
                    return;
                }
                field.setAttribute(
                    "name",
                    currentName.replace(/candidates\[\d+\]/, "candidates[" + index + "]")
                );
            });
        });
    }

    addRowButton.addEventListener("click", function () {
        if (!hasOpenVacancy()) {
            alert("Selected designation is already fully filled. You cannot add more candidates.");
            return;
        }
        var index = tableBody.querySelectorAll(".candidate-input-row").length;
        var row = createRow(index);
        tableBody.appendChild(row);
        updateResignationFields(row);
        validateAllNameFields();
        validateAllEmailFields();
        validateAllMobileFields();
    });

    tableBody.addEventListener("click", function (event) {
        if (!event.target.classList.contains("remove-candidate-row")) {
            return;
        }

        var rows = tableBody.querySelectorAll(".candidate-input-row");
        if (rows.length <= 1) {
            clearRowValues(rows[0]);
            updateResignationFields(rows[0]);
            validateAllNameFields();
            validateAllEmailFields();
            validateAllMobileFields();
            return;
        }

        event.target.closest(".candidate-input-row").remove();
        resequenceRows();
        validateAllNameFields();
        validateAllEmailFields();
        validateAllMobileFields();
    });

    tableBody.addEventListener("input", function (event) {
        if (event.target.classList.contains("candidate-name-input")) {
            validateCandidateNameField(event.target);
            return;
        }
        if (event.target.classList.contains("mobile-input")) {
            event.target.value = event.target.value.replace(/[^0-9]/g, "");
            validateAllMobileFields();
            return;
        }
        if (event.target.classList.contains("email-input")) {
            validateAllEmailFields();
        }
    });

    tableBody.addEventListener("focusout", function (event) {
        if (event.target.classList.contains("candidate-name-input")) {
            validateCandidateNameField(event.target);
            return;
        }
        if (event.target.classList.contains("mobile-input")) {
            validateAllMobileFields();
            return;
        }
        if (event.target.classList.contains("email-input")) {
            validateAllEmailFields();
        }
    });

    tableBody.addEventListener("change", function (event) {
        if (event.target.classList.contains("resigned-status-input")) {
            updateResignationFields(event.target.closest(".candidate-input-row"));
        }
    });

    function updateResignationFields(row) {
        if (!row) {
            return;
        }

        var resignedSelect = row.querySelector(".resigned-status-input");
        var lastWorkingDayInput = row.querySelector(".last-working-day-input");
        if (!resignedSelect || !lastWorkingDayInput) {
            return;
        }

        var resigned = resignedSelect.value === "true";
        lastWorkingDayInput.disabled = !resigned;
        lastWorkingDayInput.required = resigned;
        if (!resigned) {
            lastWorkingDayInput.value = "";
            lastWorkingDayInput.setCustomValidity("");
            lastWorkingDayInput.classList.remove("is-invalid");
        }
    }

    function updateAllResignationFields() {
        tableBody.querySelectorAll(".candidate-input-row").forEach(updateResignationFields);
    }

    function normalizeEmail(value) {
        return (value || "").trim().toLowerCase();
    }

    function normalizeMobile(value) {
        return (value || "").replace(/[^0-9]/g, "");
    }

    function loadExistingSubmittedContacts() {
        var emails = new Set();
        var mobiles = new Set();

        document.querySelectorAll(".submitted-candidate-contact").forEach(function (row) {
            var email = normalizeEmail(row.getAttribute("data-candidate-email"));
            var mobile = normalizeMobile(row.getAttribute("data-candidate-mobile"));

            if (email) {
                emails.add(email);
            }
            if (mobile) {
                mobiles.add(mobile);
            }
        });

        return {
            emails: emails,
            mobiles: mobiles
        };
    }

    function buildDuplicateCounts(selector, normalizer) {
        var counts = new Map();

        tableBody.querySelectorAll(selector).forEach(function (field) {
            var value = normalizer(field.value);
            if (!value) {
                return;
            }
            counts.set(value, (counts.get(value) || 0) + 1);
        });

        return counts;
    }

    function setFieldValidity(field, message) {
        if (!field) {
            return true;
        }

        var feedback = field.parentElement ? field.parentElement.querySelector(".invalid-feedback") : null;
        field.setCustomValidity(message || "");
        field.classList.toggle("is-invalid", !!message);

        if (feedback && message) {
            feedback.textContent = message;
        }

        return !message;
    }

    function validateCandidateNameField(field) {
        if (!field) {
            return true;
        }

        var value = field.value;
        if (!value) {
            return setFieldValidity(field, "Candidate name is required.");
        }
        if (value.startsWith(" ")) {
            return setFieldValidity(field, "Candidate name must not start with a space.");
        }
        if (/[0-9]/.test(value)) {
            return setFieldValidity(field, "Candidate name must not contain numbers.");
        }
        if (value.length < 2 || value.length > 100) {
            return setFieldValidity(field, "Candidate name must be between 2 and 100 characters.");
        }
        return setFieldValidity(field, "");
    }

    function validateAllNameFields() {
        var valid = true;
        tableBody.querySelectorAll(".candidate-name-input").forEach(function (field) {
            if (!validateCandidateNameField(field)) {
                valid = false;
            }
        });
        return valid;
    }

    function validateEmailField(field, duplicateCounts) {
        if (!field) {
            return true;
        }

        var value = normalizeEmail(field.value);
        field.value = value;

        if (!value) {
            return setFieldValidity(field, "Candidate email is required.");
        }
        if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
            return setFieldValidity(field, "Please enter a valid email address.");
        }
        if (existingSubmittedContacts.emails.has(value)) {
            return setFieldValidity(field, "Email already exists in submitted candidates.");
        }
        if (duplicateCounts && (duplicateCounts.get(value) || 0) > 1) {
            return setFieldValidity(field, "Email is duplicated in another row.");
        }
        return setFieldValidity(field, "");
    }

    function validateMobileField(field, duplicateCounts) {
        if (!field) {
            return true;
        }

        var value = normalizeMobile(field.value);
        field.value = value;

        if (!value) {
            return setFieldValidity(field, "Candidate mobile is required.");
        }
        if (!/^[0-9]{10}$/.test(value)) {
            return setFieldValidity(field, "Mobile number must be 10 digits.");
        }
        if (existingSubmittedContacts.mobiles.has(value)) {
            return setFieldValidity(field, "Mobile number already exists in submitted candidates.");
        }
        if (duplicateCounts && (duplicateCounts.get(value) || 0) > 1) {
            return setFieldValidity(field, "Mobile number is duplicated in another row.");
        }
        return setFieldValidity(field, "");
    }

    function validateAllEmailFields() {
        var valid = true;
        var duplicateCounts = buildDuplicateCounts(".email-input", normalizeEmail);

        tableBody.querySelectorAll(".email-input").forEach(function (field) {
            if (!validateEmailField(field, duplicateCounts)) {
                valid = false;
            }
        });

        return valid;
    }

    function validateAllMobileFields() {
        var valid = true;
        var duplicateCounts = buildDuplicateCounts(".mobile-input", normalizeMobile);

        tableBody.querySelectorAll(".mobile-input").forEach(function (field) {
            if (!validateMobileField(field, duplicateCounts)) {
                valid = false;
            }
        });

        return valid;
    }

    function validateFileInput(fileInput, rowNumber) {
        if (!fileInput || !fileInput.files || fileInput.files.length === 0) {
            alert("Resume PDF is required in row " + rowNumber + ".");
            return false;
        }

        var file = fileInput.files[0];
        if (!/\.pdf$/i.test(file.name)) {
            alert("Only PDF resumes are allowed in row " + rowNumber + ".");
            return false;
        }
        return true;
    }

    function validateDuplicateValues(selector, label, normalizer) {
        var values = new Set();
        var valid = true;
        tableBody.querySelectorAll(selector).forEach(function (field, index) {
            var value = normalizer ? normalizer(field.value) : (field.value || "").trim().toLowerCase();
            if (!value) {
                return;
            }
            if (values.has(value)) {
                alert(label + " is duplicated in row " + (index + 1) + ".");
                valid = false;
                return;
            }
            values.add(value);
        });
        return valid;
    }

    function validateExperienceRows() {
        var valid = true;

        tableBody.querySelectorAll(".candidate-input-row").forEach(function (row, index) {
            var rowNumber = index + 1;
            var totalExp = parseFloat(row.querySelector(".total-exp-input").value || "0");
            var relevantExp = parseFloat(row.querySelector(".relevant-exp-input").value || "0");

            if (relevantExp > totalExp) {
                alert("Relevant experience cannot be greater than total experience in row " + rowNumber + ".");
                valid = false;
                return;
            }
        });

        return valid;
    }

    function getSelectedOpenCount() {
        var selectedOption = designationSelect.options[designationSelect.selectedIndex];
        if (!selectedOption) {
            return 0;
        }

        var openCount = parseInt(selectedOption.getAttribute("data-open-count") || "0", 10);
        return Number.isNaN(openCount) ? 0 : openCount;
    }

    function hasOpenVacancy() {
        return getSelectedOpenCount() > 0;
    }

    function getSelectedMinimumExperience() {
        var selectedOption = designationSelect.options[designationSelect.selectedIndex];
        if (!selectedOption) {
            return null;
        }

        var minExp = selectedOption.getAttribute("data-min-exp");
        return minExp === null || minExp === "" ? null : parseFloat(minExp);
    }

    function updateVacancyState() {
        if (!designationSelect.value) {
            addRowButton.disabled = false;
            submitButton.disabled = false;
            if (designationHelpText) {
                designationHelpText.textContent = "";
            }
            return;
        }

        var openCount = getSelectedOpenCount();
        var minimumExperience = getSelectedMinimumExperience();
        var isOpen = openCount > 0;

        addRowButton.disabled = !isOpen;
        submitButton.disabled = !isOpen;

        if (!designationHelpText) {
            return;
        }

        if (!isOpen) {
            designationHelpText.textContent = "All vacancies are already filled for this designation and level.";
            return;
        }

        var rangeText = "";
        if (minimumExperience !== null) {
            rangeText = " Minimum total experience: " + minimumExperience
                + " year(s). Candidates with higher experience are allowed.";
        }

        designationHelpText.textContent = "Remaining open vacancies: " + openCount + "." + rangeText;
    }

    function validateExperienceRangeRows() {
        var valid = true;
        var minimumExperience = getSelectedMinimumExperience();

        tableBody.querySelectorAll(".candidate-input-row").forEach(function (row, index) {
            var rowNumber = index + 1;
            var totalExp = parseFloat(row.querySelector(".total-exp-input").value || "0");

            if (minimumExperience !== null && totalExp < minimumExperience) {
                alert("Total experience must be at least " + minimumExperience + " year(s) in row " + rowNumber + ".");
                valid = false;
                return;
            }
        });

        return valid;
    }

    designationSelect.addEventListener("change", updateVacancyState);
    updateAllResignationFields();
    updateVacancyState();

    form.addEventListener("submit", function (event) {
        form.classList.add("was-validated");

        if (!designationSelect.value) {
            alert("Please select designation.");
            event.preventDefault();
            return;
        }

        if (!hasOpenVacancy()) {
            alert("All vacancies are already filled for the selected designation and level.");
            event.preventDefault();
            return;
        }

        var valid = true;
        updateAllResignationFields();

        if (!validateAllNameFields()) {
            valid = false;
        }
        if (!validateAllEmailFields()) {
            valid = false;
        }
        if (!validateAllMobileFields()) {
            valid = false;
        }

        if (!form.checkValidity()) {
            valid = false;
        }

        tableBody.querySelectorAll(".candidate-input-row").forEach(function (row, index) {
            var rowNumber = index + 1;
            var fileInput = row.querySelector(".resume-file-input");
            if (!validateFileInput(fileInput, rowNumber)) {
                valid = false;
            }
        });

        if (!validateExperienceRows()) {
            valid = false;
        }
        if (!validateExperienceRangeRows()) {
            valid = false;
        }

        if (!valid) {
            event.preventDefault();
        }
    });
})();
