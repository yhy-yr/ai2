<template>
  <div class="encounter-workspace">
    <section class="encounter-hero">
      <div>
        <span class="hero-kicker">接诊工作台</span>
        <h1>{{ appointment?.patientName || '当前患者' }}</h1>
        <p>{{ appointment?.symptomDescription || '查看患者预约信息，生成 AI 病历草稿并保存病历记录。' }}</p>
      </div>
      <div class="hero-meta">
        <StatusTag :text="statusText(appointment?.status)" :type="statusType(appointment?.status)" />
        <strong>{{ appointment?.appointmentDate || '' }} {{ appointment?.timeSlot || '' }}</strong>
        <span>{{ appointment?.departmentName || '未记录科室' }}</span>
      </div>
    </section>

    <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>
    <p v-if="successMessage" class="success-text">{{ successMessage }}</p>

    <div class="encounter-shell">
      <aside class="clinical-sidebar">
        <section class="info-card patient-card">
          <h3>患者与预约</h3>
          <div class="info-list">
            <div><span>性别</span><strong>{{ appointment?.gender || '未填' }}</strong></div>
            <div><span>年龄</span><strong>{{ appointment?.age ?? 0 }} 岁</strong></div>
            <div><span>手机号</span><strong>{{ appointment?.phone || '无' }}</strong></div>
            <div><span>预约编号</span><strong>{{ appointmentId || '无' }}</strong></div>
            <div class="full"><span>预约症状</span><strong>{{ appointment?.symptomDescription || '无' }}</strong></div>
          </div>
        </section>

        <section class="info-card risk-card" :class="riskClass(riskAssessment)">
          <div class="risk-card-header">
            <div>
              <h3>风险提示</h3>
              <p>根据预约症状生成建议检查项目和就医建议。</p>
            </div>
            <span v-if="riskAssessment" class="risk-level-tag">{{ riskAssessment.levelText }}</span>
          </div>

          <template v-if="riskAssessment">
            <div class="risk-content">
              <div>
                <span>建议检查</span>
                <div class="check-list">
                  <strong v-for="item in riskAssessment.suggestedChecks" :key="item">{{ item }}</strong>
                </div>
              </div>
              <div>
                <span>建议</span>
                <strong>{{ riskAssessment.advice }}</strong>
              </div>
            </div>
            <p class="risk-disclaimer">仅供辅助参考，最终诊断以医生判断为准。</p>
          </template>

          <p v-else class="risk-disclaimer">暂无风险评估结果</p>
        </section>

        <section class="info-card completeness-card">
          <div class="completeness-header">
            <div>
              <h3>接诊闭环</h3>
              <p>病历、检查、处方和 AI 问诊的完成状态。</p>
            </div>
            <strong v-if="visitCompleteness" class="completeness-percent">{{ visitCompleteness.percent || 0 }}%</strong>
          </div>

          <template v-if="visitCompleteness">
            <div class="completeness-progress">
              <span :style="{ width: `${visitCompleteness.percent || 0}%` }"></span>
            </div>

            <p v-if="visitCompleteness.closed" class="closed-message">本次接诊流程已闭环。</p>
            <p v-else class="open-message">请先完成待办事项，再结束本次接诊。</p>

            <div class="workflow-nodes">
              <div
                v-for="node in visitCompleteness.nodes || []"
                :key="node.name"
                :class="{ done: node.done }"
              >
                <span></span>
                <strong>{{ node.name }}</strong>
              </div>
            </div>

            <div class="completion-actions">
              <button
                v-if="appointment?.status !== 'COMPLETED'"
                class="primary-button"
                :disabled="completeLoading || !visitCompleteness.closed"
                @click="completeVisit"
              >
                {{ completeButtonText }}
              </button>
              <button
                v-else
                class="secondary-button"
                :disabled="completeLoading"
                @click="reopenVisit"
              >
                {{ completeLoading ? '正在恢复接诊' : '继续编辑接诊' }}
              </button>
              <span v-if="appointment?.status === 'COMPLETED'">已归档为诊疗结果，可恢复为接诊中继续修改。</span>
              <span v-else-if="!visitCompleteness.closed">待完成：{{ visitCompleteness.pendingItems?.join('、') || '接诊事项' }}</span>
            </div>
          </template>

          <p v-else class="muted-text">暂无完整度信息</p>
        </section>
      </aside>

      <section class="clinical-document">
        <div class="document-head">
          <div>
            <span>电子病历</span>
            <h2>诊疗记录书写</h2>
          </div>
          <button class="secondary-button" :disabled="draftLoading || isVisitCompleted" @click="generateDraft">
            {{ draftLoading ? '正在生成草稿' : '生成 AI 草稿' }}
          </button>
        </div>

        <section class="draft-panel">
          <div class="title-with-tag">
            <h3>AI 病历草稿</h3>
            <span v-if="draft" class="source-tag">{{ sourceText(draft.source) }}</span>
          </div>
          <p class="ai-disclaimer">AI 结果仅供辅助参考，主诉和现病史需医生确认后生效。</p>

          <div v-if="draft" class="draft-box">
            <div><span>主诉</span><strong>{{ draft.chiefComplaint }}</strong></div>
            <div><span>现病史</span><strong>{{ draft.presentIllness }}</strong></div>
            <div><span>说明</span><strong>{{ draft.aiDraft }}</strong></div>
          </div>

          <EmptyState
            v-else
            title="暂无 AI 草稿"
            description="点击生成按钮后，系统会根据预约症状生成主诉和现病史草稿。"
          />
        </section>

        <section class="record-form-card">
          <div class="form-grid">
            <label class="form-field">
              <span>主诉</span>
              <input v-model.trim="form.chiefComplaint" :disabled="isVisitCompleted" placeholder="请输入主诉" />
            </label>
            <label class="form-field full">
              <span>现病史</span>
              <textarea v-model.trim="form.presentIllness" :disabled="isVisitCompleted" rows="5" placeholder="请输入现病史" />
            </label>
            <label class="form-field">
              <span>医生诊断</span>
              <input v-model.trim="form.diagnosis" :disabled="isVisitCompleted" placeholder="由医生填写诊断" />
            </label>
            <label class="form-field full">
              <span>治疗建议</span>
              <textarea v-model.trim="form.treatmentPlan" :disabled="isVisitCompleted" rows="5" placeholder="请输入治疗建议" />
            </label>
          </div>

          <div class="form-actions document-actions">
            <router-link class="secondary-button" to="/workspace?workflow=doctor-appointments">返回预约</router-link>
            <template v-if="isVisitCompleted">
              <router-link class="primary-button" to="/workspace?workflow=doctor-records">查看电子病历</router-link>
              <router-link class="secondary-button" to="/workspace?workflow=doctor-examinations">查看检查结果</router-link>
              <router-link class="primary-button" to="/workspace?workflow=doctor-prescriptions">查看处方记录</router-link>
            </template>
            <template v-else>
              <button class="primary-button" :disabled="saveLoading" @click="saveRecord">
                {{ saveLoading ? '正在保存记录' : '保存记录' }}
              </button>
              <button class="secondary-button" @click="openExaminationModal">录入检查结果</button>
              <button class="primary-button" @click="openPrescriptionModal">开具处方</button>
            </template>
          </div>
        </section>

        <section v-if="medicalRecordId" class="info-card next-actions-card">
          <h3>后续诊疗安排</h3>
          <p>{{ isVisitCompleted ? '本次接诊已完成，处方和检查结果已归档为可追溯记录。' : '信息已更新，可以录入检查结果或开具处方。' }}</p>
          <div class="quick-actions">
            <template v-if="isVisitCompleted">
              <router-link class="secondary-button" to="/workspace?workflow=doctor-examinations">查看检查结果</router-link>
              <router-link class="primary-button" to="/workspace?workflow=doctor-prescriptions">查看处方记录</router-link>
              <router-link class="secondary-button" to="/workspace?workflow=doctor-records">查看电子病历</router-link>
            </template>
            <template v-else>
              <button class="secondary-button" @click="openExaminationModal">录入检查结果</button>
              <button class="primary-button" @click="openPrescriptionModal">开具处方</button>
              <router-link class="secondary-button" to="/workspace?workflow=doctor-records">查看电子病历</router-link>
            </template>
          </div>
        </section>
      </section>
    </div>

    <div v-if="showPrescriptionModal" class="form-overlay">
      <section class="form-card wide-form-card">
        <div class="form-header">
          <h2>开具处方</h2>
          <button class="secondary-button" @click="showPrescriptionModal = false">返回</button>
        </div>
        <label class="form-field full">
          <span>医嘱</span>
          <textarea v-model.trim="prescriptionForm.advice" rows="3" placeholder="请输入用药建议和注意事项" />
        </label>

        <div class="prescription-items">
          <div v-for="(item, index) in prescriptionForm.items" :key="index" class="prescription-row">
            <label class="form-field">
              <span>药品</span>
              <select v-model.number="item.medicineId">
                <option value="">请选择药品</option>
                <option v-for="medicine in medicines" :key="medicine.id" :value="medicine.id">
                  {{ medicine.name }}（库存 {{ medicine.stock }}）
                </option>
              </select>
            </label>
            <label class="form-field">
              <span>剂量</span>
              <input v-model.trim="item.dosage" placeholder="如 每次 1 片" />
            </label>
            <label class="form-field">
              <span>频次</span>
              <input v-model.trim="item.frequency" placeholder="如 每日 3 次" />
            </label>
            <label class="form-field">
              <span>天数</span>
              <input v-model.number="item.days" type="number" min="1" />
            </label>
            <label class="form-field">
              <span>数量</span>
              <input v-model.number="item.quantity" type="number" min="1" />
            </label>
            <label class="form-field">
              <span>备注</span>
              <input v-model.trim="item.remark" placeholder="可选" />
            </label>
            <button class="danger-button row-remove" :disabled="prescriptionForm.items.length === 1" @click="removePrescriptionItem(index)">
              删除
            </button>
            <div class="medication-warning-panel">
              <div class="medication-warning-title">用药安全提醒</div>
              <ul v-if="getMedicationWarningsByItem(item).length" class="medication-warning-list">
                <li v-for="warning in getMedicationWarningsByItem(item)" :key="warning">{{ warning }}</li>
              </ul>
              <p v-else class="medication-warning-empty">暂无特殊用药风险提示，请按规范核对患者情况。</p>
            </div>
          </div>
        </div>

        <p v-if="prescriptionError" class="error-text">{{ prescriptionError }}</p>
        <div class="form-actions">
          <button class="secondary-button" @click="addPrescriptionItem">登记药品行</button>
          <button class="primary-button" :disabled="prescriptionLoading" @click="submitPrescription">
            {{ prescriptionLoading ? '正在确认提交' : '确认提交处方' }}
          </button>
        </div>
      </section>
    </div>

    <div v-if="showExaminationModal" class="form-overlay">
      <section class="form-card">
        <div class="form-header">
          <h2>录入检查结果</h2>
          <button class="secondary-button" @click="showExaminationModal = false">返回</button>
        </div>
        <div class="form-grid">
          <label class="form-field">
            <span>检查类型</span>
            <input v-model.trim="examinationForm.examType" placeholder="如 实验室检查" />
          </label>
          <label class="form-field">
            <span>检查项目</span>
            <input v-model.trim="examinationForm.examItem" placeholder="如 血常规" />
          </label>
          <label class="form-field full">
            <span>检查结果</span>
            <textarea v-model.trim="examinationForm.result" rows="4" placeholder="请输入检查结果" />
          </label>
          <label class="form-field full">
            <span>检查结论</span>
            <textarea v-model.trim="examinationForm.conclusion" rows="3" placeholder="请输入检查结论" />
          </label>
        </div>
        <p v-if="examinationError" class="error-text">{{ examinationError }}</p>
        <div class="form-actions">
          <button class="secondary-button" @click="showExaminationModal = false">返回</button>
          <button class="primary-button" :disabled="examinationLoading" @click="submitExamination">
            {{ examinationLoading ? '正在确认提交' : '保存记录' }}
          </button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import api from '../../services/api'
