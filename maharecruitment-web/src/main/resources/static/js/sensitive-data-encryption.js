(function () {
    "use strict";

    var keyPromise;
    var cachedKeyUrl;
    var defaultKeyUrl = document.documentElement.dataset.sensitiveKeyUrl
        || document.querySelector('meta[name="sensitive-key-url"]')?.content;

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
        var fields = Array.from(form.querySelectorAll("[data-sensitive-encrypted-name]"))
            .filter(function (field) { return field.value; });
        if (!fields.length) {
            showError(form, "Sensitive identity information is required.");
            return;
        }
        form.dataset.sensitiveSubmitting = "true";
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
                    form.appendChild(input);
                });
                encryptedFields.forEach(function (item) { item.field.value = ""; });
                HTMLFormElement.prototype.submit.call(form);
            });
        }).catch(function () {
            delete form.dataset.sensitiveSubmitting;
            showError(form, "Unable to secure the submitted identity information. Please refresh and try again.");
        });
    });

    function showError(form, message) {
        var element = form.querySelector("[data-sensitive-encryption-status]");
        if (!element) {
            element = document.createElement("div");
            element.className = "alert alert-danger";
            element.dataset.sensitiveEncryptionStatus = "true";
            form.prepend(element);
        }
        element.textContent = message;
    }
}());
