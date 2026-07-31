<template>
  <div class="appointment-workspace">
    <section class="appointment-hero">
      <div>
        <span>医生接诊队列</span>
        <h1>我的预约</h1>
        <p>查看预约到当前医生名下的患者，按状态开始、继续或回看接诊记录。</p>
      </div>
      <div class="queue-stats">
        <div><strong>{{ appointments.length }}</strong><span>全部预约</span></div>
        <div><strong>{{ statusCount('PENDING') }}</strong><span>待就诊</span></div>
        <div><strong>{{ statusCount('IN_PROGRESS') }}</strong><span>接诊中</span></div>
      </div>
    </section>

    <div class="queue-toolbar">
      <input v-model.trim="keyword" class="search-input" placeholder="搜索患者、症状或状态" />
      <button class="secondary-button" @click="loadData">刷新</button>
    </div>

    <div class="status-tabs">
      <button
        v-for="tab in statusTabs"
        :key="tab.value"
        type="button"
        :class="{ active: activeStatus === tab.value }"
        @click="activeStatus = tab.value"
      >
        {{ tab.label }}
        <span>{{ tab.count }}</span>
      </button>
    </div>

    <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>

    <div v-if="filteredAppointments.length" class="appointment-feed">
      <article
        v-for="item in filteredAppointments"
        :key="item.id"
        class="appointment-card"
        :class="statusClass(item.status)"
      >
        <div class="card-main">
          <time>{{ item.appointmentDate || '未定日期' }} · {{ item.timeSlot || '未定时间' }}</time>
          <div class="patient-row">
            <div class="avatar">{{ initial(item.patientName) }}</div>
            <div>
              <h2>{{ item.patientName || '未命名患者' }}</h2>
              <p>{{ item.gender || '未填' }} · {{ item.age ?? 0 }} 岁 · {{ item.phone || '无手机号' }}</p>
            </div>
          </div>
          <div class="symptom-box">
            <span>预约症状</span>
            <strong>{{ item.symptomDescription || '无症状描述' }}</strong>
          </div>
        </div>

        <div class="card-side">
          <StatusTag :text="statusText(item.status)" :type="statusType(item.status)" />
          <div class="side-copy">
            <span>预约编号</span>
            <strong>#{{ item.id }}</strong>
          </div>
          <button
            v-if="item.status === 'PENDING'"
            class="primary-button"
            @click="startVisit(item)"
          >
            开始接诊
          </button>
          <button
            v-else-if="item.status === 'IN_PROGRESS'"
            class="secondary-button"
            @click="goVisit(item.id)"
          >
            继续接诊
          </button>
          <router-link
            v-else-if="item.status === 'COMPLETED'"
            class="secondary-button"
            to="/workspace?workflow=doctor-records"
          >
            查看病历
          </router-link>
          <span v-else class="muted-text">不可接诊</span>
        </div>
      </article>
    </div>

    <EmptyState
      v-else-if="!loading"
      title="暂无预约"
      description="当前没有预约到你名下的患者。"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../../services/api'
import EmptyState from '../../components/ui/EmptyState.vue'
import StatusTag from '../../components/medical/StatusTag.vue'

const router = useRouter()
const appointments = ref([])
const keyword = ref('')
const loading = ref(false)
const errorMessage = ref('')
const activeStatus = ref('all')

const statusTabs = computed(() => [
  { label: '全部', value: 'all', count: appointments.value.length },
  { label: '待就诊', value: 'PENDING', count: statusCount('PENDING') },
  { label: '接诊中', value: 'IN_PROGRESS', count: statusCount('IN_PROGRESS') },
  { label: '已完成', value: 'COMPLETED', count: statusCount('COMPLETED') },
  { label: '已撤回', value: 'CANCELLED', count: statusCount('CANCELLED') }
])

const filteredAppointments = computed(() => {
  const key = keyword.value
  return appointments.value.filter((item) => {
    const matchStatus = activeStatus.value === 'all' || item.status === activeStatus.value
    const matchKeyword = !key || [item.patientName, item.symptomDescription, item.status, item.phone]
      .filter(Boolean)
      .some((value) => String(value).includes(key))
    return matchStatus && matchKeyword
  })
})

onMounted(loadData)

async function loadData() {
  loading.value = true
  errorMessage.value = ''
  try {
    appointments.value = await api.appointmentApi.doctorList()
  } catch (error) {
    errorMessage.value = error.message || '数据同步失败'
  } finally {
    loading.value = false
  }
}

