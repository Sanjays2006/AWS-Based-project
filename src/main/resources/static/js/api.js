/**
 * API Service for Cloud Identity Behavior Fingerprinting System
 * Handles JWT token storage, request headers, error handling, and API calls.
 */

const API_BASE_URL = '/api/v1';

class ApiService {
    static getToken() {
        return localStorage.getItem('jwt_token');
    }

    static setToken(token) {
        localStorage.setItem('jwt_token', token);
    }

    static removeToken() {
        localStorage.removeItem('jwt_token');
        localStorage.removeItem('user_info');
    }

    static getUser() {
        const userStr = localStorage.getItem('user_info');
        return userStr ? JSON.parse(userStr) : null;
    }

    static setUser(user) {
        localStorage.setItem('user_info', JSON.stringify(user));
    }

    static getHeaders(customHeaders = {}) {
        const headers = {
            'Content-Type': 'application/json',
            'Accept': 'application/json',
            ...customHeaders
        };

        const token = this.getToken();
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        return headers;
    }

    static async request(endpoint, options = {}) {
        const url = `${API_BASE_URL}${endpoint}`;
        const config = {
            method: options.method || 'GET',
            headers: this.getHeaders(options.headers),
            ...options
        };

        if (options.body && typeof options.body === 'object' && !(options.body instanceof FormData)) {
            config.body = JSON.stringify(options.body);
        }

        try {
            const response = await fetch(url, config);
            
            // Handle 401 Unauthorized globally
            if (response.status === 401 && !endpoint.includes('/auth/login')) {
                this.removeToken();
                window.location.href = '/pages/login.html';
                return Promise.reject(new Error('Session expired. Please log in again.'));
            }

            const data = await response.json().catch(() => ({}));

            if (!response.ok) {
                const errorMessage = data.message || `Request failed with status ${response.status}`;
                const error = new Error(errorMessage);
                error.status = response.status;
                error.data = data;
                throw error;
            }

            return data;
        } catch (error) {
            console.error(`API Error [${options.method || 'GET'} ${endpoint}]:`, error);
            throw error;
        }
    }

    // === Auth Endpoints ===
    static login(credentials) {
        return this.request('/auth/login', { method: 'POST', body: credentials });
    }

    static register(userData) {
        return this.request('/auth/register', { method: 'POST', body: userData });
    }

    static logout() {
        return this.request('/auth/logout', { method: 'POST' }).finally(() => {
            this.removeToken();
        });
    }

    static getCurrentUser() {
        return this.request('/auth/me');
    }

    static changePassword(data) {
        return this.request('/auth/change-password', { method: 'PUT', body: data });
    }

    // === Dashboard Endpoints ===
    static getDashboardSummary() {
        return this.request('/dashboard/summary');
    }

    static getHeatmapData() {
        return this.request('/dashboard/heatmap');
    }

    static getWorldMapData() {
        return this.request('/dashboard/world-map');
    }

    static getRiskTrend(days = 30) {
        return this.request(`/dashboard/risk-trend?days=${days}`);
    }

    static getServiceUsage() {
        return this.request('/dashboard/service-usage');
    }

    // === Investigation Console ===
    static getInvestigationData(identityId) {
        return this.request(`/investigation/${identityId}`);
    }

    // === Anomaly & Alerts ===
    static getAnomalies(params = {}) {
        const query = new URLSearchParams(params).toString();
        return this.request(`/anomalies${query ? '?' + query : ''}`);
    }

    static acknowledgeAnomaly(anomalyId) {
        return this.request(`/anomalies/${anomalyId}/acknowledge`, { method: 'PATCH' });
    }

    // === Self Service Portal ===
    static getMyAlerts() {
        return this.request('/self-service/my-alerts');
    }

    static respondToAlert(alertId, responseAction) {
        return this.request(`/self-service/alerts/${alertId}/respond`, {
            method: 'POST',
            body: { response: responseAction }
        });
    }

