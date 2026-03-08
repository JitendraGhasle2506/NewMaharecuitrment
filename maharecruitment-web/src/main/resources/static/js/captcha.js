let code = "";

function randomChar() {
    const chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    return chars[Math.floor(Math.random() * chars.length)];
}

function CreateCaptcha() {
    code = "";
    for (let i = 0; i < 6; i++) {
        code += randomChar();
    }

    const canvas = document.getElementById("CapCode");
    if (!canvas) return;
    const ctx = canvas.getContext("2d");

    ctx.clearRect(0, 0, canvas.width, canvas.height);
    ctx.fillStyle = "#f3f6fb";
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    for (let i = 0; i < 6; i++) {
        ctx.beginPath();
        ctx.strokeStyle = `rgba(${50 + i * 20}, ${100 + i * 10}, 180, 0.7)`;
        ctx.moveTo(Math.random() * canvas.width, Math.random() * canvas.height);
        ctx.lineTo(Math.random() * canvas.width, Math.random() * canvas.height);
        ctx.stroke();
    }

    ctx.font = "bold 36px Arial";
    ctx.fillStyle = "#123b7a";
    ctx.setTransform(1, 0, 0, 1, 0, 0);
    ctx.fillText(code, 35, 52);
}

function isCaptchaValid() {
    const input = document.getElementById("UserCaptchaCode");
    const err = document.getElementById("WrongCaptchaError");
    const ok = document.getElementById("SuccessMessage");

    if (!input) return true;

    if (input.value.trim() === code) {
        if (err) err.textContent = "";
        if (ok) {
            ok.textContent = "Captcha verified";
            ok.style.display = "inline";
        }
        return true;
    }

    if (err) err.textContent = "Invalid captcha.";
    if (ok) ok.style.display = "none";
    return false;
}

function myFunction() {
    const err = document.getElementById("WrongCaptchaError");
    if (err) err.textContent = "";
}

document.addEventListener("DOMContentLoaded", function () {
    CreateCaptcha();

    const form = document.getElementById("loginForm");
    if (form) {
        form.addEventListener("submit", function (e) {
            if (!isCaptchaValid()) {
                e.preventDefault();
            }
        });
    }
});
