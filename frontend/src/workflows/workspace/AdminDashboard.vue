<template>
  <div>
    <section class="info-card dashboard-hero">
      <div>
        <span class="hero-kicker">ADMIN 控制台</span>
        <h1 class="page-title">欢迎回来，{{ auth.user?.realName || '院内维护员' }}</h1>
        <p class="page-subtitle">今日诊疗运行概览与基础资料维护。</p>
      </div>
      <div class="hero-chip">LocalCare AI</div>
    </section>

    <section class="feature-strip">
      <div>
        <span>AI</span>
        <strong>AI 辅助问诊</strong>
        <p>辅助患者整理症状与风险等级。</p>
      </div>
      <div>
        <span>闭</span>
        <strong>接诊闭环协同</strong>
        <p>跟踪预约、病历、检查和处方流程。</p>
      </div>
      <div>
        <span>健</span>
        <strong>患者健康追踪</strong>
        <p>展示就诊进度与复诊提醒。</p>
      </div>
    </section>

    <div class="dashboard-grid">
      <StatCard title="患者总数" icon="患" :value="stats.patientCount" desc="系统内患者账号" color="green" />
      <StatCard title="医生总数" icon="医" :value="stats.doctorCount" desc="系统内医生账号" />
      <StatCard title="科室数量" icon="科" :value="stats.departmentCount" desc="基础科室数据" color="purple" />
      <StatCard title="药品数量" icon="药" :value="stats.medicineCount" desc="基础药品数据" color="orange" />
      <StatCard title="预约数量" icon="约" :value="stats.appointmentCount" desc="患者预约记录" />
      <StatCard title="病历数量" icon="历" :value="stats.medicalRecordCount" desc="电子病历数据" color="green" />
      <StatCard title="处方数量" icon="方" :value="stats.prescriptionCount" desc="医生开具处方" color="orange" />
      <StatCard title="检查结果" icon="检" :value="stats.examinationCount" desc="医生录入检查结果" color="purple" />
    </div>

    <section class="info-card">
      <h3>快捷入口</h3>
      <div class="quick-actions">
        <router-link class="quick-link" to="/patients?view=patients">患者信息维护</router-link>
        <router-link class="quick-link" to="/patients?view=doctors">医生信息维护</router-link>
        <router-link class="quick-link" to="/workspace?workflow=medicines">药品维护</router-link>
        <router-link class="quick-link" to="/workspace?workflow=appointments">诊疗记录</router-link>
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
  patientCount: 0,
  doctorCount: 0,
  departmentCount: 0,
  medicineCount: 0,
  appointmentCount: 0,
  medicalRecordCount: 0,
  prescriptionCount: 0,
  examinationCount: 0
})

const errorMessage = ref('')

onMounted(async () => {
  try {
    Object.assign(stats, await api.adminDashboard())
  } catch (error) {
    errorMessage.value = error.message || '数据同步失败'
  }
})
</script>
