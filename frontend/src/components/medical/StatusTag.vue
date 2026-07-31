<template>
  <span class="status-tag" :class="typeClass">{{ displayText }}</span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  value: {
    type: [String, Number],
    default: ''
  },
  text: {
    type: String,
    default: ''
  },
  type: {
    type: String,
    default: ''
  }
})

const normalized = computed(() => String(props.value || props.text || '').toUpperCase())

const displayText = computed(() => {
  const map = {
    PENDING: '待就诊',
    IN_PROGRESS: '接诊中',
    COMPLETED: '已完成',
    CANCELLED: '已撤回',
    LOW: 'LOW 低风险',
    MEDIUM: 'MEDIUM 中风险',
    HIGH: 'HIGH 高风险',
    ISSUED: '已开具',
    ENOUGH: '库存充足',
    LOW_STOCK: '库存偏低',
    SHORTAGE: '库存紧张'
  }
  return props.text || map[normalized.value] || props.value || '正常'
})

const typeClass = computed(() => {
  if (props.type) {
    return `status-${props.type}`
  }
  if (['COMPLETED', 'LOW', 'ISSUED', 'ENOUGH'].includes(normalized.value)) {
    return 'status-normal'
  }
  if (['PENDING', 'IN_PROGRESS', 'MEDIUM', 'LOW_STOCK'].includes(normalized.value)) {
    return 'status-warning'
  }
  if (['CANCELLED', 'HIGH', 'SHORTAGE'].includes(normalized.value)) {
    return 'status-danger'
  }
  return 'status-normal'
})
</script>

<style scoped>
.status-tag {
  display: inline-flex;
  align-items: center;
  padding: 5px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
}

.status-normal {
  color: #15803d;
  background: #ccfbf1;
}

.status-warning {
  color: #b45309;
  background: #fef3c7;
}

.status-danger {
  color: #dc2626;
  background: #fee2e2;
}
</style>
