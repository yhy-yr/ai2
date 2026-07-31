<template>
  <section class="role-home admin-home">
    <header class="role-hero">
      <div>
        <span class="eyebrow">院内运营</span>
        <h1>院内诊疗协同工作台</h1>
        <p>查看今日接诊、资料维护、处方与检查结果同步情况。</p>
      </div>
      <div class="hero-metric">
        <strong>{{ loading ? '--' : `${stats.todayCompletionRate}%` }}</strong>
        <span>今日流程完成度</span>
      </div>
    </header>

    <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>

    <div class="metric-grid">
      <section v-for="item in metrics" :key="item.label" class="metric-card">
        <span :class="item.tone"><component :is="item.icon" :size="20" /></span>
        <div>
          <strong>{{ item.value }}</strong>
          <p>{{ item.label }}</p>
        </div>
      </section>
    </div>

    <div class="admin-grid">
      <article class="product-panel">
        <div class="panel-head">
          <h2>业务动态</h2>
          <small>根据实时数据更新</small>
        </div>
        <div class="work-row">
          <span>检</span>
          <div>
            <strong>{{ stats.examinationCount }} 份检查结果已录入</strong>
            <p>可进入检查结果查看详细内容</p>
          </div>
          <router-link to="/workspace?workflow=examinations">查看</router-link>
        </div>
        <div class="work-row">
          <span>约</span>
          <div>
            <strong>{{ stats.actionableAppointmentCount }} 条预约待处理</strong>
            <p>包含待接诊和接诊中的预约</p>
          </div>
          <router-link to="/workspace?workflow=appointments">处理</router-link>
        </div>
        <div class="work-row">
          <span>药</span>
          <div>
            <strong>{{ stockSummaryTitle }}</strong>
            <p>{{ stockSummaryDescription }}</p>
          </div>
          <router-link to="/workspace?workflow=medicines">维护</router-link>
        </div>
      </article>

      <article class="product-panel">
        <div class="panel-head">
          <h2>院内资料入口</h2>
          <small>覆盖院内资料与诊疗协同流程</small>
        </div>
        <div class="entry-grid">
          <router-link v-for="entry in entries" :key="entry.path" :to="entry.path">
            <component :is="entry.icon" :size="19" />
            <span>{{ entry.label }}</span>
          </router-link>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  CalendarClock,
  ClipboardList,
  FileClock,
  FlaskConical,
  Hospital,
  Pill,
  Stethoscope,
  UsersRound,
} from '@lucide/vue'
import api from '../../services/api'

const loading = ref(false)
const errorMessage = ref('')
const stats = reactive({
  todayAppointmentCount: 0,
  inProgressAppointmentCount: 0,
  medicalRecordCount: 0,
  examinationCount: 0,
  actionableAppointmentCount: 0,
  lowStockMedicineCount: 0,
  todayCompletionRate: 0,
})

const metrics = computed(() => [
  { label: '今日预约', value: loading.value ? '--' : stats.todayAppointmentCount, icon: CalendarClock, tone: 'blue' },
  { label: '接诊中', value: loading.value ? '--' : stats.inProgressAppointmentCount, icon: Stethoscope, tone: 'teal' },
  { label: '病历记录', value: loading.value ? '--' : stats.medicalRecordCount, icon: FileClock, tone: 'green' },
  { label: '检查结果', value: loading.value ? '--' : stats.examinationCount, icon: FlaskConical, tone: 'rose' },
])

const stockSummaryTitle = computed(() => {
  return stats.lowStockMedicineCount > 0
    ? `${stats.lowStockMedicineCount} 种药品库存需补充`
    : '药品库存充足'
})

const stockSummaryDescription = computed(() => {
  return stats.lowStockMedicineCount > 0
    ? '库存低于 20 的药品需要及时补充'
    : '暂无低于安全库存的药品'
})

