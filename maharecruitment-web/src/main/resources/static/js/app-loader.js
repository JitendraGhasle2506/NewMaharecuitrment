(function () {
    const DEFAULT_MESSAGE = "Please wait while we prepare your screen.";
    const DOWNLOAD_MESSAGE = "Please wait while we prepare your report download.";
    const MIN_VISIBLE_MS = 350;
    const DOWNLOAD_HIDE_MS = 4000;

    let overlay;
    let messageNode;
    let statusNode;
    let visibleSince = 0;
    let hideTimerId = null;
    let activeAsyncRequests = 0;

    const getContextPath = () => {
        const meta = document.querySelector('meta[name="app-context-path"]');
        const value = meta?.getAttribute("content")?.trim();
        return value ? value.replace(/\/+$/, "") : "";
    };

    const toAbsolutePath = (path) => {
        const contextPath = getContextPath();
        if (!path) {
            return "";
        }
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        return `${contextPath}${path.startsWith("/") ? path : `/${path}`}`;
    };

    const createOverlay = () => {
        if (overlay) {
            return overlay;
        }

        overlay = document.createElement("div");
        overlay.className = "app-loader-overlay";
        overlay.setAttribute("aria-hidden", "true");
        overlay.innerHTML = `
            <div class="app-loader-card" role="status" aria-live="polite" aria-atomic="true">
                <div class="app-loader-body">
                    <div class="app-loader-brand">
                        <div class="app-loader-badge">
                            <img class="app-loader-logo" src="${toAbsolutePath("/img/mahait_logo.png")}" alt="MahaIT">
                        </div>
                        <div class="app-loader-copy">
                            <span class="app-loader-kicker">MahaIT Recruitment</span>
                            <h2 class="app-loader-title">Loading Workspace</h2>
                            <p class="app-loader-message">${DEFAULT_MESSAGE}</p>
                        </div>
                    </div>
                    <div class="app-loader-progress" aria-hidden="true"></div>
                    <div class="app-loader-status">
                        <div class="app-loader-points" aria-hidden="true">
                            <span class="app-loader-point"></span>
                            <span class="app-loader-point"></span>
                            <span class="app-loader-point"></span>
                        </div>
                        <strong class="app-loader-status-text">Securing session</strong>
                    </div>
                </div>
            </div>
        `;

        document.body.appendChild(overlay);
        messageNode = overlay.querySelector(".app-loader-message");
        statusNode = overlay.querySelector(".app-loader-status-text");
        return overlay;
    };

    const clearHideTimer = () => {
        if (hideTimerId) {
            window.clearTimeout(hideTimerId);
            hideTimerId = null;
        }
    };

    const scheduleHide = (delayMs) => {
        clearHideTimer();
        hideTimerId = window.setTimeout(() => {
            if (!overlay) {
                return;
            }
            overlay.classList.remove("is-visible");
            overlay.setAttribute("aria-hidden", "true");
            document.body.classList.remove("app-loader-active");
        }, Math.max(delayMs, 0));
    };

    const show = (options = {}) => {
        const root = createOverlay();
        clearHideTimer();

        const message = options.message || DEFAULT_MESSAGE;
        const status = options.status || "Securing session";
        if (messageNode) {
            messageNode.textContent = message;
        }
        if (statusNode) {
            statusNode.textContent = status;
        }

        root.classList.add("is-visible");
        root.setAttribute("aria-hidden", "false");
        document.body.classList.add("app-loader-active");
        visibleSince = Date.now();
    };

    const hide = (options = {}) => {
        if (!overlay) {
            return;
        }

        const elapsed = Date.now() - visibleSince;
        const delay = options.immediate ? 0 : Math.max(MIN_VISIBLE_MS - elapsed, 0);
        scheduleHide(delay);
    };

    const showForDownload = (message) => {
        show({
            message: message || DOWNLOAD_MESSAGE,
            status: "Preparing download"
        });
        scheduleHide(DOWNLOAD_HIDE_MS);
    };

    const isInternalUrl = (url) => {
        try {
            const parsed = new URL(url, window.location.origin);
            return parsed.origin === window.location.origin;
        } catch (error) {
            return false;
        }
    };

    const shouldIgnoreLink = (link) => {
        const href = link.getAttribute("href");
        return !href
            || href === "#"
            || href.startsWith("javascript:")
            || link.hasAttribute("download")
            || link.dataset.appLoader === "off"
            || link.target === "_blank";
    };

    const wireNavigationLoader = () => {
        document.addEventListener("click", (event) => {
            const link = event.target.closest("a[href]");
            if (!link || shouldIgnoreLink(link)) {
                return;
            }

            if (event.defaultPrevented || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) {
                return;
            }

            const href = link.href;
            if (!isInternalUrl(href)) {
                return;
            }

            if (new URL(href, window.location.origin).pathname === window.location.pathname
                && new URL(href, window.location.origin).search === window.location.search
                && !new URL(href, window.location.origin).hash) {
                return;
            }

            show({
                message: link.dataset.appLoaderMessage || DEFAULT_MESSAGE,
                status: link.dataset.appLoaderStatus || "Opening page"
            });
        }, true);
    };

    const wireFormLoader = () => {
        document.addEventListener("submit", (event) => {
            const form = event.target;
            if (!(form instanceof HTMLFormElement) || form.dataset.appLoader === "off") {
                return;
            }

            const submitter = event.submitter;
            const behavior = submitter?.dataset.appLoaderBehavior || form.dataset.appLoaderBehavior || "";
            const message = submitter?.dataset.appLoaderMessage || form.dataset.appLoaderMessage || DEFAULT_MESSAGE;
            const status = submitter?.dataset.appLoaderStatus || form.dataset.appLoaderStatus || "Validating request";

            if (behavior === "download") {
                showForDownload(message);
                return;
            }

            show({ message, status });
        }, true);
    };

    const wireFetchLoader = () => {
        if (typeof window.fetch !== "function") {
            return;
        }

        const nativeFetch = window.fetch.bind(window);
        window.fetch = function (...args) {
            const init = args[1] || {};
            const disabled = init.headers && (
                init.headers["X-App-Loader"] === "off"
                || init.headers["x-app-loader"] === "off"
            );

            if (!disabled) {
                activeAsyncRequests += 1;
                if (activeAsyncRequests === 1) {
                    show({
                        message: "Please wait while we update your data.",
                        status: "Processing request"
                    });
                }
            }

            return nativeFetch(...args)
                .finally(() => {
                    if (disabled) {
                        return;
                    }
                    activeAsyncRequests = Math.max(activeAsyncRequests - 1, 0);
                    if (activeAsyncRequests === 0) {
                        hide();
                    }
                });
        };
    };

    document.addEventListener("DOMContentLoaded", () => {
        createOverlay();
        wireNavigationLoader();
        wireFormLoader();
        wireFetchLoader();
    });

    window.AppLoader = {
        show,
        hide,
        showForDownload
    };
})();
