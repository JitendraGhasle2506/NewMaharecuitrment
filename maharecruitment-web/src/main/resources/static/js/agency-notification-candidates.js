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

    if (!form || !tableBody || !addRowButton || !designationSelect || !submitButton) {
        return;
    }

    function createRow(index) {
        var row = document.createElement("tr");
        row.className = "candidate-input-row";
        row.innerHTML =
            '<td class="text-center candidate-index">' + (index + 1) + "</td>" +
            '<td><input type="text" class="form-control" name="candidates[' + index + '].candidateName" required></td>' +
            '<td><input type="email" class="form-control" name="candidates[' + index + '].email" required></td>' +
            '<td><input type="text" class="form-control mobile-input" name="candidates[' + index + '].mobile" pattern="[0-9]{10,15}" maxlength="15" required></td>' +
            '<td><input type="text" class="form-control" name="candidates[' + index + '].candidateEducation" required></td>' +
            '<td><input type="number" class="form-control total-exp-input" name="candidates[' + index + '].totalExp" min="0" step="0.1" required></td>' +
            '<td><input type="number" class="form-control relevant-exp-input" name="candidates[' + index + '].relevantExp" min="0" step="0.1" required></td>' +
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
                return;
            }
            field.value = "";
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
        tableBody.appendChild(createRow(index));
    });

    tableBody.addEventListener("click", function (event) {
        if (!event.target.classList.contains("remove-candidate-row")) {
            return;
        }

        var rows = tableBody.querySelectorAll(".candidate-input-row");
        if (rows.length <= 1) {
            clearRowValues(rows[0]);
            return;
        }

        event.target.closest(".candidate-input-row").remove();
        resequenceRows();
    });

    tableBody.addEventListener("input", function (event) {
        if (event.target.classList.contains("mobile-input")) {
            event.target.value = event.target.value.replace(/[^0-9]/g, "");
        }
    });

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

    function validateDuplicateValues(selector, label) {
        var values = new Set();
        var valid = true;
        tableBody.querySelectorAll(selector).forEach(function (field, index) {
            var value = (field.value || "").trim().toLowerCase();
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

    function getSelectedExperienceRange() {
        var selectedOption = designationSelect.options[designationSelect.selectedIndex];
        if (!selectedOption) {
            return { min: null, max: null };
        }

        var minExp = selectedOption.getAttribute("data-min-exp");
        var maxExp = selectedOption.getAttribute("data-max-exp");

        return {
            min: minExp === null || minExp === "" ? null : parseFloat(minExp),
            max: maxExp === null || maxExp === "" ? null : parseFloat(maxExp)
        };
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
        var experienceRange = getSelectedExperienceRange();
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
        if (experienceRange.min !== null || experienceRange.max !== null) {
            var minText = experienceRange.min !== null ? experienceRange.min : "0";
            var maxText = experienceRange.max !== null ? experienceRange.max : "Any";
            rangeText = " Required total experience: " + minText + " to " + maxText + " year(s).";
        }

        designationHelpText.textContent = "Remaining open vacancies: " + openCount + "." + rangeText;
    }

    function validateExperienceRangeRows() {
        var valid = true;
        var experienceRange = getSelectedExperienceRange();

        tableBody.querySelectorAll(".candidate-input-row").forEach(function (row, index) {
            var rowNumber = index + 1;
            var totalExp = parseFloat(row.querySelector(".total-exp-input").value || "0");

            if (experienceRange.min !== null && totalExp < experienceRange.min) {
                alert("Total experience must be at least " + experienceRange.min + " year(s) in row " + rowNumber + ".");
                valid = false;
                return;
            }

            if (experienceRange.max !== null && totalExp > experienceRange.max) {
                alert("Total experience must not exceed " + experienceRange.max + " year(s) in row " + rowNumber + ".");
                valid = false;
                return;
            }
        });

        return valid;
    }

    designationSelect.addEventListener("change", updateVacancyState);
    updateVacancyState();

    form.addEventListener("submit", function (event) {
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

        tableBody.querySelectorAll(".candidate-input-row").forEach(function (row, index) {
            var rowNumber = index + 1;
            var fileInput = row.querySelector(".resume-file-input");
            if (!validateFileInput(fileInput, rowNumber)) {
                valid = false;
            }
        });

        if (!validateDuplicateValues('input[name$=".email"]', "Email")) {
            valid = false;
        }
        if (!validateDuplicateValues('input[name$=".mobile"]', "Mobile")) {
            valid = false;
        }
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
