<template>
  <div class="auth-page">
    <main class="auth-shell">
      <section class="brand-panel">
      <div class="brand-mark">
          <span class="medical-cross"><Cross :size="25" :stroke-width="2.5" /></span>
        <div>
          <strong>LocalCare AI</strong>
            <small>基层智慧诊疗工作台</small>
        </div>
      </div>

        <div class="brand-copy">
          <span class="product-tag">Clinical Workspace</span>
          <h1>让每一次基层诊疗<br />都有清晰记录</h1>
          <p>患者问诊、医生接诊与诊疗结果，在同一条临床工作流中协同完成。</p>
      </div>

        <div class="trust-note">
          <ShieldCheck :size="18" />
          <span>AI 内容仅供辅助参考，诊疗结论由医生确认</span>
      </div>
    </section>

      <section class="login-panel">
      <div class="auth-card-header">
        <div>
            <span>{{ mode === 'login' ? '欢迎回来' : '创建患者账号' }}</span>
          <h2>{{ mode === 'login' ? '登录诊疗工作台' : '患者注册' }}</h2>
          <p>{{ mode === 'login' ? '输入账号信息，或选择下方演示身份。' : '医生和院内维护账号由院内统一配置。' }}</p>
        </div>
        <button class="switch-button" type="button" @click="toggleMode">
            {{ mode === 'login' ? '患者注册' : '返回登录' }}
        </button>
      </div>

        <form v-if="mode === 'login'" class="auth-form" @submit.prevent="handleLogin">
        <label>
          <span>用户名</span>
            <div class="input-shell">
              <UserRound :size="18" />
              <input v-model.trim="form.username" autocomplete="username" placeholder="请输入用户名" />
            </div>
        </label>

        <label>
          <span>密码</span>
            <div class="input-shell">
              <LockKeyhole :size="18" />
              <input
                v-model.trim="form.password"
                autocomplete="current-password"
                placeholder="请输入密码"
                :type="showPassword ? 'text' : 'password'"
              />
              <button class="password-toggle" type="button" :aria-label="showPassword ? '隐藏密码' : '显示密码'" @click="showPassword = !showPassword">
                <EyeOff v-if="showPassword" :size="18" />
                <Eye v-else :size="18" />
              </button>
            </div>
        </label>

          <div v-if="errorMessage" class="error-text" aria-live="polite">{{ errorMessage }}</div>
          <div v-if="successMessage" class="success-text" aria-live="polite">{{ successMessage }}</div>

          <button class="submit-button" type="submit" :disabled="loading">
            {{ loading ? '正在进入工作台' : '登录工作台' }}
        </button>
      </form>

        <form v-else class="auth-form register-form" @submit.prevent="handleRegister">
        <label>
          <span>用户名</span>
            <div class="input-shell"><input v-model.trim="registerForm.username" autocomplete="username" placeholder="请输入用户名" /></div>
        </label>
        <label>
          <span>真实姓名</span>
            <div class="input-shell"><input v-model.trim="registerForm.name" placeholder="请输入真实姓名" /></div>
        </label>
        <label>
          <span>手机号</span>
            <div class="input-shell"><input v-model.trim="registerForm.phone" inputmode="tel" placeholder="请输入手机号" type="tel" /></div>
        </label>
        <label>
          <span>邮箱（可选）</span>
            <div class="input-shell"><input v-model.trim="registerForm.email" autocomplete="email" placeholder="请输入邮箱" type="email" /></div>
        </label>
        <label>
          <span>密码</span>
            <div class="input-shell"><input v-model.trim="registerForm.password" autocomplete="new-password" placeholder="至少 6 位" type="password" /></div>
        </label>
        <label>
          <span>确认密码</span>
            <div class="input-shell"><input v-model.trim="registerForm.confirmPassword" autocomplete="new-password" placeholder="请再次输入密码" type="password" /></div>
        </label>

          <div v-if="errorMessage" class="error-text" aria-live="polite">{{ errorMessage }}</div>
          <div v-if="successMessage" class="success-text" aria-live="polite">{{ successMessage }}</div>

          <button class="submit-button" type="submit" :disabled="registerLoading">
            {{ registerLoading ? '正在创建患者账号' : '确认注册' }}
        </button>
      </form>

      <div v-if="mode === 'login'" class="account-list">
        <div class="account-title">
            <div>
              <h3>演示身份</h3>
              <p>选择身份后即可填充对应账号</p>
            </div>
            <span>Demo</span>
        </div>
          <div class="account-options">
            <button
              v-for="account in demoAccounts"
              :key="account.username"
              class="account-item"
              :class="{ selected: form.username === account.username }"
              type="button"
              @click="fillAccount(account.username)"
            >
              <span class="account-icon"><component :is="account.icon" :size="18" /></span>
              <span class="account-copy"><strong>{{ account.label }}</strong><small>{{ account.username }} / 123456</small></span>
              <Check v-if="form.username === account.username" :size="17" />
            </button>
          </div>
      </div>
    </section>
    </main>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Building2, Check, Cross, Eye, EyeOff, LockKeyhole, ShieldCheck, Stethoscope, UserRound } from '@lucide/vue'
