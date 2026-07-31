<template>
  <aside class="patient-queue-panel">
    <div class="queue-head">
      <div>
        <h2>{{ panelTitle }}</h2>
        <p>{{ panelSubtitle }}</p>
      </div>
      <button type="button" :aria-label="showFunctions ? '收起功能工作区' : '展开功能工作区'" @click="showFunctions = !showFunctions">
        <component :is="showFunctions ? PanelTopClose : SlidersHorizontal" :size="18" />
      </button>
    </div>

    <section v-if="showFunctions" class="function-shelf">
      <div class="shelf-title">
        <span>功能工作区</span>
        <small>{{ roleLabel }}</small>
      </div>
      <div class="function-grid">
        <router-link
          v-for="item in functionCards"
          :key="item.path"
          class="function-card"
          :class="{ 'is-active': isFunctionActive(item.path) }"
          :to="item.path"
        >
          <component :is="item.icon" :size="17" />
          <span>{{ item.label }}</span>
        </router-link>
      </div>
    </section>

    <div v-if="showPatientQueue" class="queue-tabs">
      <button
        v-for="tab in tabs"
        :key="tab.value"
        type="button"
        :class="{ active: activeTab === tab.value }"
        @click="activeTab = tab.value"
      >
        {{ tab.label }}
        <span>{{ tab.count }}</span>
      </button>
    </div>

    <nav v-if="showPatientQueue" class="patient-queue">
      <button
        v-for="patient in filteredPatients"
        :key="patient.id"
        class="patient-row"
        :class="[patient.status, { selected: patient.id === selectedId }]"
        type="button"
        @click="openPatient(patient)"
      >
        <time>{{ patient.time }}</time>
        <img v-if="patient.avatar" :src="patient.avatar" :alt="patient.name" />
        <span v-else class="avatar-initial">{{ patient.name.slice(0, 1) }}</span>
        <div class="patient-copy">
          <strong>{{ patient.name }}</strong>
          <small>{{ patient.complaint }}</small>
        </div>
        <div class="row-meta">
          <em>{{ patient.state }}</em>
          <span :class="patient.risk">{{ riskLabel(patient.risk) }}</span>
        </div>
      </button>
    </nav>

    <p v-if="showPatientQueue && errorMessage" class="queue-message">{{ errorMessage }}</p>
    <p v-else-if="showPatientQueue && !loading && !filteredPatients.length" class="queue-message">{{ emptyQueueMessage }}</p>

    <router-link v-if="showPatientQueue" class="queue-action" to="/workspace?workflow=doctor-appointments">
      <CalendarPlus :size="18" />
      查看接诊预约
    </router-link>
  </aside>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Bot,
  CalendarPlus,
  ClipboardList,
  FileClock,
  FlaskConical,
  Hospital,
  Pill,
  PanelTopClose,
  SlidersHorizontal,
  Stethoscope,
  UsersRound,
} from '@lucide/vue'
import { useAuthStore } from '../../app/store'
import api from '../../services/api'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const activeTab = ref('active')
const selectedId = ref(null)
const showFunctions = ref(true)
const appointments = ref([])
const loading = ref(false)
const errorMessage = ref('')

onMounted(() => {
  if (auth.user?.role === 'DOCTOR') {
    loadDoctorQueue()
  }
})

const roleLabel = computed(() => {
  if (auth.user?.role === 'ADMIN') return '院内维护'
  if (auth.user?.role === 'DOCTOR') return '医生接诊'
  return '患者服务'
})

const showPatientQueue = computed(() => auth.user?.role === 'DOCTOR')

const panelTitle = computed(() => {
  if (auth.user?.role === 'ADMIN') return '院内工作'
  if (auth.user?.role === 'DOCTOR') return '今日接诊'
  return '我的服务'
})

const panelSubtitle = computed(() => {
  if (auth.user?.role === 'ADMIN') return '资料、预约与诊疗记录'
  if (auth.user?.role === 'DOCTOR') {
    const visibleCount = queuePatients.value.filter((patient) => ['waiting', 'active'].includes(patient.status)).length
    return `${visibleCount} 人`
  }
  return '预约、病历与健康记录'
})

