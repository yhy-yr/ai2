<template>
  <div>
    <section class="exam-hero">
      <div>
        <span>医生检查记录</span>
        <h1>检查结果</h1>
        <p>查看和修改自己录入的检查结果。新增检查结果必须从接诊工作台进入，确保结果绑定到一次真实接诊。</p>
      </div>
      <router-link class="primary-button" to="/workspace?workflow=doctor-appointments">选择接诊预约</router-link>
    </section>

    <div class="toolbar">
      <input v-model.trim="keyword" class="search-input" placeholder="搜索患者、检查类型、项目或结论" />
      <button class="secondary-button" @click="loadData">刷新</button>
    </div>

    <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>

    <div v-if="filteredRecords.length" class="exam-feed">
      <article v-for="item in filteredRecords" :key="item.id" class="exam-card" :class="examNotice(item)?.type">
        <div class="exam-head">
          <div>
            <span class="type-pill">{{ item.examType || '检查' }}</span>
            <h2>{{ item.examItem || '无检查项目' }}</h2>
            <p>{{ item.patientName || '无患者' }} · {{ item.departmentName || '无科室' }} · {{ formatDateTime(item.createdAt) }}</p>
          </div>
          <button class="primary-button" @click="openEdit(item)">修改信息</button>
        </div>

        <div class="exam-grid">
          <section>
            <span>检查结果</span>
            <div class="highlight-text">
              <span
                v-for="(segment, index) in highlightExamResult(item.result)"
                :key="`${item.id}-result-${index}`"
                :class="segmentClass(segment.type)"
              >
                {{ segment.text }}
              </span>
            </div>
            <p v-if="examNotice(item)" :class="['exam-notice', examNotice(item).type]">{{ examNotice(item).message }}</p>
          </section>
          <section>
            <span>检查结论</span>
            <div class="highlight-text">
              <span
                v-for="(segment, index) in highlightExamResult(item.conclusion)"
                :key="`${item.id}-conclusion-${index}`"
                :class="segmentClass(segment.type)"
              >
                {{ segment.text }}
              </span>
            </div>
          </section>
        </div>
      </article>
    </div>

    <EmptyState v-else-if="!loading" title="暂无检查结果" description="在接诊工作台录入检查结果后，记录会显示在这里。" />

    <div v-if="editingId" class="form-overlay">
      <section class="form-card">
        <div class="form-header">
          <h2>修改检查结果</h2>
          <button class="secondary-button" @click="closeEdit">返回</button>
        </div>
        <div class="form-grid">
          <label class="form-field">
            <span>检查类型</span>
            <input v-model.trim="form.examType" />
          </label>
          <label class="form-field">
            <span>检查项目</span>
            <input v-model.trim="form.examItem" />
          </label>
          <label class="form-field full">
            <span>检查结果</span>
            <textarea v-model.trim="form.result" rows="4" />
          </label>
          <label class="form-field full">
            <span>检查结论</span>
            <textarea v-model.trim="form.conclusion" rows="3" />
          </label>
        </div>
        <p v-if="formError" class="error-text">{{ formError }}</p>
        <div class="form-actions">
          <button class="secondary-button" @click="closeEdit">返回</button>
          <button class="primary-button" @click="submitEdit">保存记录</button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import api from '../../services/api'
import EmptyState from '../../components/ui/EmptyState.vue'
import { analyzeExamResult, highlightExamResult } from '../../utils/examHighlight'

const records = ref([])
const keyword = ref('')
const loading = ref(false)
const errorMessage = ref('')
const formError = ref('')
const editingId = ref(null)
const form = reactive({
  examType: '',
  examItem: '',
  result: '',
  conclusion: ''
})

const filteredRecords = computed(() => {
  if (!keyword.value) return records.value
  const key = keyword.value
  return records.value.filter((item) => {
    return [item.patientName, item.departmentName, item.examType, item.examItem, item.result, item.conclusion]
      .filter(Boolean)
      .some((value) => String(value).includes(key))
  })
})

onMounted(loadData)

async function loadData() {
  loading.value = true
  errorMessage.value = ''
  try {
    records.value = await api.examinationApi.doctorList()
  } catch (error) {
    errorMessage.value = error.message || '数据同步失败'
  } finally {
    loading.value = false
  }
}

function openEdit(item) {
  editingId.value = item.id
  Object.assign(form, {
    examType: item.examType || '',
    examItem: item.examItem || '',
    result: item.result || '',
    conclusion: item.conclusion || ''
  })
  formError.value = ''
}

function closeEdit() {
  editingId.value = null
}

async function submitEdit() {
  formError.value = ''
  if (!form.examType || !form.examItem || !form.result || !form.conclusion) {
    formError.value = '请填写检查类型、检查项目、检查结果和检查结论'
    return
  }
  try {
    await api.examinationApi.update(editingId.value, form)
    closeEdit()
    await loadData()
  } catch (error) {
    formError.value = error.message || '数据同步失败'
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
.exam-hero {
  display: flex;
  justify-content: space-between;
  gap: 22px;
  align-items: flex-start;
  padding: 26px;
  margin-bottom: 18px;
  border: 1px solid rgba(205, 218, 234, 0.86);
  border-radius: 22px;
  background:
    linear-gradient(135deg, rgba(8, 145, 178, 0.12), rgba(20, 184, 166, 0.08)),
    rgba(255, 255, 255, 0.9);
  box-shadow: 0 18px 48px rgba(22, 45, 74, 0.07);
}

.exam-hero span {
  color: #0891b2;
  font-size: 12px;
  font-weight: 900;
}

.exam-hero h1 {
  margin: 7px 0 0;
  color: #0f172a;
  font-size: 34px;
  line-height: 1.08;
  font-weight: 900;
}

.exam-hero p {
  max-width: 700px;
  margin: 12px 0 0;
  color: #475569;
  font-size: 15px;
  line-height: 1.8;
}

.exam-feed {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 18px;
  margin-left: 18px;
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
  border-left: 5px solid #0891b2;
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

.exam-card.warning {
  border-left-color: #f59e0b;
}

.exam-card.normal {
  border-left-color: #14b8a6;
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

.exam-head p {
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

.exam-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 16px;
}

.exam-grid section {
  min-width: 260px;
  flex: 1 1 300px;
  padding: 14px;
  border: 1px solid #e2edf5;
  border-radius: 14px;
  background: #f8fcff;
}

.exam-grid section > span {
  display: block;
  margin-bottom: 8px;
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
}

.highlight-text {
  line-height: 1.8;
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
  margin: 8px 0 0;
  padding: 8px 10px;
  border-radius: 10px;
  font-size: 13px;
  line-height: 1.6;
}

.exam-notice.warning {
  color: #92400e;
  background: #fffbeb;
}

.exam-notice.normal {
  color: #0f766e;
  background: #f0fdfa;
}

@media (max-width: 760px) {
  .exam-hero {
    flex-direction: column;
  }

  .exam-head {
    flex-direction: column;
  }

  .exam-grid {
    grid-template-columns: 1fr;
  }
}
</style>