import { roleHome, useAuthStore } from '../app/store'
import api from '../services/api'

const router = useRouter()
const auth = useAuthStore()
const showPassword = ref(false)

const demoAccounts = [
  { label: '院内维护员', username: 'admin', icon: Building2 },
  { label: '医生', username: 'doctor', icon: Stethoscope },
  { label: '患者', username: 'patient', icon: UserRound }
]

const form = reactive({
  username: 'admin',
  password: '123456'
})

const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  name: '',
  phone: '',
  email: ''
})

const mode = ref('login')
const loading = ref(false)
const registerLoading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

async function handleLogin() {
  errorMessage.value = ''
  successMessage.value = ''

  if (!form.username || !form.password) {
    errorMessage.value = '请输入用户名和密码'
    return
  }

  loading.value = true
  try {
    const user = await auth.login(form.username, form.password)
    router.push(roleHome(user.role))
  } catch (error) {
    errorMessage.value = error.message || '账号信息校验未通过'
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  errorMessage.value = ''
  successMessage.value = ''

  if (!registerForm.username || !registerForm.password || !registerForm.confirmPassword || !registerForm.name || !registerForm.phone) {
    errorMessage.value = '请填写用户名、密码、确认密码、真实姓名和手机号'
    return
  }
  if (registerForm.password.length < 6) {
    errorMessage.value = '密码长度至少 6 位'
    return
  }
  if (registerForm.password !== registerForm.confirmPassword) {
    errorMessage.value = '两次密码不一致'
    return
  }

  registerLoading.value = true
  try {
    const result = await api.register(registerForm)
    form.username = result?.username || registerForm.username
    form.password = ''
    resetRegisterForm()
    mode.value = 'login'
    successMessage.value = result?.message || '信息已更新，请登录'
  } catch (error) {
    errorMessage.value = error.message || '数据同步失败'
  } finally {
    registerLoading.value = false
  }
}

function toggleMode() {
  mode.value = mode.value === 'login' ? 'register' : 'login'
  errorMessage.value = ''
  successMessage.value = ''
}

function fillAccount(username) {
  form.username = username
  form.password = '123456'
}

function resetRegisterForm() {
  Object.assign(registerForm, {
    username: '',
    password: '',
    confirmPassword: '',
    name: '',
    phone: '',
    email: ''
  })
}
</script>

<style scoped>
.auth-page {
  position: relative;
  overflow: hidden;
  display: grid;
  min-height: 100vh;
  grid-template-columns: minmax(0, 1.15fr) minmax(400px, 460px);
  gap: 42px;
  align-items: center;
  padding: 54px;
  background:
    linear-gradient(120deg, rgba(37, 99, 235, 0.12), transparent 32%),
    radial-gradient(circle at 12% 18%, rgba(37, 99, 235, 0.18), transparent 28%),
    radial-gradient(circle at 88% 84%, rgba(20, 184, 166, 0.16), transparent 30%),
    linear-gradient(135deg, #f8fbff 0%, #eef7ff 48%, #ecfeff 100%);
}

.auth-orb {
  position: absolute;
  border-radius: 999px;
  filter: blur(2px);
  opacity: 0.55;
  pointer-events: none;
}

.orb-one {
  width: 210px;
  height: 210px;
  top: -60px;
  right: 30%;
  background: rgba(37, 99, 235, 0.16);
}

.orb-two {
  width: 260px;
  height: 260px;
  right: -80px;
  bottom: -80px;
  background: rgba(20, 184, 166, 0.14);
}

.hero-panel {
  position: relative;
  z-index: 1;
  padding: 48px;
  border: 1px solid rgba(37, 99, 235, 0.16);
  border-radius: 28px;
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.9), rgba(240, 253, 250, 0.64)),
    rgba(255, 255, 255, 0.76);
  box-shadow: 0 28px 78px rgba(15, 23, 42, 0.1);
  backdrop-filter: blur(14px);
}

.brand-mark {
  display: flex;
  gap: 14px;
  align-items: center;
  margin-bottom: 26px;
}