const functionCards = computed(() => {
  if (auth.user?.role === 'ADMIN') {
    return [
      { label: '患者资料', path: '/patients?view=patients', icon: UsersRound },
      { label: '医生档案', path: '/patients?view=doctors', icon: Stethoscope },
      { label: '科室维护', path: '/workspace?workflow=departments', icon: Hospital },
      { label: '药品维护', path: '/workspace?workflow=medicines', icon: Pill },
      { label: '预约协同', path: '/workspace?workflow=appointments', icon: CalendarPlus },
      { label: '病历维护', path: '/workspace?workflow=records', icon: FileClock },
      { label: '处方维护', path: '/workspace?workflow=prescriptions', icon: ClipboardList },
      { label: '检查结果', path: '/workspace?workflow=examinations', icon: FlaskConical },
    ]
  }

  if (auth.user?.role === 'DOCTOR') {
    return [
      { label: '诊疗工作台', path: '/workspace', icon: Stethoscope },
      { label: '接诊预约', path: '/workspace?workflow=doctor-appointments', icon: CalendarPlus },
      { label: '病历记录', path: '/workspace?workflow=doctor-records', icon: FileClock },
      { label: '处方维护', path: '/workspace?workflow=doctor-prescriptions', icon: ClipboardList },
      { label: '检查结果', path: '/workspace?workflow=doctor-examinations', icon: FlaskConical },
    ]
  }

  return [
    { label: '诊疗首页', path: '/workspace', icon: Stethoscope },
    { label: 'AI 预问诊', path: '/ai', icon: Bot },
    { label: '预约挂号', path: '/workspace?workflow=create-appointment', icon: CalendarPlus },
    { label: '就诊进度', path: '/workspace?workflow=patient-appointments', icon: FileClock },
    { label: '病历记录', path: '/patients?view=records', icon: ClipboardList },
    { label: '处方记录', path: '/patients?view=prescriptions', icon: Pill },
    { label: '检查结果', path: '/patients?view=examinations', icon: FlaskConical },
  ]
})

const tabs = computed(() => {
  const patients = queuePatients.value
  return [
    { label: '待接诊', value: 'waiting', count: patients.filter((item) => item.status === 'waiting').length },
    { label: '接诊中', value: 'active', count: patients.filter((item) => item.status === 'active').length },
  ]
})

const queuePatients = computed(() => {
  return appointments.value.map((item) => {
    const complaint = item.symptomDescription || '无症状描述'
    return {
      id: item.id,
      time: item.timeSlot || item.appointmentDate || '',
      name: item.patientName || '未命名患者',
      complaint,
      phone: item.phone || '',
      state: statusText(item.status),
      status: queueStatus(item.status),
      rawStatus: item.status,
      risk: inferRisk(complaint),
      avatar: '',
    }
  })
})

const filteredPatients = computed(() => {
  return queuePatients.value.filter((patient) => patient.status === activeTab.value)
})

const emptyQueueMessage = computed(() => {
  return activeTab.value === 'waiting' ? '当前暂无待接诊患者' : '当前暂无接诊中患者'
})

function isFunctionActive(path) {
  return route.fullPath === path
}

function riskLabel(risk) {
  return {
    high: '高风险',
    medium: '中风险',
    low: '低风险',
  }[risk]
}

async function loadDoctorQueue() {
  loading.value = true
  errorMessage.value = ''
  try {
    appointments.value = await api.appointmentApi.doctorList()
    if (!queuePatients.value.some((patient) => patient.status === activeTab.value)) {
      activeTab.value = ['active', 'waiting']
        .find((status) => queuePatients.value.some((patient) => patient.status === status)) || 'active'
    }
    if (!selectedId.value && appointments.value.length) {
      selectedId.value = appointments.value[0].id
    }
  } catch (error) {
    errorMessage.value = error.message || '数据同步失败'
  } finally {
    loading.value = false
  }
}

async function openPatient(patient) {
  selectedId.value = patient.id
  if (auth.user?.role !== 'DOCTOR') {
    return
  }

  if (patient.rawStatus === 'COMPLETED') {
    router.push('/workspace?workflow=doctor-records')
    return
  }

  if (patient.rawStatus === 'PENDING') {
    try {
      await api.appointmentApi.updateStatus(patient.id, 'IN_PROGRESS')
      await loadDoctorQueue()
    } catch (error) {
      errorMessage.value = error.message || '数据同步失败'
      return
    }
  }

  if (patient.rawStatus === 'PENDING' || patient.rawStatus === 'IN_PROGRESS') {
    router.push(`/encounter/${patient.id}`)
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

function queueStatus(status) {
  if (status === 'PENDING') return 'waiting'
  if (status === 'IN_PROGRESS') return 'active'
  if (status === 'COMPLETED') return 'done'
  return 'cancelled'
}

function inferRisk(text) {
  if (/胸闷|呼吸|高热|慢阻肺|脑|心|血压波动/.test(text)) return 'high'
  if (/咳嗽|糖尿病|高血压|胃痛|头晕/.test(text)) return 'medium'
  return 'low'
}
</script>

<style scoped>
.patient-queue-panel {
  display: flex;
  width: 390px;
  min-height: 100vh;
  flex-direction: column;
  padding: 22px 18px;
  border-right: 1px solid rgba(203, 213, 225, 0.74);
  background: rgba(248, 252, 255, 0.96);
}

.queue-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.queue-head h2 {
  margin: 0;
  color: #0f172a;
  font-size: 22px;
  font-weight: 900;
  letter-spacing: 0;
}

.queue-head p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 13px;
  font-weight: 800;
}

.queue-head button {
  display: inline-grid;
  width: 38px;
  height: 38px;
  place-items: center;
  border: 1px solid #dbe5ef;
  border-radius: 12px;
  color: #0f5f70;
  background: #ffffff;
}

.queue-tabs {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-top: 16px;
}

.function-shelf {
  margin-top: 18px;
  padding: 14px;
  border: 1px solid #dceaf4;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.74);
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.045);
}

