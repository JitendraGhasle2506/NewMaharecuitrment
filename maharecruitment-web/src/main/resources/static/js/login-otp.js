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
        var otpResendCooldownSeconds = parseInt(form.dataset.otpResendCooldownSeconds || "60", 10);
        var expiryTimerId = null;
        var resendTimerId = null;
        var hasSentOtpOnce = false;
        var initialSendButtonLabel = "Send OTP";
        var resendButtonLabel = "Resend OTP";

        var setStatus = function (message, mode) {
            statusElement.textContent = message || "";
            statusElement.classList.remove("is-error", "is-pending", "is-success");
            if (mode) {
                statusElement.classList.add(mode);
            }
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

        var startLockCountdown = function () {
            if (!lockCountdownElement) {
                return;
            }
            var remaining = parseInt(lockCountdownElement.dataset.lockSeconds || "0", 10);
            if (!remaining || remaining <= 0) {
                lockCountdownElement.textContent = "";
                return;
            }

            var render = function () {
                lockCountdownElement.textContent = "OTP verification failed. Please try again in " + formatDuration(remaining) + ".";
            };
            render();
            var timerId = window.setInterval(function () {
                remaining -= 1;
                if (remaining <= 0) {
                    window.clearInterval(timerId);
                    lockCountdownElement.textContent = "You can request a new OTP now.";
                    return;
                }
                render();
            }, 1000);
        };

        var startOtpTimers = function (shouldCooldown) {
            clearTimers();

            var expiryRemaining = otpExpirySeconds;
            var resendRemaining = shouldCooldown ? otpResendCooldownSeconds : 0;
            sendButton.disabled = resendRemaining > 0;
            updateTimingMessage(expiryRemaining, resendRemaining);

            expiryTimerId = window.setInterval(function () {
                expiryRemaining -= 1;
                if (expiryRemaining <= 0) {
                    expiryRemaining = 0;
                    if (expiryTimerId) {
                        window.clearInterval(expiryTimerId);
                        expiryTimerId = null;
                    }
                }
                updateTimingMessage(expiryRemaining, resendRemaining);
            }, 1000);

            if (shouldCooldown) {
                resendTimerId = window.setInterval(function () {
                    resendRemaining -= 1;
                    if (resendRemaining <= 0) {
                        resendRemaining = 0;
                        sendButton.disabled = false;
                        if (resendTimerId) {
                            window.clearInterval(resendTimerId);
                            resendTimerId = null;
                        }
                    }
                    updateTimingMessage(expiryRemaining, resendRemaining);
                }, 1000);
            } else {
                sendButton.disabled = false;
            }
        };

        var setOtpSectionVisible = function (visible) {
            if (!otpSection) {
                return;
            }
            otpSection.classList.toggle("is-visible", visible);
            if (otpInput) {
                otpInput.disabled = !visible;
                if (!visible) {
                    otpInput.value = "";
                }
            }
        };

        var resetStatus = function () {
            clearTimers();
            setStatus("", null);
            setTiming("");
            sendButton.disabled = false;
            hasSentOtpOnce = false;
            updateSendButtonLabel();
            setOtpSectionVisible(false);
        };

        var validateIdentifier = function (value) {
            return value.trim().length > 0;
        };

        identifierInput.addEventListener("input", resetStatus);
        channelSelect.addEventListener("change", resetStatus);

        sendButton.addEventListener("click", async function () {
            var identifier = identifierInput.value.trim();
            var channel = channelSelect.value;

            if (!validateIdentifier(identifier)) {
                setStatus("Enter your registered username, email, or mobile number first.", "is-error");
                identifierInput.focus();
                return;
            }

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
                        channel: channel
                    })
                });

                var data = await response.json().catch(function () {
                    return {
                        message: "Unexpected response received."
                    };
                });

                if (!response.ok) {
                    throw new Error(data.message || "Unable to send OTP.");
                }

                if (data.expirySeconds && data.expirySeconds > 0) {
                    otpExpirySeconds = data.expirySeconds;
                }
                var isResend = hasSentOtpOnce;
                hasSentOtpOnce = true;
                updateSendButtonLabel();
                setStatus(data.message || "OTP sent successfully.", "is-success");
                setOtpSectionVisible(true);
                startOtpTimers(isResend);
                otpInput.focus();
            } catch (error) {
                setOtpSectionVisible(false);
                setStatus(error.message || "Unable to send OTP.", "is-error");
                setTiming("");
            } finally {
                if (!resendTimerId) {
                    sendButton.disabled = false;
                }
            }
        });

        form.addEventListener("submit", function () {
            if (verifyButton) {
                verifyButton.disabled = true;
                verifyButton.textContent = "Verifying...";
            }
        });

        var isOtpSentOnLoad = form.dataset.otpSent === "true";
        if (isOtpSentOnLoad) {
            hasSentOtpOnce = true;
            setOtpSectionVisible(true);
        } else {
            setOtpSectionVisible(false);
        }
        setTiming("");
        updateSendButtonLabel();
        startLockCountdown();

        // Toggle password visibility
        var togglePasswordButton = document.getElementById("togglePassword");
        var passwordInput = document.getElementById("password");
        var togglePasswordIcon = document.getElementById("togglePasswordIcon");

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
