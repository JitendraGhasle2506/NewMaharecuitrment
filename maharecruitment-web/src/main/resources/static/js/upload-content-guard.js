(function () {
    "use strict";

    var MAX_CLIENT_INSPECTION_BYTES = 8 * 1024 * 1024;
    var MALICIOUS_FILE_MESSAGE = "Selected file is malicious because it contains script or active content.";
    var SCRIPT_PATTERNS = [
        /<\s*\/?\s*script\b/i,
        /\bjavascript\s*:/i,
        /\bvbscript\s*:/i,
        /\bdata\s*:\s*text\/html/i,
        /<\s*(iframe|object|embed|applet|svg|meta|link)\b/i,
        /\bon(load|error|click|mouseover|focus|submit|readystatechange)\s*=/i,
        /\beval\s*\(/i,
        /\bdocument\s*\.\s*(cookie|write)/i,
        /\bwindow\s*\.\s*location/i
    ];
    var PDF_ACTIVE_PATTERNS = [
        /\/(JavaScript|JS|OpenAction|AA|Launch|RichMedia|EmbeddedFile)\b/i
    ];
    var OFFICE_ACTIVE_PATTERNS = [
        /\b(vbaproject|vba project|macros?|activex|oleobject)\b/i,
        /attribute\s+vb_name/i,
        /(^|\/)vbaproject\.bin/i,
        /(^|\/)activex\//i,
        /(^|\/)embeddings\//i
    ];

    var decodeBytes = function (bytes, encoding) {
        if (!window.TextDecoder) {
            return "";
        }

        try {
            return new window.TextDecoder(encoding, { fatal: false }).decode(bytes).replace(/\u0000/g, "");
        } catch (error) {
            return "";
        }
    };

    var hasPattern = function (text, patterns) {
        return patterns.some(function (pattern) {
            return pattern.test(text);
        });
    };

    var extensionOf = function (fileName) {
        var dotIndex = fileName.lastIndexOf(".");
        return dotIndex >= 0 ? fileName.substring(dotIndex + 1).toLowerCase() : "";
    };

    var isSuspicious = function (file, bytes) {
        var latinText = decodeBytes(bytes, "iso-8859-1");
        var utf8Text = decodeBytes(bytes, "utf-8");
        var utf16Text = decodeBytes(bytes, "utf-16le");
        var combinedText = [latinText, utf8Text, utf16Text].join("\n");
        var extension = extensionOf(file.name || "");

        if (hasPattern(combinedText, SCRIPT_PATTERNS)) {
            return true;
        }
        if (extension === "pdf" && hasPattern(combinedText, PDF_ACTIVE_PATTERNS)) {
            return true;
        }
        return (extension === "doc" || extension === "docx") && hasPattern(combinedText, OFFICE_ACTIVE_PATTERNS);
    };

    var inspectFile = function (file) {
        if (!file || file.size > MAX_CLIENT_INSPECTION_BYTES) {
            return Promise.resolve(false);
        }

        return file.arrayBuffer()
            .then(function (buffer) {
                return isSuspicious(file, new Uint8Array(buffer));
            })
            .catch(function () {
                return false;
            });
    };

    var clearFileInput = function (input) {
        input.value = "";
        input.setCustomValidity(MALICIOUS_FILE_MESSAGE);
        input.dispatchEvent(new Event("input", { bubbles: true }));
    };

    document.addEventListener("change", function (event) {
        var input = event.target;
        if (!input || input.type !== "file" || !input.files || input.files.length === 0) {
            return;
        }

        input.setCustomValidity("");
        Promise.all(Array.prototype.map.call(input.files, inspectFile))
            .then(function (results) {
                if (!results.some(Boolean)) {
                    input.setCustomValidity("");
                    return;
                }

                clearFileInput(input);
                window.alert(MALICIOUS_FILE_MESSAGE);
            });
    }, true);
})();
