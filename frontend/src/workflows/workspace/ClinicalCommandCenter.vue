<template>
  <section class="clinical-home">
    <header class="workspace-header">
      <div>
        <span class="eyebrow">医生工作台</span>
        <h1>今日接诊</h1>
        <p>{{ auth.user?.realName || '医生' }}，这里仅展示当前账号的真实预约与接诊进度。</p>
      </div>
    </header>

    <div class="visit-summary" aria-label="接诊概览">
      <div>
        <span>待接诊</span>
        <strong>{{ statusCount('PENDING') }}</strong>
      </div>
      <div>
        <span>接诊中</span>
        <strong>{{ statusCount('IN_PROGRESS') }}</strong>
      </div>
      <div>
        <span>已完成</span>
        <strong>{{ statusCount('COMPLETED') }}</strong>
      </div>
    </div>

    <p v-if="errorMessage" class="state-message error">{{ errorMessage }}</p>
    <p v-else-if="loading" class="state-message">正在获取患者信息</p>

    <article v-else-if="activeAppointment" class="priority-visit" :class="riskClass(activeAppointment)">
      <div class="priority-main">
        <div class="visit-label-row">
          <span>当前优先处理</span>
          <em>{{ statusText(activeAppointment.status) }}</em>
        </div>

        <div class="patient-identity">
          <span class="patient-initial">{{ initial(activeAppointment.patientName) }}</span>
          <div>
            <h2>{{ activeAppointment.patientName || '未命名患者' }}</h2>
            <p>
              {{ activeAppointment.gender || '未填' }} · {{ activeAppointment.age ?? 0 }} 岁
              <template v-if="activeAppointment.phone"> · {{ activeAppointment.phone }}</template>
            </p>
          </div>
        </div>

        <div class="complaint-block">
          <span>预约症状</span>
          <strong>{{ activeAppointment.symptomDescription || '无症状描述' }}</strong>
        </div>

        <div class="visit-facts">
          <span><CalendarDays :size="16" />{{ activeAppointment.appointmentDate || '未定日期' }}</span>
          <span><Clock3 :size="16" />{{ activeAppointment.timeSlot || '未定时间' }}</span>
          <span><Building2 :size="16" />{{ activeAppointment.departmentName || '未记录科室' }}</span>
          <span><Hash :size="16" />预约 {{ activeAppointment.id }}</span>
        </div>
      </div>

      <div class="priority-action">
        <span :class="['risk-badge', riskClass(activeAppointment)]">{{ riskText(activeAppointment) }}</span>
        <button
          v-if="activeAppointment.status === 'PENDING'"
          class="primary-action"
          :disabled="actionLoadingId === activeAppointment.id"
          @click="startVisit(activeAppointment)"
        >
          <Stethoscope :size="18" />
          {{ actionLoadingId === activeAppointment.id ? '正在进入接诊' : '开始接诊' }}
        </button>
        <router-link
          v-else-if="activeAppointment.status === 'IN_PROGRESS'"
          class="primary-action"
          :to="`/encounter/${activeAppointment.id}`"
        >
          <Stethoscope :size="18" />
          继续接诊
        </router-link>
        <router-link v-else class="primary-action" to="/workspace?workflow=doctor-records">
          <FileClock :size="18" />
          查看病历
        </router-link>
      </div>
    </article>

    <section v-else-if="!loading" class="empty-visit">
      <CalendarCheck2 :size="28" />
      <div>
        <h2>当前没有待处理接诊</h2>
        <p>新的预约进入当前医生名下后，会显示在这里。</p>
      </div>
      <router-link class="secondary-action" to="/workspace?workflow=doctor-appointments">查看预约记录</router-link>
    </section>

    <section v-if="!loading && recentAppointments.length" class="recent-section">
      <div class="section-heading">
        <div>
          <span class="eyebrow">真实预约记录</span>
          <h2>近期接诊</h2>
        </div>
        <router-link to="/workspace?workflow=doctor-appointments">查看全部</router-link>
      </div>

      <div class="appointment-feed">
        <article v-for="item in recentAppointments" :key="item.id" class="appointment-row">
          <time>
            <strong>{{ item.appointmentDate || '未定日期' }}</strong>
            <span>{{ item.timeSlot || '未定时间' }}</span>
          </time>

          <div class="row-patient">
            <span class="small-initial">{{ initial(item.patientName) }}</span>
            <div>
              <h3>{{ item.patientName || '未命名患者' }}</h3>
              <p>{{ item.symptomDescription || '无症状描述' }}</p>
            </div>
          </div>

          <span :class="['status-badge', statusClass(item.status)]">{{ statusText(item.status) }}</span>

          <button
            v-if="item.status === 'PENDING'"
            class="row-action"
            :disabled="actionLoadingId === item.id"
            @click="startVisit(item)"
          >
            开始接诊
          </button>
          <router-link v-else-if="item.status === 'IN_PROGRESS'" class="row-action" :to="`/encounter/${item.id}`">
            继续接诊
          </router-link>
          <router-link v-else-if="item.status === 'COMPLETED'" class="row-action" to="/workspace?workflow=doctor-records">
            查看病历
          </router-link>
          <span v-else class="row-state">无需操作</span>
        </article>
      </div>
    </section>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Building2,
  CalendarCheck2,
  CalendarDays,
  Clock3,
  FileClock,
  Hash,
  Stethoscope,
} from '@lucide/vue'
import { useAuthStore } from '../../app/store'
import api from '../../services/api'

