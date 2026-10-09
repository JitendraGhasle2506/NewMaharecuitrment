(function () {
    const root = document.querySelector('.employee-dashboard');
    if (!root) {
        return;
    }

    function openDialog(dialog) {
        if (typeof dialog.showModal === 'function') {
            dialog.showModal();
            return;
        }
        dialog.setAttribute('open', '');
    }

    function closeDialog(dialog) {
        if (typeof dialog.close === 'function') {
            dialog.close();
            return;
        }
        dialog.removeAttribute('open');
    }

    root.addEventListener('click', (event) => {
        const opener = event.target.closest('[data-dashboard-dialog-target]');
        if (opener) {
            const dialog = document.getElementById(opener.dataset.dashboardDialogTarget);
            if (dialog && !dialog.open) {
                openDialog(dialog);
            }
            return;
        }

        const closeButton = event.target.closest('[data-dashboard-dialog-close]');
        if (closeButton) {
            const dialog = closeButton.closest('dialog');
            if (dialog) {
                closeDialog(dialog);
            }
        }
    });

    root.querySelectorAll('.dashboard-dialog').forEach((dialog) => {
        dialog.addEventListener('click', (event) => {
            if (event.target === dialog) {
                closeDialog(dialog);
            }
        });
        dialog.addEventListener('close', () => {
            dialog.querySelectorAll('details[open]').forEach((details) => details.removeAttribute('open'));
        });
    });

    const festivalCarousel = root.querySelector('[data-employee-festival-carousel]');
    if (!festivalCarousel) {
        return;
    }

    const slides = Array.from(festivalCarousel.querySelectorAll('[data-employee-festival-slide]'));
    const dots = Array.from(festivalCarousel.querySelectorAll('[data-employee-festival-target]'));
    const previousButton = festivalCarousel.querySelector('[data-employee-festival-previous]');
    const nextButton = festivalCarousel.querySelector('[data-employee-festival-next]');
    const status = festivalCarousel.querySelector('[data-employee-festival-status]');
    const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    let slideIndex = 0;
    let photoIndex = 0;
    let autoPlayTimer;

    function photosForSlide(index) {
        return Array.from(slides[index]?.querySelectorAll('[data-employee-festival-photo]') || []);
    }

    function renderFestival() {
        slides.forEach((slide, index) => {
            slide.hidden = index !== slideIndex;
            slide.querySelectorAll('[data-employee-festival-photo]').forEach((photo, currentPhotoIndex) => {
                photo.hidden = currentPhotoIndex !== photoIndex;
            });
            const photoCurrent = slide.querySelector('[data-employee-festival-photo-current]');
            if (photoCurrent) {
                photoCurrent.textContent = String(photoIndex + 1);
            }
        });
        dots.forEach((dot, index) => {
            dot.setAttribute('aria-current', index === slideIndex ? 'true' : 'false');
        });
        if (status) {
            const name = slides[slideIndex]?.getAttribute('aria-label') || 'Festival';
            status.textContent = `${name}, image ${photoIndex + 1} of ${Math.max(photosForSlide(slideIndex).length, 1)}`;
        }
    }

    function moveFestival(direction) {
        const photos = photosForSlide(slideIndex);
        if (photos.length > 1 && photoIndex + direction >= 0 && photoIndex + direction < photos.length) {
            photoIndex += direction;
        } else {
            slideIndex = (slideIndex + direction + slides.length) % slides.length;
            const nextPhotos = photosForSlide(slideIndex);
            photoIndex = direction < 0 ? Math.max(nextPhotos.length - 1, 0) : 0;
        }
        renderFestival();
    }

    function stopAutoPlay() {
        window.clearInterval(autoPlayTimer);
    }

    function startAutoPlay() {
        stopAutoPlay();
        if (!prefersReducedMotion && (slides.length > 1 || photosForSlide(0).length > 1)) {
            autoPlayTimer = window.setInterval(() => moveFestival(1), 5500);
        }
    }

    previousButton?.addEventListener('click', () => {
        moveFestival(-1);
        startAutoPlay();
    });
    nextButton?.addEventListener('click', () => {
        moveFestival(1);
        startAutoPlay();
    });
    dots.forEach((dot, index) => {
        dot.addEventListener('click', () => {
            slideIndex = index;
            photoIndex = 0;
            renderFestival();
            startAutoPlay();
        });
    });
    festivalCarousel.addEventListener('mouseenter', stopAutoPlay);
    festivalCarousel.addEventListener('mouseleave', startAutoPlay);
    festivalCarousel.addEventListener('focusin', stopAutoPlay);
    festivalCarousel.addEventListener('focusout', (event) => {
        if (!festivalCarousel.contains(event.relatedTarget)) {
            startAutoPlay();
        }
    });

    const frameCount = slides.reduce(
        (total, slide) => total + Math.max(slide.querySelectorAll('[data-employee-festival-photo]').length, 1),
        0);
    if (frameCount < 2) {
        previousButton?.setAttribute('hidden', '');
        nextButton?.setAttribute('hidden', '');
    }
    renderFestival();
    startAutoPlay();
})();
