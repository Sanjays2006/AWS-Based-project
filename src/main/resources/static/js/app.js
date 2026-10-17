/**
 * Common Navigation, Header & Layout Management
 */

document.addEventListener('DOMContentLoaded', () => {
    // 1. Check Authentication on protected pages
    const path = window.location.pathname;
    if (!path.includes('login.html') && !path.includes('register.html')) {
        const token = ApiService.getToken();
        if (!token) {
            window.location.href = '/pages/login.html';
            return;
        }
        setupNavigationUser();
        loadNotifications();
        initSseStream();
    }

    // 2. Setup Mobile Sidebar Toggle
    const toggleBtn = document.getElementById('sidebarToggle');
    const sidebar = document.querySelector('.sidebar');
    if (toggleBtn && sidebar) {
        toggleBtn.addEventListener('click', () => {
            sidebar.classList.toggle('open');
        });
    }
});

function initSseStream() {
    if (!window.EventSource) return;
    try {
        const eventSource = new EventSource('/api/v1/stream/threats');
        eventSource.addEventListener('THREAT_ALERT', (event) => {
            try {
                const data = JSON.parse(event.data);
                console.warn('[SSE THREAT ALERT]', data);
                if (window.showToast) {
                    window.showToast(`🚨 THREAT ALERT [${data.severity}]: Identity '${data.identityName}' triggered risk score ${data.riskScore}`, data.severity === 'CRITICAL' ? 'danger' : 'warning');
                }
                loadNotifications();
            } catch (e) {
                console.error('Error parsing SSE event:', e);
            }
        });
    } catch (err) {
        console.warn('Could not initialize SSE threat stream:', err);
    }
}

function setupNavigationUser() {
    const user = ApiService.getUser();
    if (user) {
        const nameEl = document.getElementById('navUsername');
        const roleEl = document.getElementById('navRole');
        const avatarEl = document.getElementById('navAvatar');

        if (nameEl) nameEl.textContent = user.username || 'User';
        if (roleEl) roleEl.textContent = (user.roles && user.roles[0]) ? user.roles[0] : 'VIEWER';
        if (avatarEl) avatarEl.textContent = (user.username || 'U').charAt(0).toUpperCase();

        // Hide admin links for non-admins
        if (user.roles && !user.roles.includes('ROLE_ADMIN') && !user.roles.includes('ADMIN')) {
            document.querySelectorAll('.admin-only').forEach(el => el.style.display = 'none');
        }
    }
}

async function loadNotifications() {
    try {
        const res = await ApiService.getUnreadCount();
        const badge = document.getElementById('notifBadge');
        if (badge) {
            if (res.count > 0) {
                badge.textContent = res.count > 9 ? '9+' : res.count;
                badge.style.display = 'flex';
            } else {
                badge.style.display = 'none';
            }
        }
    } catch (e) {
        console.warn('Could not load notification badge count', e);
    }
}

function handleLogout() {
    ApiService.logout().finally(() => {
        window.location.href = '/pages/login.html';
    });
}