const auth = useAuthStore()
const router = useRouter()
const appointments = ref([])
const loading = ref(false)
const actionLoadingId = ref(null)
const errorMessage = ref('')

onMounted(loadAppointments)

const activeAppointment = computed(() => {
  return appointments.value.find((item) => item.status === 'IN_PROGRESS')
    || appointments.value.find((item) => item.status === 'PENDING')
    || appointments.value.find((item) => item.status === 'COMPLETED')
    || null
})

const recentAppointments = computed(() => {
  const activeId = activeAppointment.value?.id
  return appointments.value.filter((item) => item.id !== activeId).slice(0, 5)
})

async function loadAppointments() {
  loading.value = true
  errorMessage.value = ''
  try {
    appointments.value = await api.appointmentApi.doctorList()
  } catch (error) {
    appointments.value = []
    errorMessage.value = error.message || '数据同步失败'
  } finally {
    loading.value = false
  }
}

async function startVisit(item) {
  actionLoadingId.value = item.id
  errorMessage.value = ''
  try {
    await api.appointmentApi.updateStatus(item.id, 'IN_PROGRESS')
    router.push(`/encounter/${item.id}`)
  } catch (error) {
    errorMessage.value = error.message || '数据同步失败'
  } finally {
    actionLoadingId.value = null
  }
}

function statusCount(status) {
  return appointments.value.filter((item) => item.status === status).length
}

function initial(name) {
  return String(name || '患').slice(0, 1)
}

function statusText(status) {
  return {
    PENDING: '待接诊',
    IN_PROGRESS: '接诊中',
    COMPLETED: '已完成',
    CANCELLED: '已撤回',
  }[status] || '待确认'
}

function statusClass(status) {
  return {
    PENDING: 'pending',
    IN_PROGRESS: 'active',
    COMPLETED: 'completed',
    CANCELLED: 'cancelled',
  }[status] || 'pending'
}

function riskClass(item) {
  const text = item?.symptomDescription || ''
  if (/胸闷|呼吸困难|高热|昏厥|意识|心痛|剧烈/.test(text)) return 'high'
  if (/咳嗽|发热|头晕|腹痛|高血压|糖尿病/.test(text)) return 'medium'
  return 'low'
}

function riskText(item) {
  return {
    high: '重点关注',
    medium: '需要关注',
    low: '常规接诊',
  }[riskClass(item)]
}
</script>

<style scoped>
.clinical-home {
  display: flex;
  min-height: calc(100vh - 96px);
  flex-direction: column;
  gap: 18px;
  padding-bottom: 44px;
}

