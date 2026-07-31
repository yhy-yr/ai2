<template>
  <aside class="assist-panel">
    <div class="assist-head">
      <div>
        <Sparkles :size="22" />
        <h2>{{ panelTitle }}</h2>
      </div>
      <button type="button" :aria-label="collapsed ? '展开辅助面板' : '收起辅助面板'" @click="collapsed = !collapsed">
        <component :is="collapsed ? PanelRightOpen : Minus" :size="18" />
      </button>
    </div>

    <template v-if="!collapsed">
      <p v-if="loading" class="panel-message">正在获取患者信息</p>
      <p v-else-if="errorMessage" class="panel-message error">{{ errorMessage }}</p>

      <template v-if="isDoctor && !loading">
        <section v-if="currentAppointment" class="assist-section active-visit">
          <div class="assist-title">
            <span><Stethoscope :size="19" /></span>
            <strong>当前接诊</strong>
            <em>{{ statusText(currentAppointment.status) }}</em>
          </div>
          <div class="patient-line">
            <strong>{{ currentAppointment.patientName || '未命名患者' }}</strong>
            <small>{{ currentAppointment.appointmentDate }} {{ currentAppointment.timeSlot }}</small>
          </div>
          <p>{{ currentAppointment.symptomDescription || '暂无症状描述' }}</p>
          <router-link class="primary-panel-link" :to="visitAction.path">{{ visitAction.label }}</router-link>
        </section>

        <section v-if="currentAppointment && riskAssessment" class="assist-section">
          <div class="assist-title">
            <span><ShieldCheck :size="19" /></span>
            <strong>风险评估</strong>
            <em :class="riskTone">{{ riskAssessment.levelText || riskAssessment.level || '待评估' }}</em>
          </div>
          <p>{{ riskAssessment.advice || '暂无风险建议' }}</p>
          <div v-if="riskAssessment.suggestedChecks?.length" class="check-list">
            <div v-for="item in riskAssessment.suggestedChecks" :key="item">
              <CheckCircle2 :size="15" />
              <strong>{{ item }}</strong>
            </div>
          </div>
        </section>

        <section v-if="currentAppointment && visitCompleteness" class="assist-section">
          <div class="assist-title simple">
            <span><ClipboardCheck :size="19" /></span>
            <strong>接诊闭环</strong>
            <em>{{ visitCompleteness.percent ?? 0 }}%</em>
          </div>
          <div class="progress-track">
            <i :style="{ width: `${visitCompleteness.percent || 0}%` }"></i>
          </div>
          <div class="node-list">
            <div v-for="node in visitCompleteness.nodes || []" :key="node.name" :class="{ done: node.done }">
              <CheckCircle2 v-if="node.done" :size="16" />
              <CircleAlert v-else :size="16" />
              <span>{{ node.name }}</span>
            </div>
          </div>
          <p>{{ visitCompleteness.message }}</p>
        </section>

        <section v-if="currentAppointment" class="assist-section">
          <div class="assist-title simple">
            <span><FileText :size="19" /></span>
            <strong>病历状态</strong>
          </div>
          <p v-if="existingRecord">
            已保存病历记录：{{ existingRecord.diagnosis || existingRecord.chiefComplaint || '医生已完成记录' }}
          </p>
          <p v-else>当前预约尚未保存病历，可进入接诊页生成 AI 草稿并由医生确认。</p>
          <router-link class="primary-panel-link" :to="recordActionPath">
            {{ existingRecord ? '查看病历记录' : '进入病历书写' }}
          </router-link>
        </section>

        <section v-if="!currentAppointment" class="assist-section empty-context">
          <div class="assist-title simple">
            <span><CalendarClock :size="19" /></span>
            <strong>暂无接诊上下文</strong>
          </div>
          <p>当前没有可处理的医生预约，请先进入接诊预约选择患者。</p>
          <router-link class="primary-panel-link" to="/workspace?workflow=doctor-appointments">查看接诊预约</router-link>
        </section>

        <section v-if="aiStatus" class="assist-section service-status">
          <div class="assist-title simple">
            <span><Activity :size="19" /></span>
            <strong>AI 服务状态</strong>
          </div>
          <p>{{ aiStatus.message }} · {{ aiStatus.provider }} · {{ aiStatus.mode }}</p>
        </section>
      </template>

      <template v-else-if="!isDoctor && !loading">
        <section class="assist-section">
          <div class="assist-title simple">
            <span><Activity :size="19" /></span>
            <strong>{{ roleContextTitle }}</strong>
          </div>
          <p>{{ roleContextDescription }}</p>
          <div class="role-links">
            <router-link v-for="item in roleLinks" :key="item.path" :to="item.path">
              {{ item.label }}
              <ChevronRight :size="15" />
            </router-link>
          </div>
        </section>

        <section v-if="aiStatus" class="assist-section service-status">
          <div class="assist-title simple">
            <span><Sparkles :size="19" /></span>
            <strong>AI 服务状态</strong>
          </div>
          <p>{{ aiStatus.message }} · {{ aiStatus.provider }} · {{ aiStatus.mode }}</p>
        </section>
      </template>

      <p class="assist-footnote">所有 AI 内容仅供辅助参考，最终诊疗结论以医生确认为准。</p>
    </template>
  </aside>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import {
  Activity,
  CalendarClock,
  CheckCircle2,
  ChevronRight,
  CircleAlert,
  ClipboardCheck,
  FileText,
  Minus,
  PanelRightOpen,
  ShieldCheck,
  Sparkles,
  Stethoscope,
} from '@lucide/vue'
import { useAuthStore } from '../../app/store'
import api from '../../services/api'

