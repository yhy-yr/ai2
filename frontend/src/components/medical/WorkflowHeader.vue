<template>
  <section class="workflow-header" :class="`tone-${tone}`">
    <div class="workflow-copy">
      <span class="workflow-kicker">{{ kicker }}</span>
      <h1>{{ title }}</h1>
      <p>{{ description }}</p>
    </div>
    <div v-if="$slots.metrics" class="workflow-metrics">
      <slot name="metrics" />
    </div>
  </section>
</template>

<script setup>
defineProps({
  kicker: { type: String, default: '临床工作流' },
  title: { type: String, required: true },
  description: { type: String, required: true },
  tone: { type: String, default: 'cyan' }
})
</script>

<style scoped>
.workflow-header {
  position: relative;
  overflow: hidden;
  display: flex;
  min-height: 168px;
  align-items: center;
  justify-content: space-between;
  gap: 26px;
  padding: 26px 28px;
  border: 1px solid rgba(125, 211, 252, 0.44);
  border-radius: 22px;
  background:
    radial-gradient(circle at 94% 12%, rgba(20, 184, 166, 0.18), transparent 28%),
    linear-gradient(135deg, #ffffff, #f0f9ff 58%, #ecfeff);
  box-shadow: 0 22px 52px rgba(15, 23, 42, 0.07);
}

.workflow-header.tone-rose {
  border-color: rgba(251, 113, 133, 0.28);
  background:
    radial-gradient(circle at 94% 12%, rgba(244, 63, 94, 0.12), transparent 28%),
    linear-gradient(135deg, #ffffff, #fff7ed 58%, #fff1f2);
}

.workflow-header.tone-green {
  border-color: rgba(52, 211, 153, 0.3);
  background:
    radial-gradient(circle at 94% 12%, rgba(16, 185, 129, 0.14), transparent 28%),
    linear-gradient(135deg, #ffffff, #f0fdf4 58%, #ecfdf5);
}

.workflow-copy {
  max-width: 650px;
}

.workflow-kicker {
  display: inline-flex;
  padding: 6px 10px;
  border-radius: 999px;
  color: #0e7490;
  background: rgba(207, 250, 254, 0.86);
  font-size: 12px;
  font-weight: 900;
}

.tone-rose .workflow-kicker {
  color: #be123c;
  background: #ffe4e6;
}

.tone-green .workflow-kicker {
  color: #047857;
  background: #d1fae5;
}

h1 {
  margin: 10px 0 0;
  color: #0f172a;
  font-size: 30px;
  font-weight: 900;
  letter-spacing: 0;
}

p {
  margin: 10px 0 0;
  color: #526579;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.8;
}

.workflow-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: flex-end;
}

.workflow-metrics :deep(div) {
  min-width: 108px;
  padding: 15px;
  border: 1px solid rgba(203, 226, 240, 0.9);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.84);
  box-shadow: 0 12px 26px rgba(15, 23, 42, 0.05);
}

.workflow-metrics :deep(strong),
.workflow-metrics :deep(span) {
  display: block;
}

.workflow-metrics :deep(strong) {
  color: #0f172a;
  font-size: 26px;
  font-weight: 900;
}

.workflow-metrics :deep(span) {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
  font-weight: 900;
}

@media (max-width: 780px) {
  .workflow-header {
    min-height: auto;
    align-items: flex-start;
    flex-direction: column;
    padding: 22px;
  }

  .workflow-metrics {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
