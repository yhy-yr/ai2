<template>
  <section class="stat-card" :class="colorClass">
    <div class="stat-icon">{{ displayIcon }}</div>
    <div>
      <div class="label">{{ displayTitle }}</div>
      <div class="value">{{ safeValue }}</div>
      <div class="hint">{{ displayDesc }}</div>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  title: {
    type: String,
    default: ''
  },
  label: {
    type: String,
    default: ''
  },
  value: {
    type: [Number, String],
    default: 0
  },
  desc: {
    type: String,
    default: ''
  },
  hint: {
    type: String,
    default: '当前统计'
  },
  icon: {
    type: String,
    default: ''
  },
  color: {
    type: String,
    default: 'blue'
  }
})

const safeValue = computed(() => {
  const value = props.value ?? 0
  return Number.isNaN(value) ? 0 : value
})
const displayTitle = computed(() => props.title || props.label || '统计')
const displayDesc = computed(() => props.desc || props.hint || '当前统计')
const displayIcon = computed(() => props.icon || displayTitle.value.slice(0, 1))
const colorClass = computed(() => `stat-${props.color}`)
</script>

<style scoped>
.stat-card {
  display: flex;
  gap: 16px;
  align-items: center;
  padding: 22px;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  background: #ffffff;
  box-shadow: 0 14px 36px rgba(31, 41, 55, 0.08);
}

.stat-icon {
  display: grid;
  width: 46px;
  height: 46px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 14px;
  color: #2563eb;
  background: #dbeafe;
  font-weight: 900;
}

.label {
  color: #6b7280;
  font-size: 14px;
}

.value {
  margin-top: 9px;
  color: #1f2937;
  font-size: 34px;
  font-weight: 800;
  line-height: 1;
}

.hint {
  margin-top: 9px;
  color: #9ca3af;
  font-size: 13px;
}

.stat-green .stat-icon {
  color: #0f766e;
  background: #ccfbf1;
}

.stat-orange .stat-icon {
  color: #b45309;
  background: #fef3c7;
}

.stat-red .stat-icon {
  color: #dc2626;
  background: #fee2e2;
}

.stat-purple .stat-icon {
  color: #7e22ce;
  background: #f3e8ff;
}
</style>