import EmptyState from '../../components/ui/EmptyState.vue'
import StatusTag from '../../components/medical/StatusTag.vue'

const route = useRoute()
const appointmentId = Number(route.params.id)

const appointment = ref(null)
const draft = ref(null)
const riskAssessment = ref(null)
const visitCompleteness = ref(null)
const errorMessage = ref('')
const successMessage = ref('')
const draftLoading = ref(false)
const saveLoading = ref(false)
const savedRecord = ref(null)
const medicalRecordId = ref(null)
const medicines = ref([])
const showPrescriptionModal = ref(false)
const showExaminationModal = ref(false)
const prescriptionLoading = ref(false)
const examinationLoading = ref(false)
const completeLoading = ref(false)
const prescriptionError = ref('')
const examinationError = ref('')

const form = reactive({
  chiefComplaint: '',
  presentIllness: '',
  diagnosis: '',
  treatmentPlan: '',
  aiDraft: ''
})

const prescriptionForm = reactive({
  advice: '',
  items: [newPrescriptionItem()]
})

const examinationForm = reactive({
  examType: '',
  examItem: '',
  result: '',
  conclusion: ''
})

onMounted(loadAppointment)

const completeButtonText = computed(() => {
  if (completeLoading.value) return '正在完成接诊'
  if (appointment.value?.status === 'COMPLETED') return '接诊已完成'
  return '完成本次接诊'
})

