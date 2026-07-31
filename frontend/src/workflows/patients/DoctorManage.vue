<template>
  <div class="directory-workspace">
    <WorkflowHeader kicker="医护资源目录" title="医生信息维护" description="维护医生账号、所属科室、职称和擅长方向。">
      <template #metrics>
        <div><strong>{{ doctors.length }}</strong><span>医生档案</span></div>
        <div><strong>{{ doctors.filter((item) => item.status === 'ENABLED').length }}</strong><span>在岗医生</span></div>
      </template>
    </WorkflowHeader>

    <div class="toolbar">
      <input
        v-model.trim="keyword"
        class="search-input"
        placeholder="搜索医生账号、姓名、科室"
        @keyup.enter="loadData"
      />
      <button class="primary-button" @click="openCreate">登记医生</button>
    </div>

    <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>

    <div v-if="doctors.length" class="profile-grid">
      <article v-for="item in doctors" :key="item.id" class="profile-card">
        <div class="profile-head">
          <div class="avatar">{{ initial(item.realName) }}</div>
          <div>
            <h2>{{ item.realName }}</h2>
            <p>{{ item.username }} · {{ item.phone || '无手机号' }}</p>
          </div>
          <StatusTag
            :text="item.status === 'ENABLED' ? '启用' : '停用'"
            :type="item.status === 'ENABLED' ? 'normal' : 'warning'"
          />
        </div>
        <div class="profile-meta">
          <div><span>科室</span><strong>{{ item.departmentName || '未分配' }}</strong></div>
          <div><span>职称</span><strong>{{ item.title || '无' }}</strong></div>
          <div class="full"><span>擅长方向</span><strong>{{ item.specialty || '无' }}</strong></div>
        </div>
        <div class="profile-actions">
          <button class="secondary-button" @click="openEdit(item)">修改信息</button>
          <button class="danger-button" @click="removeItem(item)">删除</button>
        </div>
      </article>
    </div>

    <EmptyState
      v-else-if="!loading"
      title="暂无医生"
      description="可以点击登记医生创建医生账号。"
    />

    <div v-if="showForm" class="form-overlay">
      <section class="form-card">
        <div class="form-header">
          <h2>{{ editingId ? '修改医生信息' : '登记医生' }}</h2>
          <button class="secondary-button" @click="closeForm">返回</button>
        </div>

        <div class="form-grid">
          <label class="form-field">
            <span>登录账号</span>
            <input v-model.trim="form.username" placeholder="请输入登录账号" />
          </label>
          <label class="form-field">
            <span>姓名</span>
            <input v-model.trim="form.realName" placeholder="请输入医生姓名" />
          </label>
          <label class="form-field">
            <span>手机号</span>
            <input v-model.trim="form.phone" placeholder="请输入手机号" />
          </label>
          <label class="form-field">
            <span>科室</span>
            <select v-model.number="form.departmentId">
              <option disabled value="">请选择科室</option>
              <option v-for="item in departments" :key="item.id" :value="item.id">
                {{ item.name }}
              </option>
            </select>
          </label>
          <label class="form-field">
            <span>职称</span>
            <input v-model.trim="form.title" placeholder="如：主治医师" />
          </label>
          <label class="form-field">
            <span>状态</span>
            <select v-model="form.status">
              <option value="ENABLED">启用</option>
              <option value="DISABLED">停用</option>
            </select>
          </label>
          <label class="form-field full">
            <span>擅长方向</span>
            <input v-model.trim="form.specialty" placeholder="请输入擅长方向" />
          </label>
          <label class="form-field full">
            <span>简介</span>
            <textarea v-model.trim="form.introduction" rows="4" placeholder="请输入医生简介" />
          </label>
        </div>

        <p class="muted-text">登记医生账号默认密码为 123456。</p>
        <p v-if="formError" class="error-text">{{ formError }}</p>

        <div class="form-actions">
          <button class="secondary-button" @click="closeForm">返回</button>
          <button class="primary-button" @click="submitForm">保存记录</button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import api from '../../services/api'
import EmptyState from '../../components/ui/EmptyState.vue'
import StatusTag from '../../components/medical/StatusTag.vue'
import WorkflowHeader from '../../components/medical/WorkflowHeader.vue'

