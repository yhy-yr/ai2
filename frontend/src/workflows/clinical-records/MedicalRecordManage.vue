<template>
  <div class="records-workspace">
    <WorkflowHeader kicker="院内病历审阅" title="病历维护" description="按患者与接诊医生核对院内电子病历。">
      <template #metrics><div><strong>{{ records.length }}</strong><span>院内病历</span></div></template>
    </WorkflowHeader>

    <div class="toolbar">
      <input v-model.trim="keyword" class="search-input" placeholder="搜索患者、医生、科室或诊断" />
      <button class="secondary-button" @click="loadData">刷新</button>
    </div>

    <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>

    <div v-if="filteredRecords.length" class="record-feed">
      <article v-for="item in filteredRecords" :key="item.id" class="record-card">
        <div class="record-head">
          <div>
            <span>电子病历</span>
            <h2>{{ item.patientName || '无患者' }}</h2>
            <p>{{ item.doctorName || '无医生' }} · {{ item.departmentName || '无科室' }} · {{ formatDateTime(item.createdAt) }}</p>
          </div>
          <button class="secondary-button" @click="openDetail(item)">查看详情</button>
        </div>
        <div class="record-grid">
          <div><span>主诉</span><strong>{{ item.chiefComplaint || '无' }}</strong></div>
          <div><span>诊断</span><strong>{{ item.diagnosis || '无' }}</strong></div>
          <div class="full"><span>治疗建议</span><strong>{{ item.treatmentPlan || '无' }}</strong></div>
        </div>
      </article>
    </div>

    <EmptyState
      v-else-if="!loading"
      title="暂无病历"
      description="医生保存病历记录后，记录会显示在这里。"
    />

    <div v-if="detailRecord" class="form-overlay">
      <section class="form-card">
        <div class="form-header">
          <h2>病历详情</h2>
          <button class="secondary-button" @click="detailRecord = null">返回</button>
        </div>
        <div class="detail-grid">
          <div><span>患者</span><strong>{{ detailRecord.patientName }}</strong></div>
          <div><span>医生</span><strong>{{ detailRecord.doctorName }}</strong></div>
          <div><span>科室</span><strong>{{ detailRecord.departmentName }}</strong></div>
          <div><span>主诉</span><strong>{{ detailRecord.chiefComplaint }}</strong></div>
          <div><span>现病史</span><strong>{{ detailRecord.presentIllness }}</strong></div>
          <div><span>医生诊断</span><strong>{{ detailRecord.diagnosis }}</strong></div>
          <div><span>治疗建议</span><strong>{{ detailRecord.treatmentPlan }}</strong></div>
          <div><span>创建时间</span><strong>{{ formatDateTime(detailRecord.createdAt) }}</strong></div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import api from '../../services/api'
import EmptyState from '../../components/ui/EmptyState.vue'
import WorkflowHeader from '../../components/medical/WorkflowHeader.vue'

const records = ref([])
const keyword = ref('')
const loading = ref(false)
const errorMessage = ref('')
const detailRecord = ref(null)

const filteredRecords = computed(() => {
  if (!keyword.value) {
    return records.value
  }
  const key = keyword.value
  return records.value.filter((item) => {
    return [item.patientName, item.doctorName, item.departmentName, item.chiefComplaint, item.diagnosis, item.treatmentPlan]
      .filter(Boolean)
      .some((value) => String(value).includes(key))
  })
})

onMounted(loadData)

async function loadData() {
  loading.value = true
  errorMessage.value = ''
  try {
    records.value = await api.medicalRecordApi.list()
  } catch (error) {
    errorMessage.value = error.message || '数据同步失败'
  } finally {
    loading.value = false
  }
}

async function openDetail(item) {
  errorMessage.value = ''
  try {
    detailRecord.value = await api.medicalRecordApi.detail(item.id)
  } catch (error) {
    errorMessage.value = error.message || '数据同步失败'
  }
}

function formatDateTime(value) {
  if (!value) {
    return ''
  }
  return String(value).replace('T', ' ').slice(0, 16)
}
</script>

<style scoped>
.record-feed {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 18px;
  margin: 24px 0 0 18px;
  padding-left: 30px;
}

.record-feed::before {
  content: '';
  position: absolute;
  top: 12px;
  bottom: 12px;
  left: 5px;
  width: 2px;
  background: linear-gradient(#0891b2, #cbd5e1);
}

.record-card {
  position: relative;
  padding: 20px;
  border: 1px solid rgba(205, 218, 234, 0.86);
  border-left: 5px solid #0891b2;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 18px 42px rgba(22, 45, 74, 0.06);
}

.record-card::before {
  content: '';
  position: absolute;
  top: 28px;
  left: -38px;
  width: 14px;
  height: 14px;
  border: 4px solid #ffffff;
  border-radius: 999px;
  background: #0891b2;
  box-shadow: 0 0 0 2px #bae6fd;
}

.records-workspace .toolbar {
  margin-top: 18px;
}

.record-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.record-head span {
  color: #0891b2;
  font-size: 12px;
  font-weight: 900;
}

.record-head h2 {
  margin: 9px 0 0;
  color: #0f172a;
  font-size: 23px;
  font-weight: 900;
}

.record-head p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 13px;
  font-weight: 800;
}

.record-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 16px;
}

.record-grid div {
  display: flex;
  min-width: 240px;
  flex: 1 1 240px;
  flex-direction: column;
  gap: 7px;
  padding: 14px;
  border: 1px solid #e2edf5;
  border-radius: 14px;
  background: #f8fcff;
}

.record-grid .full {
  flex-basis: 100%;
}

.record-grid span {
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
}

.record-grid strong {
  color: #0f172a;
  line-height: 1.7;
}

.detail-grid {
  display: grid;
  gap: 12px;
}

.detail-grid div {
  display: grid;
  gap: 6px;
  padding: 12px;
  border-radius: 8px;
  background: #f8fafc;
}

.detail-grid span {
  color: #64748b;
  font-size: 13px;
}

.detail-grid strong {
  color: #172033;
  line-height: 1.7;
}

@media (max-width: 760px) {
  .record-head {
    flex-direction: column;
  }

  .record-grid {
    grid-template-columns: 1fr;
  }
}
</style>