const isVisitCompleted = computed(() => appointment.value?.status === 'COMPLETED')

async function loadAppointment() {
  errorMessage.value = ''
  try {
    const list = await api.appointmentApi.doctorList()
    appointment.value = list.find((item) => Number(item.id) === appointmentId) || null
    if (!appointment.value) {
      errorMessage.value = '预约不存在或不属于当前医生'
      return
    }
    await loadRiskAssessment()
    await loadVisitCompleteness()
    await loadExistingRecord()
  } catch (error) {
    errorMessage.value = error.message || '数据同步失败'
  }
}

async function loadRiskAssessment() {
  try {
    riskAssessment.value = await api.aiRisk(appointment.value?.symptomDescription || '')
  } catch (error) {
    riskAssessment.value = null
  }
}

async function loadVisitCompleteness() {
  try {
    visitCompleteness.value = await api.visitCompleteness(appointmentId)
  } catch (error) {
    visitCompleteness.value = null
  }
}

async function loadExistingRecord() {
  try {
    const records = await api.medicalRecordApi.doctorList()
    const existing = records.find((item) => Number(item.appointmentId) === appointmentId)
    if (existing) {
      savedRecord.value = existing
      medicalRecordId.value = existing.id
      Object.assign(form, {
        chiefComplaint: existing.chiefComplaint || form.chiefComplaint,
        presentIllness: existing.presentIllness || form.presentIllness,
        diagnosis: existing.diagnosis || form.diagnosis,
        treatmentPlan: existing.treatmentPlan || form.treatmentPlan,
        aiDraft: existing.aiDraft || form.aiDraft
      })
    }
  } catch (error) {
    errorMessage.value = error.message || '数据同步失败'
  }
}

