<template>
  <div class="appointment-board">
    <section class="appointment-hero">
      <div>
        <span class="hero-kicker">院内预约流转</span>
        <h1>预约协同</h1>
        <p>按就诊阶段查看患者预约，快速识别接诊中、已完成和已撤回的流程状态。</p>
      </div>
      <div class="hero-metrics">
        <div>
          <strong>{{ appointments.length }}</strong>
          <span>全部预约</span>
        </div>
        <div>
          <strong>{{ statusCount('IN_PROGRESS') }}</strong>
          <span>接诊中</span>
        </div>
        <div>
          <strong>{{ statusCount('COMPLETED') }}</strong>
          <span>已完成</span>
        </div>
      </div>
    </section>

    <section class="care-toolbar">
      <label>
        <span>检索预约</span>
        <input v-model.trim="keyword" class="search-input" placeholder="患者、医生、科室或症状" />
      </label>
      <div class="status-chips">
        <button
          v-for="option in statusOptions"
          :key="option.value"
          type="button"
          :class="{ active: statusFilter === option.value }"
          @click="statusFilter = option.value"
        >
          {{ option.label }}
          <span>{{ option.count }}</span>
        </button>
      </div>
      <button class="secondary-button" @click="loadData">刷新</button>
    </section>

    <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>

    <div v-if="filteredAppointments.length" class="stage-stack">
      <section v-for="group in visibleGroups" :key="group.status" class="stage-section">
        <div class="stage-head">
          <div>
            <span :class="['stage-dot', group.status]"></span>
            <h2>{{ group.label }}</h2>
          </div>
          <strong>{{ group.items.length }}</strong>
        </div>

        <article v-for="item in group.items" :key="item.id" class="appointment-card" :class="item.status">
          <div class="appointment-time">
            <strong>{{ item.timeSlot || '未定时间' }}</strong>
            <span>{{ item.appointmentDate || '未定日期' }}</span>
          </div>

          <div class="patient-block">
            <div class="avatar">{{ initial(item.patientName) }}</div>
            <div>
              <div class="patient-title">
                <h3>{{ item.patientName || '未命名患者' }}</h3>
                <span class="status-pill" :class="item.status">{{ statusText(item.status) }}</span>
              </div>
              <p>{{ item.doctorName || '未分配医生' }} · {{ item.departmentName || '未记录科室' }}</p>
              <div class="symptom-box">
                <span>症状描述</span>
                <strong>{{ item.symptomDescription || '暂无症状描述' }}</strong>
              </div>
            </div>
          </div>

          <div class="appointment-meta">
            <span>创建 {{ formatDateTime(item.createdAt) || '未记录' }}</span>
            <strong>#{{ item.id }}</strong>
          </div>
        </article>
      </section>
    </div>

    <EmptyState v-else-if="!loading" title="暂无预约" description="患者确认提交预约后，记录会显示在这里。" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import api from '../../services/api'
import EmptyState from '../../components/ui/EmptyState.vue'

const appointments = ref([])
const keyword = ref('')
const statusFilter = ref('')
const loading = ref(false)
const errorMessage = ref('')

const statusOptions = computed(() => [
  { label: '全部', value: '', count: appointments.value.length },
  { label: '待就诊', value: 'PENDING', count: statusCount('PENDING') },
  { label: '接诊中', value: 'IN_PROGRESS', count: statusCount('IN_PROGRESS') },
  { label: '已完成', value: 'COMPLETED', count: statusCount('COMPLETED') },
  { label: '已撤回', value: 'CANCELLED', count: statusCount('CANCELLED') }
])

const filteredAppointments = computed(() => {
  const key = keyword.value
  return appointments.value.filter((item) => {
    if (statusFilter.value && item.status !== statusFilter.value) {
      return false
    }
    if (!key) {
      return true
    }
    return [item.patientName, item.doctorName, item.departmentName, item.symptomDescription]
      .filter(Boolean)
      .some((value) => String(value).includes(key))
  })
})

