<template>
  <div class="ai-consultation-workspace">
    <WorkflowHeader kicker="AI 预问诊" title="AI 辅助问诊" description="整理症状、建议就诊科室并提供风险提示；最终诊疗结论由医生确认。">
      <template #metrics><div><strong>{{ history.length }}</strong><span>问诊记录</span></div></template>
    </WorkflowHeader>

    <div class="ai-workspace">
      <section class="consult-card">
        <div class="card-heading">
          <h2>症状输入</h2>
          <p>请尽量描述症状、持续时间和明显诱因，便于系统整理问诊摘要。</p>
        </div>

        <label class="form-field full">
          <span>症状描述</span>
          <textarea
            v-model.trim="symptoms"
            rows="8"
            placeholder="例如：发热、咳嗽、咽痛两天，夜间咳嗽明显，体温最高 38.5℃"
          />
        </label>

        <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>

        <div class="action-row">
          <button class="primary-button" :disabled="loading" @click="analyze">
            {{ loading ? '分析中...' : '开始分析' }}
          </button>
          <button class="secondary-button" :disabled="loading" @click="clearForm">清空</button>
        </div>
      </section>

      <section class="result-card">
        <div class="result-header">
          <div class="title-with-tag">
            <h2>AI 分析结果</h2>
            <span v-if="result" class="source-tag">{{ sourceText(result.source) }}</span>
          </div>
          <StatusTag v-if="result" :value="result.riskLevel || 'LOW'" />
        </div>

        <template v-if="result">
          <div v-if="result.riskLevel === 'HIGH'" class="high-risk-alert">
            存在较高风险，建议及时线下就医或拨打急救电话。
          </div>

          <div class="result-grid">
            <div>
              <span>症状摘要</span>
              <strong>{{ result.symptomSummary || '无' }}</strong>
            </div>
            <div>
              <span>建议科室</span>
              <strong>{{ result.suggestedDepartment || '内科' }}</strong>
            </div>
            <div>
              <span>风险等级</span>
              <strong>{{ result.riskLevel || 'LOW' }}</strong>
            </div>
            <div>
              <span>风险提示</span>
              <strong>{{ result.riskNotice || '无' }}</strong>
            </div>
            <div>
              <span>就诊前建议</span>
              <strong>{{ result.preVisitAdvice || '无' }}</strong>
            </div>
            <div class="full-row">
              <span>AI 辅助声明</span>
              <strong>{{ result.disclaimer || 'AI 结果仅供辅助参考，不能替代医生诊断。' }}</strong>
            </div>
          </div>

          <button class="primary-button" @click="goAppointment">去预约挂号</button>
        </template>

        <EmptyState
          v-else
          title="等待症状输入"
          description="填写症状并点击开始分析后，系统会在这里展示分诊建议。"
        />
      </section>
    </div>

    <section class="risk-card" :class="riskClass(riskAssessment)">
      <div class="risk-card-header">
        <div>
          <span class="hero-kicker">风险评估</span>
          <h2>智能风险提示</h2>
        </div>
        <span v-if="riskAssessment" class="risk-level-tag">{{ riskAssessment.levelText }}</span>
      </div>

      <template v-if="riskAssessment">
        <div class="risk-content">
          <div>
            <span>建议检查项目</span>
            <div class="check-list">
              <strong v-for="item in riskAssessment.suggestedChecks" :key="item">{{ item }}</strong>
            </div>
          </div>
          <div>
            <span>就医建议</span>
            <strong>{{ riskAssessment.advice }}</strong>
          </div>
        </div>
        <p>该结果仅作为辅助参考，最终诊断以医生判断为准。</p>
      </template>

      <p v-else>暂无风险评估结果</p>
    </section>

    <section class="history-section">
      <div class="section-title">
        <h2>我的 AI 问诊历史</h2>
        <button class="secondary-button" @click="loadHistory">刷新</button>
      </div>

      <div v-if="history.length" class="history-list">
        <button
          v-for="item in history"
          :key="item.id"
          class="history-item"
          @click="selectHistory(item)"
        >
          <div>
            <strong>{{ item.symptoms }}</strong>
            <span>{{ formatDateTime(item.createdAt) }}</span>
          </div>
          <StatusTag :value="item.riskLevel || 'LOW'" />
        </button>
      </div>

      <EmptyState
        v-else-if="!historyLoading"
        title="暂无问诊记录"
        description="输入症状并完成分析后，历史记录会显示在这里。"
      />
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../../services/api'
import EmptyState from '../../components/ui/EmptyState.vue'
import WorkflowHeader from '../../components/medical/WorkflowHeader.vue'
import StatusTag from '../../components/medical/StatusTag.vue'

const router = useRouter()
const symptoms = ref('')
const result = ref(null)
const riskAssessment = ref(null)
const history = ref([])
const loading = ref(false)
const historyLoading = ref(false)
const errorMessage = ref('')

onMounted(loadHistory)

async function analyze() {
  errorMessage.value = ''
  if (!symptoms.value) {
    errorMessage.value = '请输入症状描述'
    return
  }

  loading.value = true
  try {
    result.value = await api.aiConsultationApi.analyze(symptoms.value)
    riskAssessment.value = result.value?.risk || await loadRisk(symptoms.value)
    await loadHistory()
  } catch (error) {
    errorMessage.value = error.message || '数据同步失败'
  } finally {
    loading.value = false
  }
}

