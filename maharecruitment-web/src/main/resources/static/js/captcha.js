(function () {
    "use strict";

    var sections = function () {
        return document.querySelectorAll("[data-login-captcha]");
    };

    var challengeImageUrl = function (captchaUrl, challengeId) {
        return captchaUrl.replace(/\/$/, "") + "/" + encodeURIComponent(challengeId) + "/image";
    };

    var applyChallenge = function (challenge, captchaUrl) {
        sections().forEach(function (section) {
            section.querySelector("[data-captcha-id]").value = challenge.id;
            section.querySelector("[data-captcha-image]").src = challengeImageUrl(captchaUrl, challenge.id);
            section.querySelector('[name="loginCaptchaAnswer"]').value = "";
            section.querySelector("[data-captcha-error]").textContent = "";
        });
    };

    var refresh = function () {
        var form = document.querySelector("[data-captcha-url]");
        if (!form) {
            return Promise.reject(new Error("CAPTCHA is unavailable."));
        }
        var currentIdField = document.querySelector("[data-login-captcha] [data-captcha-id]");
        var currentId = currentIdField ? currentIdField.value : "";
        var refreshUrl = form.dataset.captchaUrl
            + (currentId ? "?previousCaptchaId=" + encodeURIComponent(currentId) : "");
        return fetch(refreshUrl, {
            method: "GET",
            credentials: "same-origin",
            cache: "no-store",
            headers: { "Accept": "application/json" }
        }).then(function (response) {
            if (!response.ok) {
                throw new Error("Unable to load a new CAPTCHA.");
            }
            return response.json();
        }).then(function (challenge) {
            applyChallenge(challenge, form.dataset.captchaUrl);
            return challenge;
        });
    };

    var validate = function (section) {
        var answer = section && section.querySelector('[name="loginCaptchaAnswer"]');
        var error = section && section.querySelector("[data-captcha-error]");
        if (answer && /^[A-Za-z0-9]{6}$/.test(answer.value.trim())) {
            if (error) {
                error.textContent = "";
            }
            return true;
        }
        if (error) {
            error.textContent = "Enter all 6 CAPTCHA characters.";
        }
        if (answer) {
            answer.focus();
        }
        return false;
    };

    window.LoginCaptcha = { refresh: refresh, validate: validate };

    document.addEventListener("DOMContentLoaded", function () {
        document.querySelectorAll("[data-captcha-refresh]").forEach(function (button) {
            button.addEventListener("click", function () {
                button.disabled = true;
                refresh().catch(function (error) {
                    var section = button.closest("[data-login-captcha]");
                    section.querySelector("[data-captcha-error]").textContent = error.message;
                }).finally(function () {
                    button.disabled = false;
                });
            });
        });
    });
})();
