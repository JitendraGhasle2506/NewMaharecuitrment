(function () {
    const notificationRoot = document.getElementById("notificationRoot");
    const bell = document.getElementById("notificationBell");
    const dropdown = document.getElementById("notificationDropdown");
    const dropdownList = document.getElementById("notificationDropdownList");
    const unreadCountNode = document.getElementById("notificationUnreadCount");
    const dropdownUnreadCountNode = document.getElementById("notificationDropdownUnreadCount");
    const dropdownUnseenCountNode = document.getElementById("notificationDropdownUnseenCount");
    const totalCountNode = document.getElementById("notificationTotalCount");
    const pageUnreadCountNode = document.getElementById("notificationPageUnreadCount");
    const pageUnseenCountNode = document.getElementById("notificationPageUnseenCount");
    const pageTotalCountNode = document.getElementById("notificationPageTotalCount");
    const pageList = document.getElementById("notificationPageList");

    if (!notificationRoot || !bell || !dropdown || !dropdownList || !unreadCountNode) {
        return;
    }

    const contextPath = document.querySelector("meta[name='app-context-path']")?.content || "";
    const csrfToken = document.querySelector("meta[name='_csrf']")?.content;
    const csrfHeader = document.querySelector("meta[name='_csrf_header']")?.content;
    const pollIntervalMs = 30000;

    function withContext(path) {
        if (!path || !path.startsWith("/")) {
            return path;
        }
        if (!contextPath) {
            return path;
        }
        return contextPath + path;
    }

    function closeDropdown() {
        dropdown.classList.remove("show");
        bell.setAttribute("aria-expanded", "false");
        dropdown.setAttribute("aria-hidden", "true");
    }

    function postOptions() {
        const headers = {};
        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }
        return {
            method: "POST",
            headers: headers
        };
    }

    async function postWithoutBody(path) {
        await fetch(withContext(path), postOptions());
    }

    function bindNotificationLinks(container) {
        if (!container) {
            return;
        }

        container.querySelectorAll(".notification-link[data-notification-id]").forEach((link) => {
            link.addEventListener("click", async (event) => {
                event.preventDefault();

                const notificationId = link.dataset.notificationId;
                const redirectUrl = link.dataset.redirectUrl || link.getAttribute("href") || withContext("/notifications");

                try {
                    await markAsRead(notificationId);
                } catch (error) {
                    console.error("Unable to mark notification as read.", error);
                } finally {
                    window.location.href = redirectUrl;
                }
            });
        });
    }

    async function loadNotifications() {
        const response = await fetch(withContext("/notifications/list"), {
            headers: {
                "X-Requested-With": "XMLHttpRequest"
            }
        });

        if (!response.ok) {
            throw new Error("Unable to load notifications list.");
        }

        const html = await response.text();
        dropdownList.innerHTML = html;
        bindNotificationLinks(dropdownList);

        if (pageList) {
            pageList.innerHTML = html;
            bindNotificationLinks(pageList);
        }
    }

    async function loadUnreadCount() {
        const response = await fetch(withContext("/notifications/unread-count"), {
            headers: {
                "Accept": "application/json"
            }
        });

        if (!response.ok) {
            throw new Error("Unable to load unread count.");
        }

        const payload = await response.json();
        const unreadCount = Number(payload.unreadCount || 0);
        const unseenCount = Number(payload.unseenCount || 0);

        unreadCountNode.classList.toggle("is-zero", unreadCount <= 0);
        unreadCountNode.textContent = unreadCount > 99 ? "99+" : String(unreadCount);

        if (pageUnreadCountNode) {
            pageUnreadCountNode.textContent = String(unreadCount);
        }
        if (dropdownUnreadCountNode) {
            dropdownUnreadCountNode.textContent = String(unreadCount);
        }
        if (dropdownUnseenCountNode) {
            dropdownUnseenCountNode.textContent = String(unseenCount);
        }
        if (pageUnseenCountNode) {
            pageUnseenCountNode.textContent = String(unseenCount);
        }
    }

    async function loadTotalCount() {
        const response = await fetch(withContext("/notifications/count"), {
            headers: {
                "Accept": "application/json"
            }
        });

        if (!response.ok) {
            throw new Error("Unable to load total notification count.");
        }

        const payload = await response.json();
        const totalCount = Number(payload.totalCount || 0);
        const displayCount = totalCount > 999 ? "999+" : String(totalCount);

        if (totalCountNode) {
            totalCountNode.textContent = displayCount;
        }
        if (pageTotalCountNode) {
            pageTotalCountNode.textContent = displayCount;
        }
    }

    async function markAsRead(id) {
        if (!id) {
            return;
        }
        await postWithoutBody("/notifications/" + id + "/read");
        await Promise.all([loadUnreadCount(), loadTotalCount()]);
    }

    async function markAllAsSeen() {
        await postWithoutBody("/notifications/seen");
    }

    bell.addEventListener("click", async (event) => {
        event.preventDefault();

        const shouldOpen = !dropdown.classList.contains("show");
        if (!shouldOpen) {
            closeDropdown();
            return;
        }

        dropdown.classList.add("show");
        bell.setAttribute("aria-expanded", "true");
        dropdown.setAttribute("aria-hidden", "false");

        try {
            await markAllAsSeen();
            await Promise.all([loadNotifications(), loadUnreadCount(), loadTotalCount()]);
        } catch (error) {
            console.error("Unable to refresh notifications dropdown.", error);
        }
    });

    document.addEventListener("click", (event) => {
        if (!notificationRoot.contains(event.target)) {
            closeDropdown();
        }
    });

    setInterval(async () => {
        try {
            await loadUnreadCount();
            await loadTotalCount();
            if (dropdown.classList.contains("show") || pageList) {
                await loadNotifications();
            }
        } catch (error) {
            console.error("Notification polling failed.", error);
        }
    }, pollIntervalMs);

    window.loadNotifications = loadNotifications;
    window.loadUnreadCount = loadUnreadCount;
    window.loadTotalCount = loadTotalCount;
    window.markAsRead = markAsRead;

    loadNotifications().catch((error) => console.error(error));
    loadUnreadCount().catch((error) => console.error(error));
    loadTotalCount().catch((error) => console.error(error));
})();
