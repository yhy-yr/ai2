<template>
  <div class="prescription-workspace">
    <WorkflowHeader kicker="院内处方审阅" title="处方维护" description="核对处方归属、用药明细和医生医嘱。" tone="green">
      <template #metrics><div><strong>{{ records.length }}</strong><span>院内处方</span></div></template>
    </WorkflowHeader>

    <div class="toolbar">
      <input v-model.trim="keyword" class="search-input" placeholder="搜索患者、医生、科室、药品或医嘱" />
      <button class="secondary-button" @click="loadData">刷新</button>
    </div>

    <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>

    <div v-if="filteredRecords.length" class="prescription-feed">
      <article v-for="item in filteredRecords" :key="item.id" class="prescription-card">
        <div class="prescription-head">
          <div>
            <span class="status-pill">{{ item.status || 'ISSUED' }}</span>
            <h2>{{ item.patientName || '无患者' }}的处方</h2>
            <p>{{ item.doctorName || '无医生' }} · {{ item.departmentName || '无科室' }} · {{ formatDateTime(item.createdAt) }}</p>
          </div>
        </div>

        <div class="medicine-list">
          <div class="item-detail" v-for="row in item.items" :key="row.id">
            <strong>{{ row.medicineName || '未命名药品' }}</strong>
            <span>{{ row.dosage }} · {{ row.frequency }} · {{ row.days }}天 · 数量{{ row.quantity }} · {{ row.remark || '无备注' }}</span>
          </div>
        </div>

        <div class="advice-box">
          <span>医嘱</span>
          <strong>{{ item.advice || '无' }}</strong>
        </div>
      </article>
    </div>

    <EmptyState v-else-if="!loading" title="暂无处方" description="医生开具处方后，记录会显示在这里。" />
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

const filteredRecords = computed(() => {
  if (!keyword.value) return records.value
  const key = keyword.value
  return records.value.filter((item) => {
    return [item.patientName, item.doctorName, item.departmentName, item.advice, medicineNames(item)]
      .filter(Boolean)
      .some((value) => String(value).includes(key))
  })
})

onMounted(loadData)

async function loadData() {
  loading.value = true
  errorMessage.value = ''
  try {
    records.value = await api.prescriptionApi.list()
  } catch (error) {
    errorMessage.value = error.message || '数据同步失败'
  } finally {
    loading.value = false
  }
}

function medicineNames(item) {
  return (item.items || []).map((row) => row.medicineName).join('、') || '无'
}

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : ''
}
</script>

<style scoped>
.prescription-feed {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 18px;
  margin: 24px 0 0 18px;
  padding-left: 30px;
}

.prescription-feed::before {
  content: '';
  position: absolute;
  top: 12px;
  bottom: 12px;
  left: 5px;
  width: 2px;
  background: linear-gradient(#14b8a6, #bbf7d0);
}

.prescription-card {
  position: relative;
  padding: 20px;
  border: 1px solid rgba(205, 218, 234, 0.86);
  border-left: 5px solid #14b8a6;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 18px 42px rgba(22, 45, 74, 0.06);
}

.prescription-card::before {
  content: '';
  position: absolute;
  top: 28px;
  left: -38px;
  width: 14px;
  height: 14px;
  border: 4px solid #fff;
  border-radius: 999px;
  background: #14b8a6;
  box-shadow: 0 0 0 2px #99f6e4;
}

.prescription-workspace .toolbar {
  margin-top: 18px;
}

.prescription-head h2 {
  margin: 9px 0 0;
  color: #0f172a;
  font-size: 23px;
  font-weight: 900;
}

.prescription-head p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 13px;
  font-weight: 800;
}

.status-pill {
  display: inline-flex;
  padding: 6px 10px;
  border-radius: 999px;
  color: #0f766e;
  background: #ccfbf1;
  font-size: 12px;
  font-weight: 900;
}

.medicine-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}

.item-detail {
  min-width: 240px;
  flex: 1 1 240px;
  display: grid;
  gap: 6px;
  padding: 13px;
  border: 1px solid #e2edf5;
  border-radius: 13px;
  background: #f8fcff;
}

.item-detail strong {
  color: #0f172a;
}

.item-detail span,
.advice-box span {
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
  line-height: 1.6;
}

.advice-box {
  display: grid;
  gap: 7px;
  margin-top: 14px;
  padding: 14px;
  border-radius: 14px;
  background: #f8fafc;
}

.advice-box strong {
  color: #0f172a;
  line-height: 1.7;
}

@media (max-width: 760px) {
  .medicine-list {
    grid-template-columns: 1fr;
  }
}
</style>
