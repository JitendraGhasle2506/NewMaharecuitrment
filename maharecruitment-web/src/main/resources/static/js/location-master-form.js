(function () {
    "use strict";

    const mapElement = document.getElementById("locationMap");
    const locationNameInput = document.getElementById("locationName");
    const latitudeInput = document.getElementById("latitude");
    const longitudeInput = document.getElementById("longitude");
    const radiusMetersInput = document.getElementById("radiusMeters");
    const findButton = document.getElementById("findLocationOnMap");
    const statusElement = document.getElementById("locationMapStatus");
    const selectedAddressElement = document.getElementById("locationSelectedAddress");
    const searchResultsElement = document.getElementById("locationSearchResults");

    if (!mapElement || !locationNameInput || !latitudeInput || !longitudeInput) {
        return;
    }

    const DEFAULT_CENTER = [19.7515, 75.7139];
    const DEFAULT_ZOOM = 6;
    const SELECTED_ZOOM = 16;
    const MIN_QUERY_LENGTH = 3;
    const SEARCH_DELAY_MS = 1000;
    const GEOCODE_LIMIT = 8;
    const MAHARASHTRA_VIEWBOX = "72.6,22.1,80.9,15.6";
    const DEFAULT_RADIUS_METERS = 100;
    const MIN_RADIUS_METERS = 1;
    const MAX_RADIUS_METERS = 10000;
    let marker = null;
    let coverageCircle = null;
    let lastMapSuggestedName = "";
    let searchTimeoutId = null;
    let geocodeAbortController = null;
    let reverseAbortController = null;

    if (!window.L) {
        setStatus("Map library could not be loaded.");
        return;
    }

    const initialLatitude = parseCoordinate(latitudeInput.value, -90, 90);
    const initialLongitude = parseCoordinate(longitudeInput.value, -180, 180);
    const hasInitialCoordinates = initialLatitude !== null && initialLongitude !== null;
    const map = window.L.map(mapElement, {
        scrollWheelZoom: false
    }).setView(
            hasInitialCoordinates ? [initialLatitude, initialLongitude] : DEFAULT_CENTER,
            hasInitialCoordinates ? SELECTED_ZOOM : DEFAULT_ZOOM);

    window.L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
        maxZoom: 19,
        attribution: "&copy; OpenStreetMap contributors"
    }).addTo(map);

    if (hasInitialCoordinates) {
        placeMarker(initialLatitude, initialLongitude, { updateFields: false, moveMap: false });
    }

    window.setTimeout(() => map.invalidateSize(), 150);

    map.on("click", (event) => {
        updateFromCoordinates(event.latlng.lat, event.latlng.lng, {
            moveMap: false,
            lookupAddress: true,
            updateLocationName: true,
            status: "Location selected on map."
        });
    });

    map.on("focus", () => {
        map.scrollWheelZoom.enable();
    });

    map.on("blur", () => {
        map.scrollWheelZoom.disable();
    });

    if (findButton) {
        findButton.addEventListener("click", () => {
            clearScheduledSearch();
            void searchLocation({ manual: true });
        });
    }

    locationNameInput.addEventListener("input", () => {
        scheduleSearch();
    });

    locationNameInput.addEventListener("keydown", (event) => {
        if (event.key === "Enter") {
            event.preventDefault();
            clearScheduledSearch();
            void searchLocation({ manual: true });
        }
    });

    latitudeInput.addEventListener("change", syncMapFromManualCoordinates);
    longitudeInput.addEventListener("change", syncMapFromManualCoordinates);
    if (radiusMetersInput) {
        radiusMetersInput.addEventListener("input", syncCoverageCircle);
        radiusMetersInput.addEventListener("change", syncCoverageCircle);
    }

    function scheduleSearch() {
        clearScheduledSearch();
        const query = locationNameInput.value.trim();
        if (query.length < MIN_QUERY_LENGTH) {
            hideSearchResults();
            setStatus("");
            return;
        }
        searchTimeoutId = window.setTimeout(() => {
            void searchLocation({ manual: false });
        }, SEARCH_DELAY_MS);
    }

    function clearScheduledSearch() {
        if (searchTimeoutId !== null) {
            window.clearTimeout(searchTimeoutId);
            searchTimeoutId = null;
        }
    }

    async function searchLocation(options) {
        const settings = Object.assign({ manual: false }, options);
        const query = locationNameInput.value.trim();
        if (query.length < MIN_QUERY_LENGTH) {
            setStatus("Enter at least 3 characters.");
            return;
        }

        abortGeocodeRequest();
        geocodeAbortController = new AbortController();
        setLoading(true);
        setStatus("Searching location...");

        try {
            const results = await collectSearchResults(query, geocodeAbortController.signal);
            if (results.length === 0) {
                hideSearchResults();
                setStatus("No map result found. Select on map or enter coordinates.");
                return;
            }

            renderSearchResults(results);
            selectSearchResult(results[0], {
                status: results.length > 1 ? "Best match selected." : "Location found.",
                focusResultList: settings.manual && results.length > 1
            });
        } catch (error) {
            if (error.name !== "AbortError") {
                console.error("Unable to search location.", error);
                setStatus("Unable to search location right now.");
            }
        } finally {
            setLoading(false);
        }
    }

    async function collectSearchResults(query, signal) {
        const queries = buildSearchQueries(query);
        const uniqueResults = new Map();

        for (let index = 0; index < queries.length; index += 1) {
            const currentResults = await fetchSearchResults(queries[index], signal);
            currentResults.forEach((result) => {
                const normalized = normalizeSearchResult(result);
                if (normalized) {
                    uniqueResults.set(normalized.key, normalized);
                }
            });

            if (uniqueResults.size > 0 && (index === 0 || uniqueResults.size >= 3)) {
                break;
            }
        }

        return Array.from(uniqueResults.values())
                .sort(compareSearchResults)
                .slice(0, GEOCODE_LIMIT);
    }

    async function fetchSearchResults(query, signal) {
        const endpoint = new URL("https://nominatim.openstreetmap.org/search");
        endpoint.searchParams.set("format", "jsonv2");
        endpoint.searchParams.set("limit", String(GEOCODE_LIMIT));
        endpoint.searchParams.set("addressdetails", "1");
        endpoint.searchParams.set("extratags", "1");
        endpoint.searchParams.set("namedetails", "1");
        endpoint.searchParams.set("dedupe", "1");
        endpoint.searchParams.set("countrycodes", "in");
        endpoint.searchParams.set("viewbox", MAHARASHTRA_VIEWBOX);
        endpoint.searchParams.set("accept-language", "en");
        endpoint.searchParams.set("q", query);

        const response = await fetch(endpoint, {
            headers: { Accept: "application/json" },
            signal
        });
        if (!response.ok) {
            throw new Error(`Location search failed with HTTP ${response.status}`);
        }

        const results = await response.json();
        return Array.isArray(results) ? results : [];
    }

    function buildSearchQueries(query) {
        const normalized = normalizeSearchQuery(query);
        const expanded = expandAddressAbbreviations(normalized);
        const withoutNear = removeNearWords(expanded);
        const parts = splitAddressParts(withoutNear);
        const localityParts = parts.filter(isMeaningfulAddressPart);
        const queries = [
            normalized,
            expanded,
            withoutNear,
            appendIndia(withoutNear),
            appendIndia(localityParts.join(", ")),
            appendIndia(buildRoadLocalityQuery(localityParts)),
            appendIndia(buildLandmarkLocalityQuery(localityParts)),
            appendIndia(localityParts.slice(-4).join(", ")),
            appendIndia(localityParts.slice(-3).join(", "))
        ];

        return Array.from(new Set(queries.map(normalizeSearchQuery).filter((value) => value.length >= MIN_QUERY_LENGTH)));
    }

    function normalizeSearchQuery(value) {
        return String(value || "")
                .replace(/[|;]/g, ",")
                .replace(/\s*,\s*/g, ", ")
                .replace(/\s+/g, " ")
                .replace(/,+/g, ",")
                .replace(/^,\s*|\s*,$/g, "")
                .trim();
    }

    function expandAddressAbbreviations(value) {
        return value
                .replace(/\bRd\.?\b/gi, "Road")
                .replace(/\bStn\.?\b/gi, "Station")
                .replace(/\bNr\.?\b/gi, "near")
                .replace(/\bW\b/g, "West")
                .replace(/\bE\b/g, "East");
    }

    function removeNearWords(value) {
        return value
                .replace(/\bnear\b/gi, "")
                .replace(/\bopp\.?\b/gi, "")
                .replace(/\bopposite\b/gi, "")
                .replace(/\s+/g, " ")
                .replace(/\s*,\s*/g, ", ")
                .trim();
    }

    function splitAddressParts(value) {
        return normalizeSearchQuery(value)
                .split(",")
                .map((part) => part.trim())
                .filter(Boolean);
    }

    function isMeaningfulAddressPart(value) {
        return value.length > 2 && !/^(near|opp|opposite)$/i.test(value);
    }

    function appendIndia(value) {
        const normalized = normalizeSearchQuery(value);
        if (!normalized) {
            return "";
        }
        if (/\bindia\b/i.test(normalized)) {
            return normalized;
        }
        return `${normalized}, India`;
    }

    function buildRoadLocalityQuery(parts) {
        if (parts.length === 0) {
            return "";
        }
        const road = parts.find((part) => /\b(road|marg|street|lane|nagar)\b/i.test(part)) || parts[0];
        const localities = parts.filter((part) => part !== road).slice(-4);
        return [road].concat(localities).join(", ");
    }

    function buildLandmarkLocalityQuery(parts) {
        const landmark = parts.find((part) => /\b(station|depot|hospital|school|college|market|mandir|temple)\b/i.test(part));
        if (!landmark) {
            return "";
        }
        const localities = parts.filter((part) => part !== landmark).slice(-4);
        return [landmark].concat(localities).join(", ");
    }

    function normalizeSearchResult(result) {
        const latitude = parseCoordinate(result.lat, -90, 90);
        const longitude = parseCoordinate(result.lon, -180, 180);
        if (latitude === null || longitude === null) {
            return null;
        }

        const displayName = result.display_name || "";
        return Object.assign({}, result, {
            key: result.osm_type && result.osm_id
                    ? `${result.osm_type}:${result.osm_id}`
                    : `${latitude.toFixed(6)}:${longitude.toFixed(6)}:${displayName}`,
            parsedLatitude: latitude,
            parsedLongitude: longitude,
            display_name: displayName
        });
    }

    function compareSearchResults(left, right) {
        const leftScore = Number(left.importance || 0);
        const rightScore = Number(right.importance || 0);
        return rightScore - leftScore;
    }

    function renderSearchResults(results) {
        if (!searchResultsElement) {
            return;
        }
        searchResultsElement.innerHTML = "";
        results.forEach((result, index) => {
            const button = document.createElement("button");
            button.type = "button";
            button.className = "location-search-result";
            button.setAttribute("role", "option");
            button.dataset.index = String(index);

            const title = document.createElement("span");
            title.className = "location-search-result-title";
            title.textContent = buildResultTitle(result);

            const meta = document.createElement("span");
            meta.className = "location-search-result-meta";
            meta.textContent = result.display_name || "";

            button.appendChild(title);
            button.appendChild(meta);
            button.addEventListener("click", () => {
                selectSearchResult(result, { status: "Location selected." });
            });
            searchResultsElement.appendChild(button);
        });
        searchResultsElement.classList.remove("d-none");
    }

    function hideSearchResults() {
        if (searchResultsElement) {
            searchResultsElement.innerHTML = "";
            searchResultsElement.classList.add("d-none");
        }
    }

    function selectSearchResult(result, options) {
        const settings = Object.assign({
            status: "Location selected.",
            focusResultList: false
        }, options);
        selectedAddressElement.textContent = result.display_name || "";
        setLocationNameFromResult(result);
        updateFromCoordinates(result.parsedLatitude, result.parsedLongitude, {
            moveMap: true,
            lookupAddress: false,
            status: settings.status
        });

        if (settings.focusResultList && searchResultsElement) {
            const firstResult = searchResultsElement.querySelector(".location-search-result");
            if (firstResult) {
                firstResult.focus({ preventScroll: true });
            }
        }
    }

    function buildResultTitle(result) {
        const address = result.address || {};
        const title = firstPresent(
                result.name,
                result.namedetails && result.namedetails.name,
                address.road,
                address.suburb,
                address.neighbourhood,
                address.city,
                address.town,
                address.village,
                result.display_name);
        return sanitizeLocationNamePart(title).slice(0, 90) || "Search result";
    }

    async function reverseGeocode(latitude, longitude, options) {
        const settings = Object.assign({ updateLocationName: false }, options);
        abortReverseRequest();
        reverseAbortController = new AbortController();
        try {
            const endpoint = new URL("https://nominatim.openstreetmap.org/reverse");
            endpoint.searchParams.set("format", "jsonv2");
            endpoint.searchParams.set("lat", latitude.toFixed(7));
            endpoint.searchParams.set("lon", longitude.toFixed(7));
            endpoint.searchParams.set("accept-language", "en");

            const response = await fetch(endpoint, {
                headers: { Accept: "application/json" },
                signal: reverseAbortController.signal
            });
            if (!response.ok) {
                throw new Error(`Reverse geocode failed with HTTP ${response.status}`);
            }

            const result = await response.json();
            const displayName = result && result.display_name ? result.display_name : "";
            selectedAddressElement.textContent = displayName;
            applySuggestedLocationName(buildLocationNameFromDisplayName(displayName) || buildSuggestedLocationName(result), {
                force: settings.updateLocationName
            });
        } catch (error) {
            if (error.name !== "AbortError") {
                console.error("Unable to identify selected map location.", error);
                selectedAddressElement.textContent = "";
            }
        }
    }

    function updateFromCoordinates(latitude, longitude, options) {
        const settings = Object.assign({
            moveMap: true,
            lookupAddress: false,
            updateLocationName: false,
            status: ""
        }, options);

        placeMarker(latitude, longitude, {
            updateFields: true,
            moveMap: settings.moveMap
        });
        setStatus(settings.status);

        if (settings.lookupAddress) {
            void reverseGeocode(latitude, longitude, {
                updateLocationName: settings.updateLocationName
            });
        }
    }

    function placeMarker(latitude, longitude, options) {
        const settings = Object.assign({
            updateFields: true,
            moveMap: true
        }, options);
        const latLng = [latitude, longitude];

        if (!marker) {
            marker = window.L.marker(latLng, { draggable: true }).addTo(map);
            marker.on("dragend", () => {
                const position = marker.getLatLng();
                updateFromCoordinates(position.lat, position.lng, {
                    moveMap: false,
                    lookupAddress: true,
                    updateLocationName: true,
                    status: "Location marker moved."
                });
            });
        } else {
            marker.setLatLng(latLng);
        }

        if (settings.updateFields) {
            latitudeInput.value = formatCoordinate(latitude);
            longitudeInput.value = formatCoordinate(longitude);
        }
        syncCoverageCircle();
        if (settings.moveMap) {
            map.setView(latLng, Math.max(map.getZoom(), SELECTED_ZOOM));
        }
    }

    function syncCoverageCircle() {
        if (!marker) {
            return;
        }
        const radiusMeters = parseRadiusMeters();
        if (radiusMeters === null) {
            if (coverageCircle) {
                coverageCircle.remove();
                coverageCircle = null;
            }
            return;
        }

        const latLng = marker.getLatLng();
        if (!coverageCircle) {
            coverageCircle = window.L.circle(latLng, {
                radius: radiusMeters,
                color: "#0d6efd",
                weight: 2,
                opacity: 0.9,
                fillColor: "#0d6efd",
                fillOpacity: 0.12
            }).addTo(map);
            return;
        }

        coverageCircle.setLatLng(latLng);
        coverageCircle.setRadius(radiusMeters);
    }

    function syncMapFromManualCoordinates() {
        const latitude = parseCoordinate(latitudeInput.value, -90, 90);
        const longitude = parseCoordinate(longitudeInput.value, -180, 180);
        if (latitude === null || longitude === null) {
            return;
        }
        placeMarker(latitude, longitude, { updateFields: false, moveMap: true });
        hideSearchResults();
        setStatus("Map updated from coordinates.");
    }

    function setLocationNameFromResult(result) {
        const selectedName = buildLocationNameFromDisplayName(result.display_name)
                || buildSuggestedLocationName(result)
                || buildResultTitle(result);
        applySuggestedLocationName(selectedName, { force: true });
    }

    function applySuggestedLocationName(displayName, options) {
        const settings = Object.assign({ force: false }, options);
        if (!displayName) {
            return;
        }

        const currentName = locationNameInput.value.trim();
        if (settings.force || !currentName || currentName === lastMapSuggestedName) {
            locationNameInput.value = displayName.slice(0, 150);
            lastMapSuggestedName = locationNameInput.value;
        }
    }

    function buildLocationNameFromDisplayName(displayName) {
        return sanitizeLocationNamePart(displayName).slice(0, 150);
    }

    function buildSuggestedLocationName(result) {
        if (!result) {
            return "";
        }

        const address = result.address || {};
        const primaryName = firstPresent(
                result.name,
                address.amenity,
                address.building,
                address.office,
                address.shop,
                address.road,
                address.suburb,
                address.neighbourhood,
                address.village,
                address.town,
                address.city,
                address.county,
                address.state);
        const secondaryName = firstPresent(
                address.city,
                address.town,
                address.village,
                address.county);
        const parts = [primaryName, secondaryName, address.state]
                .map(sanitizeLocationNamePart)
                .filter(Boolean)
                .filter((value, index, values) => values.indexOf(value) === index);

        return parts.join(", ").slice(0, 150);
    }

    function firstPresent() {
        return Array.from(arguments).find((value) => value && String(value).trim()) || "";
    }

    function sanitizeLocationNamePart(value) {
        return String(value || "")
                .replace(/[^A-Za-z0-9\s\-/().,]/g, " ")
                .replace(/\s+/g, " ")
                .trim();
    }

    function parseCoordinate(value, minimum, maximum) {
        if (value === null || value === undefined || String(value).trim() === "") {
            return null;
        }
        const parsed = Number(value);
        if (!Number.isFinite(parsed) || parsed < minimum || parsed > maximum) {
            return null;
        }
        return parsed;
    }

    function parseRadiusMeters() {
        if (!radiusMetersInput) {
            return DEFAULT_RADIUS_METERS;
        }
        const rawValue = String(radiusMetersInput.value || "").trim();
        if (!rawValue) {
            return null;
        }
        const parsed = Number(rawValue);
        if (!Number.isInteger(parsed) || parsed < MIN_RADIUS_METERS || parsed > MAX_RADIUS_METERS) {
            return null;
        }
        return parsed;
    }

    function formatCoordinate(value) {
        const formatted = Number(value).toFixed(7);
        return formatted === "-0.0000000" ? "0.0000000" : formatted;
    }

    function setStatus(message) {
        if (statusElement) {
            statusElement.textContent = message || "";
        }
    }

    function setLoading(isLoading) {
        if (!findButton) {
            return;
        }
        findButton.disabled = isLoading;
        findButton.classList.toggle("disabled", isLoading);
    }

    function abortGeocodeRequest() {
        if (geocodeAbortController) {
            geocodeAbortController.abort();
            geocodeAbortController = null;
        }
    }

    function abortReverseRequest() {
        if (reverseAbortController) {
            reverseAbortController.abort();
            reverseAbortController = null;
        }
    }
})();