.workspace-header,
.visit-summary,
.priority-visit,
.recent-section,
.empty-visit {
  border: 1px solid rgba(205, 218, 234, 0.86);
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 18px 46px rgba(22, 45, 74, 0.065);
}

.workspace-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  padding: 25px 27px;
  border-radius: 20px;
}

.eyebrow {
  color: #0e7490;
  font-size: 12px;
  font-weight: 900;
}

.workspace-header h1 {
  margin: 6px 0 0;
  color: #0f172a;
  font-size: 32px;
  line-height: 1.1;
  font-weight: 900;
  letter-spacing: 0;
}

.workspace-header p {
  margin: 10px 0 0;
  color: #64748b;
  font-size: 14px;
  line-height: 1.7;
}

.secondary-action,
.primary-action,
.row-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 1px solid #d7e4ee;
  border-radius: 11px;
  font-weight: 900;
  white-space: nowrap;
}

.secondary-action {
  min-height: 42px;
  padding: 0 15px;
  color: #1f4f64;
  background: #ffffff;
}

.visit-summary {
  display: flex;
  overflow: hidden;
  border-radius: 16px;
}

.visit-summary div {
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: space-between;
  min-width: 0;
  padding: 16px 20px;
}

.visit-summary div + div {
  border-left: 1px solid #e6edf3;
}

.visit-summary span {
  color: #64748b;
  font-size: 13px;
  font-weight: 800;
}

.visit-summary strong {
  color: #0f172a;
  font-size: 24px;
  font-weight: 900;
}

.priority-visit {
  display: flex;
  overflow: hidden;
  border-left: 5px solid #14b8a6;
  border-radius: 19px;
}

.priority-visit.medium {
  border-left-color: #eab308;
}

.priority-visit.high {
  border-left-color: #ef4444;
}

.priority-main {
  flex: 1;
  min-width: 0;
  padding: 25px 27px;
}

.visit-label-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.visit-label-row > span {
  color: #0e7490;
  font-size: 12px;
  font-weight: 900;
}

.visit-label-row em {
  padding: 4px 8px;
  border-radius: 999px;
  color: #0f766e;
  background: #ccfbf1;
  font-size: 11px;
  font-style: normal;
  font-weight: 900;
}

.patient-identity {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 17px;
}

.patient-initial,
.small-initial {
  display: inline-grid;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 50%;
  color: #0f6674;
  background: #dff7f8;
  font-weight: 900;
}

.patient-initial {
  width: 52px;
  height: 52px;
  font-size: 20px;
}

.patient-identity h2 {
  margin: 0;
  color: #0f172a;
  font-size: 25px;
  font-weight: 900;
  letter-spacing: 0;
}

.patient-identity p {
  margin: 5px 0 0;
  color: #64748b;
  font-size: 13px;
}

.complaint-block {
  margin-top: 18px;
  padding: 14px 16px;
  border: 1px solid #e4edf3;
  border-radius: 12px;
  background: #f8fbfc;
}

.complaint-block span {
  display: block;
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
}

.complaint-block strong {
  display: block;
  margin-top: 6px;
  color: #1e293b;
  font-size: 14px;
  line-height: 1.65;
}

.visit-facts {
  display: flex;
  flex-wrap: wrap;
  gap: 11px 20px;
  margin-top: 17px;
}

.visit-facts span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #526779;
  font-size: 12px;
  font-weight: 800;
}

.visit-facts svg {
  color: #0891b2;
}

.priority-action {
  display: flex;
  width: 190px;
  flex: 0 0 auto;
  flex-direction: column;
  align-items: stretch;
  justify-content: space-between;
  gap: 24px;
  padding: 25px;
  border-left: 1px solid #e7eef3;
  background: #fbfdfd;
}

.risk-badge {
  align-self: flex-end;
  padding: 7px 10px;
  border-radius: 999px;
  color: #0f766e;
  background: #ccfbf1;
  font-size: 12px;
  font-weight: 900;
}

.risk-badge.medium {
  color: #a16207;
  background: #fef9c3;
}

.risk-badge.high {
  color: #b91c1c;
  background: #fee2e2;
}

