<template>
  <div class="split-workspace-layout">
    <PatientListPanel />
    <section class="clinical-main">
      <Topbar />
      <main class="workspace-content">
        <router-view />
      </main>
    </section>
    <AiAssistPanel />
  </div>
</template>

<script setup>
import PatientListPanel from '../../components/medical/PatientListPanel.vue'
import AiAssistPanel from '../../components/medical/AiAssistPanel.vue'
import Topbar from '../../components/ui/Topbar.vue'
</script>

<style scoped>
.split-workspace-layout {
  display: grid;
  min-height: 100vh;
  grid-template-columns: 390px minmax(0, 1fr) 360px;
  background:
    radial-gradient(circle at 76% 2%, rgba(14, 165, 233, 0.1), transparent 30%),
    radial-gradient(circle at 42% 96%, rgba(20, 184, 166, 0.08), transparent 34%),
    #f7fbff;
}

.clinical-main {
  display: flex;
  min-width: 0;
  flex-direction: column;
}

.workspace-content {
  flex: 1;
  width: 100%;
  max-width: none;
  padding: 18px 22px 0;
  margin: 0 auto;
}

@media (max-width: 1120px) {
  .split-workspace-layout {
    display: flex;
    flex-direction: column;
  }

  .workspace-content {
    padding: 22px;
  }
}
</style>