const auth = useAuthStore()
const route = useRoute()
const collapsed = ref(false)
const loading = ref(false)
const errorMessage = ref('')
const appointments = ref([])
const records = ref([])
const riskAssessment = ref(null)
const visitCompleteness = ref(null)
const aiStatus = ref(null)

const isDoctor = computed(() => auth.user?.role === 'DOCTOR')
const panelTitle = computed(() => (isDoctor.value ? '接诊辅助' : '工作辅助'))

const currentAppointment = computed(() => {
  if (!isDoctor.value) return null
  const routeId = Number(route.params.id)
  if (route.path.startsWith('/encounter/') && routeId) {
    return appointments.value.find((item) => Number(item.id) === routeId) || null
  }
  return appointments.value.find((item) => item.status === 'IN_PROGRESS')
    || appointments.value.find((item) => item.status === 'PENDING')
    || appointments.value.find((item) => item.status === 'COMPLETED')
    || null
})

const existingRecord = computed(() => {
  const appointmentId = currentAppointment.value?.id
  if (!appointmentId) return null
  return records.value.find((item) => Number(item.appointmentId) === Number(appointmentId)) || null
})

const visitAction = computed(() => {
  const appointment = currentAppointment.value
  if (!appointment) return { label: '查看接诊预约', path: '/workspace?workflow=doctor-appointments' }
  if (appointment.status === 'COMPLETED') return { label: '查看病历记录', path: '/workspace?workflow=doctor-records' }
  if (appointment.status === 'PENDING') return { label: '开始接诊', path: `/encounter/${appointment.id}` }
  return { label: '继续接诊', path: `/encounter/${appointment.id}` }
})

const recordActionPath = computed(() => {
  if (existingRecord.value) return '/workspace?workflow=doctor-records'
  return currentAppointment.value ? `/encounter/${currentAppointment.value.id}` : '/workspace?workflow=doctor-appointments'
})

const riskTone = computed(() => {
  const level = String(riskAssessment.value?.level || '').toUpperCase()
  if (level === 'HIGH') return 'risk-high'
  if (level === 'MEDIUM') return 'risk-medium'
  return 'risk-low'
})

const roleContextTitle = computed(() => {
  if (auth.user?.role === 'ADMIN') return '院内维护入口'
  return '患者服务入口'
})

const roleContextDescription = computed(() => {
  if (auth.user?.role === 'ADMIN') return '这里仅保留已接入后端的院内资料、预约、药品和诊疗记录入口。'
  return '这里仅保留已接入后端的预约、AI 预问诊、病历、处方和检查结果入口。'
})

const roleLinks = computed(() => {
  if (auth.user?.role === 'ADMIN') {
    return [
      { label: '患者资料', path: '/patients?view=patients' },
      { label: '医生档案', path: '/patients?view=doctors' },
      { label: '预约协同', path: '/workspace?workflow=appointments' },
      { label: '药品维护', path: '/workspace?workflow=medicines' },
    ]
  }
  return [
    { label: 'AI 预问诊', path: '/ai' },
    { label: '预约挂号', path: '/workspace?workflow=create-appointment' },
    { label: '病历记录', path: '/patients?view=records' },
    { label: '检查结果', path: '/patients?view=examinations' },
  ]
})

onMounted(loadPanel)

watch(
  () => [route.fullPath, auth.user?.role],
  () => {
    loadPanel()
  }
)

watch(
  () => currentAppointment.value?.id,
  () => {
    loadAppointmentContext()
  }
)

async function loadPanel() {
  loading.value = true
  errorMessage.value = ''
  try {
    aiStatus.value = await api.aiStatus()
    if (isDoctor.value) {
      const [doctorAppointments, doctorRecords] = await Promise.all([
        api.appointmentApi.doctorList(),
        api.medicalRecordApi.doctorList(),
      ])
      appointments.value = doctorAppointments
      records.value = doctorRecords
      await loadAppointmentContext()
    }
  } catch (error) {
    errorMessage.value = error.message || '数据同步失败'
  } finally {
    loading.value = false
  }
}

