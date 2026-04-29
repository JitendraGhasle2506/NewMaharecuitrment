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

    const DEFAULT_REQUEST_TIMEOUT_MS = 15000;

    const createOtpVerification = (config) => {
        if (!config || !config.referenceInput || !config.sendButton || !config.verifyButton) {
            throw new Error("OTP verification configuration is incomplete.");
        }
        if (!config.sendUrl || !config.verifyUrl) {
            throw new Error("OTP verification endpoints are not configured.");
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
            config.statusElement.classList.remove("is-pending", "is-error", "is-success");
            if (mode) {
                config.statusElement.classList.add(mode);
            }
        };

        const notify = () => {
            listeners.forEach((listener) => listener(state.verified));
        };

        const apiPost = async (url, payload) => {
            const controller = typeof AbortController === "function" ? new AbortController() : null;
            const timeoutMs = Number(config.requestTimeoutMs || DEFAULT_REQUEST_TIMEOUT_MS);
            const timeoutId = controller
                ? window.setTimeout(() => controller.abort(), timeoutMs)
                : null;

            try {
                const response = await fetch(url, {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json",
                        "X-CSRF-TOKEN": config.csrfToken || ""
                    },
                    body: JSON.stringify(payload),
                    signal: controller ? controller.signal : undefined
                });

                const rawResponse = await response.text();
                let data;
                try {
                    data = rawResponse
                        ? JSON.parse(rawResponse)
                        : { message: "", verified: false };
                } catch (parseError) {
                    data = {
                        message: response.ok
                            ? "Unexpected response received."
                            : rawResponse || "Request failed.",
                        verified: false
                    };
                }

                if (!response.ok) {
                    throw new Error(data.message || "Request failed.");
                }

                return data;
            } catch (error) {
                if (error && error.name === "AbortError") {
                    throw new Error("OTP request timed out. Please try again. If the issue continues, contact support.");
                }
                throw error;
            } finally {
                if (timeoutId) {
                    window.clearTimeout(timeoutId);
                }
            }
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
                config.referenceInput.focus();
                return;
            }

            config.sendButton.disabled = true;
            setStatus("Sending OTP...", "is-pending");

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
                if (config.otpInput) {
                    config.otpInput.focus();
                }
            } catch (error) {
                setStatus(error.message, "is-error");
            } finally {
                config.sendButton.disabled = false;
            }
        };

        const verifyOtp = async () => {
            const otp = config.otpInput ? config.otpInput.value.trim() : "";
            if (!/^[0-9]{6}$/.test(otp)) {
                setStatus(defaults.invalidOtp, "is-error");
                if (config.otpInput) {
                    config.otpInput.focus();
                }
                return;
            }

            config.verifyButton.disabled = true;
            setStatus("Verifying OTP...", "is-pending");

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
                setStatus(data.message, "is-success");
                notify();
            } catch (error) {
                state.verified = false;
                setStatus(error.message, "is-error");
                notify();
            } finally {
                config.verifyButton.disabled = false;
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
