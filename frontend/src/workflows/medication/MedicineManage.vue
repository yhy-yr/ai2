<template>
  <div class="directory-workspace">
    <WorkflowHeader kicker="院内药品资源" title="药品维护" description="维护常用药品、库存、规格和用法说明。" tone="green">
      <template #metrics>
        <div><strong>{{ medicines.length }}</strong><span>在库药品</span></div>
        <div><strong>{{ medicines.filter((item) => Number(item.stock) < 20).length }}</strong><span>低库存</span></div>
      </template>
    </WorkflowHeader>

    <div class="toolbar">
      <input
        v-model.trim="keyword"
        class="search-input"
        placeholder="搜索药品名称、类型、规格"
        @keyup.enter="loadData"
      />
      <button class="primary-button" @click="openCreate">登记药品</button>
    </div>

    <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>

    <div v-if="medicines.length" class="medicine-grid">
      <article v-for="item in medicines" :key="item.id" class="medicine-card" :class="{ low: Number(item.stock) < 20 }">
        <div class="medicine-head">
          <span>{{ item.type || '未分类' }}</span>
          <h2>{{ item.name }}</h2>
          <p>{{ item.specification || '无规格' }}</p>
        </div>
        <div class="medicine-meta">
          <div><span>价格</span><strong>￥{{ formatPrice(item.price) }}</strong></div>
          <div><span>库存</span><StatusTag :value="stockValue(item.stock)" /></div>
        </div>
        <p class="usage">{{ item.usageText || '暂无用法说明' }}</p>
        <div class="medicine-actions">
          <button class="secondary-button" @click="openEdit(item)">修改信息</button>
          <button class="danger-button" @click="removeItem(item)">删除</button>
        </div>
      </article>
    </div>

    <EmptyState
      v-else-if="!loading"
      title="暂无药品"
      description="可以点击登记药品创建基础数据。"
    />

    <div v-if="showForm" class="form-overlay">
      <section class="form-card">
        <div class="form-header">
          <h2>{{ editingId ? '修改药品信息' : '登记药品' }}</h2>
          <button class="secondary-button" @click="closeForm">返回</button>
        </div>

        <div class="form-grid">
          <label class="form-field">
            <span>药品名称</span>
            <input v-model.trim="form.name" placeholder="请输入药品名称" />
          </label>
          <label class="form-field">
            <span>类型</span>
            <input v-model.trim="form.type" placeholder="如：退热止痛药" />
          </label>
          <label class="form-field">
            <span>规格</span>
            <input v-model.trim="form.specification" placeholder="如：0.2g*24片" />
          </label>
          <label class="form-field">
            <span>价格</span>
            <input v-model.number="form.price" min="0" step="0.01" type="number" />
          </label>
          <label class="form-field">
            <span>库存</span>
            <input v-model.number="form.stock" min="0" type="number" />
          </label>
          <label class="form-field full">
            <span>用法说明</span>
            <textarea v-model.trim="form.usageText" rows="4" placeholder="请输入用法说明" />
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
import StatusTag from '../../components/medical/StatusTag.vue'
import WorkflowHeader from '../../components/medical/WorkflowHeader.vue'

const medicines = ref([])
const keyword = ref('')
const loading = ref(false)
const errorMessage = ref('')
const formError = ref('')
const showForm = ref(false)
const editingId = ref(null)

const form = reactive({
  name: '',
  type: '',
  specification: '',
  price: 0,
  stock: 0,
  usageText: ''
})

onMounted(loadData)

async function loadData() {
  loading.value = true
  errorMessage.value = ''
  try {
    medicines.value = await api.medicineApi.list(keyword.value)
  } catch (error) {
    errorMessage.value = error.message || '数据同步失败'
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, {
    name: '',
    type: '',
    specification: '',
    price: 0,
    stock: 0,
    usageText: ''
  })
  formError.value = ''
  showForm.value = true
}

function openEdit(item) {
  editingId.value = item.id
  Object.assign(form, {
    name: item.name || '',
    type: item.type || '',
    specification: item.specification || '',
    price: Number(item.price || 0),
    stock: Number(item.stock || 0),
    usageText: item.usageText || ''
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
    formError.value = '请输入药品名称'
    return
  }
  if (Number(form.price) < 0 || Number(form.stock) < 0) {
    formError.value = '价格和库存不能小于 0'
    return
  }

  const payload = {
    ...form,
    price: Number(form.price || 0),
    stock: Number(form.stock || 0)
  }

  try {
    if (editingId.value) {
      await api.medicineApi.update(editingId.value, payload)
    } else {
      await api.medicineApi.create(payload)
    }
    showForm.value = false
    await loadData()
  } catch (error) {
    formError.value = error.message || '数据同步失败'
  }
}

async function removeItem(item) {
  if (!window.confirm(`确认删除药品“${item.name}”吗？`)) {
    return
  }

  try {
    await api.medicineApi.remove(item.id)
    await loadData()
  } catch (error) {
    errorMessage.value = error.message || '数据同步失败'
  }
}

function formatPrice(price) {
  return Number(price || 0).toFixed(2)
}

function stockValue(stock) {
  const value = Number(stock || 0)
  if (value >= 20) {
    return 'ENOUGH'
  }
  if (value >= 5) {
    return 'LOW_STOCK'
  }
  return 'SHORTAGE'
}
</script>

<style scoped>
.medicine-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.medicine-card {
  width: min(100%, 460px);
  flex: 1 1 330px;
  padding: 20px;
  border: 1px solid rgba(205, 218, 234, 0.86);
  border-left: 5px solid #14b8a6;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 18px 42px rgba(22, 45, 74, 0.06);
}

.medicine-card.low {
  border-left-color: #f59e0b;
}

.medicine-head span {
  display: inline-flex;
  padding: 6px 10px;
  border-radius: 999px;
  color: #0e7490;
  background: #e6f8fb;
  font-size: 12px;
  font-weight: 900;
}

.medicine-head h2 {
  margin: 11px 0 0;
  color: #0f172a;
  font-size: 22px;
  font-weight: 900;
}

.medicine-head p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 13px;
  font-weight: 800;
}

.medicine-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 15px;
}

.medicine-meta div {
  display: flex;
  min-width: 130px;
  flex: 1 1 130px;
  flex-direction: column;
  gap: 7px;
  padding: 12px;
  border-radius: 13px;
  background: #f8fcff;
}

.directory-workspace .toolbar { margin-top: 18px; }

.medicine-meta span {
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
}

.medicine-meta strong {
  color: #0f172a;
}

.usage {
  min-height: 50px;
  margin: 14px 0 0;
  color: #475569;
  line-height: 1.75;
}

.medicine-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}
</style>