const visibleGroups = computed(() => {
  return ['IN_PROGRESS', 'PENDING', 'COMPLETED', 'CANCELLED']
    .map((status) => ({
      status,
      label: statusText(status),
      items: filteredAppointments.value.filter((item) => item.status === status)
    }))
    .filter((group) => group.items.length)
})

onMounted(loadData)

async function loadData() {
  loading.value = true
  errorMessage.value = ''
  try {
    appointments.value = await api.appointmentApi.list()
  } catch (error) {
    errorMessage.value = error.message || '数据同步失败'
  } finally {
    loading.value = false
  }
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

function statusCount(status) {
  return appointments.value.filter((item) => item.status === status).length
}

function initial(name) {
  return String(name || '患').slice(0, 1)
}

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : ''
}
</script>

<style scoped>
.appointment-board {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.appointment-hero {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  padding: 26px;
  border: 1px solid rgba(125, 211, 252, 0.45);
  border-radius: 24px;
  background:
    radial-gradient(circle at 92% 16%, rgba(20, 184, 166, 0.18), transparent 28%),
    linear-gradient(135deg, #ffffff 0%, #f0f9ff 56%, #ecfeff 100%);
  box-shadow: 0 22px 54px rgba(15, 23, 42, 0.08);
}

.appointment-hero h1 {
  margin: 9px 0 0;
  color: #0f172a;
  font-size: 30px;
  font-weight: 900;
}

.appointment-hero p {
  max-width: 560px;
  margin: 10px 0 0;
  color: #526579;
  font-size: 14px;
  line-height: 1.8;
  font-weight: 700;
}

.hero-metrics {
  display: flex;
  gap: 12px;
  align-items: stretch;
}

.hero-metrics div {
  min-width: 112px;
  padding: 16px;
  border: 1px solid rgba(203, 226, 240, 0.86);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.06);
}

.hero-metrics strong {
  display: block;
  color: #0f172a;
  font-size: 28px;
  font-weight: 900;
}

.hero-metrics span {
  display: block;
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
  font-weight: 900;
}

.care-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  align-items: flex-end;
  padding: 16px;
  border: 1px solid rgba(205, 218, 234, 0.82);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.05);
}

.care-toolbar label {
  display: flex;
  min-width: 280px;
  flex: 1;
  flex-direction: column;
  gap: 8px;
}

.care-toolbar label span {
  color: #64748b;
  font-size: 12px;
  font-weight: 900;
}

.status-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.status-chips button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 40px;
  padding: 9px 13px;
  border: 1px solid #d9e8f3;
  border-radius: 999px;
  color: #526579;
  background: #f8fcff;
  font-weight: 900;
}

.status-chips button.active {
  border-color: #0891b2;
  color: #ffffff;
  background: linear-gradient(135deg, #0891b2, #14b8a6);
  box-shadow: 0 12px 24px rgba(8, 145, 178, 0.2);
}

.status-chips span {
  display: inline-flex;
  min-width: 22px;
  height: 22px;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.62);
  font-size: 12px;
}

.stage-stack {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.stage-section {
  padding: 18px;
  border: 1px solid rgba(205, 218, 234, 0.8);
  border-radius: 22px;
  background: rgba(248, 252, 255, 0.78);
}

.stage-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.stage-head div {
  display: flex;
  gap: 10px;
  align-items: center;
}

.stage-head h2 {
  margin: 0;
  color: #0f172a;
  font-size: 18px;
  font-weight: 900;
}

.stage-head strong {
  display: inline-flex;
  min-width: 34px;
  height: 30px;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  color: #0e7490;
  background: #e6f8fb;
  font-size: 13px;
  font-weight: 900;
}

.stage-dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: #0891b2;
  box-shadow: 0 0 0 5px rgba(8, 145, 178, 0.12);
}

