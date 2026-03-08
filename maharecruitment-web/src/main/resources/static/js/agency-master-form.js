document.addEventListener("DOMContentLoaded", () => {
    const form = document.getElementById("agencyMasterForm");
    if (!form) {
        return;
    }

    const escalationTableBody = document.querySelector("#escalationTable tbody");
    const addRowButton = document.getElementById("addEscalationRowBtn");

    const renumberRows = () => {
        escalationTableBody.querySelectorAll("tr").forEach((row, index) => {
            row.querySelectorAll("input, select").forEach((field) => {
                const name = field.getAttribute("name");
                const id = field.getAttribute("id");
                if (name) {
                    field.setAttribute(
                        "name",
                        name.replace(/escalationMatrixEntries\[\d+\]/, `escalationMatrixEntries[${index}]`)
                    );
                }
                if (id) {
                    field.setAttribute(
                        "id",
                        id.replace(/escalationMatrixEntries\d+/, `escalationMatrixEntries${index}`)
                    );
                }
                field.value = field.value || "";
            });

            row.querySelectorAll(".text-danger.small").forEach((errorBlock) => {
                if (!errorBlock.hasAttribute("data-static-error")) {
                    errorBlock.remove();
                }
            });
        });
    };

    const createRow = () => {
        const index = escalationTableBody.querySelectorAll("tr").length;
        const row = document.createElement("tr");
        row.innerHTML = `
            <td><input type="text" class="form-control" name="escalationMatrixEntries[${index}].contactName"></td>
            <td><input type="text" class="form-control" name="escalationMatrixEntries[${index}].mobileNumber"></td>
            <td>
                <select class="form-select" name="escalationMatrixEntries[${index}].level">
                    <option value="">Select</option>
                    <option value="L1">L1</option>
                    <option value="L2">L2</option>
                    <option value="L3">L3</option>
                </select>
            </td>
            <td><input type="text" class="form-control" name="escalationMatrixEntries[${index}].designation"></td>
            <td><input type="email" class="form-control" name="escalationMatrixEntries[${index}].companyEmailId"></td>
            <td class="text-center">
                <button class="btn btn-outline-danger btn-sm remove-escalation-row" type="button">Remove</button>
            </td>
        `;
        escalationTableBody.appendChild(row);
        renumberRows();
    };

    addRowButton.addEventListener("click", createRow);

    escalationTableBody.addEventListener("click", (event) => {
        if (!event.target.classList.contains("remove-escalation-row")) {
            return;
        }

        if (escalationTableBody.querySelectorAll("tr").length === 1) {
            escalationTableBody.querySelector("tr").querySelectorAll("input, select").forEach((field) => {
                field.value = "";
            });
            return;
        }

        event.target.closest("tr").remove();
        renumberRows();
    });

    ["panNumber", "gstNumber", "ifscCode"].forEach((fieldId) => {
        const input = document.getElementById(fieldId);
        if (input) {
            input.addEventListener("input", () => {
                input.value = input.value.toUpperCase();
            });
        }
    });
});
