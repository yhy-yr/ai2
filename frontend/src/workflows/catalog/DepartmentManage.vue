<template>
  <div class="directory-workspace">
    <WorkflowHeader kicker="院内科室资源" title="科室维护" description="维护可预约、可接诊的基层诊疗科室。">
      <template #metrics><div><strong>{{ departments.length }}</strong><span>接诊科室</span></div></template>
    </WorkflowHeader>

    <div class="toolbar">
      <input
        v-model.trim="keyword"
        class="search-input"
        placeholder="搜索科室名称或说明"
        @keyup.enter="loadData"
      />
      <button class="primary-button" @click="openCreate">登记科室</button>
    </div>

    <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>

    <div v-if="departments.length" class="resource-grid">
      <article v-for="item in departments" :key="item.id" class="resource-card">
        <div class="resource-head">
          <span>#{{ item.id }}</span>
          <h2>{{ item.name }}</h2>
        </div>
        <p>{{ item.description || '暂无科室说明' }}</p>
        <div class="resource-actions">
          <button class="secondary-button" @click="openEdit(item)">修改信息</button>
          <button class="danger-button" @click="removeItem(item)">删除</button>
        </div>
      </article>
    </div>

    <EmptyState
      v-else-if="!loading"
      title="暂无科室"
      description="可以点击登记科室创建基础数据。"
    />

    <div v-if="showForm" class="form-overlay">
      <section class="form-card">
        <div class="form-header">
          <h2>{{ editingId ? '修改科室信息' : '登记科室' }}</h2>
          <button class="secondary-button" @click="closeForm">返回</button>
        </div>

        <div class="form-grid">
          <label class="form-field">
            <span>科室名称</span>
            <input v-model.trim="form.name" placeholder="请输入科室名称" />
          </label>
          <label class="form-field full">
            <span>科室说明</span>
            <textarea v-model.trim="form.description" rows="4" placeholder="请输入科室说明" />
          </label>
        </div>

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
import WorkflowHeader from '../../components/medical/WorkflowHeader.vue'

const departments = ref([])
const keyword = ref('')
const loading = ref(false)
const errorMessage = ref('')
const formError = ref('')
const showForm = ref(false)
const editingId = ref(null)

const form = reactive({
  name: '',
  description: ''
})

onMounted(loadData)

async function loadData() {
  loading.value = true
  errorMessage.value = ''
  try {
    departments.value = await api.departmentApi.list(keyword.value)
  } catch (error) {
    errorMessage.value = error.message || '数据同步失败'
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, { name: '', description: '' })
  formError.value = ''
  showForm.value = true
}

function openEdit(item) {
  editingId.value = item.id
  Object.assign(form, {
    name: item.name || '',
    description: item.description || ''
  })
  formError.value = ''
  showForm.value = true
}

function closeForm() {
  showForm.value = false
}

async function submitForm() {
  formError.value = ''
  if (!form.name) {
    formError.value = '请输入科室名称'
    return
  }

  try {
    if (editingId.value) {
      await api.departmentApi.update(editingId.value, form)
    } else {
      await api.departmentApi.create(form)
    }
    showForm.value = false
    await loadData()
  } catch (error) {
    formError.value = error.message || '数据同步失败'
  }
}

async function removeItem(item) {
  if (!window.confirm(`确认删除科室“${item.name}”吗？`)) {
    return
  }

  try {
    await api.departmentApi.remove(item.id)
    await loadData()
  } catch (error) {
    errorMessage.value = error.message || '数据同步失败'
  }
}
</script>

<style scoped>
.resource-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.resource-card {
  width: min(100%, 430px);
  flex: 1 1 300px;
  padding: 20px;
  border: 1px solid rgba(205, 218, 234, 0.86);
  border-left: 5px solid #0891b2;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 18px 42px rgba(22, 45, 74, 0.06);
}

.directory-workspace .toolbar { margin-top: 18px; }

.resource-head span {
  color: #0891b2;
  font-size: 12px;
  font-weight: 900;
}

.resource-head h2 {
  margin: 8px 0 0;
  color: #0f172a;
  font-size: 22px;
  font-weight: 900;
}

.resource-card p {
  min-height: 54px;
  margin: 12px 0 0;
  color: #475569;
  line-height: 1.8;
}

.resource-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}
</style>