.medical-cross {
  display: inline-grid;
  width: 46px;
  height: 46px;
  place-items: center;
  border-radius: 16px;
  color: #ffffff;
  background: linear-gradient(135deg, #2563eb, #14b8a6);
  font-size: 30px;
  font-weight: 900;
  box-shadow: 0 14px 28px rgba(37, 99, 235, 0.24);
}

.brand-mark strong {
  display: block;
  color: #111827;
  font-size: 20px;
}

.brand-mark small {
  color: #64748b;
}

.badge {
  display: inline-flex;
  padding: 7px 12px;
  border-radius: 999px;
  color: #1d4ed8;
  background: #dbeafe;
  font-size: 13px;
  font-weight: 800;
}

.hero-panel h1 {
  max-width: 780px;
  margin: 20px 0 12px;
  color: #1f2937;
  font-size: 48px;
  line-height: 1.12;
  letter-spacing: 0;
}

.slogan {
  margin: 0;
  color: #0f766e;
  font-size: 20px;
  font-weight: 900;
  line-height: 1.7;
}

.hero-desc {
  margin: 0;
  max-width: 720px;
  color: #6b7280;
  font-size: 17px;
  line-height: 1.8;
}

.feature-grid {
  display: grid;
  max-width: 720px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: 32px;
}

.feature-grid div {
  display: grid;
  gap: 10px;
  padding: 18px;
  border: 1px solid rgba(37, 99, 235, 0.13);
  border-radius: 18px;
  color: #1f2937;
  background: rgba(255, 255, 255, 0.82);
  font-weight: 800;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.06);
}

.feature-grid span {
  display: inline-grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border-radius: 12px;
  color: #1d4ed8;
  background: #dbeafe;
  font-size: 13px;
  font-weight: 900;
}

.pulse-card {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  max-width: 720px;
  margin-top: 30px;
  padding: 16px;
  border: 1px solid #ccfbf1;
  border-radius: 18px;
  background: rgba(240, 253, 250, 0.72);
  color: #0f766e;
}

.pulse-card span {
  width: 40px;
  height: 20px;
  border-bottom: 3px solid #14b8a6;
  border-left: 3px solid transparent;
  transform: skewX(-24deg);
}

.pulse-card i {
  width: 44px;
  height: 2px;
  background: linear-gradient(90deg, #14b8a6, #2563eb);
}

.account-list {
  display: grid;
  gap: 10px;
  margin-top: 24px;
  padding-top: 22px;
  border-top: 1px solid #e5e7eb;
}

.account-title {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.account-title h3 {
  margin: 0 0 4px;
  font-size: 16px;
}

.account-title span {
  padding: 5px 8px;
  border-radius: 999px;
  color: #0f766e;
  background: #ccfbf1;
  font-size: 12px;
  font-weight: 800;
}

.account-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 13px;
  border: 1px solid #e8eef7;
  border-radius: 14px;
  color: #6b7280;
  background: linear-gradient(135deg, #ffffff, #f8fbff);
  box-shadow: none;
  transition:
    border-color 0.2s ease,
    transform 0.2s ease,
    box-shadow 0.2s ease;
}

.account-item:hover {
  border-color: #bfdbfe;
  transform: translateY(-1px);
  box-shadow: 0 10px 22px rgba(37, 99, 235, 0.1);
}

.account-item span {
  display: inline-flex;
  gap: 8px;
  align-items: center;
}

.account-item i {
  display: inline-grid;
  width: 26px;
  height: 26px;
  place-items: center;
  border-radius: 9px;
  color: #ffffff;
  background: #2563eb;
  font-style: normal;
  font-size: 12px;
  font-weight: 900;
}

.account-item strong {
  color: #2563eb;
}

.login-card {
  position: relative;
  z-index: 1;
  padding: 34px;
  border: 1px solid #e5e7eb;
  border-radius: 24px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 252, 0.98)),
    #ffffff;
  box-shadow: 0 28px 72px rgba(15, 23, 42, 0.13);
}

.login-card h2 {
  margin: 0 0 8px;
  color: #1f2937;
  font-size: 26px;
}

.login-card p {
  margin: 0 0 28px;
  color: #6b7280;
}

.auth-card-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 24px;
}

.auth-card-header p {
  margin-bottom: 0;
}

.switch-button {
  padding: 8px 12px;
  border: 1px solid #bfdbfe;
  border-radius: 999px;
  color: #1d4ed8;
  background: #eff6ff;
  font-weight: 800;
  white-space: nowrap;
  transition:
    background 0.2s ease,
    transform 0.2s ease;
}