.primary-action {
  min-height: 46px;
  padding: 0 16px;
  border-color: #0f766e;
  color: #ffffff;
  background: #0f766e;
  box-shadow: 0 12px 24px rgba(15, 118, 110, 0.18);
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.recent-section {
  padding: 23px;
  border-radius: 19px;
}

.section-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 15px;
}

.section-heading h2 {
  margin: 5px 0 0;
  color: #0f172a;
  font-size: 21px;
  font-weight: 900;
  letter-spacing: 0;
}

.section-heading > a {
  color: #0e7490;
  font-size: 13px;
  font-weight: 900;
}

.appointment-feed {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.appointment-row {
  display: flex;
  align-items: center;
  gap: 18px;
  min-height: 78px;
  padding: 13px 14px;
  border: 1px solid #e3ebf1;
  border-radius: 13px;
  background: #ffffff;
}

.appointment-row time {
  width: 108px;
  flex: 0 0 auto;
}

.appointment-row time strong,
.appointment-row time span {
  display: block;
}

.appointment-row time strong {
  color: #1e293b;
  font-size: 13px;
}

.appointment-row time span {
  margin-top: 5px;
  color: #64748b;
  font-size: 12px;
}

.row-patient {
  display: flex;
  flex: 1;
  min-width: 0;
  align-items: center;
  gap: 11px;
}

.small-initial {
  width: 38px;
  height: 38px;
  font-size: 14px;
}

.row-patient h3 {
  margin: 0;
  color: #0f172a;
  font-size: 15px;
}

.row-patient p {
  overflow: hidden;
  margin: 5px 0 0;
  color: #64748b;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-badge {
  flex: 0 0 auto;
  padding: 6px 9px;
  border-radius: 999px;
  color: #a16207;
  background: #fef9c3;
  font-size: 11px;
  font-weight: 900;
}

.status-badge.active {
  color: #0369a1;
  background: #e0f2fe;
}

.status-badge.completed {
  color: #0f766e;
  background: #ccfbf1;
}

.status-badge.cancelled {
  color: #64748b;
  background: #f1f5f9;
}

.row-action {
  min-width: 88px;
  min-height: 36px;
  padding: 0 11px;
  color: #0e6672;
  background: #f4fbfc;
  font-size: 12px;
}

.row-state {
  width: 88px;
  color: #94a3b8;
  font-size: 12px;
  text-align: center;
}

.state-message,
.empty-visit {
  margin: 0;
  color: #526779;
}

.state-message {
  padding: 18px 20px;
  border: 1px solid #dce8ef;
  border-radius: 14px;
  background: #ffffff;
  font-weight: 800;
}

.state-message.error {
  color: #b91c1c;
  background: #fff7f7;
}

.empty-visit {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 24px;
  border-radius: 18px;
}

.empty-visit > svg {
  color: #0f766e;
}

.empty-visit div {
  flex: 1;
}

.empty-visit h2,
.empty-visit p {
  margin: 0;
}

.empty-visit h2 {
  color: #0f172a;
  font-size: 18px;
}

.empty-visit p {
  margin-top: 6px;
  color: #64748b;
  font-size: 13px;
}

@media (max-width: 880px) {
  .workspace-header,
  .priority-visit,
  .empty-visit {
    flex-direction: column;
  }

  .priority-action {
    width: auto;
    border-top: 1px solid #e7eef3;
    border-left: 0;
  }

  .risk-badge {
    align-self: flex-start;
  }

  .appointment-row {
    flex-wrap: wrap;
  }

  .row-patient {
    min-width: calc(100% - 144px);
  }
}

@media (max-width: 560px) {
  .workspace-header,
  .priority-main,
  .priority-action,
  .recent-section {
    padding: 19px;
  }

  .workspace-header h1 {
    font-size: 28px;
  }

  .visit-summary div {
    flex-direction: column;
    align-items: flex-start;
    gap: 6px;
    padding: 13px;
  }

  .appointment-row time {
    width: 100%;
  }

  .row-patient {
    min-width: 100%;
  }

  .row-action,
  .row-state {
    margin-left: auto;
  }
}
</style>
