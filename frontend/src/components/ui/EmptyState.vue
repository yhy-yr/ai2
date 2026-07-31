<template>
  <div class="empty-state">
    <div class="empty-title">{{ title }}</div>
    <p>{{ description }}</p>
    <button v-if="actionText" class="empty-action" @click="$emit('action')">{{ actionText }}</button>
  </div>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    default: '暂无数据'
  },
  description: {
    type: String,
    default: '当前阶段仅实现基础骨架和首页统计。'
  },
  actionText: {
    type: String,
    default: ''
  }
})

defineEmits(['action'])
</script>

<style scoped>
.empty-state {
  padding: 34px;
  border: 1px dashed #bfdbfe;
  border-radius: 16px;
  color: #6b7280;
  background: #ffffff;
  text-align: center;
  box-shadow: 0 14px 36px rgba(31, 41, 55, 0.06);
}

.empty-title {
  margin-bottom: 8px;
  color: #1f2937;
  font-weight: 800;
}

.empty-state p {
  margin: 0;
}

.empty-action {
  margin-top: 16px;
  padding: 10px 14px;
  border: 1px solid #2563eb;
  border-radius: 12px;
  color: #ffffff;
  background: #2563eb;
  font-weight: 800;
}
</style>
