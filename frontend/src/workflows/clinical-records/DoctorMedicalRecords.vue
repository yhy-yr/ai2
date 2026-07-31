<template>
  <div class="records-workspace">
    <WorkflowHeader kicker="临床文档工作流" title="电子病历记录" description="查看、复核和修改自己保存的诊疗文档。">
      <template #metrics><div><strong>{{ records.length }}</strong><span>已保存病历</span></div></template>
    </WorkflowHeader>

    <div class="toolbar">
      <input v-model.trim="keyword" class="search-input" placeholder="搜索患者、主诉或诊断" />
      <button class="secondary-button" @click="loadData">刷新</button>
    </div>

    <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>

    <div v-if="filteredRecords.length" class="record-feed">
      <article v-for="item in filteredRecords" :key="item.id" class="record-card">
        <div class="record-head">
          <div>
            <span class="record-kicker">电子病历</span>
            <h2>{{ item.patientName || '未命名患者' }}</h2>
          </div>
          <span class="record-time">
            <CalendarClock :size="15" />
            {{ formatDateTime(item.createdAt) }}
          </span>
        </div>

        <div class="record-summary">
          <div>
            <span>主诉</span>
            <strong>{{ item.chiefComplaint || '无主诉记录' }}</strong>
          </div>
          <div>
            <span>医生诊断</span>
            <strong>{{ item.diagnosis || '待补充' }}</strong>
          </div>
        </div>

        <p>{{ item.treatmentPlan || '暂无治疗建议' }}</p>

        <div class="record-foot">
          <span>
            <Stethoscope :size="15" />
            {{ item.departmentName || '未记录科室' }}
          </span>
          <div class="record-actions">
            <button class="secondary-button" @click="openDetail(item)">
              <FileText :size="16" />
              查看详情
            </button>
            <button class="primary-button" @click="openEdit(item)">
              <Pencil :size="16" />
              修改信息
            </button>
          </div>
        </div>
      </article>
    </div>

    <EmptyState
      v-else-if="!loading"
      title="暂无病历"
      description="完成接诊并保存记录后，记录会显示在这里。"
    />

    <div v-if="detailRecord" class="form-overlay">
      <section class="form-card">
        <div class="form-header">
          <h2>病历详情</h2>
          <button class="secondary-button" @click="detailRecord = null">返回</button>
        </div>
        <div class="detail-grid">
          <div><span>患者</span><strong>{{ detailRecord.patientName }}</strong></div>
          <div><span>科室</span><strong>{{ detailRecord.departmentName }}</strong></div>
          <div><span>主诉</span><strong>{{ detailRecord.chiefComplaint }}</strong></div>
          <div><span>现病史</span><strong>{{ detailRecord.presentIllness }}</strong></div>
          <div><span>医生诊断</span><strong>{{ detailRecord.diagnosis }}</strong></div>
          <div><span>治疗建议</span><strong>{{ detailRecord.treatmentPlan }}</strong></div>
          <div><span>AI 草稿说明</span><strong>{{ detailRecord.aiDraft || '无' }}</strong></div>
        </div>
      </section>
    </div>

    <div v-if="editingId" class="form-overlay">
      <section class="form-card">
        <div class="form-header">
          <h2>修改病历信息</h2>
          <button class="secondary-button" @click="closeEdit">返回</button>
        </div>
        <div class="form-grid">
          <label class="form-field">
            <span>主诉</span>
            <input v-model.trim="form.chiefComplaint" />
          </label>
          <label class="form-field full">
            <span>现病史</span>
            <textarea v-model.trim="form.presentIllness" rows="4" />
          </label>
          <label class="form-field">
            <span>医生诊断</span>
            <input v-model.trim="form.diagnosis" />
          </label>
          <label class="form-field full">
            <span>治疗建议</span>
            <textarea v-model.trim="form.treatmentPlan" rows="4" />
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
import { CalendarClock, FileText, Pencil, Stethoscope } from '@lucide/vue'
import api from '../../services/api'
import EmptyState from '../../components/ui/EmptyState.vue'
import WorkflowHeader from '../../components/medical/WorkflowHeader.vue'

const records = ref([])
const keyword = ref('')
const loading = ref(false)
const errorMessage = ref('')
const formError = ref('')
const detailRecord = ref(null)
const editingId = ref(null)

const form = reactive({
  chiefComplaint: '',
  presentIllness: '',
  diagnosis: '',
  treatmentPlan: ''
})

const filteredRecords = computed(() => {
  if (!keyword.value) {
    return records.value
  }
  const key = keyword.value
  return records.value.filter((item) => {
    return [item.patientName, item.chiefComplaint, item.diagnosis, item.treatmentPlan]
      .filter(Boolean)
      .some((value) => String(value).includes(key))
  })
})

onMounted(loadData)

async function loadData() {
  loading.value = true
  errorMessage.value = ''
  try {
    records.value = await api.medicalRecordApi.doctorList()
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

function openEdit(item) {
  editingId.value = item.id
  Object.assign(form, {
    chiefComplaint: item.chiefComplaint || '',
    presentIllness: item.presentIllness || '',
    diagnosis: item.diagnosis || '',
    treatmentPlan: item.treatmentPlan || ''
  })
  formError.value = ''
}

function closeEdit() {
  editingId.value = null
}

async function submitEdit() {
  formError.value = ''
  if (!form.chiefComplaint || !form.presentIllness || !form.diagnosis || !form.treatmentPlan) {
    formError.value = '请填写主诉、现病史、医生诊断和治疗建议'
    return
  }

  try {
    await api.medicalRecordApi.update(editingId.value, form)
    editingId.value = null
    await loadData()
  } catch (error) {
    formError.value = error.message || '数据同步失败'
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
  background: linear-gradient(#0891b2, #bfdbfe);
}

.record-card {
  position: relative;
  padding: 22px;
  border: 1px solid rgba(205, 218, 234, 0.86);
  border-left: 5px solid #0891b2;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 18px 42px rgba(22, 45, 74, 0.07);
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
  box-shadow: 0 0 0 2px #a5f3fc;
}

.records-workspace .toolbar {
  margin-top: 18px;
}

.record-head,
.record-foot,
.record-actions,
.record-time,
.record-foot > span {
  display: flex;
  align-items: center;
}

.record-head {
  justify-content: space-between;
  gap: 18px;
}

.record-kicker {
  color: #0891b2;
  font-size: 12px;
  font-weight: 900;
}

.record-head h2 {
  margin: 6px 0 0;
  color: #0f172a;
  font-size: 24px;
  font-weight: 900;
}

.record-time {
  flex: 0 0 auto;
  gap: 7px;
  color: #64748b;
  font-size: 13px;
  font-weight: 800;
}

.record-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 18px;
}

.record-summary div {
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

.record-summary span {
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
}

.record-summary strong {
  color: #0f172a;
  line-height: 1.6;
}

.record-card p {
  margin: 16px 0 0;
  color: #475569;
  font-size: 14px;
  line-height: 1.8;
}

.record-foot {
  justify-content: space-between;
  gap: 14px;
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid #edf2f7;
}

.record-foot > span {
  gap: 7px;
  color: #0e7490;
  font-size: 13px;
  font-weight: 900;
}

.record-actions {
  gap: 10px;
}

.record-actions button {
  display: inline-flex;
  align-items: center;
  gap: 7px;
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
  .record-head,
  .record-foot {
    align-items: flex-start;
    flex-direction: column;
  }

  .record-summary {
    grid-template-columns: 1fr;
  }

  .record-actions {
    width: 100%;
    flex-wrap: wrap;
  }
}
</style>
