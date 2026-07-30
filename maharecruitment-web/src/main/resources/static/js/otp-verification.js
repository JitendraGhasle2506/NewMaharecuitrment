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
            verified: Boolean(config.initialVerified),
            captchaId: "",
            locked: false
        };
        let captchaSection = null;
        let captchaQuestion = null;
        let captchaInput = null;
        let lockTimerId = null;

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
                    const requestError = new Error(data.message || "Request failed.");
                    requestError.response = data;
                    requestError.status = response.status;
                    throw requestError;
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

        const formatDuration = (totalSeconds) => {
            const seconds = Math.max(0, totalSeconds);
            const minutesPart = Math.floor(seconds / 60);
            const secondsPart = seconds % 60;
            return String(minutesPart).padStart(2, "0") + ":" + String(secondsPart).padStart(2, "0");
        };

        const ensureCaptchaElements = () => {
            if (captchaSection) {
                return;
            }

            captchaSection = document.createElement("div");
            captchaSection.className = "verification-row otp-captcha-section";
            captchaSection.style.display = "none";

            captchaQuestion = document.createElement("label");
            captchaQuestion.className = "form-label mb-0";

            captchaInput = document.createElement("input");
            captchaInput.type = "text";
            captchaInput.className = "form-control";
            captchaInput.inputMode = "numeric";
            captchaInput.maxLength = 3;
            captchaInput.autocomplete = "off";
            captchaInput.addEventListener("input", () => {
                captchaInput.value = captchaInput.value.replace(/[^0-9]/g, "");
            });

            captchaSection.appendChild(captchaQuestion);
            captchaSection.appendChild(captchaInput);

            const anchor = config.otpSection || config.statusElement || config.verifyButton;
            if (anchor && anchor.parentElement) {
                anchor.parentElement.insertBefore(captchaSection, anchor.nextSibling);
            }
        };

        const hideCaptcha = () => {
            state.captchaId = "";
            if (captchaInput) {
                captchaInput.value = "";
            }
            if (captchaSection) {
                captchaSection.style.display = "none";
            }
        };

        const showCaptcha = (data) => {
            if (!data || data.captchaRequired !== true) {
                hideCaptcha();
                return;
            }

            ensureCaptchaElements();
            state.captchaId = data.captchaId || "";
            captchaQuestion.textContent = data.captchaQuestion || "CAPTCHA";
            captchaSection.style.display = "flex";
            captchaInput.disabled = false;
        };

        const stopLockCountdown = () => {
            if (lockTimerId) {
                window.clearInterval(lockTimerId);
                lockTimerId = null;
            }
            state.locked = false;
        };

        const startLockCountdown = (seconds) => {
            stopLockCountdown();
            let remaining = Number(seconds || 0);
            if (remaining <= 0) {
                return;
            }
            state.locked = true;
            config.verifyButton.disabled = true;
            const render = () => {
                setStatus("OTP verification failed. Please try again in " + formatDuration(remaining) + ".", "is-error");
            };
            render();
            lockTimerId = window.setInterval(() => {
                remaining -= 1;
                if (remaining <= 0) {
                    stopLockCountdown();
                    setStatus("You can request a new OTP now.", "is-pending");
                    config.verifyButton.disabled = false;
                    return;
                }
                render();
            }, 1000);
        };

        const applySecurityState = (data, fallbackMessage) => {
            if (!data) {
                setStatus(fallbackMessage, "is-error");
                return;
            }
            showCaptcha(data);
            if (data.lockSecondsRemaining && data.lockSecondsRemaining > 0) {
                startLockCountdown(data.lockSecondsRemaining);
                return;
            }

            const parts = [data.message || fallbackMessage];
            if (data.remainingAttempts && data.remainingAttempts > 0) {
                parts.push("Remaining attempts: " + data.remainingAttempts);
            }
            if (data.retryAfterSeconds && data.retryAfterSeconds > 0) {
                parts.push("Retry after " + formatDuration(data.retryAfterSeconds));
            }
            setStatus(parts.join(" "), "is-error");
        };

        const reset = () => {
            state.verified = false;
            stopLockCountdown();
            hideCaptcha();
            if (config.otpInput) {
                config.otpInput.value = "";
                config.otpInput.disabled = true;
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
                stopLockCountdown();
                hideCaptcha();
                if (config.otpSection) {
                    config.otpSection.style.display = "flex";
                }
                if (config.otpInput) {
                    config.otpInput.disabled = false;
                }
                setStatus(data.message, "is-pending");
                if (config.otpInput) {
                    config.otpInput.focus();
                }
            } catch (error) {
                applySecurityState(error.response, error.message);
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
                const payload = {
                    purpose: config.purpose,
                    channel: config.channel,
                    reference: config.referenceInput.value.trim(),
                    otp
                };
                if (state.captchaId) {
                    payload.captchaId = state.captchaId;
                    payload.captchaAnswer = captchaInput ? captchaInput.value.trim() : "";
                }
                const data = await apiPost(config.verifyUrl, payload);
                state.verified = data.verified === true;
                stopLockCountdown();
                hideCaptcha();
                if (config.otpSection) {
                    config.otpSection.style.display = "none";
                }
                setStatus(data.message, "is-success");
                notify();
            } catch (error) {
                state.verified = false;
                applySecurityState(error.response, error.message);
                notify();
            } finally {
                config.verifyButton.disabled = state.locked;
            }
        };

        config.referenceInput.addEventListener("input", reset);
        config.sendButton.addEventListener("click", sendOtp);
        config.verifyButton.addEventListener("click", verifyOtp);

        if (config.otpSection) {
            config.otpSection.style.display = "none";
        }
        if (config.otpInput) {
            config.otpInput.disabled = true;
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
