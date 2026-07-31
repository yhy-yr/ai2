<template>
  <div class="directory-workspace">
    <WorkflowHeader kicker="患者档案目录" title="患者信息维护" description="维护患者账号、基础资料、过敏史和既往史。">
      <template #metrics>
        <div><strong>{{ patients.length }}</strong><span>患者档案</span></div>
        <div><strong>{{ patients.filter((item) => item.status === 'ENABLED').length }}</strong><span>正常启用</span></div>
      </template>
    </WorkflowHeader>

    <div class="toolbar">
      <input
        v-model.trim="keyword"
        class="search-input"
        placeholder="搜索患者账号、姓名、手机号"
        @keyup.enter="loadData"
      />
      <button class="primary-button" @click="openCreate">登记患者</button>
    </div>

    <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>

    <div v-if="patients.length" class="profile-grid">
      <article v-for="item in patients" :key="item.id" class="profile-card">
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
          <div><span>性别</span><strong>{{ item.gender || '未填' }}</strong></div>
          <div><span>年龄</span><strong>{{ item.age ?? 0 }} 岁</strong></div>
          <div class="full"><span>地址</span><strong>{{ item.address || '无' }}</strong></div>
        </div>
        <div class="profile-actions">
          <button class="secondary-button" @click="openEdit(item)">修改信息</button>
          <button class="danger-button" @click="removeItem(item)">删除</button>
        </div>
      </article>
    </div>

    <EmptyState
      v-else-if="!loading"
      title="暂无患者"
      description="可以点击登记患者创建患者账号。"
    />

    <div v-if="showForm" class="form-overlay">
      <section class="form-card">
        <div class="form-header">
          <h2>{{ editingId ? '修改患者信息' : '登记患者' }}</h2>
          <button class="secondary-button" @click="closeForm">返回</button>
        </div>

        <div class="form-grid">
          <label class="form-field">
            <span>登录账号</span>
            <input v-model.trim="form.username" placeholder="请输入登录账号" />
          </label>
          <label class="form-field">
            <span>姓名</span>
            <input v-model.trim="form.realName" placeholder="请输入患者姓名" />
          </label>
          <label class="form-field">
            <span>手机号</span>
            <input v-model.trim="form.phone" placeholder="请输入手机号" />
          </label>
          <label class="form-field">
            <span>性别</span>
            <select v-model="form.gender">
              <option value="男">男</option>
              <option value="女">女</option>
              <option value="其他">其他</option>
            </select>
          </label>
          <label class="form-field">
            <span>年龄</span>
            <input v-model.number="form.age" min="0" max="130" type="number" />
          </label>
          <label class="form-field">
            <span>状态</span>
            <select v-model="form.status">
              <option value="ENABLED">启用</option>
              <option value="DISABLED">停用</option>
            </select>
          </label>
          <label class="form-field full">
            <span>地址</span>
            <input v-model.trim="form.address" placeholder="请输入地址" />
          </label>
          <label class="form-field full">
            <span>过敏史</span>
            <textarea v-model.trim="form.allergyHistory" rows="3" placeholder="如：无、青霉素过敏" />
          </label>
          <label class="form-field full">
            <span>既往史</span>
            <textarea v-model.trim="form.medicalHistory" rows="3" placeholder="如：无、高血压" />
          </label>
        </div>

        <p class="muted-text">登记患者账号默认密码为 123456。</p>
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

const patients = ref([])
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
  gender: '男',
  age: 0,
  address: '',
  allergyHistory: '',
  medicalHistory: '',
  status: 'ENABLED'
})

onMounted(loadData)

async function loadData() {
  loading.value = true
  errorMessage.value = ''
  try {
    patients.value = await api.patientApi.list(keyword.value)
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
    gender: '男',
    age: 0,
    address: '',
    allergyHistory: '',
    medicalHistory: '',
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
    gender: item.gender || '男',
    age: Number(item.age || 0),
    address: item.address || '',
    allergyHistory: item.allergyHistory || '',
    medicalHistory: item.medicalHistory || '',
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
  if (Number(form.age) < 0 || Number(form.age) > 130) {
    formError.value = '年龄范围不正确'
    return
  }

  const payload = {
    ...form,
    age: Number(form.age || 0)
  }

  try {
    if (editingId.value) {
      await api.patientApi.update(editingId.value, payload)
    } else {
      await api.patientApi.create(payload)
    }
    showForm.value = false
    await loadData()
  } catch (error) {
    formError.value = error.message || '数据同步失败'
  }
}

async function removeItem(item) {
  if (!window.confirm(`确认删除患者“${item.realName}”吗？对应登录账号也会被删除。`)) {
    return
  }

  try {
    await api.patientApi.remove(item.id)
    await loadData()
  } catch (error) {
    errorMessage.value = error.message || '数据同步失败'
  }
}

function initial(name) {
  return String(name || '患').slice(0, 1)
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
