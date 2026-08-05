document.addEventListener("DOMContentLoaded", () => {
    const canvas = document.getElementById("wingHierarchyCanvas");
    if (!canvas) {
        return;
    }

    const branches = Array.from(canvas.querySelectorAll(".wing-hierarchy-branch"));
    const searchInput = document.getElementById("wingHierarchySearch");
    const noResults = document.getElementById("wingHierarchyNoResults");

    const setBranchExpanded = (branch, expanded) => {
        const toggle = branch.querySelector("[data-hierarchy-target]");
        if (!toggle) {
            return;
        }
        const employeeList = document.getElementById(toggle.dataset.hierarchyTarget);
        if (!employeeList) {
            return;
        }
        toggle.setAttribute("aria-expanded", String(expanded));
        employeeList.hidden = !expanded;
        branch.classList.toggle("is-collapsed", !expanded);
    };

    const expandOnly = (branchToExpand) => {
        branches.forEach((branch) => setBranchExpanded(branch, branch === branchToExpand));
    };

    const getExpandedBranch = (candidates = branches) => candidates.find((branch) =>
        branch.querySelector("[data-hierarchy-target]")?.getAttribute("aria-expanded") === "true");

    const initiallyExpandedBranch = getExpandedBranch() || branches[0];
    if (initiallyExpandedBranch) {
        expandOnly(initiallyExpandedBranch);
    }

    branches.forEach((branch) => {
        const toggle = branch.querySelector("[data-hierarchy-target]");
        toggle?.addEventListener("click", () => {
            const shouldExpand = toggle.getAttribute("aria-expanded") !== "true";
            if (shouldExpand) {
                expandOnly(branch);
                return;
            }
            setBranchExpanded(branch, false);
        });
    });

    document.getElementById("collapseHierarchy")?.addEventListener("click", () => {
        const expandedBranch = getExpandedBranch();
        if (expandedBranch) {
            setBranchExpanded(expandedBranch, false);
        }
    });

    searchInput?.addEventListener("input", () => {
        const searchTerm = searchInput.value.trim().toLocaleLowerCase();
        const visibleBranches = [];

        branches.forEach((branch) => {
            const cellMatches = (branch.dataset.cellSearch || "").includes(searchTerm);
            const employeeMatches = Array.from(branch.querySelectorAll("[data-employee-search]"))
                .some((employee) => (employee.dataset.employeeSearch || "").includes(searchTerm));
            const isVisible = !searchTerm || cellMatches || employeeMatches;
            branch.hidden = !isVisible;
            if (isVisible) {
                visibleBranches.push(branch);
            }
        });

        if (searchTerm && visibleBranches.length > 0) {
            expandOnly(getExpandedBranch(visibleBranches) || visibleBranches[0]);
        }

        noResults?.classList.toggle("d-none", visibleBranches.length > 0);
    });

    canvas.querySelectorAll(".wing-hierarchy-avatar img").forEach((image) => {
        image.addEventListener("error", () => {
            image.classList.add("d-none");
            image.nextElementSibling?.classList.remove("d-none");
        });
    });
});