.stage-dot.PENDING {
  background: #f59e0b;
  box-shadow: 0 0 0 5px rgba(245, 158, 11, 0.14);
}

.stage-dot.COMPLETED {
  background: #14b8a6;
  box-shadow: 0 0 0 5px rgba(20, 184, 166, 0.14);
}

.stage-dot.CANCELLED {
  background: #94a3b8;
  box-shadow: 0 0 0 5px rgba(148, 163, 184, 0.16);
}

.appointment-card {
  display: flex;
  gap: 18px;
  align-items: stretch;
  margin-top: 12px;
  padding: 16px;
  border: 1px solid rgba(205, 218, 234, 0.86);
  border-left: 5px solid #0891b2;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 16px 34px rgba(22, 45, 74, 0.06);
}

.appointment-card.PENDING {
  border-left-color: #f59e0b;
}

.appointment-card.COMPLETED {
  border-left-color: #14b8a6;
}

.appointment-card.CANCELLED {
  border-left-color: #94a3b8;
}

.appointment-time {
  display: flex;
  width: 132px;
  flex: 0 0 132px;
  flex-direction: column;
  justify-content: center;
  gap: 5px;
  padding: 14px;
  border-radius: 16px;
  background: #f0f9ff;
}

.appointment-time strong {
  color: #0e7490;
  font-size: 15px;
  font-weight: 900;
}

.appointment-time span {
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
}

.patient-block {
  display: flex;
  min-width: 0;
  flex: 1;
  gap: 14px;
  align-items: flex-start;
}

.avatar {
  display: flex;
  width: 48px;
  height: 48px;
  flex: 0 0 48px;
  align-items: center;
  justify-content: center;
  border-radius: 16px;
  color: #ffffff;
  background: linear-gradient(135deg, #0891b2, #2563eb);
  box-shadow: 0 10px 24px rgba(8, 145, 178, 0.2);
  font-size: 18px;
  font-weight: 900;
}

.patient-title {
  display: flex;
  flex-wrap: wrap;
  gap: 9px;
  align-items: center;
}

.patient-title h3 {
  margin: 0;
  color: #0f172a;
  font-size: 20px;
  font-weight: 900;
}

.patient-block p,
.appointment-meta span {
  margin: 5px 0 0;
  color: #64748b;
  font-size: 13px;
  font-weight: 800;
}

.symptom-box {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 12px;
  padding: 12px 14px;
  border: 1px solid #e2edf5;
  border-radius: 14px;
  background: #f8fcff;
}

.symptom-box span {
  color: #64748b;
  font-size: 12px;
  font-weight: 900;
}

.symptom-box strong {
  color: #0f172a;
  line-height: 1.6;
}

.appointment-meta {
  display: flex;
  width: 150px;
  flex: 0 0 150px;
  flex-direction: column;
  justify-content: space-between;
  align-items: flex-end;
  gap: 12px;
}

.appointment-meta strong {
  padding: 8px 10px;
  border-radius: 999px;
  color: #0e7490;
  background: #e6f8fb;
  font-size: 12px;
  font-weight: 900;
}

.status-pill {
  display: inline-flex;
  padding: 5px 9px;
  border-radius: 999px;
  color: #475569;
  background: #e2e8f0;
  font-size: 12px;
  font-weight: 800;
}

.status-pill.PENDING,
.status-pill.IN_PROGRESS {
  color: #92400e;
  background: #fef3c7;
}

.status-pill.COMPLETED {
  color: #0f766e;
  background: #ccfbf1;
}

.status-pill.CANCELLED {
  color: #b91c1c;
  background: #fee2e2;
}

@media (max-width: 760px) {
  .appointment-hero,
  .care-toolbar,
  .appointment-card,
  .patient-block,
  .hero-metrics {
    flex-direction: column;
  }

  .appointment-time,
  .appointment-meta {
    width: 100%;
    flex-basis: auto;
    align-items: flex-start;
  }
}
</style>
