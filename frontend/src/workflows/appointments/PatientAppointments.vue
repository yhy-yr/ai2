<template>
  <div class="patient-appointment-workspace">
    <WorkflowHeader kicker="个人就诊安排" title="我的预约" description="查看挂号安排、就诊进度和复诊提醒。">
      <template #metrics>
        <div><strong>{{ appointments.length }}</strong><span>全部预约</span></div>
        <div><strong>{{ statusCount('IN_PROGRESS') }}</strong><span>接诊中</span></div>
      </template>
    </WorkflowHeader>

    <div class="toolbar">
      <input
        v-model.trim="keyword"
        class="search-input"
        placeholder="搜索科室、医生、症状"
      />
      <router-link class="primary-button" to="/workspace?workflow=create-appointment">新增预约</router-link>
    </div>

    <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>

    <div v-if="filteredAppointments.length" class="appointment-feed">
      <article v-for="item in filteredAppointments" :key="item.id" class="appointment-card" :class="statusClass(item.status)">
        <div class="appointment-head">
          <div>
            <time>{{ item.appointmentDate || '' }} · {{ item.timeSlot || '' }}</time>
            <h2>{{ item.departmentName || '无科室' }}</h2>
            <p>{{ item.doctorName || '无医生' }} · 创建于 {{ formatDateTime(item.createdAt) }}</p>
          </div>
          <StatusTag :text="statusText(item.status)" :type="statusType(item.status)" />
        </div>

        <div class="symptom-box">
          <span>症状描述</span>
          <strong>{{ item.symptomDescription || '无' }}</strong>
        </div>

        <div class="appointment-actions">
          <button class="secondary-button small-button" @click="toggleProgress(item)">
            {{ expandedId === item.id ? '收起进度' : '查看进度' }}
          </button>
          <button
            v-if="item.status === 'PENDING'"
            class="danger-button small-button"
            @click="cancelAppointment(item)"
          >
            撤回预约
          </button>
          <span v-else class="muted-text">暂不可撤回</span>
        </div>

        <div v-if="expandedId === item.id" class="progress-card">
          <div class="progress-header">
            <div>
              <h3>患者就诊进度时间线</h3>
              <p>{{ item.appointmentDate }} {{ item.timeSlot }} · {{ item.departmentName || '无科室' }}</p>
            </div>
            <StatusTag :text="statusText(item.status)" :type="statusType(item.status)" />
          </div>

          <p v-if="progressState[item.id]?.loading" class="muted-text">正在获取患者信息</p>
          <p v-else-if="progressState[item.id]?.error" class="muted-text">暂无就诊进度信息</p>
          <ol v-else-if="progressState[item.id]?.data?.progress?.length" class="visit-timeline">
            <li
              v-for="node in progressState[item.id].data.progress"
              :key="node.step"
              :class="{ done: node.done }"
            >
              <span class="timeline-dot"></span>
              <div class="timeline-content">
                <strong>{{ node.step }}</strong>
                <small v-if="node.time">{{ formatDateTime(node.time) }}</small>
              </div>
            </li>
          </ol>
          <p v-else class="muted-text">暂无就诊进度信息</p>

          <section class="follow-up-card">
            <div class="follow-up-header">
              <h3>复诊与健康提醒</h3>
              <span>辅助参考</span>
            </div>
            <p v-if="followUpState[item.id]?.loading" class="muted-text">复诊提醒生成中...</p>
            <p v-else-if="followUpState[item.id]?.error" class="muted-text">暂无复诊提醒。</p>
            <ul v-else-if="followUpState[item.id]?.reminders?.length" class="follow-up-list">
              <li v-for="reminder in followUpState[item.id].reminders" :key="reminder">{{ reminder }}</li>
            </ul>
            <p v-else class="muted-text">暂无复诊提醒。</p>
            <p class="follow-up-disclaimer">以上提醒仅作为辅助参考，具体安排请以医生建议为准。</p>
          </section>
        </div>
      </article>
    </div>

    <EmptyState
      v-else-if="!loading"
      title="暂无预约"
      description="可以点击新增预约创建挂号记录。"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import api from '../../services/api'
import EmptyState from '../../components/ui/EmptyState.vue'
import StatusTag from '../../components/medical/StatusTag.vue'
import WorkflowHeader from '../../components/medical/WorkflowHeader.vue'

const appointments = ref([])
const keyword = ref('')
const loading = ref(false)
const errorMessage = ref('')
const expandedId = ref(null)
const progressState = reactive({})
const followUpState = reactive({})
const visitData = reactive({
  loaded: false,
  records: [],
  prescriptions: [],
  examinations: []
})

const filteredAppointments = computed(() => {
  if (!keyword.value) {
    return appointments.value
  }
  const key = keyword.value
  return appointments.value.filter((item) => {
    return [item.departmentName, item.doctorName, item.symptomDescription, item.status]
      .filter(Boolean)
      .some((value) => String(value).includes(key))
  })
})

