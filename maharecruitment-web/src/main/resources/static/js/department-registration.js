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

    const primaryMobileInput = document.getElementById("primaryMobile");
    const primaryEmailInput = document.getElementById("primaryEmail");
    const sendMobileOtpButton = document.getElementById("sendMobileOtpBtn");
    const verifyMobileOtpButton = document.getElementById("verifyMobileOtpBtn");
    const mobileOtpInput = document.getElementById("mobileOtpInput");
    const mobileOtpSection = document.getElementById("mobileOtpSection");
    const mobileVerificationStatus = document.getElementById("mobileVerificationStatus");

    const sendEmailOtpButton = document.getElementById("sendEmailOtpBtn");
    const verifyEmailOtpButton = document.getElementById("verifyEmailOtpBtn");
    const emailOtpInput = document.getElementById("emailOtpInput");
    const emailOtpSection = document.getElementById("emailOtpSection");
    const emailVerificationStatus = document.getElementById("emailVerificationStatus");

    const endpoints = {
        subDepartments: form.dataset.subdepartmentsUrl,
        mobileSend: form.dataset.mobileSendUrl,
        mobileVerify: form.dataset.mobileVerifyUrl,
        emailSend: form.dataset.emailSendUrl,
        emailVerify: form.dataset.emailVerifyUrl
    };

    const state = {
        mobileVerified: form.dataset.mobileVerified === "true",
        emailVerified: form.dataset.emailVerified === "true"
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

    const setStatus = (element, message, mode) => {
        element.textContent = message || "";
        element.classList.remove("is-pending", "is-error");
        if (mode) {
            element.classList.add(mode);
        }
    };

    const toggleSubmitState = () => {
        registerButton.disabled = !(agreeCheckbox.checked && state.mobileVerified && state.emailVerified);
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

    const apiPost = async (url, payload) => {
        const response = await fetch(url, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "X-CSRF-TOKEN": csrfToken
            },
            body: JSON.stringify(payload)
        });

        const data = await response.json().catch(() => ({ message: "Unexpected response received.", verified: false }));
        if (!response.ok) {
            throw new Error(data.message || "Request failed.");
        }
        return data;
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

    const resetMobileVerification = () => {
        state.mobileVerified = false;
        mobileOtpInput.value = "";
        mobileOtpSection.style.display = "none";
        setStatus(mobileVerificationStatus, "", null);
        toggleSubmitState();
    };

    const resetEmailVerification = () => {
        state.emailVerified = false;
        emailOtpInput.value = "";
        emailOtpSection.style.display = "none";
        setStatus(emailVerificationStatus, "", null);
        toggleSubmitState();
    };

    const sendMobileOtp = async () => {
        if (!/^[0-9]{10}$/.test(primaryMobileInput.value.trim())) {
            setStatus(mobileVerificationStatus, "Enter a valid 10 digit mobile number before requesting OTP.", "is-error");
            return;
        }
        try {
            const data = await apiPost(endpoints.mobileSend, { reference: primaryMobileInput.value });
            mobileOtpSection.style.display = "flex";
            setStatus(mobileVerificationStatus, data.message, "is-pending");
        } catch (error) {
            setStatus(mobileVerificationStatus, error.message, "is-error");
        }
    };

    const verifyMobileOtp = async () => {
        if (!/^[0-9]{6}$/.test(mobileOtpInput.value.trim())) {
            setStatus(mobileVerificationStatus, "Enter the 6 digit OTP sent to the mobile number.", "is-error");
            return;
        }
        try {
            const data = await apiPost(endpoints.mobileVerify, {
                reference: primaryMobileInput.value,
                otp: mobileOtpInput.value
            });
            state.mobileVerified = data.verified === true;
            setStatus(mobileVerificationStatus, data.message, null);
            mobileOtpSection.style.display = "none";
            toggleSubmitState();
        } catch (error) {
            state.mobileVerified = false;
            setStatus(mobileVerificationStatus, error.message, "is-error");
            toggleSubmitState();
        }
    };

    const sendEmailOtp = async () => {
        if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(primaryEmailInput.value.trim())) {
            setStatus(emailVerificationStatus, "Enter a valid email address before requesting OTP.", "is-error");
            return;
        }
        try {
            const data = await apiPost(endpoints.emailSend, { reference: primaryEmailInput.value });
            emailOtpSection.style.display = "flex";
            setStatus(emailVerificationStatus, data.message, "is-pending");
        } catch (error) {
            setStatus(emailVerificationStatus, error.message, "is-error");
        }
    };

    const verifyEmailOtp = async () => {
        if (!/^[0-9]{6}$/.test(emailOtpInput.value.trim())) {
            setStatus(emailVerificationStatus, "Enter the 6 digit OTP sent to the email address.", "is-error");
            return;
        }
        try {
            const data = await apiPost(endpoints.emailVerify, {
                reference: primaryEmailInput.value,
                otp: emailOtpInput.value
            });
            state.emailVerified = data.verified === true;
            setStatus(emailVerificationStatus, data.message, null);
            emailOtpSection.style.display = "none";
            toggleSubmitState();
        } catch (error) {
            state.emailVerified = false;
            setStatus(emailVerificationStatus, error.message, "is-error");
            toggleSubmitState();
        }
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

    primaryMobileInput.addEventListener("input", resetMobileVerification);
    primaryEmailInput.addEventListener("input", resetEmailVerification);

    sendMobileOtpButton.addEventListener("click", sendMobileOtp);
    verifyMobileOtpButton.addEventListener("click", verifyMobileOtp);
    sendEmailOtpButton.addEventListener("click", sendEmailOtp);
    verifyEmailOtpButton.addEventListener("click", verifyEmailOtp);

    mobileOtpSection.style.display = "none";
    emailOtpSection.style.display = "none";

    if (state.mobileVerified) {
        setStatus(mobileVerificationStatus, "Primary mobile number already verified.", null);
    }
    if (state.emailVerified) {
        setStatus(emailVerificationStatus, "Primary email address already verified.", null);
    }

    enableDeclarationAcceptance();
    toggleSubmitState();
    updateDepartmentState(true);
});