.switch-button:hover {
  background: #dbeafe;
  transform: translateY(-1px);
}

form {
  display: grid;
  gap: 18px;
}

label {
  display: grid;
  gap: 8px;
  color: #374151;
  font-weight: 700;
}

input {
  width: 100%;
  padding: 13px 14px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  outline: none;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease;
}

input:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.1);
}

form button {
  width: 100%;
  padding: 13px 16px;
  border: 0;
  border-radius: 12px;
  color: #ffffff;
  background: linear-gradient(135deg, #2563eb, #14b8a6);
  font-weight: 800;
  box-shadow: 0 12px 24px rgba(37, 99, 235, 0.22);
}

form button:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}

form button:not(:disabled):hover {
  transform: translateY(-1px);
  box-shadow: 0 16px 30px rgba(37, 99, 235, 0.26);
}

.success-text {
  padding: 10px 12px;
  border-radius: 12px;
  color: #0f766e;
  background: #ccfbf1;
  line-height: 1.6;
}

@media (max-width: 900px) {
  .auth-page {
    grid-template-columns: 1fr;
    padding: 24px;
  }

  .hero-panel h1 {
    font-size: 32px;
  }

  .feature-grid {
    grid-template-columns: 1fr;
  }

  .login-card {
    max-width: none;
  }

  .auth-card-header {
    flex-direction: column;
  }
}

/* Product login surface */
.auth-page {
  display: flex;
  min-height: 100svh;
  align-items: center;
  justify-content: center;
  padding: 28px;
  background: #edf4f6;
}

.auth-shell {
  display: flex;
  width: min(980px, 100%);
  min-height: 610px;
  overflow: hidden;
  border: 1px solid #d5e2e7;
  border-radius: 24px;
  background: #ffffff;
  box-shadow: 0 30px 80px rgba(15, 53, 64, 0.14);
}

.brand-panel {
  display: flex;
  width: 38%;
  flex: 0 0 38%;
  flex-direction: column;
  justify-content: space-between;
  padding: 40px;
  color: #ffffff;
  background: #0f6170;
}

.brand-mark {
  margin: 0;
}

.medical-cross {
  display: flex;
  width: 44px;
  height: 44px;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(255, 255, 255, 0.28);
  border-radius: 14px;
  color: #0f6170;
  background: #ffffff;
  box-shadow: none;
}

.brand-mark strong {
  color: #ffffff;
  font-size: 20px;
}

.brand-mark small {
  display: block;
  margin-top: 3px;
  color: rgba(255, 255, 255, 0.7);
  font-size: 12px;
}

.brand-copy {
  margin: auto 0;
}

.product-tag {
  display: inline-flex;
  padding: 6px 9px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 999px;
  color: #ccfbf1;
  background: rgba(255, 255, 255, 0.08);
  font-size: 11px;
  font-weight: 900;
  text-transform: uppercase;
}

.brand-copy h1 {
  margin: 20px 0 0;
  color: #ffffff;
  font-size: 34px;
  font-weight: 900;
  line-height: 1.35;
  letter-spacing: 0;
}

.brand-copy p {
  margin: 16px 0 0;
  color: rgba(255, 255, 255, 0.74);
  font-size: 14px;
  line-height: 1.85;
}

.trust-note {
  display: flex;
  gap: 9px;
  align-items: flex-start;
  padding-top: 18px;
  border-top: 1px solid rgba(255, 255, 255, 0.15);
  color: rgba(255, 255, 255, 0.72);
  font-size: 12px;
  line-height: 1.6;
}

.trust-note svg {
  flex: 0 0 auto;
  color: #99f6e4;
}

.login-panel {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  justify-content: center;
  padding: 42px 48px;
}

.auth-card-header {
  margin: 0 0 26px;
}

.auth-card-header > div > span {
  display: block;
  margin-bottom: 7px;
  color: #0f766e;
  font-size: 12px;
  font-weight: 900;
}

.auth-card-header h2 {
  margin: 0;
  color: #0f172a;
  font-size: 27px;
  font-weight: 900;
  letter-spacing: 0;
}

.auth-card-header p {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}

