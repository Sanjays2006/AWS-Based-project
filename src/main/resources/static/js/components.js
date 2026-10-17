/**
 * UI Components Utilities
 * Provides Toast Notifications, Modal Dialog handling, and helper rendering logic.
 */

class Toast {
    static container = null;

    static initContainer() {
        if (!this.container) {
            this.container = document.createElement('div');
            this.container.className = 'toast-container';
            document.body.appendChild(this.container);
        }
    }

    static show({ title, message, type = 'info', duration = 4000 }) {
        this.initContainer();

        const toast = document.createElement('div');
        toast.className = `toast ${type}`;

        const icons = {
            success: 'bi-check-circle-fill',
            error: 'bi-exclamation-triangle-fill',
            warning: 'bi-exclamation-circle-fill',
            info: 'bi-info-circle-fill'
        };

        toast.innerHTML = `
            <div class="toast-icon">
                <i class="bi ${icons[type] || icons.info}"></i>
            </div>
            <div class="toast-content">
                <div class="toast-title">${title}</div>
                <div class="toast-message">${message}</div>
            </div>
            <button class="toast-close" onclick="this.parentElement.remove()">
                <i class="bi bi-x"></i>
            </button>
        `;

        this.container.appendChild(toast);

        if (duration > 0) {
            setTimeout(() => {
                toast.classList.add('slide-out');
                setTimeout(() => toast.remove(), 300);
            }, duration);
        }
    }

    static success(message, title = 'Success') {
        this.show({ title, message, type: 'success' });
    }

    static error(message, title = 'Error') {
        this.show({ title, message, type: 'error' });
    }

    static warning(message, title = 'Warning') {
        this.show({ title, message, type: 'warning' });
    }

    static info(message, title = 'Information') {
        this.show({ title, message, type: 'info' });
    }
}

class Modal {
    static open(modalId) {
        const overlay = document.getElementById(modalId);
        if (overlay) {
            overlay.classList.add('active');
        }
    }

    static close(modalId) {
        const overlay = document.getElementById(modalId);
        if (overlay) {
            overlay.classList.remove('active');
        }
    }
}

// Global UI formatters
const UIUtils = {
    formatDate(dateString) {
        if (!dateString) return 'N/A';
        const date = new Date(dateString);
        return date.toLocaleString();
    },

    formatRiskBadge(level) {
        const lvl = (level || 'LOW').toUpperCase();
        return `<span class="risk-badge ${lvl}">${lvl}</span>`;
    },

    formatArn(arn) {
        if (!arn) return 'N/A';
        return `<span class="arn-text">${arn}</span>`;
    },

    showLoader() {
        const loader = document.getElementById('globalLoader');
        if (loader) loader.classList.add('active');
    },

    hideLoader() {
        const loader = document.getElementById('globalLoader');
        if (loader) loader.classList.remove('active');
    }
};
