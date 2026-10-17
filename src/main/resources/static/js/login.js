/**
 * Enterprise Login Module JavaScript
 * Handles real-time validation, CapsLock, Remember Me, MFA, Strength Meter, Theme, & Dev Diagnostics
 */

document.addEventListener('DOMContentLoaded', () => {
    // DOM Elements
    const loginForm = document.getElementById('loginForm');
    const usernameInput = document.getElementById('username');
    const passwordInput = document.getElementById('password');
    const togglePwdBtn = document.getElementById('togglePasswordBtn');
    const pwdIcon = document.getElementById('pwdIcon');
    const capsWarning = document.getElementById('capsLockWarning');
    const strengthBarWrapper = document.getElementById('strengthBarWrapper');
    const strengthBar = document.getElementById('strengthBar');
    const strengthLabel = document.getElementById('strengthLabel');
    const rememberMeCheckbox = document.getElementById('rememberMe');
    const submitBtn = document.getElementById('submitBtn');
    const alertBox = document.getElementById('alertBox');
    const alertMsg = document.getElementById('alertMessage');
    const mfaGroup = document.getElementById('mfaGroup');
    const themeToggleBtn = document.getElementById('themeToggleBtn');
    const forgotLink = document.getElementById('forgotPasswordLink');
    const forgotModal = document.getElementById('forgotModal');
    const closeForgotBtn = document.getElementById('closeForgotBtn');

    // Init Theme
    const savedTheme = localStorage.getItem('app_theme') || 'dark';
    document.body.setAttribute('data-theme', savedTheme);
    updateThemeIcon(savedTheme);

    // Init Remember Me
    const rememberedUser = localStorage.getItem('remembered_username');
    if (rememberedUser) {
        usernameInput.value = rememberedUser;
        rememberMeCheckbox.checked = true;
    }

    // Canvas Background Particles
    initBackgroundParticles();

    // Theme Toggle Handler
    themeToggleBtn.addEventListener('click', () => {
        const currentTheme = document.body.getAttribute('data-theme') || 'dark';
        const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
        document.body.setAttribute('data-theme', newTheme);
        localStorage.setItem('app_theme', newTheme);
        updateThemeIcon(newTheme);
    });

    function updateThemeIcon(theme) {
        themeToggleBtn.innerHTML = theme === 'dark' 
            ? '<i class="bi bi-sun-fill"></i>' 
            : '<i class="bi bi-moon-stars-fill"></i>';
    }

    // Toggle Show/Hide Password
    togglePwdBtn.addEventListener('click', () => {
        const type = passwordInput.getAttribute('type') === 'password' ? 'text' : 'password';
        passwordInput.setAttribute('type', type);
        pwdIcon.className = type === 'password' ? 'bi bi-eye' : 'bi bi-eye-slash';
    });

    // Caps Lock Detection
    passwordInput.addEventListener('keyup', (e) => {
        if (e.getModifierState && e.getModifierState('CapsLock')) {
            capsWarning.classList.add('active');
        } else {
            capsWarning.classList.remove('active');
        }
        evaluatePasswordStrength(passwordInput.value);
    });

    passwordInput.addEventListener('keydown', (e) => {
        if (e.getModifierState && e.getModifierState('CapsLock')) {
            capsWarning.classList.add('active');
        } else {
            capsWarning.classList.remove('active');
        }
    });

    // Password Strength Meter
    function evaluatePasswordStrength(pwd) {
        if (!pwd) {
            strengthBarWrapper.style.display = 'none';
            strengthLabel.style.display = 'none';
            return;
        }

        strengthBarWrapper.style.display = 'block';
        strengthLabel.style.display = 'block';

        let score = 0;
        if (pwd.length >= 8) score += 25;
        if (/[A-Z]/.test(pwd)) score += 25;
        if (/[0-9]/.test(pwd)) score += 25;
        if (/[^A-Za-z0-9]/.test(pwd)) score += 25;

        strengthBar.style.width = `${score}%`;
        if (score <= 25) {
            strengthBar.style.backgroundColor = '#ef4444';
            strengthLabel.textContent = 'Password Strength: Weak';
            strengthLabel.style.color = '#ef4444';
        } else if (score <= 75) {
            strengthBar.style.backgroundColor = '#f59e0b';
            strengthLabel.textContent = 'Password Strength: Moderate';
            strengthLabel.style.color = '#f59e0b';
        } else {
            strengthBar.style.backgroundColor = '#10b981';
            strengthLabel.textContent = 'Password Strength: Strong';
            strengthLabel.style.color = '#10b981';
        }
    }

    // Login Form Submit
    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        alertBox.classList.remove('visible', 'alert-danger', 'alert-success');
        submitBtn.classList.add('loading');
        submitBtn.disabled = true;

        const username = usernameInput.value.trim();
        const password = passwordInput.value;
        const mfaCode = document.getElementById('mfaCode') ? document.getElementById('mfaCode').value.trim() : '';

        // Handle Remember Me
        if (rememberMeCheckbox.checked) {
            localStorage.setItem('remembered_username', username);
        } else {
            localStorage.removeItem('remembered_username');
        }

        const screenResolution = `${window.screen.width}x${window.screen.height}`;
        const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC';

        try {
            const response = await ApiService.login({ username, password, mfaCode, screenResolution, timezone });

            if (response.mfaRequired) {
                mfaGroup.style.display = 'block';
                showAlert('MFA Security Code required to complete sign in.', 'danger');
                submitBtn.classList.remove('loading');
                submitBtn.disabled = false;
                return;
            }

            // Save JWT and user info
            ApiService.setToken(response.token);
            ApiService.setUser({
                id: response.userId,
                username: response.username,
                email: response.email,
                roles: response.roles
            });

            showAlert('Authentication successful! Redirecting...', 'success');
            setTimeout(() => {
                window.location.href = '/pages/dashboard.html';
            }, 600);

        } catch (err) {
            showAlert(err.message || 'Authentication failed. Please check your credentials.', 'danger');
            submitBtn.classList.remove('loading');
            submitBtn.disabled = false;
        }
    });

    function showAlert(msg, type) {
        alertMsg.textContent = msg;
        alertBox.className = `alert-box visible alert-${type}`;
    }

    // Modal Handlers (Forgot Password)
    forgotLink.addEventListener('click', (e) => {
        e.preventDefault();
        forgotModal.classList.add('active');
    });

    closeForgotBtn.addEventListener('click', () => {
        forgotModal.classList.remove('active');
    });

    // Dev Diagnostics Drawer (Part 6)
    const devTrigger = document.getElementById('devDiagTrigger');
    const devDrawer = document.getElementById('devDiagDrawer');
    if (devTrigger && devDrawer) {
        devTrigger.addEventListener('click', async () => {
            devDrawer.classList.toggle('open');
            if (devDrawer.classList.contains('open')) {
                try {
                    const res = await fetch('/api/v1/diagnostics/status');
                    if (res.ok) {
                        const data = await res.json();
                        document.getElementById('diagBackend').textContent = data.backendStatus;
                        document.getElementById('diagDb').textContent = data.postgresStatus;
                        document.getElementById('diagJwt').textContent = data.jwtStatus;
                        document.getElementById('diagAws').textContent = data.awsStatus;
                        document.getElementById('diagVer').textContent = data.version;
                    }
                } catch (e) {
                    console.error('Failed to load diagnostics', e);
                }
            }
        });
    }

    // Background Particle Renderer
    function initBackgroundParticles() {
        const canvas = document.getElementById('bgCanvas');
        if (!canvas) return;
        const ctx = canvas.getContext('2d');
        let width = canvas.width = window.innerWidth;
        let height = canvas.height = window.innerHeight;

        window.addEventListener('resize', () => {
            width = canvas.width = window.innerWidth;
            height = canvas.height = window.innerHeight;
        });

        const particles = Array.from({ length: 35 }, () => ({
            x: Math.random() * width,
            y: Math.random() * height,
            radius: Math.random() * 2 + 1,
            vx: (Math.random() - 0.5) * 0.4,
            vy: (Math.random() - 0.5) * 0.4,
            alpha: Math.random() * 0.5 + 0.2
        }));

        function render() {
            ctx.clearRect(0, 0, width, height);
            particles.forEach(p => {
                p.x += p.vx;
                p.y += p.vy;
                if (p.x < 0) p.x = width;
                if (p.x > width) p.x = 0;
                if (p.y < 0) p.y = height;
                if (p.y > height) p.y = 0;

                ctx.beginPath();
                ctx.arc(p.x, p.y, p.radius, 0, Math.PI * 2);
                ctx.fillStyle = `rgba(59, 130, 246, ${p.alpha})`;
                ctx.fill();
            });
            requestAnimationFrame(render);
        }
        render();
    }
});
