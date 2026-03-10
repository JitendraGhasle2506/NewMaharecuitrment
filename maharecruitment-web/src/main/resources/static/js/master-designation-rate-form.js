(function () {
    "use strict";

    const designationSelect = document.getElementById("designationSelect");
    const levelCodeSelect = document.getElementById("levelCodeSelect");
    const levelLoadMessage = document.getElementById("levelLoadMessage");

    if (!designationSelect || !levelCodeSelect) {
        return;
    }

    designationSelect.addEventListener("change", () => {
        void loadMappedLevels(designationSelect.value, "");
    });

    if (designationSelect.value) {
        void loadMappedLevels(designationSelect.value, levelCodeSelect.value);
    } else {
        resetLevelOptions();
    }

    async function loadMappedLevels(designationId, selectedLevelCode) {
        clearMessage();
        if (!designationId) {
            resetLevelOptions();
            return;
        }

        const baseUrl = designationSelect.dataset.levelsBaseUrl;
        if (!baseUrl) {
            return;
        }

        setLoadingState(true);
        try {
            const endpoint = `${baseUrl}/${encodeURIComponent(designationId)}/levels`;
            const response = await fetch(endpoint, { headers: { Accept: "application/json" } });
            if (!response.ok) {
                throw new Error(`Failed to fetch mapped levels. HTTP ${response.status}`);
            }

            const mappedLevels = await response.json();
            const levels = Array.isArray(mappedLevels) ? mappedLevels : [];
            renderMappedLevels(levels, selectedLevelCode);

            if (levels.length === 0) {
                setMessage("No levels are mapped to the selected designation.");
            }
        } catch (error) {
            console.error("Unable to load mapped levels for designation.", error);
            resetLevelOptions();
            setMessage("Unable to load levels for selected designation.");
        } finally {
            setLoadingState(false);
        }
    }

    function setLoadingState(isLoading) {
        levelCodeSelect.disabled = isLoading;
        if (isLoading) {
            resetLevelOptions("Loading levels...");
        }
    }

    function renderMappedLevels(levels, selectedLevelCode) {
        resetLevelOptions();
        levels.forEach((level) => {
            if (!level || !level.levelCode) {
                return;
            }
            appendLevelOption(level.levelCode, `${level.levelCode} - ${level.levelName || ""}`.trim());
        });

        if (selectedLevelCode) {
            levelCodeSelect.value = selectedLevelCode;
            if (levelCodeSelect.value !== selectedLevelCode) {
                levelCodeSelect.value = "";
            }
        }
    }

    function appendLevelOption(value, text) {
        const option = document.createElement("option");
        option.value = value;
        option.textContent = text;
        levelCodeSelect.appendChild(option);
    }

    function resetLevelOptions(placeholderText = "Select level") {
        levelCodeSelect.innerHTML = "";
        appendLevelOption("", placeholderText);
    }

    function setMessage(message) {
        if (levelLoadMessage) {
            levelLoadMessage.textContent = message || "";
        }
    }

    function clearMessage() {
        setMessage("");
    }
})();