async function loadAppointmentContext() {
  const appointment = currentAppointment.value
  riskAssessment.value = null
  visitCompleteness.value = null
  if (!isDoctor.value || !appointment) return

  try {
    const [risk, completeness] = await Promise.all([
      api.aiRisk(appointment.symptomDescription || ''),
      api.visitCompleteness(appointment.id),
    ])
    riskAssessment.value = risk
    visitCompleteness.value = completeness
  } catch (error) {
    errorMessage.value = error.message || '数据同步失败'
  }
}

function statusText(status) {
  return {
    PENDING: '待接诊',
    IN_PROGRESS: '接诊中',
    COMPLETED: '已完成',
    CANCELLED: '已撤回',
  }[status] || '待确认'
}
</script>

<style scoped>
.assist-panel {
  display: flex;
  width: 360px;
  min-height: 100vh;
  flex-direction: column;
  gap: 16px;
  padding: 22px 18px;
  border-left: 1px solid rgba(203, 213, 225, 0.74);
  background: linear-gradient(180deg, rgba(245, 253, 255, 0.98), rgba(255, 255, 255, 0.96));
}

.assist-head,
.assist-head > div,
.assist-title,
.assist-title span,
.primary-panel-link,
.role-links a {
  display: flex;
  align-items: center;
}

.assist-head {
  justify-content: space-between;
  min-height: 52px;
}

.assist-head > div {
  gap: 11px;
}

.assist-head svg {
  color: #0891b2;
}

.assist-head h2 {
  margin: 0;
  color: #0f172a;
  font-size: 21px;
  font-weight: 900;
}

.assist-head button {
  display: inline-grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border: 0;
  border-radius: 12px;
  color: #64748b;
  background: transparent;
}

.assist-section,
.panel-message {
  padding: 18px;
  border: 1px solid rgba(199, 229, 236, 0.9);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.76);
  box-shadow: 0 16px 38px rgba(22, 45, 74, 0.055);
}

.assist-title {
  gap: 10px;
}

.assist-title span {
  width: 32px;
  height: 32px;
  justify-content: center;
  border-radius: 11px;
  color: #0891b2;
  background: #e6f8fb;
}

.assist-title strong {
  flex: 1;
  color: #0f172a;
  font-weight: 900;
}

.assist-title em {
  padding: 7px 10px;
  border-radius: 999px;
  color: #0e7490;
  background: #e6f8fb;
  font-size: 12px;
  font-style: normal;
  font-weight: 900;
}

.assist-title em.risk-high {
  color: #be123c;
  background: #ffe4e6;
}

.assist-title em.risk-medium {
  color: #b45309;
  background: #fef3c7;
}

.assist-title em.risk-low {
  color: #0f766e;
  background: #ccfbf1;
}

.assist-section p,
.panel-message {
  margin: 14px 0 0;
  color: #334155;
  font-size: 14px;
  line-height: 1.75;
}

.panel-message {
  margin: 0;
}

.panel-message.error {
  color: #be123c;
  background: #fff1f2;
}

.patient-line {
  display: grid;
  gap: 5px;
  margin-top: 16px;
}

.patient-line strong {
  color: #0f172a;
  font-size: 20px;
  font-weight: 900;
}

.patient-line small {
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
}

.primary-panel-link {
  justify-content: center;
  width: 100%;
  min-height: 42px;
  margin-top: 15px;
  border: 1px solid #7dd3fc;
  border-radius: 12px;
  color: #0e7490;
  background: #f0f9ff;
  font-weight: 900;
}

.check-list,
.node-list,
.role-links {
  display: grid;
  gap: 10px;
  margin-top: 16px;
}

.check-list div,
.node-list div {
  display: grid;
  grid-template-columns: 18px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  color: #64748b;
  font-size: 13px;
  font-weight: 800;
}

.check-list svg,
.node-list .done svg {
  color: #0f766e;
}

.node-list svg {
  color: #f97316;
}

.progress-track {
  overflow: hidden;
  height: 8px;
  margin-top: 16px;
  border-radius: 999px;
  background: #e2e8f0;
}

.progress-track i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #0891b2, #14b8a6);
}

.role-links a {
  justify-content: space-between;
  min-height: 40px;
  padding: 0 12px;
  border: 1px solid #e2edf5;
  border-radius: 12px;
  color: #31506d;
  background: #f8fcff;
  font-size: 13px;
  font-weight: 900;
}

.service-status p,
.empty-context p {
  color: #64748b;
}

.assist-footnote {
  margin: auto 2px 0;
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.7;
}

@media (max-width: 1120px) {
  .assist-panel {
    width: 100%;
    min-height: auto;
    border-left: 0;
    border-top: 1px solid #e5e7eb;
  }
}
</style>