function statusCount(status) {
  return appointments.value.filter((item) => item.status === status).length
}

onMounted(loadData)

async function loadData() {
  loading.value = true
  errorMessage.value = ''
  try {
    appointments.value = await api.appointmentApi.my()
  } catch (error) {
    errorMessage.value = error.message || '数据同步失败'
  } finally {
    loading.value = false
  }
}

async function cancelAppointment(item) {
  if (!window.confirm(`确认取消 ${item.appointmentDate} ${item.timeSlot} 的预约吗？`)) {
    return
  }

  try {
    await api.appointmentApi.cancel(item.id)
    await loadData()
  } catch (error) {
    errorMessage.value = error.message || '数据同步失败'
  }
}

async function toggleProgress(item) {
  if (expandedId.value === item.id) {
    expandedId.value = null
    return
  }

  expandedId.value = item.id
  if (progressState[item.id]?.data || progressState[item.id]?.loading) {
    return
  }

  progressState[item.id] = {
    loading: true,
    data: null,
    error: ''
  }

  try {
    progressState[item.id].data = await api.visitProgress(item.id)
    await loadFollowUpReminders(item.id)
  } catch (error) {
    progressState[item.id].error = error.message || '暂无就诊进度信息'
  } finally {
    progressState[item.id].loading = false
  }
}

async function loadFollowUpReminders(appointmentId) {
  followUpState[appointmentId] = {
    loading: true,
    reminders: [],
    error: ''
  }

  try {
    await ensureVisitDataLoaded()
    followUpState[appointmentId].reminders = buildFollowUpReminders(appointmentId)
  } catch (error) {
    followUpState[appointmentId].error = error.message || '暂无复诊提醒'
  } finally {
    followUpState[appointmentId].loading = false
  }
}

async function ensureVisitDataLoaded() {
  if (visitData.loaded) {
    return
  }

  const [records, prescriptions, examinations] = await Promise.all([
    api.medicalRecordApi.my(),
    api.prescriptionApi.my(),
    api.examinationApi.my()
  ])
  visitData.records = records || []
  visitData.prescriptions = prescriptions || []
  visitData.examinations = examinations || []
  visitData.loaded = true
}

function buildFollowUpReminders(appointmentId) {
  const records = visitData.records.filter((item) => Number(item.appointmentId) === Number(appointmentId))
  const prescriptions = visitData.prescriptions.filter((item) => Number(item.appointmentId) === Number(appointmentId))
  const examinations = visitData.examinations.filter((item) => Number(item.appointmentId) === Number(appointmentId))
  const reminders = []

  const doctorFollowUpText = findDoctorFollowUpText(records, prescriptions, examinations)
  if (doctorFollowUpText) {
    reminders.push(`医生建议中提到“${doctorFollowUpText}”，请按医嘱安排复诊、复查或随访。`)
  }

  if (prescriptions.length) {
    reminders.push('请按医嘱用药，如 3 天后症状未缓解，建议复诊。')
  }

  if (hasAbnormalText(records, examinations)) {
    reminders.push('检查结果存在需要关注的项目，请按医生建议及时复诊。')
  }

  if (!reminders.length) {
    reminders.push('请继续观察身体情况，如有不适及时就医。')
  }

  return Array.from(new Set(reminders)).slice(0, 3)
}

function findDoctorFollowUpText(records, prescriptions, examinations) {
  const textSources = [
    ...records.map((item) => item.treatmentPlan || ''),
    ...records.map((item) => item.diagnosis || ''),
    ...prescriptions.map((item) => item.advice || ''),
    ...prescriptions.flatMap((item) => (item.items || []).map((row) => row.remark || '')),
    ...examinations.map((item) => item.conclusion || '')
  ]
  const keywords = ['复查', '随访', '复诊']
  const matched = textSources.find((text) => keywords.some((keyword) => String(text).includes(keyword)))
  if (!matched) {
    return ''
  }
  const value = String(matched).trim()
  return value.length > 36 ? `${value.slice(0, 36)}...` : value
}

function hasAbnormalText(records, examinations) {
  const keywords = ['异常', '偏高', '阳性', '感染', '升高', '炎症']
  const text = [
    ...records.map((item) => `${item.diagnosis || ''} ${item.treatmentPlan || ''}`),
    ...examinations.map((item) => `${item.result || ''} ${item.conclusion || ''}`)
  ].join(' ').replaceAll('未见明显异常', '').replaceAll('无明显异常', '')
  return keywords.some((keyword) => text.includes(keyword))
}

function statusText(status) {
  const map = {
    PENDING: '待就诊',
    IN_PROGRESS: '接诊中',
    COMPLETED: '已完成',
    CANCELLED: '已撤回'
  }
  return map[status] || status || '未知'
}

