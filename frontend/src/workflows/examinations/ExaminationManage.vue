<template>
  <div class="exam-workspace">
    <WorkflowHeader kicker="院内检查审阅" title="检查结果维护" description="按患者、医生和检查项目核对院内检查结论。" tone="rose">
      <template #metrics><div><strong>{{ records.length }}</strong><span>检查报告</span></div></template>
    </WorkflowHeader>

    <div class="toolbar">
      <input v-model.trim="keyword" class="search-input" placeholder="搜索患者、医生、科室、检查项目或结论" />
      <button class="secondary-button" @click="loadData">刷新</button>
    </div>

    <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>

    <div v-if="filteredRecords.length" class="exam-feed">
      <article v-for="item in filteredRecords" :key="item.id" class="exam-card">
        <div class="exam-head">
          <div>
            <span class="type-pill">{{ item.examType || '检查' }}</span>
            <h2>{{ item.examItem || '无检查项目' }}</h2>
            <p>{{ item.patientName || '无患者' }} · {{ item.doctorName || '无医生' }} · {{ item.departmentName || '无科室' }}</p>
          </div>
          <time>{{ formatDateTime(item.createdAt) }}</time>
        </div>
        <div class="conclusion-box">
          <span>检查结论</span>
          <strong>{{ item.conclusion || '无' }}</strong>
        </div>
      </article>
    </div>

    <EmptyState v-else-if="!loading" title="暂无检查结果" description="医生录入检查结果后，记录会显示在这里。" />
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
    return [item.patientName, item.doctorName, item.departmentName, item.examType, item.examItem, item.result, item.conclusion]
      .filter(Boolean)
      .some((value) => String(value).includes(key))
  })
})

onMounted(loadData)

async function loadData() {
  loading.value = true
  errorMessage.value = ''
  try {
    records.value = await api.examinationApi.list()
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
.exam-feed {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 18px;
  margin: 24px 0 0 18px;
  padding-left: 30px;
}

.exam-feed::before {
  content: '';
  position: absolute;
  top: 12px;
  bottom: 12px;
  left: 5px;
  width: 2px;
  background: linear-gradient(#f43f5e, #fed7aa);
}

.exam-card {
  position: relative;
  padding: 20px;
  border: 1px solid rgba(205, 218, 234, 0.86);
  border-left: 5px solid #f43f5e;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 18px 42px rgba(22, 45, 74, 0.06);
}

.exam-card::before {
  content: '';
  position: absolute;
  top: 28px;
  left: -38px;
  width: 14px;
  height: 14px;
  border: 4px solid #fff;
  border-radius: 999px;
  background: #f43f5e;
  box-shadow: 0 0 0 2px #fecdd3;
}

.exam-workspace .toolbar {
  margin-top: 18px;
}

.exam-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.exam-head h2 {
  margin: 9px 0 0;
  color: #0f172a;
  font-size: 23px;
  font-weight: 900;
}

.exam-head p,
.exam-head time {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 13px;
  font-weight: 800;
}

.type-pill {
  display: inline-flex;
  padding: 6px 10px;
  border-radius: 999px;
  color: #0e7490;
  background: #e6f8fb;
  font-size: 12px;
  font-weight: 900;
}

.conclusion-box {
  display: grid;
  gap: 8px;
  margin-top: 16px;
  padding: 14px;
  border: 1px solid #e2edf5;
  border-radius: 14px;
  background: #f8fcff;
}

.conclusion-box span {
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
}

.conclusion-box strong {
  color: #0f172a;
  line-height: 1.7;
}

@media (max-width: 760px) {
  .exam-head {
    flex-direction: column;
  }
}
</style>