async function generateDraft() {
  errorMessage.value = ''
  if (isVisitCompleted.value) {
    errorMessage.value = '本次接诊已完成，如需修改请进入病历记录'
    return
  }
  draftLoading.value = true
  try {
    draft.value = await api.medicalRecordApi.aiDraft(appointmentId)
    form.chiefComplaint = draft.value.chiefComplaint || ''
    form.presentIllness = draft.value.presentIllness || ''
    form.aiDraft = draft.value.aiDraft || ''
  } catch (error) {
    errorMessage.value = error.message || '数据同步失败'
  } finally {
    draftLoading.value = false
  }
}

async function saveRecord() {
  errorMessage.value = ''
  successMessage.value = ''
  if (isVisitCompleted.value) {
    errorMessage.value = '本次接诊已完成，如需修改请进入病历记录'
    return
  }
  if (!form.chiefComplaint || !form.presentIllness || !form.diagnosis || !form.treatmentPlan) {
    errorMessage.value = '请填写主诉、现病史、医生诊断和治疗建议'
    return
  }

  saveLoading.value = true
  try {
    const response = await api.medicalRecordApi.create({
      appointmentId,
      chiefComplaint: form.chiefComplaint,
      presentIllness: form.presentIllness,
      diagnosis: form.diagnosis,
      treatmentPlan: form.treatmentPlan,
      aiDraft: form.aiDraft
    })
    savedRecord.value = response
    medicalRecordId.value = response?.id || response?.data?.id || null
    if (!medicalRecordId.value) {
      errorMessage.value = '信息已更新，但暂未同步病历编号'
      return
    }
    successMessage.value = '信息已更新，可以录入检查结果或开具处方。'
    await loadAppointment()
  } catch (error) {
    errorMessage.value = error.message || '数据同步失败'
  } finally {
    saveLoading.value = false
  }
}