function statusType(status) {
  if (status === 'PENDING') {
    return 'warning'
  }
  if (status === 'CANCELLED') {
    return 'danger'
  }
  return 'normal'
}

function formatDateTime(value) {
  if (!value) {
    return ''
  }
  return String(value).replace('T', ' ').slice(0, 16)
}

function statusClass(status) {
  return {
    PENDING: 'is-pending',
    IN_PROGRESS: 'is-active',
    COMPLETED: 'is-completed',
    CANCELLED: 'is-cancelled'
  }[status] || 'is-pending'
}
</script>

<style scoped>
.patient-appointment-workspace .toolbar {
  margin-top: 18px;
}

.appointment-feed {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.appointment-card {
  padding: 20px;
  border: 1px solid rgba(205, 218, 234, 0.86);
  border-left: 5px solid #0891b2;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 18px 42px rgba(22, 45, 74, 0.06);
}

.appointment-card.is-pending {
  border-left-color: #f59e0b;
}

.appointment-card.is-active {
  border-left-color: #0891b2;
}

.appointment-card.is-completed {
  border-left-color: #14b8a6;
}

.appointment-card.is-cancelled {
  border-left-color: #94a3b8;
}

.appointment-head,
.appointment-actions {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
}

.appointment-head time {
  color: #0e7490;
  font-size: 13px;
  font-weight: 900;
}

.appointment-head h2 {
  margin: 7px 0 0;
  color: #0f172a;
  font-size: 23px;
  font-weight: 900;
}

.appointment-head p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 13px;
  font-weight: 800;
}

.symptom-box {
  display: grid;
  gap: 7px;
  margin-top: 16px;
  padding: 14px;
  border: 1px solid #e2edf5;
  border-radius: 14px;
  background: #f8fcff;
}

.symptom-box span {
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
}

.symptom-box strong {
  color: #0f172a;
  line-height: 1.6;
}

.appointment-actions {
  justify-content: flex-start;
  margin-top: 16px;
  flex-wrap: wrap;
}

.row-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.small-button {
  padding: 8px 12px;
  font-size: 13px;
}

.progress-row td {
  padding: 0 16px 16px;
  background: #f8fafc;
}

.progress-card {
  margin-top: 4px;
  padding: 18px;
  border: 1px solid #dbeafe;
  border-radius: 16px;
  background: #ffffff;
  box-shadow: 0 10px 24px rgba(37, 99, 235, 0.08);
}

.progress-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.progress-header h3 {
  margin: 0 0 6px;
  color: #1f2937;
  font-size: 18px;
}

.progress-header p {
  margin: 0;
  color: #6b7280;
}

.visit-timeline {
  display: grid;
  gap: 0;
  margin: 0;
  padding: 0;
  list-style: none;
}

.visit-timeline li {
  position: relative;
  display: grid;
  grid-template-columns: 28px 1fr;
  min-height: 58px;
  color: #9ca3af;
}

.visit-timeline li::before {
  content: '';
  position: absolute;
  left: 8px;
  top: 20px;
  bottom: -2px;
  width: 2px;
  background: #e5e7eb;
}

.visit-timeline li:last-child::before {
  display: none;
}

.timeline-dot {
  position: relative;
  z-index: 1;
  width: 18px;
  height: 18px;
  margin-top: 2px;
  border: 3px solid #d1d5db;
  border-radius: 999px;
  background: #ffffff;
}

.timeline-content {
  display: grid;
  gap: 4px;
  padding-bottom: 18px;
}

.timeline-content strong {
  color: inherit;
}

.timeline-content small {
  color: #9ca3af;
}

.follow-up-card {
  margin-top: 18px;
  padding: 16px;
  border: 1px solid #bae6fd;
  border-radius: 14px;
  background: linear-gradient(135deg, #f0f9ff, #ffffff);
}

.follow-up-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-bottom: 12px;
}

.follow-up-header h3 {
  margin: 0;
  color: #1f2937;
  font-size: 17px;
}

.follow-up-header span {
  padding: 5px 9px;
  border-radius: 999px;
  color: #0369a1;
  background: #e0f2fe;
  font-size: 12px;
  font-weight: 800;
}

.follow-up-list {
  display: grid;
  gap: 8px;
  margin: 0;
  padding-left: 18px;
  color: #1f2937;
  line-height: 1.7;
}

.follow-up-list li::marker {
  color: #14b8a6;
}

.follow-up-disclaimer {
  margin: 12px 0 0;
  color: #6b7280;
  font-size: 13px;
  line-height: 1.7;
}

.visit-timeline li.done {
  color: #0f766e;
}

.visit-timeline li.done::before {
  background: #99f6e4;
}

.visit-timeline li.done .timeline-dot {
  border-color: #14b8a6;
  background: #14b8a6;
  box-shadow: 0 0 0 4px #ccfbf1;
}

@media (max-width: 768px) {
  .appointment-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .progress-header {
    flex-direction: column;
  }
}
</style>
