<template>
  <div class="exam-workspace">
    <WorkflowHeader kicker="个人检验历程" title="检查结果" description="按时间查看检查项目、结果结论和异常提示。" tone="rose">
      <template #metrics><div><strong>{{ records.length }}</strong><span>检查报告</span></div></template>
    </WorkflowHeader>

    <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>

    <div v-if="records.length" class="record-list">
      <section v-for="item in records" :key="item.id" class="info-card record-card">
        <div class="record-title">
          <div>
            <h3>{{ item.examItem || '检查项目' }}</h3>
            <p>{{ item.doctorName || '医生' }} · {{ item.departmentName || '科室' }} · {{ formatDateTime(item.createdAt) }}</p>
          </div>
          <span class="type-pill">{{ item.examType || '检查' }}</span>
        </div>
        <div class="result-grid">
          <div>
            <span>检查结果</span>
            <strong class="highlight-text">
              <span
                v-for="(segment, index) in highlightExamResult(item.result)"
                :key="`${item.id}-result-${index}`"
                :class="segmentClass(segment.type)"
              >
                {{ segment.text }}
              </span>
            </strong>
          </div>
          <div>
            <span>检查结论</span>
            <strong class="highlight-text">
              <span
                v-for="(segment, index) in highlightExamResult(item.conclusion)"
                :key="`${item.id}-conclusion-${index}`"
                :class="segmentClass(segment.type)"
              >
                {{ segment.text }}
              </span>
            </strong>
          </div>
        </div>
        <p v-if="examNotice(item)" :class="['exam-notice', examNotice(item).type]">{{ examNotice(item).message }}</p>
      </section>
    </div>

    <EmptyState v-else-if="!loading" title="暂无检查结果" description="医生录入检查结果后，你可以在这里查看。" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import api from '../../services/api'
import EmptyState from '../../components/ui/EmptyState.vue'
import WorkflowHeader from '../../components/medical/WorkflowHeader.vue'
import { analyzeExamResult, highlightExamResult } from '../../utils/examHighlight'

const records = ref([])
const loading = ref(false)
const errorMessage = ref('')

onMounted(loadData)

async function loadData() {
  loading.value = true
  errorMessage.value = ''
  try {
    records.value = await api.examinationApi.my()
  } catch (error) {
    errorMessage.value = error.message || '数据同步失败'
  } finally {
    loading.value = false
  }
}

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : ''
}

function examNotice(item) {
  return analyzeExamResult(item.result, item.conclusion)
}

function segmentClass(type) {
  return type === 'plain' ? '' : `exam-keyword ${type}`
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
  background: linear-gradient(#f43f5e, #fed7aa);
}
.record-card {
  position: relative;
  margin-top: 0;
  border-left: 4px solid #f43f5e;
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
  background: #f43f5e;
  box-shadow: 0 0 0 2px #fecdd3;
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
.record-title p {
  margin: 6px 0 0;
  color: #6b7280;
}
.type-pill {
  padding: 6px 10px;
  border-radius: 999px;
  color: #1d4ed8;
  background: #dbeafe;
  font-size: 12px;
  font-weight: 800;
}
.result-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 16px;
}
.result-grid div {
  display: flex;
  min-width: 260px;
  flex: 1 1 300px;
  flex-direction: column;
  gap: 6px;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #f8fafc;
}
.result-grid span {
  color: #6b7280;
  font-size: 13px;
}
.result-grid strong {
  color: #1f2937;
  line-height: 1.7;
}
.highlight-text {
  white-space: pre-wrap;
}
.exam-keyword {
  display: inline;
  padding: 2px 5px;
  border-radius: 6px;
  font-weight: 800;
}
.exam-keyword.important {
  color: #b91c1c;
  background: #fee2e2;
}
.exam-keyword.attention {
  color: #92400e;
  background: #fef3c7;
}
.exam-keyword.normal {
  color: #0f766e;
  background: #ccfbf1;
}
.exam-notice {
  margin: 14px 0 0;
  padding: 10px 12px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.7;
}
.exam-notice.warning {
  color: #92400e;
  background: #fffbeb;
}
.exam-notice.normal {
  color: #0f766e;
  background: #f0fdfa;
}
</style>