.switch-button {
  padding: 8px 11px;
  border-color: #cbdfe5;
  border-radius: 10px;
  color: #0f6170;
  background: #f4fafb;
  font-size: 13px;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.auth-form label {
  display: flex;
  flex-direction: column;
  gap: 7px;
  color: #334155;
  font-size: 13px;
  font-weight: 800;
}

.input-shell {
  display: flex;
  min-height: 46px;
  align-items: center;
  gap: 10px;
  padding: 0 13px;
  border: 1px solid #dbe5ea;
  border-radius: 11px;
  color: #94a3b8;
  background: #ffffff;
  transition: border-color 0.18s ease, box-shadow 0.18s ease;
}

.input-shell:focus-within {
  border-color: #0891b2;
  box-shadow: 0 0 0 4px rgba(8, 145, 178, 0.1);
}

.input-shell input {
  min-width: 0;
  flex: 1;
  padding: 0;
  border: 0;
  border-radius: 0;
  outline: 0;
  color: #0f172a;
  background: transparent;
  box-shadow: none;
}

.input-shell input:focus {
  border: 0;
  box-shadow: none;
}

.password-toggle,
.auth-form .password-toggle {
  display: inline-flex;
  width: 30px;
  height: 30px;
  flex: 0 0 30px;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 0;
  border-radius: 8px;
  color: #64748b;
  background: transparent;
  box-shadow: none;
}

.submit-button,
.auth-form .submit-button {
  width: 100%;
  min-height: 46px;
  padding: 0 16px;
  border: 0;
  border-radius: 11px;
  color: #ffffff;
  background: #0f766e;
  box-shadow: 0 12px 24px rgba(15, 118, 110, 0.2);
  font-weight: 900;
}

.submit-button:not(:disabled):hover,
.auth-form .submit-button:not(:disabled):hover {
  background: #0d655f;
  transform: none;
  box-shadow: 0 14px 28px rgba(15, 118, 110, 0.24);
}

.account-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #e2e8f0;
}

.account-title h3 {
  margin: 0;
  color: #0f172a;
  font-size: 14px;
  font-weight: 900;
}

.account-title p {
  margin: 4px 0 0;
  color: #94a3b8;
  font-size: 12px;
}

.account-title > span {
  color: #0f766e;
  background: #ccfbf1;
}

.account-options {
  display: flex;
  flex-wrap: wrap;
  gap: 9px;
}

.account-item {
  display: flex;
  min-width: 145px;
  min-height: 58px;
  flex: 1 1 145px;
  justify-content: flex-start;
  gap: 10px;
  padding: 9px 11px;
  border: 1px solid #dfe8ed;
  border-radius: 11px;
  color: #64748b;
  background: #f8fbfc;
  box-shadow: none;
}

.account-item:hover,
.account-item.selected {
  border-color: #65b7c2;
  background: #eefafa;
  box-shadow: none;
  transform: none;
}

.account-item > svg {
  margin-left: auto;
  color: #0f766e;
}

.account-icon {
  display: flex;
  width: 32px;
  height: 32px;
  flex: 0 0 32px;
  align-items: center;
  justify-content: center;
  border-radius: 9px;
  color: #0f6170;
  background: #dff3f5;
}

.account-copy {
  display: flex !important;
  min-width: 0;
  flex-direction: column;
  gap: 2px !important;
  align-items: flex-start !important;
}

.account-copy strong {
  color: #0f172a;
  font-size: 13px;
}

.account-copy small {
  color: #64748b;
  font-size: 10px;
  white-space: nowrap;
}

.register-form {
  flex-flow: row wrap;
}

.register-form label {
  min-width: 190px;
  flex: 1 1 calc(50% - 8px);
}

.register-form .error-text,
.register-form .success-text,
.register-form .submit-button {
  flex-basis: 100%;
}

button:focus-visible,
input:focus-visible {
  outline: 3px solid rgba(8, 145, 178, 0.28);
  outline-offset: 2px;
}

@media (max-width: 760px) {
  .auth-page {
    align-items: flex-start;
    padding: 0;
    background: #ffffff;
  }

  .auth-shell {
    min-height: 100svh;
    flex-direction: column;
    border: 0;
    border-radius: 0;
    box-shadow: none;
  }

  .brand-panel {
    width: 100%;
    flex: 0 0 auto;
    padding: 20px 22px;
  }

  .brand-copy {
    margin: 22px 0 0;
  }

  .product-tag,
  .brand-copy p,
  .trust-note {
    display: none;
  }

  .brand-copy h1 {
    margin: 0;
    font-size: 24px;
    line-height: 1.4;
  }

  .login-panel {
    justify-content: flex-start;
    padding: 26px 22px 36px;
  }

  .auth-card-header {
    align-items: flex-start;
    flex-direction: row;
  }

  .auth-card-header h2 {
    font-size: 24px;
  }

  .account-item {
    min-width: 100%;
    flex-basis: 100%;
  }

  .register-form label {
    min-width: 100%;
    flex-basis: 100%;
  }
}
</style>