async function openPrescriptionModal() {
  errorMessage.value = ''
  prescriptionError.value = ''
  if (isVisitCompleted.value) {
    errorMessage.value = '本次接诊已完成，不能继续开具处方'
    return
  }
  if (!medicalRecordId.value) {
    errorMessage.value = '请先保存病历记录'
    return
  }
  if (!medicines.value.length) {
    try {
      medicines.value = await api.medicineApi.list()
    } catch (error) {
      errorMessage.value = error.message || '数据同步失败'
      return
    }
    if (!medicines.value.length) {
      errorMessage.value = '暂无药品，请先让院内维护员维护药品数据。'
      return
    }
  }
  showPrescriptionModal.value = true
}

function addPrescriptionItem() {
  prescriptionForm.items.push(newPrescriptionItem())
}

function removePrescriptionItem(index) {
  if (prescriptionForm.items.length > 1) {
    prescriptionForm.items.splice(index, 1)
  }
}

function findMedicine(medicineId) {
  return medicines.value.find((medicine) => Number(medicine.id) === Number(medicineId)) || null
}

function getMedicationWarnings(medicine) {
  if (!medicine) {
    return []
  }

  const name = medicine.name || ''
  const warnings = []

  if (['阿莫西林', '青霉素', '头孢'].some((keyword) => name.includes(keyword))) {
    warnings.push('请确认患者是否存在青霉素或头孢类抗生素过敏史。')
  }
  if (['布洛芬', '阿司匹林'].some((keyword) => name.includes(keyword))) {
    warnings.push('胃病、消化道出血风险或凝血功能异常患者慎用。')
  }
  if (['降压', '硝苯地平', '缬沙坦'].some((keyword) => name.includes(keyword))) {
    warnings.push('请注意患者血压情况，避免重复使用同类降压药。')
  }
  if (['二甲双胍', '胰岛素'].some((keyword) => name.includes(keyword))) {
    warnings.push('请结合血糖水平使用，注意低血糖风险。')
  }
  if (Number(medicine.stock) < 20) {
    warnings.push('当前药品库存偏低，请及时补药。')
  }

  return warnings
}

function getMedicationWarningsByItem(item) {
  return getMedicationWarnings(findMedicine(item.medicineId))
}

async function submitPrescription() {
  prescriptionError.value = ''
  if (!medicalRecordId.value) {
    prescriptionError.value = '请先保存病历记录'
    return
  }
  const invalid = prescriptionForm.items.some((item) => {
    return !item.medicineId || !item.dosage || !item.frequency || !item.days || !item.quantity
  })
  if (invalid) {
    prescriptionError.value = '请完整填写药品、剂量、频次、天数和数量'
    return
  }

  prescriptionLoading.value = true
  try {
    await api.prescriptionApi.create({
      medicalRecordId: medicalRecordId.value,
      appointmentId,
      advice: prescriptionForm.advice,
      items: prescriptionForm.items
    })
    successMessage.value = '信息已更新'
    showPrescriptionModal.value = false
    resetPrescriptionForm()
    medicines.value = await api.medicineApi.list()
    await loadVisitCompleteness()
  } catch (error) {
    prescriptionError.value = error.message || '数据同步失败'
  } finally {
    prescriptionLoading.value = false
  }
}

