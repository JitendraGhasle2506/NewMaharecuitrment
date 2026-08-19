(function () {
    "use strict";

    document.addEventListener("DOMContentLoaded", function () {
        var modeInputs = document.querySelectorAll('input[name="loginMode"]');
        var modePanels = document.querySelectorAll("[data-login-mode]");
        var form = document.getElementById("otpLoginForm");

        var updateLoginMode = function (selectedMode) {
            modePanels.forEach(function (panel) {
                var isActive = panel.dataset.loginMode === selectedMode;
                panel.classList.toggle("is-hidden", !isActive);
                panel.querySelectorAll("input, select, button, textarea").forEach(function (element) {
                    if (element.type === "hidden") {
                        return;
                    }
                    element.disabled = !isActive;
                });
            });
        };

        if (modeInputs.length > 0) {
            modeInputs.forEach(function (input) {
                input.addEventListener("change", function () {
                    if (input.checked) {
                        updateLoginMode(input.value);
                    }
                });
            });

            var selectedInput = Array.prototype.find.call(modeInputs, function (input) {
                return input.checked;
            });
            updateLoginMode(selectedInput ? selectedInput.value : "otp");
        }

        if (!form) {
            return;
        }

        var identifierInput = document.getElementById("otpIdentifier");
        var channelSelect = document.getElementById("otpChannel");
        var channelDisplay = document.getElementById("otpChannelDisplay");
        var otpInput = document.getElementById("loginOtp");
        var otpSection = document.getElementById("loginOtpSection");
        var sendButton = document.getElementById("sendLoginOtpBtn");
        var verifyButton = document.getElementById("loginOtpVerifyBtn");
        var statusElement = document.getElementById("otpLoginStatus");
        var timingElement = document.getElementById("otpLoginTiming");
        var lockCountdownElement = document.getElementById("otpLoginLockCountdown");
        var sendUrl = form.dataset.otpSendUrl;
        var csrfToken = form.dataset.csrfToken || "";
        var otpExpirySeconds = parseInt(form.dataset.otpExpirySeconds || "600", 10);
        var otpResendCooldownSeconds = parseInt(form.dataset.otpResendCooldownSeconds || "120", 10);
        var isEmailOtpEnabled = form.dataset.emailOtpEnabled === "true";
        var isSmsOtpEnabled = form.dataset.smsOtpEnabled === "true";
        var otpErrorCode = form.dataset.otpErrorCode || "";
        var expiryTimerId = null;
        var resendTimerId = null;
        var lockTimerId = null;
        var sendRequestInProgress = false;
        var verifyRequestInProgress = false;
        var otpActive = false;
        var temporarilyBlocked = false;
        var hasSentOtpOnce = false;
        var initialSendButtonLabel = "Send OTP";
        var resendButtonLabel = "Resend OTP";

        var isLoopbackHost = function (hostname) {
            var normalizedHostname = (hostname || "").toLowerCase();
            return normalizedHostname === "localhost"
                || normalizedHostname === "127.0.0.1"
                || normalizedHostname === "::1"
                || normalizedHostname === "[::1]";
        };

        var isInsecureTransport = function () {
            return window.location.protocol !== "https:" && !isLoopbackHost(window.location.hostname);
        };

        var setStatus = function (message, mode) {
            statusElement.textContent = message || "";
            statusElement.classList.remove("is-error", "is-pending", "is-success");
            if (mode) {
                statusElement.classList.add(mode);
            }
        };

        var showTransportError = function () {
            setStatus("HTTPS is required before submitting credentials.", "is-error");
        };

        var setElementStatus = function (element, message, mode) {
            if (!element) {
                return;
            }
            element.textContent = message || "";
            element.classList.remove("is-error", "is-pending", "is-success");
            if (mode) {
                element.classList.add(mode);
            }
        };

        var base64ToArrayBuffer = function (base64Value) {
            var binary = window.atob(base64Value);
            var bytes = new Uint8Array(binary.length);
            for (var i = 0; i < binary.length; i += 1) {
                bytes[i] = binary.charCodeAt(i);
            }
            return bytes.buffer;
        };

        var arrayBufferToBase64 = function (buffer) {
            var bytes = new Uint8Array(buffer);
            var chunks = [];
            var chunkSize = 0x8000;
            for (var i = 0; i < bytes.length; i += chunkSize) {
                chunks.push(String.fromCharCode.apply(null, bytes.subarray(i, i + chunkSize)));
            }
            return window.btoa(chunks.join(""));
        };

        var credentialKeyPromise = null;

        var getCredentialEncryptionKey = function (keyUrl) {
            if (!credentialKeyPromise) {
                credentialKeyPromise = fetch(keyUrl, {
                    method: "GET",
                    credentials: "same-origin",
                    cache: "no-store",
                    headers: {
                        "Accept": "application/json"
                    }
                }).then(function (response) {
                    if (!response.ok) {
                        throw new Error("Unable to initialize secure login.");
                    }
                    return response.json();
                }).then(function (keyData) {
                    if (!keyData || keyData.algorithm !== "RSA-OAEP-256" || !keyData.publicKey) {
                        throw new Error("Invalid secure login key received.");
                    }
                    return window.crypto.subtle.importKey(
                        "spki",
                        base64ToArrayBuffer(keyData.publicKey),
                        {
                            name: "RSA-OAEP",
                            hash: "SHA-256"
                        },
                        false,
                        ["encrypt"]
                    ).then(function (publicKey) {
                        return {
                            publicKey: publicKey,
                            encryptedPrefix: keyData.encryptedPrefix || "ENC:v1:"
                        };
                    });
                });
            }
            return credentialKeyPromise;
        };

        var encryptCredential = function (value, keyUrl) {
            if (!window.crypto || !window.crypto.subtle || !window.TextEncoder) {
                return Promise.reject(new Error("This browser does not support secure credential submission."));
            }

            return getCredentialEncryptionKey(keyUrl).then(function (keyData) {
                return window.crypto.subtle.encrypt(
                    {
                        name: "RSA-OAEP"
                    },
                    keyData.publicKey,
                    new window.TextEncoder().encode(value)
                ).then(function (ciphertext) {
                    return keyData.encryptedPrefix + arrayBufferToBase64(ciphertext);
                });
            });
        };

        var updateSendButtonLabel = function () {
            sendButton.textContent = hasSentOtpOnce ? resendButtonLabel : initialSendButtonLabel;
        };

        var formatDuration = function (totalSeconds) {
            var seconds = Math.max(0, totalSeconds);
            var minutesPart = Math.floor(seconds / 60);
            var secondsPart = seconds % 60;
            return String(minutesPart).padStart(2, "0") + ":" + String(secondsPart).padStart(2, "0");
        };

        var clearTimers = function () {
            if (expiryTimerId) {
                window.clearInterval(expiryTimerId);
                expiryTimerId = null;
            }
            if (resendTimerId) {
                window.clearInterval(resendTimerId);
                resendTimerId = null;
            }
        };

        var setTiming = function (message) {
            if (timingElement) {
                timingElement.textContent = message || "";
            }
        };

        var updateTimingMessage = function (expiryRemaining, resendRemaining) {
            var parts = [];
            if (expiryRemaining > 0) {
                parts.push("OTP valid for " + formatDuration(expiryRemaining));
            }
            if (resendRemaining > 0) {
                parts.push("Resend available in " + formatDuration(resendRemaining));
            }
            setTiming(parts.join(" | "));
        };

        var updateOtpControls = function () {
            var otpComplete = Boolean(otpInput && /^[0-9]{6}$/.test(otpInput.value.trim()));
            if (otpInput) {
                otpInput.disabled = !otpActive || temporarilyBlocked;
            }
            if (verifyButton) {
                verifyButton.disabled = verifyRequestInProgress
                    || temporarilyBlocked
                    || !otpActive
                    || !otpComplete;
            }
            sendButton.disabled = sendRequestInProgress || temporarilyBlocked || Boolean(resendTimerId);
        };

        var startSendCooldown = function (seconds) {
            if (resendTimerId) {
                window.clearInterval(resendTimerId);
                resendTimerId = null;
            }
            var remaining = Math.max(0, parseInt(seconds || "0", 10));
            if (remaining <= 0) {
                updateOtpControls();
                return;
            }
            var render = function () {
                sendButton.textContent = "Resend OTP (" + formatDuration(remaining) + ")";
                updateOtpControls();
            };
            resendTimerId = window.setInterval(function () {
                remaining -= 1;
                if (remaining <= 0) {
                    window.clearInterval(resendTimerId);
                    resendTimerId = null;
                    updateSendButtonLabel();
                    updateOtpControls();
                    return;
                }
                render();
            }, 1000);
            render();
        };

        var startTemporaryBlock = function (seconds, message) {
            if (lockTimerId) {
                window.clearInterval(lockTimerId);
            }
            var remaining = Math.max(1, parseInt(seconds || "1", 10));
            temporarilyBlocked = true;
            otpActive = false;
            setOtpSectionVisible(false);
            var render = function () {
                setStatus(
                    (message || "OTP operations are temporarily blocked.")
                        + " Try again in " + formatDuration(remaining) + ".",
                    "is-error"
                );
            };
            render();
            lockTimerId = window.setInterval(function () {
                remaining -= 1;
                if (remaining <= 0) {
                    window.clearInterval(lockTimerId);
                    lockTimerId = null;
                    temporarilyBlocked = false;
                    setStatus("You can request a new OTP now.", "is-pending");
                    updateOtpControls();
                    return;
                }
                render();
            }, 1000);
            updateOtpControls();
        };

        var startLockCountdown = function () {
            if (!lockCountdownElement) {
                return;
            }
            var remaining = parseInt(lockCountdownElement.dataset.lockSeconds || "0", 10);
            if (!remaining || remaining <= 0) {
                lockCountdownElement.textContent = "";
                return;
            }

            temporarilyBlocked = true;
            otpActive = false;
            setOtpSectionVisible(false);
            updateOtpControls();

            var render = function () {
                lockCountdownElement.textContent = "OTP verification failed. Please try again in " + formatDuration(remaining) + ".";
            };
            render();
            lockTimerId = window.setInterval(function () {
                remaining -= 1;
                if (remaining <= 0) {
                    window.clearInterval(lockTimerId);
                    lockTimerId = null;
                    temporarilyBlocked = false;
                    lockCountdownElement.textContent = "You can request a new OTP now.";
                    updateOtpControls();
                    return;
                }
                render();
            }, 1000);
        };

        var startOtpTimers = function () {
            clearTimers();

            var expiryRemaining = otpExpirySeconds;
            var resendRemaining = Math.max(0, otpResendCooldownSeconds);
            otpActive = expiryRemaining > 0;
            updateOtpControls();
            updateTimingMessage(expiryRemaining, resendRemaining);

            expiryTimerId = window.setInterval(function () {
                expiryRemaining -= 1;
                if (expiryRemaining <= 0) {
                    expiryRemaining = 0;
                    if (expiryTimerId) {
                        window.clearInterval(expiryTimerId);
                        expiryTimerId = null;
                    }
                    otpActive = false;
                    if (otpInput) {
                        otpInput.value = "";
                    }
                    setStatus("OTP expired. Please request a new OTP.", "is-error");
                    updateOtpControls();
                }
                updateTimingMessage(expiryRemaining, resendRemaining);
            }, 1000);

            if (resendRemaining > 0) {
                resendTimerId = window.setInterval(function () {
                    resendRemaining -= 1;
                    if (resendRemaining <= 0) {
                        resendRemaining = 0;
                        if (resendTimerId) {
                            window.clearInterval(resendTimerId);
                            resendTimerId = null;
                        }
                        updateOtpControls();
                    }
                    updateTimingMessage(expiryRemaining, resendRemaining);
                }, 1000);
                updateOtpControls();
            }
        };

        var setOtpSectionVisible = function (visible) {
            if (!otpSection) {
                return;
            }
            otpSection.classList.toggle("is-visible", visible);
            if (otpInput) {
                if (!visible) {
                    otpInput.value = "";
                }
            }
            updateOtpControls();
        };

        var resetStatus = function () {
            clearTimers();
            if (lockTimerId) {
                window.clearInterval(lockTimerId);
                lockTimerId = null;
            }
            setStatus("", null);
            setTiming("");
            sendButton.disabled = false;
            hasSentOtpOnce = false;
            otpActive = false;
            temporarilyBlocked = false;
            verifyRequestInProgress = false;
            updateSendButtonLabel();
            setOtpSectionVisible(false);
            updateDetectedChannel();
        };

        var validateIdentifier = function (value) {
            return value.trim().length > 0;
        };

        var detectIdentifierChannel = function (value) {
            var identifier = value.trim();
            if (!identifier) {
                return "";
            }
            if (/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(identifier)) {
                return "EMAIL";
            }
            if (/^[0-9]{10}$/.test(identifier)) {
                return "SMS";
            }
            return "";
        };

        var otpChannelLabel = function (channel) {
            return channel === "EMAIL" ? "Email OTP" : "Mobile OTP";
        };

        var isDetectedChannelEnabled = function (channel) {
            return channel === "EMAIL" ? isEmailOtpEnabled : isSmsOtpEnabled;
        };

        var setChannelDisplay = function (message, mode) {
            if (!channelDisplay) {
                return;
            }
            channelDisplay.textContent = message || "Enter Email / Mobile";
            channelDisplay.classList.remove("is-selected", "is-error");
            if (mode) {
                channelDisplay.classList.add(mode);
            }
        };

        function updateDetectedChannel() {
            var channel = detectIdentifierChannel(identifierInput.value);
            if (channelSelect) {
                channelSelect.value = channel;
            }

            if (!identifierInput.value.trim()) {
                setChannelDisplay("Enter Email / Mobile", null);
                return "";
            }

            if (!channel) {
                setChannelDisplay("Enter valid email or 10 digit mobile", "is-error");
                return "";
            }

            if (!isDetectedChannelEnabled(channel)) {
                setChannelDisplay(otpChannelLabel(channel) + " disabled", "is-error");
                return channel;
            }

            setChannelDisplay(otpChannelLabel(channel), "is-selected");
            return channel;
        }

        identifierInput.addEventListener("input", resetStatus);

        sendButton.addEventListener("click", async function () {
            if (sendRequestInProgress) {
                return;
            }
            if (isInsecureTransport()) {
                showTransportError();
                return;
            }

            var identifier = identifierInput.value.trim();
            var channel = updateDetectedChannel();

            if (!validateIdentifier(identifier)) {
                setStatus("Enter your registered email or mobile number first.", "is-error");
                identifierInput.focus();
                return;
            }

            if (!channel) {
                setStatus("Enter a valid email address or 10 digit mobile number.", "is-error");
                identifierInput.focus();
                return;
            }

            if (!isDetectedChannelEnabled(channel)) {
                setStatus(otpChannelLabel(channel) + " login is not enabled in this environment.", "is-error");
                identifierInput.focus();
                return;
            }

            sendRequestInProgress = true;
            sendButton.disabled = true;
            setStatus("Sending OTP...", "is-pending");

            try {
                var response = await fetch(sendUrl, {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json",
                        "X-CSRF-TOKEN": csrfToken
                    },
                    body: JSON.stringify({
                        identifier: identifier,
                        purpose: "LOGIN",
                        deliveryChannel: channel,
                        channel: channel
                    })
                });

                var data = await response.json().catch(function () {
                    return {
                        message: "Unexpected response received."
                    };
                });

                if (response.status === 429
                        && ((data.retryAfterSeconds && data.retryAfterSeconds > 0)
                            || (data.lockSecondsRemaining && data.lockSecondsRemaining > 0))) {
                    otpResendCooldownSeconds = data.retryAfterSeconds || 0;
                    hasSentOtpOnce = true;
                    updateSendButtonLabel();
                    if (data.code === "OTP_TEMPORARILY_BLOCKED") {
                        startTemporaryBlock(data.lockSecondsRemaining, data.message);
                    } else if (!otpActive && data.code === "OTP_RESEND_COOLDOWN") {
                        otpActive = true;
                        setOtpSectionVisible(true);
                    }
                    if (data.code !== "OTP_TEMPORARILY_BLOCKED") {
                        setStatus(data.message || "OTP already sent. Please enter the latest valid OTP.", "is-error");
                        startSendCooldown(data.retryAfterSeconds);
                    }
                    if (otpActive) {
                        otpInput.focus();
                    }
                    return;
                }

                if (!response.ok) {
                    throw new Error(data.message || "Unable to send OTP.");
                }

                if (!data.expirySeconds || data.expirySeconds <= 0) {
                    setOtpSectionVisible(false);
                    setStatus(data.message || "Unable to send OTP.", "is-error");
                    setTiming("");
                    return;
                }

                otpExpirySeconds = data.expiresInSeconds && data.expiresInSeconds > 0
                    ? data.expiresInSeconds
                    : data.expirySeconds;
                otpResendCooldownSeconds = data.resendAvailableInSeconds && data.resendAvailableInSeconds > 0
                    ? data.resendAvailableInSeconds
                    : otpResendCooldownSeconds;
                var resend = hasSentOtpOnce;
                hasSentOtpOnce = true;
                updateSendButtonLabel();
                otpActive = true;
                otpInput.value = "";
                setStatus(
                    resend ? "A new OTP has been sent successfully." : (data.message || "OTP sent successfully."),
                    "is-success"
                );
                setOtpSectionVisible(true);
                startOtpTimers();
                otpInput.focus();
            } catch (error) {
                setOtpSectionVisible(false);
                setStatus(error.message || "Unable to send OTP.", "is-error");
                setTiming("");
            } finally {
                sendRequestInProgress = false;
                updateOtpControls();
            }
        });

        otpInput.addEventListener("input", function () {
            otpInput.value = otpInput.value.replace(/[^0-9]/g, "").slice(0, 6);
            updateOtpControls();
        });

        form.addEventListener("submit", function (event) {
            if (verifyRequestInProgress || !otpActive || !/^[0-9]{6}$/.test(otpInput.value.trim())) {
                event.preventDefault();
                setStatus("Enter the complete 6 digit OTP before verification.", "is-error");
                return;
            }
            if (isInsecureTransport()) {
                event.preventDefault();
                showTransportError();
                return;
            }

            var channel = updateDetectedChannel();
            if (!identifierInput.value.trim()) {
                event.preventDefault();
                setStatus("Enter your registered email or mobile number first.", "is-error");
                identifierInput.focus();
                return;
            }

            if (!channel) {
                event.preventDefault();
                setStatus("Enter a valid email address or 10 digit mobile number.", "is-error");
                identifierInput.focus();
                return;
            }

            if (!isDetectedChannelEnabled(channel)) {
                event.preventDefault();
                setStatus(otpChannelLabel(channel) + " login is not enabled in this environment.", "is-error");
                identifierInput.focus();
                return;
            }

            if (verifyButton) {
                verifyRequestInProgress = true;
                updateOtpControls();
                verifyButton.textContent = "Verifying...";
            }
        });

        var isOtpSentOnLoad = form.dataset.otpSent === "true";
        var terminalOtpCodes = [
            "OTP_EXPIRED",
            "OTP_ATTEMPTS_EXCEEDED",
            "OTP_ALREADY_USED",
            "OTP_NOT_FOUND",
            "OTP_TEMPORARILY_BLOCKED"
        ];
        if (isOtpSentOnLoad && terminalOtpCodes.indexOf(otpErrorCode) < 0) {
            hasSentOtpOnce = true;
            otpActive = true;
            setOtpSectionVisible(true);
            startOtpTimers();
        } else {
            otpActive = false;
            setOtpSectionVisible(false);
            setTiming("");
        }
        updateSendButtonLabel();
        updateDetectedChannel();
        startLockCountdown();
        updateOtpControls();

        // Toggle password visibility
        var togglePasswordButton = document.getElementById("togglePassword");
        var passwordInput = document.getElementById("password");
        var encryptedPasswordInput = document.getElementById("encryptedPassword");
        var togglePasswordIcon = document.getElementById("togglePasswordIcon");
        var passwordStatusElement = document.getElementById("passwordLoginStatus");

        var passwordLoginForm = document.getElementById("loginForm");
        if (passwordLoginForm) {
            passwordLoginForm.addEventListener("submit", function (event) {
                if (isInsecureTransport()) {
                    event.preventDefault();
                    setElementStatus(passwordStatusElement, "HTTPS is required before submitting credentials.", "is-error");
                    return;
                }

                event.preventDefault();
                if (!passwordInput || !encryptedPasswordInput || !passwordLoginForm.dataset.credentialKeyUrl) {
                    setElementStatus(passwordStatusElement, "Secure login is unavailable. Please refresh and try again.", "is-error");
                    return;
                }

                if (!passwordLoginForm.reportValidity()) {
                    return;
                }

                setElementStatus(passwordStatusElement, "Securing credentials...", "is-pending");
                encryptCredential(passwordInput.value, passwordLoginForm.dataset.credentialKeyUrl)
                    .then(function (encryptedCredential) {
                        encryptedPasswordInput.value = encryptedCredential;
                        passwordInput.value = "";
                        setElementStatus(passwordStatusElement, "", null);
                        window.HTMLFormElement.prototype.submit.call(passwordLoginForm);
                    })
                    .catch(function (error) {
                        encryptedPasswordInput.value = "";
                        setElementStatus(
                            passwordStatusElement,
                            error.message || "Unable to secure credentials. Please refresh and try again.",
                            "is-error"
                        );
                    });
            });
        }

        if (togglePasswordButton && passwordInput && togglePasswordIcon) {
            togglePasswordButton.addEventListener("click", function () {
                var type = passwordInput.getAttribute("type") === "password" ? "text" : "password";
                passwordInput.setAttribute("type", type);
                
                if (type === "password") {
                    togglePasswordIcon.classList.remove("fa-eye-slash");
                    togglePasswordIcon.classList.add("fa-eye");
                } else {
                    togglePasswordIcon.classList.remove("fa-eye");
                    togglePasswordIcon.classList.add("fa-eye-slash");
                }
            });
        }
    });
})();
