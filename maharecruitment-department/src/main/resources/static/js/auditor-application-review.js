document.addEventListener('DOMContentLoaded', function () {
    const form = document.getElementById('auditorReviewForm');
    if (!form) {
        return;
    }

    let submitting = false;
    let confirming = false;
    form.addEventListener('submit', async function (event) {
        event.preventDefault();
        if (submitting || confirming) {
            return;
        }

        const decision = event.submitter ? event.submitter.value : '';
        if (decision !== 'APPROVE' && decision !== 'SEND_BACK') {
            return;
        }

        confirming = true;
        try {
            const remarks = form.querySelector('[name="remarks"]');
            if (decision === 'SEND_BACK' && !remarks.value.trim()) {
                await Swal.fire({
                    icon: 'warning',
                    title: 'Remarks required',
                    text: 'Please enter remarks before sending this request back.'
                });
                remarks.focus();
                return;
            }

            const result = await Swal.fire({
                icon: 'warning',
                title: decision === 'APPROVE' ? 'Confirm final approval?' : 'Send back this request?',
                text: decision === 'APPROVE'
                    ? 'This is the final approval. The request will be completed and the proforma invoice generated. After approval, the details and review cannot be changed or submitted again.'
                    : 'The request will be returned to the sub-department for corrections.',
                showCancelButton: true,
                confirmButtonText: decision === 'APPROVE' ? 'Yes, approve finally' : 'Yes, send back',
                cancelButtonText: 'Cancel',
                focusCancel: true,
                allowOutsideClick: false
            });
            if (!result.isConfirmed) {
                return;
            }

            // Disabled submit buttons are not posted, so preserve the chosen decision.
            const decisionInput = document.createElement('input');
            decisionInput.type = 'hidden';
            decisionInput.name = 'decision';
            decisionInput.value = decision;
            form.appendChild(decisionInput);
            submitting = true;
            form.querySelectorAll('button').forEach(function (button) {
                button.disabled = true;
            });
            event.submitter.textContent = 'Submitting...';
            HTMLFormElement.prototype.submit.call(form);
        } finally {
            confirming = false;
        }
    });
});
