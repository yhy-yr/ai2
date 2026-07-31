<template>
  <header class="clinical-topbar">
    <div class="product-mark">
      <span><ShieldPlus :size="22" /></span>
      <div>
        <strong>诊疗工作台</strong>
        <small>基层门诊 · AI 辅助接诊</small>
      </div>
    </div>

    <div class="topbar-actions">
      <div class="doctor-card">
        <img src="https://images.unsplash.com/photo-1594824476967-48c8b964273f?auto=format&fit=crop&w=120&q=80" alt="医生头像" />
        <div>
          <strong>{{ auth.user?.realName || '李医生' }}</strong>
          <span>{{ roleText }}</span>
        </div>
      </div>
      <button class="logout-button" type="button" aria-label="退出账号" @click="handleLogout">
        <LogOut :size="17" />
      </button>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { LogOut, ShieldPlus } from '@lucide/vue'
import { useAuthStore } from '../../app/store'

const router = useRouter()
const auth = useAuthStore()

const roleText = computed(() => {
  if (auth.user?.role === 'ADMIN') {
    return '院内维护'
  }
  if (auth.user?.role === 'DOCTOR') {
    return '内科门诊'
  }
  if (auth.user?.role === 'PATIENT') {
    return '患者端'
  }
  return '诊疗协同'
})

async function handleLogout() {
  await auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.clinical-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 72px;
  padding: 0 24px 0 28px;
  border-bottom: 1px solid rgba(207, 222, 235, 0.82);
  background: rgba(251, 254, 255, 0.9);
  backdrop-filter: blur(18px);
}

.product-mark {
  display: flex;
  gap: 12px;
  align-items: center;
}

.product-mark > span {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  border: 1px solid rgba(8, 145, 178, 0.16);
  border-radius: 13px;
  color: #0891b2;
  background: #e6f8fb;
}

.product-mark strong,
.doctor-card strong {
  display: block;
  color: #0f172a;
  font-weight: 900;
}

.product-mark small,
.doctor-card span {
  display: block;
  margin-top: 3px;
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

.topbar-actions {
  display: flex;
  gap: 14px;
  align-items: center;
}

.logout-button {
  position: relative;
  display: inline-grid;
  width: 38px;
  height: 38px;
  place-items: center;
  border: 1px solid #dbe5ef;
  border-radius: 13px;
  color: #334155;
  background: #ffffff;
}

.doctor-card {
  display: flex;
  gap: 10px;
  align-items: center;
  min-width: 142px;
}

.doctor-card img {
  width: 38px;
  height: 38px;
  border-radius: 999px;
  object-fit: cover;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.13);
}

.logout-button {
  color: #0e7490;
}

@media (max-width: 760px) {
  .clinical-topbar {
    gap: 14px;
    align-items: flex-start;
    flex-direction: column;
    padding: 18px 20px;
  }

  .topbar-actions {
    width: 100%;
    justify-content: space-between;
  }
}
</style>
