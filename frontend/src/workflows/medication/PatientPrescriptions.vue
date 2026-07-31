<template>
  <div class="prescription-workspace">
    <WorkflowHeader kicker="个人用药方案" title="我的处方" description="按开具时间查看医生医嘱、药品剂量和用药周期。" tone="green">
      <template #metrics><div><strong>{{ records.length }}</strong><span>处方方案</span></div></template>
    </WorkflowHeader>

    <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>

    <div v-if="records.length" class="record-list">
      <section v-for="item in records" :key="item.id" class="info-card record-card">
        <div class="record-title">
          <div>
            <h3>{{ item.departmentName || '处方记录' }}</h3>
            <p>{{ item.doctorName || '医生' }} · {{ formatDateTime(item.createdAt) }}</p>
          </div>
          <span class="status-pill">{{ item.status || 'ISSUED' }}</span>
        </div>
        <p class="advice">医嘱：{{ item.advice || '无' }}</p>
        <div class="item-list">
          <div v-for="row in item.items" :key="row.id" class="item-box">
            <strong>{{ row.medicineName }}</strong>
            <span>{{ row.dosage }}，{{ row.frequency }}，{{ row.days }} 天，数量 {{ row.quantity }}</span>
            <span>备注：{{ row.remark || '无' }}</span>
          </div>
        </div>
      </section>
    </div>

    <EmptyState v-else-if="!loading" title="暂无处方" description="医生开具处方后，你可以在这里查看。" />
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

onMounted(loadData)

async function loadData() {
  loading.value = true
  errorMessage.value = ''
  try {
    records.value = await api.prescriptionApi.my()
  } catch (error) {
    errorMessage.value = error.message || '数据同步失败'
  } finally {
    loading.value = false
  }
}

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : ''
}
</script>

<style scoped>
.record-list {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 18px;
  margin: 24px 0 0 18px;
  padding-left: 30px;
}
.record-list::before {
  content: '';
  position: absolute;
  top: 12px;
  bottom: 12px;
  left: 5px;
  width: 2px;
  background: linear-gradient(#14b8a6, #bbf7d0);
}
.record-card {
  position: relative;
  margin-top: 0;
  border-left: 4px solid #14b8a6;
}
.record-card::before {
  content: '';
  position: absolute;
  top: 27px;
  left: -39px;
  width: 14px;
  height: 14px;
  border: 4px solid #fff;
  border-radius: 999px;
  background: #14b8a6;
  box-shadow: 0 0 0 2px #99f6e4;
}
.record-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.record-title h3 {
  margin: 0;
  color: #1f2937;
}
.record-title p,
.advice {
  margin: 6px 0 0;
  color: #6b7280;
}
.status-pill {
  padding: 6px 10px;
  border-radius: 999px;
  color: #0f766e;
  background: #ccfbf1;
  font-size: 12px;
  font-weight: 800;
}
.item-list {
  display: grid;
  gap: 10px;
  margin-top: 14px;
}
.item-box {
  display: grid;
  gap: 6px;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #f8fafc;
}
.item-box span {
  color: #6b7280;
}
</style>
