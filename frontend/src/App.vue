<template>
  <router-view />
</template>

<style>
:root {
  --color-primary: #2563eb;
  --color-secondary: #14b8a6;
  --color-bg: #f5f8fc;
  --color-card: #ffffff;
  --color-success: #22c55e;
  --color-warning: #f59e0b;
  --color-danger: #ef4444;
  --color-text: #1f2937;
  --color-muted: #6b7280;
  --color-border: #e5e7eb;
  --shadow-card: 0 14px 36px rgba(31, 41, 55, 0.08);
}

* {
  box-sizing: border-box;
}

body {
  margin: 0;
  min-width: 320px;
  color: var(--color-text);
  background: #f7fbff;
  font-family:
    Inter, "PingFang SC", "Microsoft YaHei", Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  text-rendering: geometricPrecision;
}

button,
input {
  font: inherit;
}

button {
  cursor: pointer;
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.62;
}

a {
  color: inherit;
  text-decoration: none;
}

.page-title {
  margin: 0;
  color: var(--color-text);
  font-size: 26px;
  font-weight: 800;
  letter-spacing: 0;
}

.page-subtitle {
  margin: 10px 0 0;
  color: var(--color-muted);
  font-size: 14px;
  line-height: 1.7;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 20px;
  margin-top: 24px;
}

.info-card {
  margin-top: 20px;
  padding: 24px;
  border: 1px solid var(--color-border);
  border-radius: 16px;
  background: var(--color-card);
  box-shadow: var(--shadow-card);
}

.info-card h3 {
  margin: 0 0 10px;
  color: var(--color-text);
  font-size: 18px;
}

.info-card p {
  margin: 0;
  color: var(--color-muted);
  line-height: 1.8;
}

.dashboard-hero {
  position: relative;
  overflow: hidden;
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: center;
  padding: 30px;
  border-color: rgba(37, 99, 235, 0.14);
  border-radius: 20px;
  background:
    radial-gradient(circle at 92% 18%, rgba(20, 184, 166, 0.18), transparent 26%),
    linear-gradient(135deg, #ffffff 0%, #eef6ff 56%, #ecfeff 100%);
  box-shadow: 0 20px 54px rgba(15, 23, 42, 0.1);
}

.dashboard-hero::before {
  content: '+';
  position: absolute;
  right: 28px;
  bottom: -26px;
  color: rgba(37, 99, 235, 0.08);
  font-size: 128px;
  font-weight: 900;
  line-height: 1;
}

.dashboard-hero > * {
  position: relative;
  z-index: 1;
}

.dashboard-hero .page-title {
  margin-top: 8px;
  font-size: 30px;
}

.hero-kicker {
  display: inline-flex;
  padding: 6px 10px;
  border-radius: 999px;
  color: #1d4ed8;
  background: #dbeafe;
  font-size: 12px;
  font-weight: 900;
}

.hero-chip {
  display: inline-grid;
  min-width: 112px;
  min-height: 112px;
  place-items: center;
  border: 1px solid rgba(20, 184, 166, 0.24);
  border-radius: 28px;
  color: #0f766e;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.84), rgba(204, 251, 241, 0.82)),
    #ffffff;
  box-shadow: 0 16px 34px rgba(20, 184, 166, 0.14);
  font-weight: 900;
}

.feature-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-top: 18px;
}

.feature-strip > div {
  display: grid;
  gap: 8px;
  padding: 18px;
  border: 1px solid rgba(229, 231, 235, 0.9);
  border-radius: 16px;
  background: #ffffff;
  box-shadow: 0 12px 30px rgba(31, 41, 55, 0.06);
}

.feature-strip span {
  display: inline-grid;
  width: 38px;
  height: 38px;
  place-items: center;
  border-radius: 13px;
  color: #ffffff;
  background: linear-gradient(135deg, var(--color-primary), var(--color-secondary));
  font-weight: 900;
}

.feature-strip strong {
  color: var(--color-text);
}

.feature-strip p {
  margin: 0;
  color: var(--color-muted);
  font-size: 14px;
  line-height: 1.7;
}

.quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 18px;
}

.quick-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 42px;
  padding: 10px 16px;
  border-radius: 12px;
  color: #ffffff;
  background: linear-gradient(135deg, var(--color-primary), var(--color-secondary));
  box-shadow: 0 10px 22px rgba(37, 99, 235, 0.18);
  font-weight: 800;
}

.quick-link:hover {
  filter: brightness(0.97);
}

.error-text {
  margin-top: 12px;
  padding: 10px 12px;
  border: 1px solid #fecaca;
  border-radius: 12px;
  color: #b91c1c;
  background: #fef2f2;
  font-size: 14px;
}

.success-text {
  margin-top: 12px;
  padding: 10px 12px;
  border: 1px solid #bbf7d0;
  border-radius: 12px;
  color: #15803d;
  background: #f0fdf4;
  font-size: 14px;
}

.toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  margin-top: 24px;
}

.search-input {
  width: min(360px, 100%);
  min-height: 42px;
  padding: 11px 14px;
  border: 1px solid var(--color-border);
  border-radius: 12px;
  outline: none;
  background: #ffffff;
}

.search-input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.1);
}

.primary-button,
.secondary-button,
.danger-button {
  min-height: 40px;
  padding: 10px 15px;
  border-radius: 12px;
  font-weight: 800;
}

.primary-button {
  border: 1px solid var(--color-primary);
  color: #ffffff;
  background: var(--color-primary);
  box-shadow: 0 8px 18px rgba(37, 99, 235, 0.18);
}

.primary-button:hover {
  background: #1d4ed8;
}

.secondary-button {
  border: 1px solid var(--color-border);
  color: var(--color-primary);
  background: #ffffff;
}

.secondary-button:hover {
  background: #eff6ff;
}

.danger-button {
  border: 1px solid #fecaca;
  color: #dc2626;
  background: #fef2f2;
}

.danger-button:hover {
  background: #fee2e2;
}

.table-actions {
  display: flex;
  gap: 8px;
  white-space: nowrap;
}

.muted-text {
  color: #94a3b8;
}

.form-overlay {
  position: fixed;
  inset: 0;
  z-index: 20;
  display: grid;
  place-items: center;
  padding: 22px;
  background: rgba(15, 23, 42, 0.46);
  backdrop-filter: blur(4px);
}

.form-card {
  width: min(720px, 100%);
  max-height: calc(100vh - 44px);
  overflow: auto;
  padding: 26px;
  border: 1px solid var(--color-border);
  border-radius: 16px;
  background: #ffffff;
  box-shadow: 0 24px 70px rgba(15, 23, 42, 0.24);
}

.form-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 20px;
}

.form-header h2 {
  margin: 0;
  font-size: 20px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.form-field {
  display: grid;
  gap: 8px;
  color: #374151;
  font-weight: 700;
}

.form-field.full {
  grid-column: 1 / -1;
}

.form-field input,
.form-field select,
.form-field textarea {
  width: 100%;
  padding: 12px 13px;
  border: 1px solid var(--color-border);
  border-radius: 12px;
  outline: none;
  resize: vertical;
  background: #ffffff;
}

.form-field input:focus,
.form-field select:focus,
.form-field textarea:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.1);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 22px;
}

@media (max-width: 960px) {
  .dashboard-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .feature-strip {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .dashboard-hero {
    align-items: flex-start;
    flex-direction: column;
    padding: 22px;
  }

  .hero-chip {
    min-width: 100%;
    min-height: 64px;
    border-radius: 18px;
  }

  .toolbar,
  .form-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
