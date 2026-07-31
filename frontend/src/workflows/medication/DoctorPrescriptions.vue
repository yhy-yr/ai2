<template>
  <div class="prescription-workspace">
    <section class="prescription-hero">
      <div>
        <span>医生处方记录</span>
        <h1>处方维护</h1>
        <p>查看和追溯自己开具的处方记录。新增处方必须从接诊工作台进入，确保处方绑定到一次真实接诊。</p>
      </div>
      <div class="prescription-stats">
        <div><strong>{{ records.length }}</strong><span>处方记录</span></div>
        <div><strong>{{ medicineLineCount }}</strong><span>药品明细</span></div>
      </div>
    </section>

    <section class="notice-card">
      <strong>处方调整说明</strong>
      <span>已开具处方会保留用于追溯；需要新增处方请先选择接诊预约，调整用药可基于原处方重新开具。</span>
      <router-link class="primary-button" to="/workspace?workflow=doctor-appointments">选择接诊预约</router-link>
    </section>

    <div class="prescription-toolbar">
      <input v-model.trim="keyword" class="search-input" placeholder="搜索患者、科室、药品或医嘱" />
      <button class="secondary-button" @click="loadData">刷新</button>
    </div>

    <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>

    <div v-if="filteredRecords.length" class="record-list">
      <section v-for="item in filteredRecords" :key="item.id" class="info-card record-card">
        <div class="record-title">
          <div>
            <h3>{{ item.patientName || '患者' }}的处方</h3>
            <p>{{ item.departmentName || '无科室' }} · {{ formatDateTime(item.createdAt) }}</p>
          </div>
          <div class="record-actions">
            <span class="status-pill">{{ statusText(item.status) }}</span>
            <button class="secondary-button small-button" @click="toggleDetail(item)">
              {{ expandedId === item.id ? '收起' : '查看' }}
            </button>
            <button class="primary-button small-button" @click="openReissue(item)">重新开具</button>
          </div>
        </div>
        <p class="advice">医嘱：{{ item.advice || '无' }}</p>
        <div class="medicine-summary">
          <span v-for="row in item.items || []" :key="row.id || row.medicineName">
            {{ row.medicineName || '未命名药品' }} × {{ row.quantity || 0 }}
          </span>
        </div>
        <p class="reissue-tip">作废/重新开具提示：如当前处方需要调整，请重新开具新处方；原处方不会被覆盖。</p>
        <div v-if="expandedId === item.id" class="medicine-detail-grid">
          <article v-for="row in item.items" :key="row.id" class="medicine-detail-card">
            <div>
              <span>药品</span>
              <strong>{{ row.medicineName || '未命名药品' }}</strong>
            </div>
            <div>
              <span>剂量 / 频次</span>
              <strong>{{ row.dosage || '未填' }} · {{ row.frequency || '未填' }}</strong>
            </div>
            <div>
              <span>疗程 / 数量</span>
              <strong>{{ row.days || 0 }} 天 · {{ row.quantity || 0 }}</strong>
            </div>
            <div>
              <span>备注</span>
              <strong>{{ row.remark || '无' }}</strong>
            </div>
          </article>
        </div>
      </section>
    </div>

    <EmptyState v-else-if="!loading" title="暂无处方" description="在接诊工作台开具处方后，记录会显示在这里。" />

    <div v-if="showReissueModal" class="form-overlay">
      <section class="form-card wide-form-card">
        <div class="form-header">
          <div>
            <h2>重新开具处方</h2>
            <p>已复制原处方信息，修改后确认提交会生成一张新处方，原处方不被修改。</p>
          </div>
          <button class="secondary-button" @click="closeReissue">返回</button>
        </div>

        <label class="form-field full">
          <span>医嘱</span>
          <textarea v-model.trim="reissueForm.advice" rows="3" placeholder="请输入用药建议和注意事项" />
        </label>

        <div class="prescription-items">
          <div v-for="(row, index) in reissueForm.items" :key="index" class="prescription-row">
            <label class="form-field">
              <span>药品</span>
              <select v-model.number="row.medicineId">
                <option value="">请选择药品</option>
                <option v-for="medicine in medicines" :key="medicine.id" :value="medicine.id">
                  {{ medicine.name }}（库存 {{ medicine.stock }}）
                </option>
              </select>
            </label>
            <label class="form-field">
              <span>剂量</span>
              <input v-model.trim="row.dosage" placeholder="如 每次 1 片" />
            </label>
            <label class="form-field">
              <span>频次</span>
              <input v-model.trim="row.frequency" placeholder="如 每日 3 次" />
            </label>
            <label class="form-field">
              <span>天数</span>
              <input v-model.number="row.days" type="number" min="1" />
            </label>
            <label class="form-field">
              <span>数量</span>
              <input v-model.number="row.quantity" type="number" min="1" />
            </label>
            <label class="form-field">
              <span>备注</span>
              <input v-model.trim="row.remark" placeholder="可选" />
            </label>
            <button class="danger-button row-remove" :disabled="reissueForm.items.length === 1" @click="removeReissueItem(index)">删除</button>
          </div>
        </div>

        <p v-if="reissueError" class="error-text">{{ reissueError }}</p>
        <div class="form-actions">
          <button class="secondary-button" @click="addReissueItem">登记药品行</button>
          <button class="primary-button" :disabled="reissueLoading" @click="submitReissue">
            {{ reissueLoading ? '正在确认提交' : '确认提交新处方' }}
          </button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import api from '../../services/api'
