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
    const csrfTokenInput = form.querySelector('input[name="_csrf"]');
    const verificationPurpose = form.dataset.verificationPurpose;
    const otpBypassEnabled = form.dataset.otpBypassEnabled === "true";

    const primaryMobileInput = document.getElementById("primaryMobile");
    const primaryEmailInput = document.getElementById("primaryEmail");

    const endpoints = {
        subDepartments: form.dataset.subdepartmentsUrl,
        otpSend: form.dataset.otpSendUrl,
        otpVerify: form.dataset.otpVerifyUrl
    };

    const otherOptionValue = "-1";
    const csrfToken = csrfTokenInput ? csrfTokenInput.value : "";

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

    const enableDeclarationAcceptance = () => {
        const reachedBottom = declarationBox.scrollHeight - Math.round(declarationBox.scrollTop)
            <= declarationBox.clientHeight + 2;
        if (reachedBottom) {
            agreeCheckbox.disabled = false;
        }
    };

    const renderSubDepartments = (items, selectedValue) => {
        subDepartmentSelect.innerHTML = '<option value="">Select sub-department</option>';
        items.forEach((item) => {
            const option = document.createElement("option");
            option.value = item.subDeptId;
            option.textContent = item.subDeptName;
            if (selectedValue && String(item.subDeptId) === String(selectedValue)) {
                option.selected = true;
            }
            subDepartmentSelect.appendChild(option);
        });

        const otherOption = document.createElement("option");
        otherOption.value = otherOptionValue;
        otherOption.textContent = "Other / Not listed";
        if (selectedValue === otherOptionValue) {
            otherOption.selected = true;
        }
        subDepartmentSelect.appendChild(otherOption);
    };

    const loadSubDepartments = async (departmentId, preserveSelection) => {
        if (!departmentId || departmentId === otherOptionValue) {
            renderSubDepartments([], "");
            return;
        }

        const selectedValue = preserveSelection ? subDepartmentSelect.value : "";
        try {
            const response = await fetch(`${endpoints.subDepartments}?departmentId=${encodeURIComponent(departmentId)}`, {
                headers: {
                    "X-Requested-With": "XMLHttpRequest"
                }
            });

            if (!response.ok) {
                throw new Error("Unable to load sub-departments.");
            }

            const items = await response.json();
            renderSubDepartments(items, selectedValue);
            updateSubDepartmentState();
        } catch (error) {
            renderSubDepartments([], "");
            alert("Unable to load sub-departments for the selected department.");
        }
    };

    const updateDepartmentState = async (preserveSelection) => {
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
        await loadSubDepartments(departmentValue, preserveSelection);
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

    const mobileVerification = otpBypassEnabled
        ? createBypassVerification(mobileOtpElements.statusElement, "Mobile OTP bypass enabled for testing.")
        : window.createOtpVerification({
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
            initialVerified: form.dataset.mobileVerified === "true",
            initialVerifiedMessage: "Primary mobile number already verified."
        });

    const emailVerification = otpBypassEnabled
        ? createBypassVerification(emailOtpElements.statusElement, "Email OTP bypass enabled for testing.")
        : window.createOtpVerification({
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
            initialVerified: form.dataset.emailVerified === "true",
            initialVerifiedMessage: "Primary email address already verified."
        });

    if (otpBypassEnabled) {
        disableOtpControls(
            mobileOtpElements.sendButton,
            mobileOtpElements.verifyButton,
            mobileOtpElements.otpInput,
            mobileOtpElements.otpSection
        );
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

    departmentSelect.addEventListener("change", () => {
        updateDepartmentState(false);
    });

    subDepartmentSelect.addEventListener("change", updateSubDepartmentState);
    declarationBox.addEventListener("scroll", enableDeclarationAcceptance);
    agreeCheckbox.addEventListener("change", toggleSubmitState);
    mobileVerification.onChange(toggleSubmitState);
    emailVerification.onChange(toggleSubmitState);

    enableDeclarationAcceptance();
    toggleSubmitState();
    updateDepartmentState(true);
});