function openExaminationModal() {
  errorMessage.value = ''
  examinationError.value = ''
  if (isVisitCompleted.value) {
    errorMessage.value = '本次接诊已完成，不能继续录入检查结果'
    return
  }
  if (!medicalRecordId.value) {
    errorMessage.value = '请先保存病历记录'
    return
  }
  showExaminationModal.value = true
}

async function submitExamination() {
  examinationError.value = ''
  if (!examinationForm.examType || !examinationForm.examItem || !examinationForm.result || !examinationForm.conclusion) {
    examinationError.value = '请填写检查类型、检查项目、检查结果和检查结论'
    return
  }
  examinationLoading.value = true
  try {
    await api.examinationApi.create({
      appointmentId,
      examType: examinationForm.examType,
      examItem: examinationForm.examItem,
      result: examinationForm.result,
      conclusion: examinationForm.conclusion
    })
    successMessage.value = '信息已更新'
    showExaminationModal.value = false
    resetExaminationForm()
    await loadVisitCompleteness()
  } catch (error) {
    examinationError.value = error.message || '数据同步失败'
  } finally {
    examinationLoading.value = false
  }
}

async function completeVisit() {
  errorMessage.value = ''
  successMessage.value = ''
  if (!visitCompleteness.value?.closed) {
    errorMessage.value = `请先完成：${visitCompleteness.value?.pendingItems?.join('、') || '接诊事项'}`
    return
  }
  if (!window.confirm('确认完成本次接诊？完成后患者端将看到诊疗结果。')) {
    return
  }

  completeLoading.value = true
  try {
    appointment.value = await api.appointmentApi.updateStatus(appointmentId, 'COMPLETED')
    successMessage.value = '本次接诊已完成，患者端可查看诊疗结果。'
    await loadVisitCompleteness()
  } catch (error) {
    errorMessage.value = error.message || '数据同步失败'
  } finally {
    completeLoading.value = false
  }
}

async function reopenVisit() {
  errorMessage.value = ''
  successMessage.value = ''
  completeLoading.value = true
  try {
    appointment.value = await api.appointmentApi.updateStatus(appointmentId, 'IN_PROGRESS')
    successMessage.value = '已恢复为接诊中，可以继续修改病历、检查结果和处方。'
    await loadVisitCompleteness()
  } catch (error) {
    errorMessage.value = error.message || '数据同步失败'
  } finally {
    completeLoading.value = false
  }
}

function newPrescriptionItem() {
  return {
    medicineId: '',
    dosage: '',
    frequency: '',
    days: 1,
    quantity: 1,
    remark: ''
  }
}

function resetPrescriptionForm() {
  prescriptionForm.advice = ''
  prescriptionForm.items = [newPrescriptionItem()]
}