    static getMySessions() {
        return this.request('/self-service/my-sessions');
    }

    static terminateSession(sessionId) {
        return this.request(`/self-service/sessions/${sessionId}`, { method: 'DELETE' });
    }

    static terminateAllSessions() {
        return this.request('/self-service/sessions/all', { method: 'DELETE' });
    }

    static getMyDevices() {
        return this.request('/self-service/my-devices');
    }

    // === IAM Identity & Risk Rules ===
    static getIamIdentities(params = {}) {
        const query = new URLSearchParams(params).toString();
        return this.request(`/iam-identities${query ? '?' + query : ''}`);
    }

    static toggleIdentityMonitoring(identityId) {
        return this.request(`/iam-identities/${identityId}/toggle-monitor`, { method: 'PATCH' });
    }

    static getRiskRules() {
        return this.request('/risk-rules');
    }

    static updateRiskRule(ruleId, ruleData) {
        return this.request(`/risk-rules/${ruleId}`, { method: 'PUT', body: ruleData });
    }

    // === Activity Logs & Audit Logs ===
    static getActivityLogs(params = {}) {
        const query = new URLSearchParams(params).toString();
        return this.request(`/activity-logs${query ? '?' + query : ''}`);
    }

    static getAuditLogs(params = {}) {
        const query = new URLSearchParams(params).toString();
        return this.request(`/audit-logs${query ? '?' + query : ''}`);
    }

    // === Notifications ===
    static getUnreadNotifications() {
        return this.request('/notifications/unread');
    }

    static getUnreadCount() {
        return this.request('/notifications/unread-count');
    }

    static markNotificationAsRead(id) {
        return this.request(`/notifications/${id}/read`, { method: 'PATCH' });
    }

    static markAllNotificationsAsRead() {
        return this.request('/notifications/read-all', { method: 'PATCH' });
    }

    // === System Settings ===
    static getSystemSettings() {
        return this.request('/settings');
    }

    static updateSystemSetting(key, value) {
        return this.request(`/settings/${key}`, { method: 'PUT', body: { value } });
    }

    // === AWS Accounts (Multi-Account) ===
    static getAwsAccounts() {
        return this.request('/aws-accounts');
    }

    static createAwsAccount(data) {
        return this.request('/aws-accounts', { method: 'POST', body: data });
    }

    static updateAwsAccount(id, data) {
        return this.request(`/aws-accounts/${id}`, { method: 'PUT', body: data });
    }

    static deleteAwsAccount(id) {
        return this.request(`/aws-accounts/${id}`, { method: 'DELETE' });
    }

    // === Response Policies ===
    static getResponsePolicies() {
        return this.request('/response-policies');
    }

    static createResponsePolicy(data) {
        return this.request('/response-policies', { method: 'POST', body: data });
    }

    static updateResponsePolicy(id, data) {
        return this.request(`/response-policies/${id}`, { method: 'PUT', body: data });
    }

    static deleteResponsePolicy(id) {
        return this.request(`/response-policies/${id}`, { method: 'DELETE' });
    }

    // === Simulation Mode ===
    static getSimulationScenarios() {
        return this.request('/simulation/scenarios');
    }

    static launchSimulation(scenario) {
        return this.request(`/simulation/start/${scenario}`, { method: 'POST' });
    }

    static stopSimulation() {
        return this.request('/simulation/stop', { method: 'POST' });
    }

    // === Analytics & Reporting ===
    static getBehavioralDrift(identityId) {
        return this.request(`/analytics/drift/${identityId}`);
    }

    static getTopRiskyApis() {
        return this.request('/analytics/top-risky-apis');
    }

    static getRiskDistribution() {
        return this.request('/analytics/risk-distribution');
    }

    static exportReportCsv() {
        return fetch('/api/v1/reports/export/csv', {
            headers: this.getHeaders()
        }).then(res => res.blob());
    }

    // === FinOps Cost Dashboard ===
    static getFinOpsSummary() {
        return this.request('/finops/summary');
    }
}