.shelf-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.shelf-title span {
  color: #0f172a;
  font-size: 14px;
  font-weight: 900;
}

.shelf-title small {
  color: #0e7490;
  font-size: 12px;
  font-weight: 900;
}

.function-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 9px;
}

.function-card {
  display: flex;
  gap: 8px;
  align-items: center;
  min-height: 38px;
  padding: 9px 10px;
  border: 1px solid #e2edf5;
  border-radius: 12px;
  color: #31506d;
  background: #f8fcff;
  font-size: 12px;
  font-weight: 900;
  transition:
    border-color 0.18s ease,
    background 0.18s ease,
    color 0.18s ease;
}

.function-card svg {
  flex: 0 0 auto;
  color: #0891b2;
}

.function-card:hover {
  border-color: #8bd5e5;
  color: #0f5f70;
  background: #eafcff;
}

.function-card.is-active {
  border-color: #67c7da;
  color: #0f5f70;
  background: #eafcff;
  box-shadow: inset 3px 0 0 #0891b2;
}

.queue-tabs button {
  display: grid;
  gap: 4px;
  min-height: 48px;
  place-items: center;
  border: 1px solid transparent;
  border-radius: 12px;
  color: #64748b;
  background: #eef5fb;
  font-size: 12px;
  font-weight: 900;
}

.queue-tabs button span {
  font-size: 11px;
}

.queue-tabs button.active {
  color: #ffffff;
  background: linear-gradient(135deg, #0891b2, #0f766e);
  box-shadow: 0 12px 24px rgba(8, 145, 178, 0.2);
}

.patient-queue {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 4px;
  margin-top: 16px;
  overflow: auto;
}

.patient-row {
  position: relative;
  display: grid;
  grid-template-columns: 52px 44px minmax(0, 1fr) 70px;
  gap: 12px;
  align-items: center;
  width: 100%;
  min-height: 78px;
  padding: 12px;
  border: 1px solid transparent;
  border-left: 4px solid transparent;
  border-radius: 14px;
  text-align: left;
  background: transparent;
}

.patient-row:hover {
  background: #ffffff;
}

.patient-row.selected {
  border-color: #c7edf3;
  border-left-color: #0891b2;
  background: linear-gradient(90deg, #f0fbff 0%, #ffffff 100%);
  box-shadow: 0 16px 32px rgba(8, 145, 178, 0.1);
}

.patient-row.waiting {
  border-left-color: #f97316;
}

.patient-row.active {
  border-left-color: #0891b2;
}

.patient-row.done {
  border-left-color: #14b8a6;
}

.patient-row.cancelled {
  border-left-color: #94a3b8;
}

.patient-row time {
  color: #0f172a;
  font-size: 16px;
  font-weight: 900;
}

.patient-row img,
.avatar-initial {
  width: 42px;
  height: 42px;
  border-radius: 999px;
}

.patient-row img {
  object-fit: cover;
}

.avatar-initial {
  display: grid;
  place-items: center;
  color: #ffffff;
  background: linear-gradient(135deg, #60a5fa, #2563eb);
  font-weight: 900;
}

.patient-copy {
  min-width: 0;
}

.patient-copy strong {
  display: block;
  overflow: hidden;
  color: #0f172a;
  font-size: 16px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.patient-copy small {
  display: block;
  overflow: hidden;
  margin-top: 6px;
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.row-meta {
  display: grid;
  gap: 7px;
  justify-items: end;
}

.row-meta em {
  color: #ea580c;
  font-size: 13px;
  font-style: normal;
  font-weight: 900;
}

.row-meta span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  font-weight: 900;
}

.row-meta span::before {
  content: '';
  width: 7px;
  height: 7px;
  border-radius: 999px;
}

.row-meta .high {
  color: #e11d48;
}

.row-meta .high::before {
  background: #e11d48;
}

.row-meta .medium {
  color: #f97316;
}

.row-meta .medium::before {
  background: #f97316;
}

.row-meta .low {
  color: #0f766e;
}

.row-meta .low::before {
  background: #0f766e;
}

.queue-action {
  display: inline-flex;
  min-height: 52px;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-top: 20px;
  border: 1px solid #dbe5ef;
  border-radius: 14px;
  color: #1e3a5f;
  background: #ffffff;
  box-shadow: 0 14px 32px rgba(15, 23, 42, 0.06);
  font-weight: 900;
}

.queue-message {
  margin: 18px 4px 0;
  padding: 14px;
  border: 1px solid #dbeafe;
  border-radius: 14px;
  color: #475569;
  background: #f8fbff;
  font-size: 13px;
  font-weight: 800;
  line-height: 1.6;
}

@media (max-width: 1120px) {
  .patient-queue-panel {
    width: 100%;
    min-height: auto;
  }

  .patient-queue {
    max-height: 380px;
  }
}
</style>
