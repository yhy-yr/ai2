<template>
  <div class="medical-record-page">
    <WorkflowHeader kicker="个人临床历程" title="我的病历记录" description="按就诊时间回看主诉、诊断和医生治疗建议。" tone="green">
      <template #metrics><div><strong>{{ records.length }}</strong><span>诊疗记录</span></div></template>
    </WorkflowHeader>

    <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>

    <div v-if="records.length" class="record-list">
      <section
        v-for="item in records"
        :key="item.id"
        class="record-card"
        :class="riskClass(item)"
        @click="toggleRecord(item.id)"
      >
        <div class="record-main">
          <div class="record-copy">
            <div class="record-title-row">
              <h2>{{ formatVisitDate(item.createdAt) }}</h2>
              <span class="risk-badge" :class="riskClass(item)">
                {{ riskMeta(item).label }}
              </span>
            </div>
            <p class="chief-complaint">{{ item.chiefComplaint || '暂无主诉记录' }}</p>
            <p class="doctor-line">接诊医生：{{ item.doctorName || '待分配医生' }}</p>
          </div>
          <button class="secondary-button details-button" type="button" @click.stop="toggleRecord(item.id)">
            {{ expandedRecordId === item.id ? '收起详情' : '查看详情' }}
          </button>
        </div>

        <div v-if="expandedRecordId === item.id" class="record-details" @click.stop>
          <div class="detail-row">
            <span>接诊科室</span>
            <strong>{{ item.departmentName || '暂无记录' }}</strong>
          </div>
          <div class="detail-row">
            <span>现病史</span>
            <strong>{{ item.presentIllness || '暂无记录' }}</strong>
          </div>
          <div class="detail-row">
            <span>医生诊断</span>
            <strong>{{ item.diagnosis || '暂无记录' }}</strong>
          </div>
          <div class="detail-row">
            <span>治疗建议</span>
            <strong>{{ item.treatmentPlan || '暂无记录' }}</strong>
          </div>
        </div>
      </section>
    </div>

    <EmptyState
      v-else-if="!loading"
      title="暂无病历"
      description="医生保存病历记录后，你可以在这里查看。"
    />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import api from '../../services/api'
import EmptyState from '../../components/ui/EmptyState.vue'
import WorkflowHeader from '../../components/medical/WorkflowHeader.vue'

const records = ref([])
const loading = ref(false)
const errorMessage = ref('')
const expandedRecordId = ref(null)

onMounted(loadData)

async function loadData() {
  loading.value = true
  errorMessage.value = ''
  try {
    records.value = await api.medicalRecordApi.my()
  } catch (error) {
    errorMessage.value = error.message || '数据同步失败'
  } finally {
    loading.value = false
  }
}

function formatDateTime(value) {
  if (!value) {
    return ''
  }
  return String(value).replace('T', ' ').slice(0, 16)
}

function formatVisitDate(value) {
  return formatDateTime(value) || '暂无就诊日期'
}

function toggleRecord(id) {
  expandedRecordId.value = expandedRecordId.value === id ? null : id
}

function riskMeta(item) {
  const directRisk = String(item.riskLevel || item.aiRiskLevel || item.risk || '').toUpperCase()
  if (directRisk.includes('HIGH')) {
    return { level: 'high', label: 'AI 高风险' }
  }
  if (directRisk.includes('MEDIUM') || directRisk.includes('MID')) {
    return { level: 'medium', label: 'AI 中风险' }
  }
  if (directRisk.includes('LOW')) {
    return { level: 'low', label: 'AI 低风险' }
  }

  const clinicalText = [
    item.chiefComplaint,
    item.presentIllness,
    item.diagnosis,
    item.treatmentPlan,
  ].join(' ')

  if (/急|重|剧烈|呼吸困难|胸痛|昏迷|出血|休克|high/i.test(clinicalText)) {
    return { level: 'high', label: 'AI 高风险' }
  }
  if (/发热|感染|炎症|腹痛|呕吐|复查|异常|medium/i.test(clinicalText)) {
    return { level: 'medium', label: 'AI 中风险' }
  }
  return { level: 'low', label: 'AI 低风险' }
}

