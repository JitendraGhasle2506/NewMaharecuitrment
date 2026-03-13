(function () {
    const form = document.getElementById("preOnboardingForm");
    if (!form) {
        return;
    }

    const employmentContainer = document.getElementById("employmentContainer");
    const employmentTemplate = document.getElementById("employmentRowTemplate");
    const addEmploymentBtn = document.getElementById("addEmploymentBtn");
    const totalYearsInput = document.getElementById("totalExperienceYears");
    const totalMonthsInput = document.getElementById("totalExperienceMonths");
    const totalExperienceDisplay = document.getElementById("totalExperienceDisplay");
    const employmentValidationMessage = document.getElementById("employmentValidationMessage");
    const submitBtn = document.getElementById("submitBtn");
    const requiredCheckboxSelector = "[data-required-doc='true'], #agencyFlag";
    const mobileInput = form.querySelector("[name='mobile']");
    const joiningDateInput = form.querySelector("[name='joiningDate']");
    const onboardingDateInput = form.querySelector("[name='onboardingDate']");

    function attachRowEvents(row) {
        row.querySelectorAll(".experience-date").forEach(function (input) {
            input.addEventListener("change", calculateTotalExperience);
        });

        row.querySelectorAll("input").forEach(function (input) {
            input.addEventListener("input", function () {
                clearEmploymentFieldValidity(row);
                validateEmploymentRows();
            });
        });

        const removeButton = row.querySelector("[data-remove-employment]");
        if (removeButton) {
            removeButton.addEventListener("click", function () {
                row.remove();
                if (!employmentContainer.querySelector(".employment-row")) {
                    addEmploymentRow();
                }
                reindexEmploymentRows();
                calculateTotalExperience();
            });
        }
    }

    function reindexEmploymentRows() {
        const rows = employmentContainer.querySelectorAll(".employment-row");
        rows.forEach(function (row, index) {
            const rowNumber = index + 1;
            const rowTitle = row.querySelector(".employment-row-title");
            if (rowTitle) {
                rowTitle.textContent = "Employment #" + rowNumber;
            }

            row.querySelectorAll("input, textarea, select").forEach(function (field) {
                ["name", "id", "for"].forEach(function (attr) {
                    const value = field.getAttribute(attr);
                    if (!value) {
                        return;
                    }
                    field.setAttribute(attr, value.replace(/previousEmployments\[\d+\]/g, "previousEmployments[" + index + "]"));
                });
            });
        });
    }

    function addEmploymentRow() {
        if (!employmentTemplate) {
            return;
        }

        const index = employmentContainer.querySelectorAll(".employment-row").length;
        const html = employmentTemplate.innerHTML
            .replaceAll("__index__", String(index))
            .replaceAll("__rowNumber__", String(index + 1));

        const wrapper = document.createElement("div");
        wrapper.innerHTML = html.trim();
        const newRow = wrapper.firstElementChild;
        employmentContainer.appendChild(newRow);
        attachRowEvents(newRow);
    }

    function calculateMonthsFromDates(startDate, endDate) {
        if (!startDate || !endDate) {
            return 0;
        }

        if (Number.isNaN(startDate.getTime()) || Number.isNaN(endDate.getTime()) || endDate < startDate) {
            return 0;
        }

        let months = (endDate.getFullYear() - startDate.getFullYear()) * 12;
        months += endDate.getMonth() - startDate.getMonth();

        if (endDate.getDate() > startDate.getDate() || months === 0) {
            months += 1;
        }

        return Math.max(months, 0);
    }

    function setFieldValidity(field, message) {
        if (!field) {
            return;
        }
        field.setCustomValidity(message || "");
    }

    function clearEmploymentFieldValidity(row) {
        if (!row) {
            return;
        }
        row.querySelectorAll("input").forEach(function (field) {
            setFieldValidity(field, "");
        });
    }

    function showEmploymentValidation(message) {
        if (!employmentValidationMessage) {
            return;
        }
        employmentValidationMessage.textContent = message || "";
        employmentValidationMessage.classList.toggle("d-none", !message);
    }

    function collectEmploymentRows() {
        return Array.from(employmentContainer.querySelectorAll(".employment-row")).map(function (row, index) {
            const inputs = row.querySelectorAll("input");
            const companyInput = row.querySelector("input[name*='.companyName']");
            const startInput = row.querySelector("input[name*='.startDate']");
            const endInput = row.querySelector("input[name*='.endDate']");
            const designationInput = row.querySelector("input[name*='.designation']");
            const companyValue = companyInput ? companyInput.value.trim() : "";
            const designationValue = designationInput ? designationInput.value.trim() : "";
            const startValue = startInput ? startInput.value : "";
            const endValue = endInput ? endInput.value : "";
            const hasAnyValue = Boolean(companyValue || designationValue || startValue || endValue);

            return {
                row: row,
                rowNumber: index + 1,
                inputs: inputs,
                companyInput: companyInput,
                startInput: startInput,
                endInput: endInput,
                companyValue: companyValue,
                startValue: startValue,
                endValue: endValue,
                hasAnyValue: hasAnyValue
            };
        });
    }

    function calculateMonths(startValue, endValue) {
        if (!startValue || !endValue) {
            return 0;
        }

        return calculateMonthsFromDates(
            new Date(startValue + "T00:00:00"),
            new Date(endValue + "T00:00:00")
        );
    }

    function collectExperienceRanges() {
        const ranges = [];

        employmentContainer.querySelectorAll(".employment-row").forEach(function (row, index) {
            const dates = row.querySelectorAll(".experience-date");
            if (dates.length < 2 || !dates[0].value || !dates[1].value) {
                return;
            }

            const startDate = new Date(dates[0].value + "T00:00:00");
            const endDate = new Date(dates[1].value + "T00:00:00");
            if (Number.isNaN(startDate.getTime()) || Number.isNaN(endDate.getTime()) || endDate < startDate) {
                return;
            }

            ranges.push({
                rowNumber: index + 1,
                startValue: dates[0].value,
                endValue: dates[1].value,
                startDate: startDate,
                endDate: endDate
            });
        });

        return ranges;
    }

    function detectOverlap(ranges) {
        const sortedRanges = ranges.slice().sort(function (left, right) {
            if (left.startValue === right.startValue) {
                return left.endValue.localeCompare(right.endValue);
            }
            return left.startValue.localeCompare(right.startValue);
        });

        for (let index = 1; index < sortedRanges.length; index += 1) {
            const previous = sortedRanges[index - 1];
            const current = sortedRanges[index];

            if (current.startDate <= previous.endDate) {
                return {
                    message: "Employment #" + current.rowNumber + " overlaps with Employment #" + previous.rowNumber + ".",
                    previous: previous,
                    current: current
                };
            }
        }

        return null;
    }

    function validateEmploymentRows() {
        const rows = collectEmploymentRows();
        let firstInvalidField = null;

        rows.forEach(function (entry) {
            clearEmploymentFieldValidity(entry.row);
        });

        const filledRows = rows.filter(function (entry) {
            return entry.hasAnyValue;
        });

        if (filledRows.length === 0) {
            const firstCompanyInput = rows[0] && rows[0].companyInput ? rows[0].companyInput : null;
            if (firstCompanyInput) {
                setFieldValidity(firstCompanyInput, "At least one previous employment entry is required.");
                firstInvalidField = firstCompanyInput;
            }
            showEmploymentValidation("At least one previous employment entry is required.");
            return { valid: false, invalidField: firstInvalidField };
        }

        for (let index = 0; index < filledRows.length; index += 1) {
            const entry = filledRows[index];

            if (!entry.companyValue) {
                setFieldValidity(entry.companyInput, "Company name is required for Employment #" + entry.rowNumber + ".");
                showEmploymentValidation("Company name is required for Employment #" + entry.rowNumber + ".");
                return { valid: false, invalidField: entry.companyInput };
            }
            if (!entry.startValue) {
                setFieldValidity(entry.startInput, "Start date is required for Employment #" + entry.rowNumber + ".");
                showEmploymentValidation("Start date is required for Employment #" + entry.rowNumber + ".");
                return { valid: false, invalidField: entry.startInput };
            }
            if (!entry.endValue) {
                setFieldValidity(entry.endInput, "End date is required for Employment #" + entry.rowNumber + ".");
                showEmploymentValidation("End date is required for Employment #" + entry.rowNumber + ".");
                return { valid: false, invalidField: entry.endInput };
            }
            if (entry.endValue < entry.startValue) {
                setFieldValidity(entry.endInput, "End date cannot be before start date for Employment #" + entry.rowNumber + ".");
                showEmploymentValidation("End date cannot be before start date for Employment #" + entry.rowNumber + ".");
                return { valid: false, invalidField: entry.endInput };
            }
        }

        const ranges = filledRows.map(function (entry) {
            return {
                rowNumber: entry.rowNumber,
                startValue: entry.startValue,
                endValue: entry.endValue,
                startDate: new Date(entry.startValue + "T00:00:00"),
                endDate: new Date(entry.endValue + "T00:00:00"),
                startInput: entry.startInput,
                endInput: entry.endInput
            };
        });

        const overlap = detectOverlap(ranges);
        if (overlap) {
            setFieldValidity(overlap.current.startInput, overlap.message);
            setFieldValidity(overlap.current.endInput, overlap.message);
            showEmploymentValidation(overlap.message);
            return { valid: false, invalidField: overlap.current.startInput };
        }

        showEmploymentValidation("");
        return { valid: true, invalidField: null };
    }

    function calculateTotalExperience() {
        const ranges = collectExperienceRanges();
        const mergedRanges = ranges.slice().sort(function (left, right) {
            if (left.startValue === right.startValue) {
                return left.endValue.localeCompare(right.endValue);
            }
            return left.startValue.localeCompare(right.startValue);
        }).reduce(function (accumulator, current) {
            const last = accumulator[accumulator.length - 1];
            if (!last) {
                accumulator.push({
                    startDate: new Date(current.startDate.getTime()),
                    endDate: new Date(current.endDate.getTime())
                });
                return accumulator;
            }

            if (current.startDate <= last.endDate) {
                if (current.endDate > last.endDate) {
                    last.endDate = new Date(current.endDate.getTime());
                }
                return accumulator;
            }

            accumulator.push({
                startDate: new Date(current.startDate.getTime()),
                endDate: new Date(current.endDate.getTime())
            });
            return accumulator;
        }, []);

        const totalMonths = mergedRanges.reduce(function (sum, range) {
            return sum + calculateMonthsFromDates(range.startDate, range.endDate);
        }, 0);

        const years = Math.floor(totalMonths / 12);
        const months = totalMonths % 12;

        if (totalYearsInput) {
            totalYearsInput.value = years;
        }
        if (totalMonthsInput) {
            totalMonthsInput.value = months;
        }
        if (totalExperienceDisplay) {
            totalExperienceDisplay.textContent = years + " year(s) " + months + " month(s)";
        }

        validateEmploymentRows();
        checkFormValidity();
    }

    function validateJoiningAndOnboardingDates() {
        if (!joiningDateInput || !onboardingDateInput) {
            return true;
        }

        setFieldValidity(onboardingDateInput, "");
        if (!joiningDateInput.value || !onboardingDateInput.value) {
            return true;
        }

        if (onboardingDateInput.value < joiningDateInput.value) {
            setFieldValidity(onboardingDateInput, "Onboarding date cannot be before joining date.");
            return false;
        }

        return true;
    }

    function validateMobile() {
        if (!mobileInput) {
            return true;
        }

        const mobileValue = mobileInput.value.trim();
        setFieldValidity(mobileInput, "");
        if (!mobileValue) {
            return true;
        }

        if (!/^[0-9]{10,15}$/.test(mobileValue)) {
            setFieldValidity(mobileInput, "Mobile number must be 10 to 15 digits.");
            return false;
        }

        return true;
    }

    function checkFormValidity() {
        const requiredChecks = Array.from(form.querySelectorAll(requiredCheckboxSelector));
        const requiredDocsComplete = requiredChecks.every(function (checkbox) {
            return checkbox.checked;
        });
        const employmentValid = validateEmploymentRows().valid;
        const datesValid = validateJoiningAndOnboardingDates();
        const mobileValid = validateMobile();
        submitBtn.disabled = !requiredDocsComplete || !employmentValid || !datesValid || !mobileValid;
    }

    function openManagedDocument(path) {
        if (!path) {
            return;
        }
        try {
            const contextPath = window.preOnboardingConfig && window.preOnboardingConfig.contextPath
                ? window.preOnboardingConfig.contextPath
                : "/";
            const encodedPath = encodeURIComponent(btoa(path));
            window.open(contextPath + "documents/view?path=" + encodedPath, "_blank");
        } catch (error) {
            window.alert("Unable to open the uploaded document.");
        }
    }

    employmentContainer.querySelectorAll(".employment-row").forEach(attachRowEvents);
    if (addEmploymentBtn) {
        addEmploymentBtn.addEventListener("click", function () {
            addEmploymentRow();
            reindexEmploymentRows();
        });
    }

    form.querySelectorAll(requiredCheckboxSelector).forEach(function (checkbox) {
        checkbox.addEventListener("change", checkFormValidity);
    });

    if (joiningDateInput) {
        joiningDateInput.addEventListener("change", checkFormValidity);
    }
    if (onboardingDateInput) {
        onboardingDateInput.addEventListener("change", checkFormValidity);
    }
    if (mobileInput) {
        mobileInput.addEventListener("input", checkFormValidity);
    }

    form.addEventListener("submit", function (event) {
        const employmentValidation = validateEmploymentRows();
        const datesValid = validateJoiningAndOnboardingDates();
        const mobileValid = validateMobile();

        if (!employmentValidation.valid) {
            event.preventDefault();
            if (employmentValidation.invalidField) {
                employmentValidation.invalidField.reportValidity();
                employmentValidation.invalidField.focus();
            }
            return;
        }

        if (!datesValid) {
            event.preventDefault();
            onboardingDateInput.reportValidity();
            onboardingDateInput.focus();
            return;
        }

        if (!mobileValid) {
            event.preventDefault();
            mobileInput.reportValidity();
            mobileInput.focus();
            return;
        }

        if (!form.reportValidity()) {
            event.preventDefault();
        }
    });

    calculateTotalExperience();
    checkFormValidity();
    window.openManagedDocument = openManagedDocument;
})();
