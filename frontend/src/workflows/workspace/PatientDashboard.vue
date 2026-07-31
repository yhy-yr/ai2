<template>
  <div>
    <section class="info-card dashboard-hero">
      <div>
        <span class="hero-kicker">PATIENT 中心</span>
        <h1 class="page-title">欢迎回来，{{ auth.user?.realName || '患者' }}</h1>
        <p class="page-subtitle">我的预约、就诊进度与健康提醒。</p>
      </div>
      <div class="hero-chip">健康追踪</div>
    </section>

    <section class="feature-strip">
      <div>
        <span>AI</span>
        <strong>AI 辅助问诊</strong>
        <p>输入症状后获取辅助问诊和风险提示。</p>
      </div>
      <div>
        <span>闭</span>
        <strong>接诊闭环协同</strong>
        <p>预约、病历、检查、处方进度清晰呈现。</p>
      </div>
      <div>
        <span>健</span>
        <strong>患者健康追踪</strong>
        <p>查看诊疗结果和温和复诊提醒。</p>
      </div>
    </section>

    <div class="dashboard-grid">
      <StatCard title="我的预约" icon="约" :value="stats.appointmentCount" desc="个人预约记录" />
      <StatCard title="AI 问诊" icon="AI" :value="stats.aiConsultationCount" desc="辅助问诊次数" color="purple" />
      <StatCard title="我的病历" icon="历" :value="stats.medicalRecordCount" desc="电子病历数量" color="green" />
      <StatCard title="我的处方" icon="方" :value="stats.prescriptionCount" desc="处方记录数量" color="orange" />
      <StatCard title="检查结果" icon="检" :value="stats.examinationCount" desc="检查结果数量" />
    </div>

    <section class="info-card">
      <h3>快捷入口</h3>
      <p>
        本阶段已开放 AI 辅助问诊、预约挂号、病历、处方和检查结果查看。AI 结果仅供辅助参考，不能替代医生诊断。
      </p>
      <div class="quick-actions">
        <router-link class="quick-link" to="/ai">AI 辅助问诊</router-link>
        <router-link class="quick-link" to="/workspace?workflow=create-appointment">预约挂号</router-link>
        <router-link class="quick-link" to="/workspace?workflow=patient-appointments">我的预约</router-link>
        <router-link class="quick-link" to="/patients?view=records">我的病历</router-link>
        <router-link class="quick-link" to="/patients?view=prescriptions">我的处方</router-link>
        <router-link class="quick-link" to="/patients?view=examinations">检查结果</router-link>
      </div>
    </section>

    <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import api from '../../services/api'
import StatCard from '../../components/ui/StatCard.vue'
import { useAuthStore } from '../../app/store'

const auth = useAuthStore()

const stats = reactive({
  appointmentCount: 0,
  aiConsultationCount: 0,
  medicalRecordCount: 0,
  prescriptionCount: 0,
  examinationCount: 0
})

const errorMessage = ref('')

onMounted(async () => {
  try {
    Object.assign(stats, await api.patientDashboard())
  } catch (error) {
    errorMessage.value = error.message || '数据同步失败'
  }
})
</script>
