document.addEventListener("DOMContentLoaded", () => {
    const form = document.getElementById("departmentRegistrationForm");
    if (!form) {
        return;
    }

    const departmentSelect = document.getElementById("departmentId");
    const subDepartmentSelect = document.getElementById("subDeptId");
    const newDepartmentInput = document.getElementById("newDepartmentName");
    const newSubDepartmentInput = document.getElementById("newSubDeptName");
    const declarationBox = document.getElementById("declarationBox");
    const agreeCheckbox = document.getElementById("agreeCheckbox");
    const registerButton = document.getElementById("registerBtn");
    const documentInputs = ["gstFile", "panFile", "tanFile"]
        .map((fieldId) => document.getElementById(fieldId))
        .filter((input) => input);
    const csrfTokenInput = form.querySelector('input[name="_csrf"]');
    const verificationPurpose = form.dataset.verificationPurpose;
    const otpBypassEnabled = form.dataset.otpBypassEnabled === "true";
    const mobileOtpEnabled = form.dataset.mobileOtpEnabled === "true";
    const emailOtpEnabled = form.dataset.emailOtpEnabled === "true";
    const configuredOtpCooldown = Number.parseInt(form.dataset.otpResendCooldownSeconds || "120", 10);
    const otpResendCooldownSeconds = Number.isFinite(configuredOtpCooldown) && configuredOtpCooldown > 0
        ? configuredOtpCooldown
        : 120;

    const primaryMobileInput = document.getElementById("primaryMobile");
    const primaryEmailInput = document.getElementById("primaryEmail");

    const endpoints = {
        subDepartments: form.dataset.subdepartmentsUrl,
        otpSend: form.dataset.otpSendUrl,
        otpVerify: form.dataset.otpVerifyUrl
    };

    const otherOptionValue = "-1";
    const csrfToken = csrfTokenInput ? csrfTokenInput.value : "";
    const subDepartmentCache = new Map();
    let activeSubDepartmentRequest = null;

    const initializeEnhancedSelects = () => {
        if (!window.jQuery || !window.jQuery.fn || !window.jQuery.fn.select2) {
            return;
        }
        [departmentSelect, subDepartmentSelect].forEach((select) => {
            const enhancedSelect = window.jQuery(select);
            if (!enhancedSelect.data("select2")) {
                enhancedSelect.select2({
                    theme: "bootstrap-5",
                    placeholder: select === departmentSelect
                        ? "Select department"
                        : "Select sub-department",
                    width: "100%"
                });
            }
        });
    };

    const bindDynamicSelectEvents = () => {
        const handleDepartmentChange = () => {
            void updateDepartmentState(false, true);
        };

        if (window.jQuery) {
            window.jQuery(departmentSelect)
                .off("change.departmentRegistration")
                .on("change.departmentRegistration", handleDepartmentChange);
            window.jQuery(subDepartmentSelect)
                .off("change.departmentRegistration")
                .on("change.departmentRegistration", updateSubDepartmentState);
            return;
        }

        departmentSelect.addEventListener("change", handleDepartmentChange);
        subDepartmentSelect.addEventListener("change", updateSubDepartmentState);
    };

    const refreshSubDepartmentSelect = () => {
        if (window.jQuery && window.jQuery.fn && window.jQuery.fn.select2) {
            window.jQuery(subDepartmentSelect).trigger("change.select2");
        }
    };

    const createBypassVerification = (statusElement, message) => {
        if (statusElement) {
            statusElement.textContent = message;
            statusElement.classList.remove("is-error", "is-pending");
            statusElement.classList.add("is-success");
        }
        return {
            isVerified: () => true,
            onChange: () => {}
        };
    };

    const createUnavailableVerification = (statusElement, message) => {
        if (statusElement) {
            statusElement.textContent = message;
            statusElement.classList.remove("is-pending", "is-success");
            statusElement.classList.add("is-error");
        }
        return {
            isVerified: () => false,
            onChange: () => {}
        };
    };

    const disableOtpControls = (sendButton, verifyButton, otpInput, otpSection) => {
        if (sendButton) {
            sendButton.disabled = true;
        }
        if (verifyButton) {
            verifyButton.disabled = true;
        }
        if (otpInput) {
            otpInput.disabled = true;
            otpInput.value = "";
        }
        if (otpSection) {
            otpSection.style.display = "none";
        }
    };

    const toggleField = (element, visible) => {
        const wrapper = element.closest(".col-md-6") || element.closest(".col-12") || element.parentElement;
        if (wrapper) {
            wrapper.style.display = visible ? "" : "none";
        }
        if (!visible) {
            element.value = "";
        }
    };

    const isPdfFile = (file) => {
        if (!file) {
            return true;
        }

        const fileName = file.name || "";
        const contentType = file.type || "";
        return /\.pdf$/i.test(fileName) && contentType.toLowerCase() === "application/pdf";
    };

    const validatePdfSelection = (input) => {
        const [file] = input.files || [];
        if (!isPdfFile(file)) {
            input.value = "";
            showSelectedFile(input);
            alert("Only PDF files are allowed for GST, PAN, and TAN documents.");
            return false;
        }
        showSelectedFile(input);
        return true;
    };

    const showSelectedFile = (input) => {
        const status = form.querySelector(`[data-selected-file-name="${input.id}"]`);
        if (!status) {
            return;
        }
        const [file] = input.files || [];
        status.textContent = file ? `Selected: ${file.name}` : "";
    };

    const enableDeclarationAcceptance = () => {
        if (!agreeCheckbox.disabled) {
            declarationBox.removeEventListener("scroll", enableDeclarationAcceptance);
            return;
        }
        const reachedBottom = declarationBox.scrollHeight - Math.round(declarationBox.scrollTop)
            <= declarationBox.clientHeight + 2;
        if (reachedBottom) {
            agreeCheckbox.disabled = false;
            declarationBox.removeEventListener("scroll", enableDeclarationAcceptance);
        }
    };

    const renderSubDepartments = (items, selectedValue) => {
        const options = document.createDocumentFragment();
        const defaultOption = document.createElement("option");
        defaultOption.value = "";
        defaultOption.textContent = "Select sub-department";
        options.appendChild(defaultOption);

        items.forEach((item) => {
            const option = document.createElement("option");
            option.value = item.subDeptId;
            option.textContent = item.subDeptName;
            if (selectedValue && String(item.subDeptId) === String(selectedValue)) {
                option.selected = true;
            }
            options.appendChild(option);
        });

        const otherOption = document.createElement("option");
        otherOption.value = otherOptionValue;
        otherOption.textContent = "Other / Not listed";
        if (selectedValue === otherOptionValue) {
            otherOption.selected = true;
        }
        options.appendChild(otherOption);
        subDepartmentSelect.replaceChildren(options);
        refreshSubDepartmentSelect();
    };

    const cacheInitialSubDepartments = () => {
        const departmentId = departmentSelect.value;
        if (!departmentId || departmentId === otherOptionValue) {
            return;
        }
        const initialItems = Array.from(subDepartmentSelect.options)
            .filter((option) => option.value && option.value !== otherOptionValue)
            .map((option) => ({
                subDeptId: option.value,
                subDeptName: option.textContent
            }));
        subDepartmentCache.set(departmentId, initialItems);
    };

    const loadSubDepartments = async (departmentId, preserveSelection) => {
        if (!departmentId || departmentId === otherOptionValue) {
            if (activeSubDepartmentRequest) {
                activeSubDepartmentRequest.abort();
                activeSubDepartmentRequest = null;
            }
            renderSubDepartments([], "");
            return;
        }

        const selectedValue = preserveSelection ? subDepartmentSelect.value : "";
        if (subDepartmentCache.has(departmentId)) {
            renderSubDepartments(subDepartmentCache.get(departmentId), selectedValue);
            updateSubDepartmentState();
            return;
        }

        if (activeSubDepartmentRequest) {
            activeSubDepartmentRequest.abort();
        }
        const requestController = new AbortController();
        activeSubDepartmentRequest = requestController;
        try {
            const response = await fetch(`${endpoints.subDepartments}?departmentId=${encodeURIComponent(departmentId)}`, {
                signal: requestController.signal,
                headers: {
                    "Accept": "application/json",
                    "X-Requested-With": "XMLHttpRequest"
                }
            });

            if (!response.ok) {
                throw new Error("Unable to load sub-departments.");
            }

            const items = await response.json();
            if (requestController !== activeSubDepartmentRequest) {
                return;
            }
            subDepartmentCache.set(departmentId, items);
            renderSubDepartments(items, selectedValue);
            updateSubDepartmentState();
        } catch (error) {
            if (error && error.name === "AbortError") {
                return;
            }
            renderSubDepartments([], "");
            alert("Unable to load sub-departments for the selected department.");
        } finally {
            if (requestController === activeSubDepartmentRequest) {
                activeSubDepartmentRequest = null;
            }
        }
    };

    const updateDepartmentState = async (preserveSelection, loadOptions = true) => {
        const departmentValue = departmentSelect.value;
        const otherDepartment = departmentValue === otherOptionValue;

        toggleField(newDepartmentInput, otherDepartment);
        subDepartmentSelect.disabled = otherDepartment;

        if (otherDepartment) {
            renderSubDepartments([], "");
            subDepartmentSelect.value = "";
            toggleField(newSubDepartmentInput, true);
            return;
        }

        toggleField(newSubDepartmentInput, subDepartmentSelect.value === otherOptionValue);
        if (loadOptions) {
            await loadSubDepartments(departmentValue, preserveSelection);
        }
    };

    const updateSubDepartmentState = () => {
        toggleField(newSubDepartmentInput, subDepartmentSelect.value === otherOptionValue || departmentSelect.value === otherOptionValue);
    };

    const mobileOtpElements = {
        sendButton: document.getElementById("sendMobileOtpBtn"),
        verifyButton: document.getElementById("verifyMobileOtpBtn"),
        otpInput: document.getElementById("mobileOtpInput"),
        otpSection: document.getElementById("mobileOtpSection"),
        statusElement: document.getElementById("mobileVerificationStatus")
    };

    const emailOtpElements = {
        sendButton: document.getElementById("sendEmailOtpBtn"),
        verifyButton: document.getElementById("verifyEmailOtpBtn"),
        otpInput: document.getElementById("emailOtpInput"),
        otpSection: document.getElementById("emailOtpSection"),
        statusElement: document.getElementById("emailVerificationStatus")
    };

    const initializeOtpVerification = (config, statusElement, channelLabel) => {
        if (typeof window.createOtpVerification !== "function") {
            return createUnavailableVerification(
                statusElement,
                `${channelLabel} OTP is temporarily unavailable. Please refresh the page and try again.`
            );
        }

        try {
            return window.createOtpVerification(config);
        } catch (error) {
            if (window.console && typeof window.console.error === "function") {
                window.console.error(`${channelLabel} OTP setup failed.`, error);
            }
            return createUnavailableVerification(
                statusElement,
                `${channelLabel} OTP is temporarily unavailable. Please refresh the page and try again.`
            );
        }
    };

    const mobileVerification = otpBypassEnabled
        ? createBypassVerification(mobileOtpElements.statusElement, "Mobile OTP bypass enabled for testing.")
        : !mobileOtpEnabled
        ? createBypassVerification(mobileOtpElements.statusElement, "Mobile OTP is disabled in this environment.")
        : initializeOtpVerification({
            purpose: verificationPurpose,
            channel: "MOBILE",
            referenceInput: primaryMobileInput,
            sendButton: mobileOtpElements.sendButton,
            verifyButton: mobileOtpElements.verifyButton,
            otpInput: mobileOtpElements.otpInput,
            otpSection: mobileOtpElements.otpSection,
            statusElement: mobileOtpElements.statusElement,
            sendUrl: endpoints.otpSend,
            verifyUrl: endpoints.otpVerify,
            csrfToken,
            resendCooldownSeconds: otpResendCooldownSeconds,
            initialVerified: form.dataset.mobileVerified === "true",
            initialVerifiedMessage: "Primary mobile number already verified."
        }, mobileOtpElements.statusElement, "Mobile");

    const emailVerification = otpBypassEnabled
        ? createBypassVerification(emailOtpElements.statusElement, "Email OTP bypass enabled for testing.")
        : !emailOtpEnabled
        ? createBypassVerification(emailOtpElements.statusElement, "Email OTP is disabled in this environment.")
        : initializeOtpVerification({
            purpose: verificationPurpose,
            channel: "EMAIL",
            referenceInput: primaryEmailInput,
            sendButton: emailOtpElements.sendButton,
            verifyButton: emailOtpElements.verifyButton,
            otpInput: emailOtpElements.otpInput,
            otpSection: emailOtpElements.otpSection,
            statusElement: emailOtpElements.statusElement,
            sendUrl: endpoints.otpSend,
            verifyUrl: endpoints.otpVerify,
            csrfToken,
            resendCooldownSeconds: otpResendCooldownSeconds,
            initialVerified: form.dataset.emailVerified === "true",
            initialVerifiedMessage: "Primary email address already verified."
        }, emailOtpElements.statusElement, "Email");

    if (otpBypassEnabled || !mobileOtpEnabled) {
        disableOtpControls(
            mobileOtpElements.sendButton,
            mobileOtpElements.verifyButton,
            mobileOtpElements.otpInput,
            mobileOtpElements.otpSection
        );
    }

    if (otpBypassEnabled || !emailOtpEnabled) {
        disableOtpControls(
            emailOtpElements.sendButton,
            emailOtpElements.verifyButton,
            emailOtpElements.otpInput,
            emailOtpElements.otpSection
        );
    }

    const toggleSubmitState = () => {
        registerButton.disabled = !(agreeCheckbox.checked
            && mobileVerification.isVerified()
            && emailVerification.isVerified());
    };

    ["gstNo", "panNo", "tanNo"].forEach((fieldId) => {
        const input = document.getElementById(fieldId);
        if (input) {
            input.addEventListener("input", () => {
                input.value = input.value.toUpperCase();
            });
        }
    });

    documentInputs.forEach((input) => {
        input.addEventListener("change", () => {
            validatePdfSelection(input);
        });
    });

    declarationBox.addEventListener("scroll", enableDeclarationAcceptance, { passive: true });
    agreeCheckbox.addEventListener("change", toggleSubmitState);
    form.addEventListener("submit", (event) => {
        const invalidInput = documentInputs.find((input) => !validatePdfSelection(input));
        if (invalidInput) {
            event.preventDefault();
            invalidInput.focus();
        }
    });
    mobileVerification.onChange(toggleSubmitState);
    emailVerification.onChange(toggleSubmitState);

    const secondaryMobileInput = document.getElementById("secondaryMobile");
    const secondaryMobileError = document.getElementById("secondaryMobileError");

    if (secondaryMobileInput && secondaryMobileError) {
        secondaryMobileInput.addEventListener("input", () => {
            const val = secondaryMobileInput.value.replace(/[^0-9]/g, '');
            secondaryMobileInput.value = val;
            
            if (val.length > 0 && val.length < 10) {
                secondaryMobileError.textContent = "Secondary mobile number must be 10 digits";
            } else {
                secondaryMobileError.textContent = "";
            }
        });
    }

    cacheInitialSubDepartments();
    initializeEnhancedSelects();
    bindDynamicSelectEvents();
    enableDeclarationAcceptance();
    toggleSubmitState();
    updateDepartmentState(true, false);
});