function riskClass(item) {
  return `risk-${riskMeta(item).level}`
}
</script>

<style scoped>
.medical-record-page {
  max-width: 940px;
}

.records-header {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  justify-content: space-between;
  padding: 2px 0 4px;
}

.records-count {
  display: inline-flex;
  min-height: 34px;
  align-items: center;
  padding: 7px 12px;
  border: 1px solid #dbeafe;
  border-radius: 999px;
  color: #1d4ed8;
  background: #eff6ff;
  font-size: 13px;
  font-weight: 900;
}

.record-list {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 22px;
  margin: 24px 0 0 18px;
  padding-left: 28px;
}

.record-list::before {
  content: '';
  position: absolute;
  top: 10px;
  bottom: 10px;
  left: 5px;
  width: 2px;
  background: linear-gradient(#14b8a6, #bfdbfe);
}

.record-card {
  position: relative;
  padding: 24px;
  border: 1px solid var(--color-border);
  border-left-width: 7px;
  border-radius: 14px;
  background: #ffffff;
  box-shadow: 0 16px 42px rgba(15, 23, 42, 0.08);
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease,
    border-color 0.18s ease;
}

.record-card::before {
  content: '';
  position: absolute;
  top: 28px;
  left: -36px;
  width: 14px;
  height: 14px;
  border: 4px solid #ffffff;
  border-radius: 999px;
  background: #14b8a6;
  box-shadow: 0 0 0 2px #99f6e4;
}

.record-card:hover {
  border-color: #bfdbfe;
  box-shadow: 0 20px 52px rgba(15, 23, 42, 0.11);
  transform: translateY(-2px);
}

.record-card.risk-low {
  border-left-color: #22c55e;
}

.record-card.risk-medium {
  border-left-color: #f59e0b;
}

.record-card.risk-high {
  border-left-color: #ef4444;
}

.record-main {
  display: flex;
  gap: 18px;
  align-items: flex-start;
  justify-content: space-between;
}

.record-copy {
  min-width: 0;
}

.record-title-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.record-title-row h2 {
  margin: 0;
  color: #111827;
  font-size: 24px;
  font-weight: 900;
  letter-spacing: 0;
}

.chief-complaint {
  margin: 12px 0 0;
  color: #1f2937;
  font-size: 15px;
  line-height: 1.7;
}

.doctor-line {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 14px;
  font-weight: 800;
}

.risk-badge {
  display: inline-flex;
  min-height: 30px;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 800;
}

.risk-badge.risk-low {
  color: #166534;
  background: #dcfce7;
}

.risk-badge.risk-medium {
  color: #92400e;
  background: #fef3c7;
}

.risk-badge.risk-high {
  color: #991b1b;
  background: #fee2e2;
}

.details-button {
  flex: 0 0 auto;
  white-space: nowrap;
}

.record-details {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 22px;
  padding-top: 20px;
  border-top: 1px solid #e5e7eb;
}

.detail-row {
  display: flex;
  gap: 18px;
  align-items: flex-start;
  justify-content: space-between;
  padding: 14px 16px;
  border: 1px solid #e8eef7;
  border-radius: 12px;
  background: #f8fafc;
}

.detail-row span {
  flex: 0 0 150px;
  color: #64748b;
  font-size: 13px;
  font-weight: 800;
}

.detail-row strong {
  flex: 1;
  color: #1f2937;
  line-height: 1.7;
  text-align: left;
}

@media (max-width: 760px) {
  .records-header,
  .record-main {
    align-items: flex-start;
    flex-direction: column;
  }

  .records-count {
    align-self: flex-start;
  }

  .details-button {
    width: 100%;
  }

  .detail-row {
    flex-direction: column;
    gap: 6px;
  }

  .detail-row span {
    flex: 0 0 auto;
  }
}
</style>