const entries = [
  { label: '患者资料', path: '/patients?view=patients', icon: UsersRound },
  { label: '医生档案', path: '/patients?view=doctors', icon: Stethoscope },
  { label: '科室维护', path: '/workspace?workflow=departments', icon: Hospital },
  { label: '药品维护', path: '/workspace?workflow=medicines', icon: Pill },
  { label: '预约协同', path: '/workspace?workflow=appointments', icon: CalendarClock },
  { label: '病历维护', path: '/workspace?workflow=records', icon: FileClock },
  { label: '处方维护', path: '/workspace?workflow=prescriptions', icon: ClipboardList },
  { label: '检查结果', path: '/workspace?workflow=examinations', icon: FlaskConical },
]

onMounted(loadStats)

async function loadStats() {
  loading.value = true
  errorMessage.value = ''
  try {
    Object.assign(stats, await api.adminDashboard())
  } catch (error) {
    errorMessage.value = error.message || '数据同步失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.role-home {
  display: grid;
  gap: 18px;
  padding-bottom: 32px;
}

.role-hero,
.metric-card,
.product-panel {
  border: 1px solid rgba(205, 218, 234, 0.82);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 18px 48px rgba(22, 45, 74, 0.07);
}

.role-hero {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: center;
  padding: 30px;
}

.eyebrow {
  color: #0891b2;
  font-size: 13px;
  font-weight: 900;
}

.role-hero h1 {
  margin: 8px 0 0;
  color: #0f172a;
  font-size: 31px;
  font-weight: 900;
  letter-spacing: 0;
}

.role-hero p {
  margin: 10px 0 0;
  color: #64748b;
  line-height: 1.7;
}

.hero-metric {
  display: grid;
  min-width: 142px;
  min-height: 112px;
  place-items: center;
  border-radius: 20px;
  background: linear-gradient(135deg, #e6f8fb, #f8fcff);
}

.hero-metric strong {
  color: #0f766e;
  font-size: 34px;
  font-weight: 900;
}

.hero-metric span {
  color: #64748b;
  font-size: 13px;
  font-weight: 800;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.metric-card {
  display: flex;
  gap: 14px;
  align-items: center;
  padding: 18px;
}

.metric-card > span {
  display: grid;
  width: 44px;
  height: 44px;
  place-items: center;
  border-radius: 15px;
}

.blue { color: #2563eb; background: #dbeafe; }
.teal { color: #0f766e; background: #ccfbf1; }
.green { color: #15803d; background: #dcfce7; }
.rose { color: #e11d48; background: #ffe4e6; }

.metric-card strong {
  color: #0f172a;
  font-size: 24px;
  font-weight: 900;
}

.metric-card p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 13px;
  font-weight: 800;
}

.admin-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(0, 0.95fr);
  gap: 18px;
}

.product-panel {
  padding: 22px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: baseline;
  margin-bottom: 16px;
}

.panel-head h2 {
  margin: 0;
  color: #0f172a;
  font-size: 19px;
  font-weight: 900;
}

.panel-head small {
  color: #94a3b8;
  font-weight: 800;
}

.work-row {
  display: grid;
  grid-template-columns: 36px 1fr auto;
  gap: 14px;
  align-items: center;
  min-height: 82px;
  border-top: 1px solid #edf2f7;
}

.work-row > span {
  display: grid;
  width: 30px;
  height: 30px;
  place-items: center;
  border-radius: 10px;
  color: #0e7490;
  background: #e6f8fb;
  font-weight: 900;
}

.work-row.high > span {
  color: #e11d48;
  background: #ffe4e6;
}

.work-row strong {
  color: #0f172a;
}

.work-row p {
  margin: 5px 0 0;
  color: #64748b;
  font-size: 13px;
}

.work-row a,
.entry-grid a {
  color: #0e7490;
  font-weight: 900;
}

.entry-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.entry-grid a {
  display: flex;
  gap: 10px;
  align-items: center;
  min-height: 54px;
  padding: 14px;
  border: 1px solid #e2edf5;
  border-radius: 15px;
  background: #f8fcff;
}

@media (max-width: 980px) {
  .role-hero,
  .admin-grid {
    grid-template-columns: 1fr;
  }

  .role-hero {
    align-items: flex-start;
    flex-direction: column;
  }

  .metric-grid,
  .entry-grid {
    grid-template-columns: 1fr;
  }
}
</style>
