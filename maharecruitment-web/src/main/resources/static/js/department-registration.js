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

    const primaryMobileInput = document.getElementById("primaryMobile");
    const primaryEmailInput = document.getElementById("primaryEmail");

    const endpoints = {
        subDepartments: form.dataset.subdepartmentsUrl,
        otpSend: form.dataset.otpSendUrl,
        otpVerify: form.dataset.otpVerifyUrl
    };

    const otherOptionValue = "-1";
    const csrfToken = csrfTokenInput ? csrfTokenInput.value : "";

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

    const mobileVerification = window.createOtpVerification({
        purpose: verificationPurpose,
        channel: "MOBILE",
        referenceInput: primaryMobileInput,
        sendButton: document.getElementById("sendMobileOtpBtn"),
        verifyButton: document.getElementById("verifyMobileOtpBtn"),
        otpInput: document.getElementById("mobileOtpInput"),
        otpSection: document.getElementById("mobileOtpSection"),
        statusElement: document.getElementById("mobileVerificationStatus"),
        sendUrl: endpoints.otpSend,
        verifyUrl: endpoints.otpVerify,
        csrfToken,
        initialVerified: form.dataset.mobileVerified === "true",
        initialVerifiedMessage: "Primary mobile number already verified."
    });

    const emailVerification = window.createOtpVerification({
        purpose: verificationPurpose,
        channel: "EMAIL",
        referenceInput: primaryEmailInput,
        sendButton: document.getElementById("sendEmailOtpBtn"),
        verifyButton: document.getElementById("verifyEmailOtpBtn"),
        otpInput: document.getElementById("emailOtpInput"),
        otpSection: document.getElementById("emailOtpSection"),
        statusElement: document.getElementById("emailVerificationStatus"),
        sendUrl: endpoints.otpSend,
        verifyUrl: endpoints.otpVerify,
        csrfToken,
        initialVerified: form.dataset.emailVerified === "true",
        initialVerifiedMessage: "Primary email address already verified."
    });

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
