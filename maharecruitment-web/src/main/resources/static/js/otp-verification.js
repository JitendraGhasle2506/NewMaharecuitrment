(function (window) {
    "use strict";

    const channelValidators = {
        MOBILE: (value) => /^[0-9]{10}$/.test(value.trim()),
        EMAIL: (value) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value.trim())
    };

    const channelMessages = {
        MOBILE: {
            invalidReference: "Enter a valid 10 digit mobile number before requesting OTP.",
            invalidOtp: "Enter the 6 digit OTP sent to the mobile number.",
            verified: "Mobile number already verified."
        },
        EMAIL: {
            invalidReference: "Enter a valid email address before requesting OTP.",
            invalidOtp: "Enter the 6 digit OTP sent to the email address.",
            verified: "Email address already verified."
        }
    };

    const createOtpVerification = (config) => {
        if (!config || !config.referenceInput || !config.sendButton || !config.verifyButton) {
            throw new Error("OTP verification configuration is incomplete.");
        }

        const defaults = channelMessages[config.channel] || channelMessages.EMAIL;
        const validateReference = config.validateReference || channelValidators[config.channel];
        const listeners = [];
        const state = {
            verified: Boolean(config.initialVerified)
        };

        const setStatus = (message, mode) => {
            if (!config.statusElement) {
                return;
            }
            config.statusElement.textContent = message || "";
            config.statusElement.classList.remove("is-pending", "is-error");
            if (mode) {
                config.statusElement.classList.add(mode);
            }
        };

        const notify = () => {
            listeners.forEach((listener) => listener(state.verified));
        };

        const apiPost = async (url, payload) => {
            const response = await fetch(url, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "X-CSRF-TOKEN": config.csrfToken || ""
                },
                body: JSON.stringify(payload)
            });

            const data = await response.json().catch(() => ({
                message: "Unexpected response received.",
                verified: false
            }));

            if (!response.ok) {
                throw new Error(data.message || "Request failed.");
            }

            return data;
        };

        const reset = () => {
            state.verified = false;
            if (config.otpInput) {
                config.otpInput.value = "";
            }
            if (config.otpSection) {
                config.otpSection.style.display = "none";
            }
            setStatus("", null);
            notify();
        };

        const sendOtp = async () => {
            const reference = config.referenceInput.value.trim();
            if (!validateReference || !validateReference(reference)) {
                setStatus(defaults.invalidReference, "is-error");
                return;
            }

            try {
                const data = await apiPost(config.sendUrl, {
                    purpose: config.purpose,
                    channel: config.channel,
                    reference
                });
                if (config.otpSection) {
                    config.otpSection.style.display = "flex";
                }
                setStatus(data.message, "is-pending");
            } catch (error) {
                setStatus(error.message, "is-error");
            }
        };

        const verifyOtp = async () => {
            const otp = config.otpInput ? config.otpInput.value.trim() : "";
            if (!/^[0-9]{6}$/.test(otp)) {
                setStatus(defaults.invalidOtp, "is-error");
                return;
            }

            try {
                const data = await apiPost(config.verifyUrl, {
                    purpose: config.purpose,
                    channel: config.channel,
                    reference: config.referenceInput.value.trim(),
                    otp
                });
                state.verified = data.verified === true;
                if (config.otpSection) {
                    config.otpSection.style.display = "none";
                }
                setStatus(data.message, null);
                notify();
            } catch (error) {
                state.verified = false;
                setStatus(error.message, "is-error");
                notify();
            }
        };

        config.referenceInput.addEventListener("input", reset);
        config.sendButton.addEventListener("click", sendOtp);
        config.verifyButton.addEventListener("click", verifyOtp);

        if (config.otpSection) {
            config.otpSection.style.display = "none";
        }

        if (state.verified) {
            setStatus(config.initialVerifiedMessage || defaults.verified, null);
        }

        return {
            isVerified: () => state.verified,
            reset,
            onChange: (listener) => {
                if (typeof listener === "function") {
                    listeners.push(listener);
                }
            }
        };
    };

    window.createOtpVerification = createOtpVerification;
})(window);