const doctors = ref([])
const departments = ref([])
const keyword = ref('')
const loading = ref(false)
const errorMessage = ref('')
const formError = ref('')
const showForm = ref(false)
const editingId = ref(null)

const form = reactive({
  username: '',
  realName: '',
  phone: '',
  departmentId: '',
  title: '',
  specialty: '',
  introduction: '',
  status: 'ENABLED'
})

onMounted(async () => {
  await Promise.all([loadDepartments(), loadData()])
})

async function loadDepartments() {
  try {
    departments.value = await api.departmentApi.list()
  } catch (error) {
    errorMessage.value = error.message || '数据同步失败'
  }
}

async function loadData() {
  loading.value = true
  errorMessage.value = ''
  try {
    doctors.value = await api.doctorApi.list(keyword.value)
  } catch (error) {
    errorMessage.value = error.message || '数据同步失败'
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, {
    username: '',
    realName: '',
    phone: '',
    departmentId: departments.value[0]?.id || '',
    title: '',
    specialty: '',
    introduction: '',
    status: 'ENABLED'
  })
}

function openCreate() {
  editingId.value = null
  resetForm()
  formError.value = ''
  showForm.value = true
}

function openEdit(item) {
  editingId.value = item.id
  Object.assign(form, {
    username: item.username || '',
    realName: item.realName || '',
    phone: item.phone || '',
    departmentId: Number(item.departmentId || ''),
    title: item.title || '',
    specialty: item.specialty || '',
    introduction: item.introduction || '',
    status: item.status || 'ENABLED'
  })
  formError.value = ''
  showForm.value = true
}

function closeForm() {
  showForm.value = false
}

async function submitForm() {
  formError.value = ''
  if (!form.username || !form.realName) {
    formError.value = '请输入登录账号和姓名'
    return
  }
  if (!form.departmentId) {
    formError.value = '请选择科室'
    return
  }

  const payload = {
    ...form,
    departmentId: Number(form.departmentId)
  }

  try {
    if (editingId.value) {
      await api.doctorApi.update(editingId.value, payload)
    } else {
      await api.doctorApi.create(payload)
    }
    showForm.value = false
    await loadData()
  } catch (error) {
    formError.value = error.message || '数据同步失败'
  }
}

async function removeItem(item) {
  if (!window.confirm(`确认删除医生“${item.realName}”吗？对应登录账号也会被删除。`)) {
    return
  }

  try {
    await api.doctorApi.remove(item.id)
    await loadData()
  } catch (error) {
    errorMessage.value = error.message || '数据同步失败'
  }
}

function initial(name) {
  return String(name || '医').slice(0, 1)
}
</script>

<style scoped>
.profile-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.profile-card {
  width: min(100%, 520px);
  flex: 1 1 380px;
  padding: 20px;
  border: 1px solid rgba(205, 218, 234, 0.86);
  border-left: 5px solid #0891b2;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 18px 42px rgba(22, 45, 74, 0.06);
}

.profile-head {
  display: flex;
  gap: 13px;
  align-items: center;
}

.avatar {
  display: grid;
  width: 46px;
  height: 46px;
  place-items: center;
  border-radius: 999px;
  color: #ffffff;
  background: linear-gradient(135deg, #0891b2, #14b8a6);
  font-weight: 900;
}

.profile-head h2 {
  margin: 0;
  color: #0f172a;
  font-size: 22px;
  font-weight: 900;
}

.profile-head p {
  margin: 5px 0 0;
  color: #64748b;
  font-size: 13px;
  font-weight: 800;
}

.profile-head > :last-child {
  margin-left: auto;
}

.profile-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 15px;
}

.profile-meta div {
  display: flex;
  min-width: 150px;
  flex: 1 1 150px;
  flex-direction: column;
  gap: 7px;
  padding: 12px;
  border-radius: 13px;
  background: #f8fcff;
}

.profile-meta .full {
  flex-basis: 100%;
}

.directory-workspace .toolbar { margin-top: 18px; }

.profile-meta span {
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
}

.profile-meta strong {
  color: #0f172a;
  line-height: 1.6;
}

.profile-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}
</style>