async function loadHistory() {
  historyLoading.value = true
  try {
    history.value = await api.aiConsultationApi.my()
  } catch (error) {
    errorMessage.value = error.message || '数据同步失败'
  } finally {
    historyLoading.value = false
  }
}

function selectHistory(item) {
  result.value = item
  symptoms.value = item.symptoms || ''
  riskAssessment.value = item.risk || null
  if (!riskAssessment.value && symptoms.value) {
    loadRisk(symptoms.value).then((risk) => {
      riskAssessment.value = risk
    })
  }
}

function clearForm() {
  symptoms.value = ''
  result.value = null
  riskAssessment.value = null
  errorMessage.value = ''
}

function goAppointment() {
  if (!result.value) {
    return
  }

  router.push({
    path: '/workspace',
    query: {
      workflow: 'create-appointment',
      symptoms: result.value.symptoms || symptoms.value,
      department: result.value.suggestedDepartment || ''
    }
  })
}

function riskType(level) {
  if (level === 'HIGH') {
    return 'danger'
  }
  if (level === 'MEDIUM') {
    return 'warning'
  }
  return 'normal'
}

async function loadRisk(value) {
  try {
    return await api.aiRisk(value)
  } catch (error) {
    return null
  }
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

function sourceText(source) {
  return source === 'DEEPSEEK' ? 'DeepSeek AI 生成' : '本地规则生成'
}

function formatDateTime(value) {
  if (!value) {
    return ''
  }
  return String(value).replace('T', ' ').slice(0, 16)
}
</script>

<style scoped>
.ai-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-top: 18px;
  overflow: hidden;
  border: 1px solid rgba(37, 99, 235, 0.12);
  background:
    linear-gradient(135deg, rgba(37, 99, 235, 0.1), rgba(20, 184, 166, 0.12)),
    #ffffff;
}

.ai-hero h2 {
  margin: 6px 0 8px;
  color: #1f2937;
  font-size: 24px;
}

.ai-hero p {
  margin: 0;
  color: #6b7280;
}

.ai-hero strong {
  flex: 0 0 auto;
  padding: 11px 14px;
  border-radius: 999px;
  color: #0f766e;
  background: #ccfbf1;
  font-size: 13px;
}

.hero-kicker {
  color: #2563eb;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.ai-workspace {
  display: grid;
  grid-template-columns: minmax(0, 0.92fr) minmax(0, 1.08fr);
  gap: 20px;
  margin-top: 20px;
}

.consult-card,
.result-card,
.risk-card,
.history-section {
  padding: 22px;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  background: #ffffff;
  box-shadow: 0 14px 36px rgba(31, 41, 55, 0.08);
}

.consult-card {
  display: grid;
  gap: 16px;
}

.history-section {
  margin-top: 20px;
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

.risk-card-header h2 {
  margin: 6px 0 0;
  font-size: 20px;
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
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 16px;
}

.risk-content > div {
  display: flex;
  min-width: 260px;
  flex: 1 1 320px;
  flex-direction: column;
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

.risk-card p {
  margin: 14px 0 0;
  color: #6b7280;
  line-height: 1.7;
}

.card-heading h2 {
  margin: 0 0 8px;
  font-size: 20px;
}

.card-heading p {
  margin: 0;
  color: #6b7280;
  line-height: 1.7;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.result-header,
.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.result-header h2,
.section-title h2 {
  margin: 0;
  font-size: 20px;
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

.high-risk-alert {
  margin-top: 16px;
  padding: 13px 14px;
  border: 1px solid #fecaca;
  border-radius: 12px;
  color: #991b1b;
  background: #fef2f2;
  font-weight: 800;
}

.result-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin: 18px 0;
}

.result-grid div {
  display: flex;
  min-width: 230px;
  flex: 1 1 230px;
  flex-direction: column;
  gap: 8px;
  padding: 14px;
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

.full-row {
  flex-basis: 100% !important;
}

.history-list {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin: 18px 0 0 14px;
  padding-left: 28px;
}

.history-list::before {
  content: '';
  position: absolute;
  top: 10px;
  bottom: 10px;
  left: 5px;
  width: 2px;
  background: linear-gradient(#2563eb, #a5f3fc);
}

.history-item {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
  padding: 14px;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  background: #ffffff;
  text-align: left;
  box-shadow: 0 8px 20px rgba(31, 41, 55, 0.04);
}

.history-item::before {
  content: '';
  position: absolute;
  top: 20px;
  left: -35px;
  width: 12px;
  height: 12px;
  border: 4px solid #fff;
  border-radius: 999px;
  background: #2563eb;
  box-shadow: 0 0 0 2px #bfdbfe;
}

.history-item:hover {
  border-color: rgba(37, 99, 235, 0.25);
  background: #f8fafc;
}

.history-item div {
  display: grid;
  gap: 6px;
}

.history-item span {
  color: #94a3b8;
  font-size: 13px;
}

@media (max-width: 900px) {
  .ai-hero,
  .result-header,
  .section-title,
  .history-item {
    align-items: flex-start;
    flex-direction: column;
  }

  .ai-hero strong {
    flex: auto;
  }

  .ai-workspace,
  .risk-content,
  .result-grid {
    grid-template-columns: 1fr;
  }
}
</style>
