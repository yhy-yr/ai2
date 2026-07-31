<template>
  <div class="appointment-create-workspace">
    <WorkflowHeader kicker="预约就诊" title="预约挂号" description="选择科室、医生和就诊时间，提交后可在个人预约中查看进度。" />

    <section class="form-card appointment-card">
      <div class="form-grid">
        <label class="form-field">
          <span>选择科室</span>
          <select v-model.number="form.departmentId">
            <option disabled value="">请选择科室</option>
            <option v-for="item in departments" :key="item.id" :value="item.id">
              {{ item.name }}
            </option>
          </select>
        </label>

        <label class="form-field">
          <span>选择医生</span>
          <select v-model.number="form.doctorId">
            <option disabled value="">请选择医生</option>
            <option v-for="item in doctors" :key="item.id" :value="item.id">
              {{ item.realName }} - {{ item.title || '医生' }}
            </option>
          </select>
        </label>

        <label class="form-field">
          <span>预约日期</span>
          <input v-model="form.appointmentDate" :min="minDate" type="date" />
        </label>

        <label class="form-field">
          <span>时间段</span>
          <select v-model="form.timeSlot">
            <option v-for="slot in timeSlots" :key="slot" :value="slot">{{ slot }}</option>
          </select>
        </label>

        <label class="form-field full">
          <span>症状描述</span>
          <textarea
            v-model.trim="form.symptomDescription"
            rows="5"
            placeholder="请填写本次就诊的主要症状"
          />
        </label>
      </div>

      <p v-if="suggestedDepartment" class="muted-text">
        已根据 AI 问诊建议科室进行预填：{{ suggestedDepartment }}
      </p>
      <p v-if="successMessage" class="success-text">{{ successMessage }}</p>
      <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>

      <div class="form-actions">
        <router-link class="secondary-button" to="/workspace?workflow=patient-appointments">查看我的预约</router-link>
        <button class="primary-button" :disabled="loading" @click="submit">
          {{ loading ? '正在确认提交' : '确认提交预约' }}
        </button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../../services/api'
import WorkflowHeader from '../../components/medical/WorkflowHeader.vue'

const route = useRoute()
const router = useRouter()

const departments = ref([])
const doctors = ref([])
const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const suggestedDepartment = ref(String(route.query.department || ''))

const timeSlots = ['09:00-10:00', '10:00-11:00', '14:00-15:00', '15:00-16:00']
const minDate = formatDate(new Date())

const form = reactive({
  departmentId: '',
  doctorId: '',
  appointmentDate: formatDate(addDays(new Date(), 1)),
  timeSlot: timeSlots[0],
  symptomDescription: String(route.query.symptoms || '')
})

onMounted(async () => {
  await loadDepartments()
  matchSuggestedDepartment()
  if (!form.departmentId && departments.value.length) {
    form.departmentId = departments.value[0].id
  }
})

watch(
  () => form.departmentId,
  async (departmentId) => {
    if (departmentId) {
      await loadDoctors(departmentId)
    } else {
      doctors.value = []
      form.doctorId = ''
    }
  },
  { immediate: false }
)

async function loadDepartments() {
  errorMessage.value = ''
  try {
    departments.value = await api.departmentApi.list()
  } catch (error) {
    errorMessage.value = error.message || '数据同步失败'
  }
}

async function loadDoctors(departmentId) {
  try {
    doctors.value = await api.doctorApi.byDepartment(departmentId)
    form.doctorId = doctors.value[0]?.id || ''
  } catch (error) {
    doctors.value = []
    form.doctorId = ''
    errorMessage.value = error.message || '数据同步失败'
  }
}

function matchSuggestedDepartment() {
  if (!suggestedDepartment.value) {
    return
  }

  const target = departments.value.find((item) => {
    return (
      suggestedDepartment.value.includes(item.name) ||
      item.name.includes(suggestedDepartment.value)
    )
  })

  if (target) {
    form.departmentId = target.id
  }
}

async function submit() {
  errorMessage.value = ''
  successMessage.value = ''

  if (!form.departmentId || !form.doctorId) {
    errorMessage.value = '请选择科室和医生'
    return
  }
  if (!form.appointmentDate || form.appointmentDate < minDate) {
    errorMessage.value = '不能预约过去日期'
    return
  }
  if (!form.symptomDescription) {
    errorMessage.value = '请填写症状描述'
    return
  }

  loading.value = true
  try {
    await api.appointmentApi.create({
      departmentId: Number(form.departmentId),
      doctorId: Number(form.doctorId),
      appointmentDate: form.appointmentDate,
      timeSlot: form.timeSlot,
      symptomDescription: form.symptomDescription
    })
    successMessage.value = '信息已更新，即将返回预约记录。'
    setTimeout(() => {
      router.push('/workspace?workflow=patient-appointments')
    }, 600)
  } catch (error) {
    errorMessage.value = error.message || '数据同步失败'
  } finally {
    loading.value = false
  }
}

function addDays(date, days) {
  const next = new Date(date)
  next.setDate(next.getDate() + days)
  return next
}

function formatDate(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}
</script>

<style scoped>
.appointment-card {
  margin-top: 24px;
  border-color: rgba(125, 211, 252, 0.45);
  border-radius: 20px;
  box-shadow: 0 20px 50px rgba(15, 23, 42, 0.08);
}

.success-text {
  color: #0f766e;
  font-size: 14px;
}
</style>
