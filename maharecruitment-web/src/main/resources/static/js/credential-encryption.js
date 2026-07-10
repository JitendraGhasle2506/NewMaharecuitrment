(function () {
    "use strict";

    document.addEventListener("DOMContentLoaded", function () {
        var encryptedForms = document.querySelectorAll('form[data-encrypt-credentials="true"]');
        if (!encryptedForms.length) {
            return;
        }

        var credentialKeyPromise = null;

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

        var setStatus = function (form, message) {
            var statusElement = form.querySelector("[data-credential-encryption-status]");
            if (!statusElement) {
                statusElement = document.createElement("div");
                statusElement.className = "alert alert-danger d-none";
                statusElement.setAttribute("data-credential-encryption-status", "true");
                form.prepend(statusElement);
            }
            statusElement.textContent = message || "";
            statusElement.classList.toggle("d-none", !message);
        };

        var showLoader = function (form) {
            if (!window.AppLoader || typeof window.AppLoader.show !== "function") {
                return;
            }

            window.AppLoader.show({
                message: form.dataset.appLoaderMessage || "Please wait while we secure your credentials.",
                status: form.dataset.appLoaderStatus || "Securing credentials"
            });
        };

        var resetLoader = function () {
            if (window.AppLoader && typeof window.AppLoader.reset === "function") {
                window.AppLoader.reset();
            }
        };

        var getCredentialEncryptionKey = function (keyUrl) {
            if (!credentialKeyPromise) {
                credentialKeyPromise = fetch(keyUrl, {
                    method: "GET",
                    credentials: "same-origin",
                    cache: "no-store",
                    headers: {
                        "Accept": "application/json",
                        "X-App-Loader": "off"
                    }
                }).then(function (response) {
                    if (!response.ok) {
                        throw new Error("Unable to initialize secure credential submission.");
                    }
                    return response.json();
                }).then(function (keyData) {
                    if (!keyData || keyData.algorithm !== "RSA-OAEP-256" || !keyData.publicKey) {
                        throw new Error("Invalid secure credential key received.");
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
                }).catch(function (error) {
                    credentialKeyPromise = null;
                    throw error;
                });
            }
            return credentialKeyPromise;
        };

        var encryptCredential = function (value, keyUrl) {
            if (!value) {
                return Promise.resolve("");
            }

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

        var preparePasswordInputs = function (form) {
            return Array.prototype.map.call(
                form.querySelectorAll('input[type="password"][name]'),
                function (input) {
                    var originalName = input.getAttribute("name");
                    var hiddenInput = document.createElement("input");
                    hiddenInput.type = "hidden";
                    hiddenInput.name = originalName;
                    hiddenInput.autocomplete = "off";
                    input.dataset.credentialFieldName = originalName;
                    input.removeAttribute("name");
                    input.insertAdjacentElement("afterend", hiddenInput);
                    return {
                        visibleInput: input,
                        hiddenInput: hiddenInput
                    };
                }
            );
        };

        encryptedForms.forEach(function (form) {
            var credentialFields = preparePasswordInputs(form);
            if (!credentialFields.length) {
                return;
            }

            form.addEventListener("submit", function (event) {
                event.preventDefault();

                if (form.dataset.credentialSubmitting === "true") {
                    return;
                }

                if (isInsecureTransport()) {
                    resetLoader();
                    setStatus(form, "HTTPS is required before submitting credentials.");
                    return;
                }

                if (!form.dataset.credentialKeyUrl) {
                    resetLoader();
                    setStatus(form, "Secure credential submission is unavailable. Please refresh and try again.");
                    return;
                }

                if (!form.reportValidity()) {
                    resetLoader();
                    return;
                }

                form.dataset.credentialSubmitting = "true";
                showLoader(form);

                Promise.all(credentialFields.map(function (field) {
                    return encryptCredential(field.visibleInput.value, form.dataset.credentialKeyUrl)
                        .then(function (encryptedValue) {
                            field.hiddenInput.value = encryptedValue;
                            field.visibleInput.value = "";
                        });
                })).then(function () {
                    setStatus(form, "");
                    window.HTMLFormElement.prototype.submit.call(form);
                }).catch(function (error) {
                    delete form.dataset.credentialSubmitting;
                    credentialFields.forEach(function (field) {
                        field.hiddenInput.value = "";
                    });
                    resetLoader();
                    setStatus(form, error.message || "Unable to secure credentials. Please refresh and try again.");
                });
            });
        });
    });
})();