function resetExaminationForm() {
  Object.assign(examinationForm, {
    examType: '',
    examItem: '',
    result: '',
    conclusion: ''
  })
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

function sourceText(source) {
  return source === 'DEEPSEEK' ? 'DeepSeek AI 生成' : '本地规则生成'
}

function riskClass(risk) {
  if (!risk) {
    return 'risk-unknown'
  }
  if (risk.level === 'HIGH') {
    return 'risk-high'
  }
  if (risk.level === 'MEDIUM') {
    return 'risk-medium'
  }
  return 'risk-low'
}
</script>

<style scoped>
.encounter-workspace {
  display: flex;
  min-height: calc(100vh - 96px);
  flex-direction: column;
  gap: 18px;
  padding-bottom: 42px;
}

.encounter-hero {
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

.hero-kicker,
.document-head span {
  color: #0891b2;
  font-size: 12px;
  font-weight: 900;
}

.encounter-hero h1 {
  margin: 7px 0 0;
  color: #0f172a;
  font-size: 34px;
  line-height: 1.08;
  font-weight: 900;
}

.encounter-hero p {
  max-width: 720px;
  margin: 12px 0 0;
  color: #475569;
  font-size: 15px;
  line-height: 1.8;
}

.hero-meta {
  display: grid;
  min-width: 190px;
  justify-items: end;
  gap: 9px;
  color: #64748b;
  font-size: 13px;
  font-weight: 800;
}

.hero-meta strong {
  color: #0f172a;
  font-size: 15px;
}

.encounter-shell {
  display: grid;
  grid-template-columns: minmax(280px, 0.42fr) minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.clinical-sidebar,
.clinical-document {
  display: grid;
  gap: 18px;
}

.clinical-sidebar {
  position: sticky;
  top: 88px;
}

.patient-card,
.clinical-document,
.draft-panel,
.record-form-card {
  border: 1px solid rgba(205, 218, 234, 0.86);
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 18px 42px rgba(22, 45, 74, 0.06);
}

.patient-card,
.draft-panel,
.record-form-card {
  padding: 20px;
  border-radius: 18px;
}

.clinical-document {
  padding: 22px;
  border-radius: 22px;
}

.document-head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: center;
}

.document-head h2 {
  margin: 6px 0 0;
  color: #0f172a;
  font-size: 25px;
  font-weight: 900;
}

.draft-panel {
  border-left: 4px solid #0891b2;
}

.draft-panel h3,
.record-form-card h3 {
  margin: 0;
}

.visit-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 20px;
}

.flow-tip {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-top: 18px;
  padding: 18px 20px;
  border: 1px solid rgba(20, 184, 166, 0.24);
  border-radius: 16px;
  color: #115e59;
  background:
    linear-gradient(135deg, rgba(20, 184, 166, 0.14), rgba(37, 99, 235, 0.08)),
    #ffffff;
  box-shadow: 0 14px 36px rgba(31, 41, 55, 0.08);
  line-height: 1.7;
}

.flow-tip span {
  color: #0f766e;
  font-weight: 800;
}

.visit-card {
  margin-top: 24px;
  min-height: 100%;
}

.info-list,
.draft-box {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.info-list div,
.draft-box div {
  display: grid;
  gap: 6px;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #f8fafc;
}

.info-list .full,
.draft-box div {
  grid-column: 1 / -1;
}

.info-list span,
.draft-box span {
  color: #64748b;
  font-size: 13px;
}

.info-list strong,
.draft-box strong {
  color: #1f2937;
  line-height: 1.7;
}

.card-title-row {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
}

.title-with-tag {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.source-tag {
  display: inline-flex;
  padding: 5px 9px;
  border-radius: 999px;
  color: #0f766e;
  background: #ccfbf1;
  font-size: 12px;
  font-weight: 800;
}

.ai-disclaimer {
  margin: 12px 0 0;
  color: #0f766e;
  font-weight: 800;
}

.record-form-card {
  margin-top: 20px;
  border-top: 4px solid #2563eb;
}

.risk-card {
  margin-top: 20px;
  border-left: 4px solid #22c55e;
}

.risk-high {
  border-left-color: #ef4444;
  background: linear-gradient(135deg, #fff5f5, #ffffff);
}

.risk-medium {
  border-left-color: #f59e0b;
  background: linear-gradient(135deg, #fffbeb, #ffffff);
}

.risk-low {
  border-left-color: #22c55e;
  background: linear-gradient(135deg, #f0fdf4, #ffffff);
}

.risk-unknown {
  border-left-color: #9ca3af;
}

.risk-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.risk-card-header p {
  margin-top: 6px;
  color: #6b7280;
}

.risk-level-tag {
  padding: 7px 12px;
  border-radius: 999px;
  color: #ffffff;
  background: #2563eb;
  font-size: 13px;
  font-weight: 800;
}

.risk-high .risk-level-tag {
  background: #ef4444;
}

.risk-medium .risk-level-tag {
  background: #f59e0b;
}

.risk-low .risk-level-tag {
  background: #22c55e;
}

.risk-content {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.2fr);
  gap: 14px;
  margin-top: 16px;
}

.risk-content > div {
  display: grid;
  gap: 8px;
  padding: 14px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.74);
}

.risk-content span {
  color: #6b7280;
  font-size: 13px;
}

.risk-content strong {
  color: #1f2937;
  line-height: 1.7;
}

.check-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.check-list strong {
  padding: 6px 10px;
  border-radius: 999px;
  background: #eef2ff;
  font-size: 13px;
}

.risk-disclaimer {
  margin-top: 14px;
  color: #6b7280;
  line-height: 1.7;
}

.completeness-card {
  margin-top: 20px;
  border-top: 4px solid #2563eb;
}

.completeness-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.completeness-header h3 {
  margin: 0;
}

.completeness-header p {
  margin: 6px 0 0;
  color: #6b7280;
}

.completeness-percent {
  min-width: 72px;
  padding: 8px 12px;
  border-radius: 999px;
  color: #1d4ed8;
  background: #dbeafe;
  text-align: center;
  font-size: 20px;
}

.completeness-progress {
  height: 12px;
  margin-top: 18px;
  overflow: hidden;
  border-radius: 999px;
  background: #e5e7eb;
}

.completeness-progress span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #2563eb, #14b8a6);
}

.closed-message {
  margin: 14px 0 0;
  padding: 12px 14px;
  border: 1px solid #99f6e4;
  border-radius: 12px;
  color: #0f766e;
  background: #f0fdfa;
  font-weight: 800;
}

.open-message {
  margin: 14px 0 0;
  padding: 12px 14px;
  border: 1px solid #fed7aa;
  border-radius: 12px;
  color: #c2410c;
  background: #fff7ed;
  font-weight: 800;
}

.completeness-lists {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 16px;
}

.completeness-lists > div {
  padding: 14px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #f8fafc;
}

.completeness-lists span {
  display: block;
  margin-bottom: 10px;
  color: #475569;
  font-size: 13px;
  font-weight: 800;
}

.completeness-lists ul {
  display: grid;
  gap: 8px;
  margin: 0;
  padding-left: 18px;
  color: #1f2937;
}

.completeness-lists p {
  margin: 0;
  color: #9ca3af;
}

.completion-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid #e5e7eb;
}

.completion-actions span {
  color: #64748b;
  font-size: 13px;
  font-weight: 800;
}

.next-actions-card {
  margin-top: 20px;
  border-top: 4px solid #14b8a6;
}

.next-actions-card p {
  color: #64748b;
}

.wide-form-card {
  width: min(980px, calc(100vw - 32px));
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

.medication-warning-panel {
  grid-column: 1 / -1;
  padding: 12px 14px;
  border: 1px solid #fde68a;
  border-radius: 12px;
  background: #fffbeb;
}

.medication-warning-title {
  margin-bottom: 8px;
  color: #92400e;
  font-size: 14px;
  font-weight: 700;
}

.medication-warning-list {
  display: grid;
  gap: 6px;
  margin: 0;
  padding-left: 18px;
  color: #92400e;
  line-height: 1.6;
}

.medication-warning-empty {
  margin: 0;
  color: #9ca3af;
  font-size: 14px;
}

.clinical-sidebar .risk-content {
  grid-template-columns: 1fr;
}

.workflow-nodes {
  display: grid;
  gap: 10px;
  margin-top: 16px;
}

.workflow-nodes div {
  display: grid;
  grid-template-columns: 12px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  min-height: 34px;
  padding: 8px 10px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #f8fafc;
}

.workflow-nodes span {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: #f97316;
}

.workflow-nodes .done span {
  background: #14b8a6;
}

.workflow-nodes strong {
  color: #334155;
  font-size: 13px;
}

.row-remove {
  height: 40px;
}

.success-text {
  color: #0f766e;
  font-size: 14px;
}

@media (max-width: 1180px) {
  .encounter-hero,
  .document-head {
    flex-direction: column;
  }

  .hero-meta {
    justify-items: start;
  }

  .encounter-shell {
    grid-template-columns: 1fr;
  }

  .clinical-sidebar {
    position: static;
  }
}

@media (max-width: 960px) {
  .visit-grid,
  .info-list,
  .draft-box,
  .risk-content,
  .completeness-lists {
    grid-template-columns: 1fr;
  }

  .completeness-header {
    flex-direction: column;
  }

  .prescription-row {
    grid-template-columns: 1fr;
  }
}
</style>