async function startVisit(item) {
  errorMessage.value = ''
  try {
    await api.appointmentApi.updateStatus(item.id, 'IN_PROGRESS')
    goVisit(item.id)
  } catch (error) {
    errorMessage.value = error.message || '数据同步失败'
  }
}

function goVisit(id) {
  router.push(`/encounter/${id}`)
}

function statusCount(status) {
  return appointments.value.filter((item) => item.status === status).length
}

function initial(name) {
  return String(name || '患').slice(0, 1)
}

function statusClass(status) {
  return {
    PENDING: 'is-pending',
    IN_PROGRESS: 'is-active',
    COMPLETED: 'is-completed',
    CANCELLED: 'is-cancelled'
  }[status] || 'is-pending'
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
  if (status === 'PENDING' || status === 'IN_PROGRESS') {
    return 'warning'
  }
  if (status === 'CANCELLED') {
    return 'danger'
  }
  return 'normal'
}
</script>

<style scoped>
.appointment-workspace {
  display: grid;
  gap: 18px;
  padding-bottom: 44px;
}

.appointment-hero {
  display: flex;
  justify-content: space-between;
  gap: 22px;
  align-items: flex-start;
  padding: 26px;
  border: 1px solid rgba(205, 218, 234, 0.86);
  border-radius: 22px;
  background:
    linear-gradient(135deg, rgba(8, 145, 178, 0.12), rgba(20, 184, 166, 0.08)),
    rgba(255, 255, 255, 0.9);
  box-shadow: 0 18px 48px rgba(22, 45, 74, 0.07);
}

.appointment-hero span {
  color: #0891b2;
  font-size: 12px;
  font-weight: 900;
}

.appointment-hero h1 {
  margin: 7px 0 0;
  color: #0f172a;
  font-size: 34px;
  line-height: 1.08;
  font-weight: 900;
}

.appointment-hero p {
  max-width: 660px;
  margin: 12px 0 0;
  color: #475569;
  font-size: 15px;
  line-height: 1.8;
}

.queue-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(88px, 1fr));
  gap: 10px;
}

.queue-stats div {
  min-width: 92px;
  padding: 13px;
  border: 1px solid #dbeafe;
  border-radius: 15px;
  background: #ffffff;
}

.queue-stats strong {
  display: block;
  color: #0f172a;
  font-size: 24px;
  font-weight: 900;
}

.queue-toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
}

.queue-toolbar .search-input {
  flex: 1;
}

.status-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.status-tabs button {
  display: inline-flex;
  min-height: 38px;
  align-items: center;
  gap: 8px;
  padding: 0 14px;
  border: 1px solid #dbe5ef;
  border-radius: 999px;
  color: #475569;
  background: #ffffff;
  font-weight: 900;
}

.status-tabs button.active {
  border-color: #8bd5e5;
  color: #0f5f70;
  background: #eafcff;
}

.status-tabs span {
  color: #0891b2;
}

.appointment-feed {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.appointment-card {
  display: flex;
  gap: 18px;
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

.card-main {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  gap: 14px;
}

.card-main time {
  color: #0e7490;
  font-size: 13px;
  font-weight: 900;
}

.patient-row {
  display: flex;
  gap: 13px;
  align-items: center;
}

.avatar {
  display: grid;
  width: 46px;
  height: 46px;
  place-items: center;
  border-radius: 999px;
  color: #ffffff;
  background: linear-gradient(135deg, #0891b2, #14b8a6);
  font-weight: 900;
}

.patient-row h2 {
  margin: 0;
  color: #0f172a;
  font-size: 22px;
  font-weight: 900;
}

.patient-row p {
  margin: 5px 0 0;
  color: #64748b;
  font-size: 13px;
  font-weight: 800;
}

.symptom-box {
  display: grid;
  gap: 7px;
  padding: 14px;
  border: 1px solid #e2edf5;
  border-radius: 14px;
  background: #f8fcff;
}

.symptom-box span,
.side-copy span {
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
}

.symptom-box strong,
.side-copy strong {
  color: #0f172a;
  line-height: 1.6;
}

.card-side {
  display: flex;
  width: 170px;
  flex: 0 0 170px;
  flex-direction: column;
  gap: 13px;
}

.side-copy {
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 12px;
  border-radius: 13px;
  background: #f8fafc;
}

@media (max-width: 860px) {
  .appointment-hero,
  .queue-toolbar {
    flex-direction: column;
  }

  .appointment-card {
    flex-direction: column;
  }

  .card-side { width: 100%; flex-basis: auto; }
}
</style>
