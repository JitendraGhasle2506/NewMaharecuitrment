(function () {
    const root = document.querySelector('.employee-dashboard');
    if (!root) {
        return;
    }

    const maxPhotoSize = 2 * 1024 * 1024;
    const allowedPhotoTypes = ['image/jpeg', 'image/png'];
    const allowedExtensions = ['jpg', 'jpeg', 'png'];

    const form = document.getElementById('employeeProfileForm');
    const photoForm = document.getElementById('photoUploadForm');
    const photoFile = document.getElementById('photoFile');
    const maritalStatus = document.getElementById('maritalStatus');
    const marriageDetails = document.getElementById('marriageDetails');
    const csrfToken = document.getElementById('csrfToken')?.value;
    const csrfHeader = document.getElementById('csrfHeader')?.value;

    function headers() {
        const values = {
            'Accept': 'application/json',
            'X-Requested-With': 'XMLHttpRequest'
        };
        if (csrfToken && csrfHeader) {
            values[csrfHeader] = csrfToken;
        }
        return values;
    }

    async function responsePayload(response) {
        const contentType = response.headers.get('content-type') || '';
        if (contentType.toLowerCase().includes('application/json')) {
            const payload = await response.json();
            if (!payload.message && (payload.error === 'SESSION_EXPIRED' || response.status === 401 || response.status === 403)) {
                payload.message = 'Your session has expired. Please sign in and try again.';
            }
            return payload;
        }

        await response.text();
        return {
            success: false,
            message: response.redirected || response.status === 401 || response.status === 403
                ? 'Your session has expired. Please sign in and try again.'
                : `The server could not process the request (HTTP ${response.status}).`
        };
    }

    function showAlert(icon, title, message) {
        const text = String(message || title || '').trim();
        if (window.Swal && typeof window.Swal.fire === 'function') {
            return Promise.resolve(window.Swal.fire({
                icon,
                title,
                text,
                confirmButtonText: 'OK',
                confirmButtonColor: icon === 'error' ? '#d33' : '#1f4f78',
                allowOutsideClick: false
            }));
        }
        window.alert(text);
        return Promise.resolve();
    }

    function field(name) {
        return form?.querySelector(`[name="${name}"]`)
            || document.getElementById(name)
            || photoForm?.querySelector(`[name="${name}"]`);
    }

    function setError(name, message) {
        const input = field(name);
        const error = document.querySelector(`[data-error-for="${name}"]`);
        if (input) {
            input.classList.toggle('is-invalid', Boolean(message));
        }
        if (error) {
            error.textContent = message || '';
        }
    }

    function clearErrors() {
        document.querySelectorAll('.field-error').forEach((element) => {
            element.textContent = '';
        });
        document.querySelectorAll('.is-invalid').forEach((element) => {
            element.classList.remove('is-invalid');
        });
    }

    function validateProfile() {
        clearErrors();
        const formData = new FormData(form);
        const dob = String(formData.get('dob') || '').trim();
        const marriageDate = String(formData.get('marriageDate') || '').trim();
        let gender = String(formData.get('gender') || '').trim();
        let panNo = String(field('panNo')?.value || '').trim().toUpperCase();
        let valid = true;

        if (dob === '1900-01-01') {
            setError('dob', 'Please select a valid date of birth');
            valid = false;
        }

        if (marriageDate && marriageDate > localDateValue(new Date())) {
            setError('marriageDate', 'Marriage date cannot be in the future');
            valid = false;
        }

        if (isPlaceholder(gender)) {
            gender = '';
            const genderInput = field('gender');
            if (genderInput) {
                genderInput.value = '';
            }
        }

        if (isPlaceholder(panNo)) {
            panNo = '';
        }

        if (panNo && !/^[A-Z]{5}[0-9]{4}[A-Z]$/.test(panNo)) {
            setError('panNo', 'PAN must match ABCDE1234F format');
            valid = false;
        }

        const panInput = field('panNo');
        if (panInput) {
            panInput.value = panNo;
        }

        return valid;
    }

    function localDateValue(date) {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    }

    function updateMarriageDetails() {
        const married = maritalStatus?.value.trim().toLowerCase() === 'married';
        if (marriageDetails) {
            marriageDetails.hidden = !married;
            marriageDetails.querySelectorAll('input').forEach((input) => {
                input.disabled = !married;
            });
        }
        if (!married) {
            setError('spouseName', '');
            setError('marriageDate', '');
        }
    }

    function isPlaceholder(value) {
        const normalized = String(value || '').trim().toUpperCase();
        return normalized === 'NOT_PROVIDED' || normalized === 'NOT_SPECIFIED';
    }

    function updateDashboard(profile) {
        text('summaryFullName', profile.fullName || 'Employee');
        text('summaryEmployeeCode', profile.employeeCode || '-');
        text('summaryRole', profile.role || '-');
        text('summaryDepartment', profile.department || '-');
        text('summaryEmail', profile.email || '-');
        text('summaryMobile', profile.mobileNo || '-');
        text('summaryAltMobile', profile.alternateMobileNo ? ` / ${profile.alternateMobileNo}` : '');
        text('profileStatus', profile.profileAvailable ? 'Profile available' : 'Complete your profile');
        text('completionValue', `${profile.completionPercentage || 0}%`);

        const meter = document.querySelector('.completion-meter');
        if (meter) {
            meter.style.setProperty('--completion', String(profile.completionPercentage || 0));
        }

        if (profile.photoUrl) {
            const cacheBustUrl = `${root.dataset.photoUrl}?v=${Date.now()}`;
            showImage('profilePhoto', 'profilePhotoFallback', cacheBustUrl);
            showImage('photoPreview', 'photoPreviewFallback', cacheBustUrl);
            updateSharedAvatars(cacheBustUrl);
        }
    }

    function text(id, value) {
        const element = document.getElementById(id);
        if (element) {
            element.textContent = value;
        }
    }

    function showImage(imageId, fallbackId, source) {
        let image = document.getElementById(imageId);
        const fallback = document.getElementById(fallbackId);
        if (!image && fallback) {
            image = document.createElement('img');
            image.id = imageId;
            image.alt = 'Employee profile photo';
            fallback.parentElement.prepend(image);
        }
        if (image) {
            image.src = source;
            image.classList.remove('d-none');
        }
        if (fallback) {
            fallback.classList.add('d-none');
        }
    }

    function updateSharedAvatars(source) {
        document.querySelectorAll('.user-avatar, .profile-dropdown-avatar').forEach((avatar) => {
            let image = avatar.querySelector('img.app-user-photo');
            if (!image) {
                image = document.createElement('img');
                image.className = 'app-user-photo';
                image.alt = avatar.classList.contains('profile-dropdown-avatar') ? '' : 'Profile photo';
                avatar.prepend(image);
            }
            image.src = source;
            avatar.querySelectorAll('i').forEach((icon) => icon.remove());
        });
    }

    function validatePhoto(file) {
        setError('file', '');
        if (!file) {
            setError('file', 'Select a photo to upload');
            return false;
        }
        const extension = file.name.includes('.') ? file.name.split('.').pop().toLowerCase() : '';
        if (!allowedExtensions.includes(extension) || !allowedPhotoTypes.includes(file.type)) {
            setError('file', 'Only JPG, JPEG, and PNG files are allowed');
            return false;
        }
        if (file.size > maxPhotoSize) {
            setError('file', 'Photo must be 2 MB or smaller');
            return false;
        }
        return true;
    }

    const marriageDateInput = field('marriageDate');
    if (marriageDateInput) {
        marriageDateInput.max = localDateValue(new Date());
    }
    maritalStatus?.addEventListener('change', updateMarriageDetails);
    updateMarriageDetails();

    form?.addEventListener('submit', async (event) => {
        event.preventDefault();
        if (!validateProfile()) {
            showAlert('error', 'Validation failed', 'Please correct the highlighted fields.');
            return;
        }

        try {
            if (!window.SensitiveDataEncryption?.createEncryptedFormData) {
                throw new Error('Secure identity submission is unavailable.');
            }
            const encryptedFormData = await window.SensitiveDataEncryption.createEncryptedFormData(form);
            const response = await fetch(root.dataset.saveUrl, {
                method: 'POST',
                headers: headers(),
                credentials: 'same-origin',
                body: encryptedFormData
            });
            const payload = await responsePayload(response);
            if (!response.ok || !payload.success) {
                Object.entries(payload.errors || {}).forEach(([name, message]) => setError(name, message));
                showAlert('error', 'Unable to save profile', payload.message || 'Please check the form and try again.');
                return;
            }
            updateDashboard(payload.profile);
            await showAlert('success', 'Success!', payload.message || 'Profile updated successfully.');
            window.location.reload();
        } catch (error) {
            const message = error instanceof Error && error.message
                ? error.message
                : 'Please try again after some time.';
            await showAlert('error', 'Unable to save profile', message);
        }
    });

    photoFile?.addEventListener('change', () => {
        const file = photoFile.files && photoFile.files[0];
        if (!validatePhoto(file)) {
            photoFile.value = '';
            return;
        }
        const previewUrl = URL.createObjectURL(file);
        showImage('photoPreview', 'photoPreviewFallback', previewUrl);
    });

    photoForm?.addEventListener('submit', async (event) => {
        event.preventDefault();
        const file = photoFile.files && photoFile.files[0];
        if (!validatePhoto(file)) {
            showAlert('error', 'Invalid photo', 'Please select a valid JPG, JPEG, or PNG file.');
            return;
        }

        try {
            const response = await fetch(photoForm.action || root.dataset.photoUploadUrl, {
                method: 'POST',
                headers: headers(),
                credentials: 'same-origin',
                body: new FormData(photoForm)
            });
            const payload = await responsePayload(response);
            if (!response.ok || !payload.success) {
                setError('file', payload.message || 'Unable to upload photo');
                showAlert('error', 'Unable to upload photo', payload.message || 'Please try another file.');
                return;
            }
            photoFile.value = '';
            updateDashboard(payload.profile);
            showAlert('success', 'Uploaded', payload.message || 'Photo uploaded successfully.');
        } catch (error) {
            showAlert('error', 'Unable to upload photo', 'Please try again after some time.');
        }
    });
})();