import EmptyState from '../../components/ui/EmptyState.vue'

const records = ref([])
const medicines = ref([])
const keyword = ref('')
const loading = ref(false)
const reissueLoading = ref(false)
const errorMessage = ref('')
const reissueError = ref('')
const expandedId = ref(null)
const showReissueModal = ref(false)
const sourcePrescription = ref(null)

const reissueForm = reactive({
  advice: '',
  items: [newReissueItem()]
})

const medicineLineCount = computed(() => {
  return records.value.reduce((total, item) => total + (item.items?.length || 0), 0)
})

const filteredRecords = computed(() => {
  if (!keyword.value) return records.value
  const key = keyword.value
  return records.value.filter((item) => {
    const medicines = (item.items || []).map((row) => row.medicineName).join(',')
    return [item.patientName, item.departmentName, item.advice, medicines].filter(Boolean).some((value) => String(value).includes(key))
  })
})

onMounted(loadData)

async function loadData() {
  loading.value = true
  errorMessage.value = ''
  try {
    records.value = await api.prescriptionApi.doctorList()
  } catch (error) {
    errorMessage.value = error.message || '数据同步失败'
  } finally {
    loading.value = false
  }
}

function toggleDetail(item) {
  expandedId.value = expandedId.value === item.id ? null : item.id
}

async function openReissue(item) {
  errorMessage.value = ''
  reissueError.value = ''
  sourcePrescription.value = item
  reissueForm.advice = item.advice || ''
  reissueForm.items = (item.items || []).map((row) => ({
    medicineId: row.medicineId || '',
    dosage: row.dosage || '',
    frequency: row.frequency || '',
    days: row.days || 1,
    quantity: row.quantity || 1,
    remark: row.remark || ''
  }))
  if (!reissueForm.items.length) {
    reissueForm.items = [newReissueItem()]
  }

  try {
    if (!medicines.value.length) {
      medicines.value = await api.medicineApi.list()
    }
    showReissueModal.value = true
  } catch (error) {
    errorMessage.value = error.message || '数据同步失败'
  }
}

function closeReissue() {
  showReissueModal.value = false
  sourcePrescription.value = null
  reissueError.value = ''
}

function addReissueItem() {
  reissueForm.items.push(newReissueItem())
}

function removeReissueItem(index) {
  if (reissueForm.items.length > 1) {
    reissueForm.items.splice(index, 1)
  }
}

async function submitReissue() {
  reissueError.value = ''
  if (!sourcePrescription.value) {
    reissueError.value = '未找到原处方信息'
    return
  }
  const invalid = reissueForm.items.some((row) => {
    return !row.medicineId || !row.dosage || !row.frequency || !row.days || !row.quantity
  })
  if (invalid) {
    reissueError.value = '请完整填写药品、剂量、频次、天数和数量'
    return
  }

  reissueLoading.value = true
  try {
    await api.prescriptionApi.create({
      medicalRecordId: sourcePrescription.value.medicalRecordId,
      appointmentId: sourcePrescription.value.appointmentId,
      advice: reissueForm.advice,
      items: reissueForm.items
    })
    closeReissue()
    await loadData()
  } catch (error) {
    reissueError.value = error.message || '数据同步失败'
  } finally {
    reissueLoading.value = false
  }
}

