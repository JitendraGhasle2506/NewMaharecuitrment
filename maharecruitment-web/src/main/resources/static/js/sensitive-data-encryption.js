(function () {
    "use strict";

    var keyPromise;
    var cachedKeyUrl;
    var defaultKeyUrl = document.documentElement.dataset.sensitiveKeyUrl
        || document.querySelector('meta[name="sensitive-key-url"]')?.content;
    var fieldIdByServerName = {
        gstNumberEncrypted: "gstNo",
        panNumberEncrypted: "panNo",
        tanNumberEncrypted: "tanNo",
        isTermsConditionAccepted: "agreeCheckbox"
    };

    function decode(value) {
        var raw = atob(value), bytes = new Uint8Array(raw.length);
        for (var i = 0; i < raw.length; i += 1) bytes[i] = raw.charCodeAt(i);
        return bytes;
    }

    function encode(buffer) {
        var bytes = new Uint8Array(buffer), result = "";
        for (var i = 0; i < bytes.length; i += 1) result += String.fromCharCode(bytes[i]);
        return btoa(result);
    }

    function nonce() {
        var bytes = crypto.getRandomValues(new Uint8Array(24));
        return encode(bytes).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
    }

    function getKey(keyUrl) {
        if (!keyUrl) return Promise.reject(new Error("Secure identity submission is unavailable."));
        if (!keyPromise || cachedKeyUrl !== keyUrl) {
            cachedKeyUrl = keyUrl;
            keyPromise = fetch(keyUrl, { credentials: "same-origin", cache: "no-store" })
            .then(function (response) { if (!response.ok) throw new Error(); return response.json(); })
            .then(function (data) {
                if (!data || data.algorithm !== "RSA-OAEP-256" || !data.publicKey || !data.keyId
                        || !Number.isFinite(data.serverTime)) throw new Error();
                var clockOffset = data.serverTime - Date.now();
                return crypto.subtle.importKey("spki", decode(data.publicKey),
                    { name: "RSA-OAEP", hash: "SHA-256" }, false, ["encrypt"])
                    .then(function (key) { return { key: key, metadata: data, clockOffset: clockOffset }; });
            }).catch(function (error) {
                keyPromise = null;
                cachedKeyUrl = null;
                throw error;
            });
        }
        return keyPromise;
    }

    function warmEncryptionKey() {
        var form = document.querySelector('form[data-encrypt-sensitive="true"]');
        if (!form) return;
        var keyUrl = form.dataset.sensitiveKeyUrl || defaultKeyUrl;
        if (keyUrl) getKey(keyUrl).catch(function () { /* Retry during submission. */ });
    }

    if (typeof window.requestIdleCallback === "function") {
        window.requestIdleCallback(warmEncryptionKey, { timeout: 2000 });
    } else {
        window.setTimeout(warmEncryptionKey, 0);
    }

    function createEncryptedFormData(form) {
        if (!(form instanceof HTMLFormElement)) {
            return Promise.reject(new Error("A valid form is required for secure submission."));
        }
        if (location.protocol !== "https:"
                && !["localhost", "127.0.0.1", "::1", "[::1]"].includes(location.hostname)) {
            return Promise.reject(new Error("HTTPS is required before submitting identity information."));
        }

        var sensitiveFields = Array.from(form.querySelectorAll("[data-sensitive-encrypted-name]"));
        var fields = sensitiveFields.filter(function (field) { return field.value; });
        if (!fields.length) {
            if (form.dataset.sensitiveValuesRequired !== "false") {
                return Promise.reject(new Error("Sensitive identity information is required."));
            }
            return Promise.resolve(new FormData(form));
        }

        var keyUrl = form.dataset.sensitiveKeyUrl || defaultKeyUrl;
        return getKey(keyUrl).then(function (published) {
            var requestTimestamp = Date.now() + published.clockOffset;
            var requestNonce = nonce();
            var requestPurpose = form.dataset.sensitivePurpose || "DEPARTMENT_REGISTRATION";
            return Promise.all(fields.map(function (field) {
                var encryptedName = field.dataset.sensitiveEncryptedName;
                var logicalName = field.dataset.sensitiveField;
                if (!encryptedName || !logicalName) throw new Error("Sensitive field configuration is invalid.");
                var envelope = [
                    "SENSITIVE:v1",
                    published.metadata.keyId,
                    String(requestTimestamp),
                    requestNonce,
                    requestPurpose,
                    logicalName,
                    field.value
                ].join("\n");
                return crypto.subtle.encrypt({ name: "RSA-OAEP" }, published.key,
                    new TextEncoder().encode(envelope)).then(function (encrypted) {
                        return {
                            name: encryptedName,
                            value: (published.metadata.encryptedPrefix || "ENC:v1:") + encode(encrypted)
                        };
                    });
            })).then(function (encryptedFields) {
                var formData = new FormData(form);
                sensitiveFields.forEach(function (field) {
                    if (field.name) formData.delete(field.name);
                });
                encryptedFields.concat([
                    { name: "encryptionKeyId", value: published.metadata.keyId },
                    { name: "timestamp", value: requestTimestamp },
                    { name: "nonce", value: requestNonce }
                ]).forEach(function (item) {
                    formData.append(item.name, String(item.value));
                });
                return formData;
            });
        });
    }

    window.SensitiveDataEncryption = Object.freeze({
        createEncryptedFormData: createEncryptedFormData
    });

    document.addEventListener("submit", function (event) {
        var form = event.target;
        if (!(form instanceof HTMLFormElement) || form.dataset.encryptSensitive !== "true") return;
        if (form.dataset.sensitiveSubmitting === "true") {
            event.preventDefault();
            return;
        }
        if (event.defaultPrevented) return;
        event.preventDefault();
        if (!form.reportValidity()) return;
        if (location.protocol !== "https:"
                && !["localhost", "127.0.0.1", "::1", "[::1]"].includes(location.hostname)) {
            showError(form, "HTTPS is required before submitting identity information.");
            return;
        }
        var keyUrl = form.dataset.sensitiveKeyUrl || defaultKeyUrl;
        var sensitiveFields = Array.from(form.querySelectorAll("[data-sensitive-encrypted-name]"));
        var fields = sensitiveFields
            .filter(function (field) { return field.value; });
        if (!fields.length) {
            if (form.dataset.sensitiveValuesRequired !== "false") {
                showError(form, "Sensitive identity information is required.");
                return;
            }
            form.dataset.sensitiveSubmitting = "true";
            clearValidationErrors(form);
            setSubmitting(form, true);
            disableSensitiveFields(sensitiveFields);
            if (form.dataset.preserveOnValidationError === "true") {
                submitWithoutNavigation(form).catch(function (error) {
                    showError(form, error && error.message
                        ? error.message
                        : "Unable to submit the form securely. Please refresh and try again.");
                });
                return;
            }
            HTMLFormElement.prototype.submit.call(form);
            return;
        }
        form.dataset.sensitiveSubmitting = "true";
        clearValidationErrors(form);
        setSubmitting(form, true);
        getKey(keyUrl).then(function (published) {
            var requestTimestamp = Date.now() + published.clockOffset;
            var requestNonce = nonce();
            var requestPurpose = form.dataset.sensitivePurpose || "DEPARTMENT_REGISTRATION";
            return Promise.all(fields.map(function (field) {
                var encryptedName = field.dataset.sensitiveEncryptedName;
                var logicalName = field.dataset.sensitiveField;
                if (!encryptedName || !logicalName) throw new Error();
                var envelope = [
                    "SENSITIVE:v1",
                    published.metadata.keyId,
                    String(requestTimestamp),
                    requestNonce,
                    requestPurpose,
                    logicalName,
                    field.value
                ].join("\n");
                return crypto.subtle.encrypt({ name: "RSA-OAEP" }, published.key,
                    new TextEncoder().encode(envelope)).then(function (encrypted) {
                        return {
                            field: field,
                            name: encryptedName,
                            value: (published.metadata.encryptedPrefix || "ENC:v1:") + encode(encrypted)
                        };
                    });
            })).then(function (encryptedFields) {
                encryptedFields.concat([
                    { name: "encryptionKeyId", value: published.metadata.keyId },
                    { name: "timestamp", value: requestTimestamp },
                    { name: "nonce", value: requestNonce }
                ]).forEach(function (item) {
                    var input = document.createElement("input");
                    input.type = "hidden"; input.name = item.name; input.value = item.value;
                    input.autocomplete = "off";
                    input.dataset.sensitiveGenerated = "true";
                    form.appendChild(input);
                });
                // Disabled controls are omitted from both FormData and native form
                // serialization, ensuring clear-text values never enter the request.
                disableSensitiveFields(sensitiveFields);
                if (form.dataset.preserveOnValidationError === "true") {
                    return submitWithoutNavigation(form);
                }
                HTMLFormElement.prototype.submit.call(form);
            });
        }).catch(function (error) {
            clearGeneratedFields(form);
            restoreSensitiveFields(form);
            delete form.dataset.sensitiveSubmitting;
            setSubmitting(form, false);
            showError(form, error && error.message
                ? error.message
                : "Unable to secure the submitted identity information. Please refresh and try again.");
        });
    });

    function submitWithoutNavigation(form) {
        return fetch(form.action, {
            method: (form.method || "POST").toUpperCase(),
            body: new FormData(form),
            credentials: "same-origin",
            redirect: "follow",
            headers: {
                "Accept": "application/json",
                "X-Requested-With": "XMLHttpRequest"
            }
        }).then(function (response) {
            if (response.redirected) {
                window.location.assign(response.url);
                return;
            }
            return response.json().catch(function () { return null; }).then(function (result) {
                if (response.ok && result && result.success && result.redirectUrl) {
                    window.location.assign(result.redirectUrl);
                    return;
                }
                if (!renderValidationErrors(form, result)) {
                    throw new Error(response.ok
                        ? "Unable to complete registration. Please review the form and try again."
                        : "Unable to complete registration at this time. Please try again.");
                }
            });
        }).finally(function () {
            clearGeneratedFields(form);
            restoreSensitiveFields(form);
            delete form.dataset.sensitiveSubmitting;
            setSubmitting(form, false);
        });
    }

    function renderValidationErrors(form, result) {
        if (!result || typeof result !== "object") return false;
        var firstInvalidField = null;
        Object.entries(result.fieldErrors || {}).forEach(function (entry) {
            var fieldName = entry[0];
            var errorMessage = entry[1];
            var fieldId = fieldIdByServerName[fieldName] || fieldName;
            var candidate = document.getElementById(fieldId);
            var field = candidate && form.contains(candidate) ? candidate : null;
            if (!field || !errorMessage) return;

            var error = document.createElement("div");
            error.className = "text-danger small registration-async-error";
            error.dataset.clientFieldError = fieldName;
            error.textContent = String(errorMessage);
            var anchor = field.closest(".verification-input-group") || field;
            anchor.insertAdjacentElement("afterend", error);
            field.setAttribute("aria-invalid", "true");
            if (!firstInvalidField) firstInvalidField = field;
        });

        var globalMessages = Array.isArray(result.globalErrors)
            ? result.globalErrors.filter(Boolean)
            : [];
        if (globalMessages.length) {
            showError(form, Array.from(new Set(globalMessages)).join(" "));
        } else {
            showNotice(form, "Please correct the highlighted errors. Your entered values and selected PDF documents have been kept.");
        }

        if (firstInvalidField) {
            firstInvalidField.focus({ preventScroll: true });
            firstInvalidField.scrollIntoView({ behavior: "smooth", block: "center" });
        }
        return true;
    }

    function clearGeneratedFields(form) {
        form.querySelectorAll('[data-sensitive-generated="true"]').forEach(function (field) {
            field.remove();
        });
    }

    function disableSensitiveFields(fields) {
        fields.forEach(function (field) {
            if (!field.disabled) {
                field.disabled = true;
                field.dataset.sensitiveDisabledForSubmission = "true";
            }
        });
    }

    function restoreSensitiveFields(form) {
        form.querySelectorAll('[data-sensitive-disabled-for-submission="true"]').forEach(function (field) {
            field.disabled = false;
            delete field.dataset.sensitiveDisabledForSubmission;
        });
    }

    function clearValidationErrors(form) {
        form.querySelectorAll(".registration-async-error").forEach(function (error) { error.remove(); });
        form.querySelectorAll('[aria-invalid="true"]').forEach(function (field) {
            field.removeAttribute("aria-invalid");
        });
        var status = form.querySelector("[data-sensitive-encryption-status]");
        if (status) {
            status.textContent = "";
            status.classList.add("d-none");
        }
    }

    function setSubmitting(form, submitting) {
        var button = form.querySelector('[type="submit"]');
        if (!button) return;
        if (submitting) {
            button.dataset.originalLabel = button.textContent;
            button.textContent = "Submitting...";
            button.disabled = true;
            return;
        }
        button.textContent = button.dataset.originalLabel || "Submit Registration";
        delete button.dataset.originalLabel;
        button.disabled = false;
    }

    function showNotice(form, message) {
        var element = getStatusElement(form);
        element.className = "alert alert-warning";
        element.textContent = message;
        element.classList.remove("d-none");
    }

    function showError(form, message) {
        var element = getStatusElement(form);
        element.className = "alert alert-danger";
        element.textContent = message;
        element.classList.remove("d-none");
    }

    function getStatusElement(form) {
        var element = form.querySelector("[data-sensitive-encryption-status]");
        if (!element) {
            element = document.createElement("div");
            element.dataset.sensitiveEncryptionStatus = "true";
            form.prepend(element);
        }
        return element;
    }
}());
