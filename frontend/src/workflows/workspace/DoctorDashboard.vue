<template>
  <div>
    <section class="info-card dashboard-hero">
      <div>
        <span class="hero-kicker">DOCTOR 工作台</span>
        <h1 class="page-title">欢迎回来，{{ auth.user?.realName || '医生' }}</h1>
        <p class="page-subtitle">今日接诊任务与诊疗闭环跟踪。</p>
      </div>
      <div class="hero-chip">诊疗闭环</div>
    </section>

    <section class="feature-strip">
      <div>
        <span>AI</span>
        <strong>AI 辅助问诊</strong>
        <p>查看患者症状与智能风险提示。</p>
      </div>
      <div>
        <span>闭</span>
        <strong>接诊闭环协同</strong>
        <p>病历、检查、处方完成度清晰可见。</p>
      </div>
      <div>
        <span>健</span>
        <strong>患者健康追踪</strong>
        <p>帮助患者查看结果和复诊提醒。</p>
      </div>
    </section>

    <div class="dashboard-grid">
      <StatCard title="今日预约" icon="今" :value="stats.todayAppointments" desc="今日预约患者" />
      <StatCard title="待接诊" icon="待" :value="stats.pendingVisits" desc="等待医生接诊" color="orange" />
      <StatCard title="已完成" icon="完" :value="stats.completedVisits" desc="已完成接诊" color="green" />
      <StatCard title="电子病历" icon="历" :value="stats.medicalRecordCount" desc="已保存记录" />
      <StatCard title="处方记录" icon="方" :value="stats.prescriptionCount" desc="已开具处方" color="purple" />
      <StatCard title="检查结果" icon="检" :value="stats.examinationCount" desc="已录入检查" color="green" />
    </div>

    <section class="info-card">
      <h3>快捷入口</h3>
      <p>
        可进入我的预约开始接诊，也可以查看自己保存过的电子病历、处方和检查结果。
      </p>
      <div class="quick-actions">
        <router-link class="quick-link" to="/workspace?workflow=doctor-appointments">我的预约</router-link>
        <router-link class="quick-link" to="/workspace?workflow=doctor-records">电子病历记录</router-link>
        <router-link class="quick-link" to="/workspace?workflow=doctor-prescriptions">处方维护</router-link>
        <router-link class="quick-link" to="/workspace?workflow=doctor-examinations">检查结果</router-link>
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
  todayAppointments: 0,
  pendingVisits: 0,
  completedVisits: 0,
  medicalRecordCount: 0,
  prescriptionCount: 0,
  examinationCount: 0
})

const errorMessage = ref('')

onMounted(async () => {
  try {
    Object.assign(stats, await api.doctorDashboard())
  } catch (error) {
    errorMessage.value = error.message || '数据同步失败'
  }
})
</script>