function newReissueItem() {
  return {
    medicineId: '',
    dosage: '',
    frequency: '',
    days: 1,
    quantity: 1,
    remark: ''
  }
}

function statusText(status) {
  const map = {
    ISSUED: '已开具',
    VOIDED: '已作废'
  }
  return map[status] || status || '已开具'
}

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : ''
}
</script>

<style scoped>
.prescription-workspace {
  display: grid;
  gap: 18px;
  padding-bottom: 44px;
}

.prescription-hero {
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

.prescription-hero span {
  color: #0891b2;
  font-size: 12px;
  font-weight: 900;
}

.prescription-hero h1 {
  margin: 7px 0 0;
  color: #0f172a;
  font-size: 34px;
  line-height: 1.08;
  font-weight: 900;
}

.prescription-hero p {
  max-width: 700px;
  margin: 12px 0 0;
  color: #475569;
  font-size: 15px;
  line-height: 1.8;
}

.prescription-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(96px, 1fr));
  gap: 10px;
}

.prescription-stats div {
  min-width: 98px;
  padding: 13px;
  border: 1px solid #dbeafe;
  border-radius: 15px;
  background: #ffffff;
}

.prescription-stats strong {
  display: block;
  color: #0f172a;
  font-size: 24px;
  font-weight: 900;
}

.prescription-toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
}

.prescription-toolbar .search-input {
  flex: 1;
}

.record-list {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 18px;
  margin-left: 18px;
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

.notice-card {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  margin: 16px 0;
  padding: 14px 16px;
  border: 1px solid #bfdbfe;
  border-radius: 14px;
  background: #eff6ff;
  color: #1e40af;
}
.notice-card strong {
  white-space: nowrap;
}

.notice-card span {
  flex: 1;
  min-width: 260px;
}

.record-card {
  position: relative;
  border: 1px solid rgba(205, 218, 234, 0.86);
  border-left: 5px solid #14b8a6;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 18px 42px rgba(22, 45, 74, 0.06);
  margin-top: 0;
}

.record-card::before {
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

.record-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.record-title h3 {
  margin: 0;
  color: #0f172a;
  font-size: 22px;
  font-weight: 900;
}

.record-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
  align-items: center;
}
.record-title p,
.advice {
  margin: 6px 0 0;
  color: #64748b;
  line-height: 1.7;
}

.medicine-summary,
.medicine-detail-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.medicine-summary span {
  padding: 7px 10px;
  border-radius: 999px;
  color: #0f766e;
  background: #ccfbf1;
  font-size: 12px;
  font-weight: 900;
}

.reissue-tip {
  margin: 10px 0 0;
  padding: 10px 12px;
  border-radius: 10px;
  background: #fffbeb;
  color: #92400e;
  font-size: 13px;
}
.status-pill {
  padding: 6px 10px;
  border-radius: 999px;
  color: #0f766e;
  background: #ccfbf1;
  font-size: 12px;
  font-weight: 800;
}
.small-button {
  padding: 8px 12px;
  font-size: 13px;
}

.medicine-detail-grid {
  display: flex;
  flex-wrap: wrap;
  margin-top: 16px;
}

.medicine-detail-card {
  min-width: 250px;
  flex: 1 1 250px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  padding: 14px;
  border: 1px solid #e2edf5;
  border-radius: 14px;
  background: #f8fcff;
}

.medicine-detail-card div {
  display: grid;
  gap: 6px;
}

.medicine-detail-card span {
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
}

.medicine-detail-card strong {
  color: #0f172a;
  line-height: 1.6;
}

.wide-form-card {
  width: min(980px, calc(100vw - 32px));
}
.form-header p {
  margin: 6px 0 0;
  color: #6b7280;
}
.prescription-items {
  display: grid;
  gap: 12px;
  margin-top: 16px;
}
.prescription-row {
  display: grid;
  grid-template-columns: 1.5fr repeat(5, minmax(110px, 1fr)) auto;
  gap: 10px;
  align-items: end;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #f8fafc;
}
.row-remove {
  height: 40px;
}
@media (max-width: 960px) {
  .record-title,
  .notice-card,
  .prescription-hero,
  .prescription-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }
  .record-actions {
    justify-content: flex-start;
  }
  .prescription-row {
    grid-template-columns: 1fr;
  }

  .medicine-detail-grid,
  .medicine-detail-card,
  .prescription-stats {
    grid-template-columns: 1fr;
  }
}
</style>
