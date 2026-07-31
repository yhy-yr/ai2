<template>
  <component :is="activeComponent" />
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '../../app/store'
import PatientManage from './PatientManage.vue'
import DoctorManage from './DoctorManage.vue'
import PatientMedicalRecords from '../clinical-records/PatientMedicalRecords.vue'
import PatientPrescriptions from '../medication/PatientPrescriptions.vue'
import PatientExaminations from '../examinations/PatientExaminations.vue'

const route = useRoute()
const auth = useAuthStore()

const adminComponents = {
  doctors: DoctorManage,
  patients: PatientManage
}

const patientComponents = {
  records: PatientMedicalRecords,
  prescriptions: PatientPrescriptions,
  examinations: PatientExaminations
}

const activeComponent = computed(() => {
  const view = String(route.query.view || '')
  if (auth.user?.role === 'ADMIN') {
    return adminComponents[view] || PatientManage
  }
  return patientComponents[view] || PatientMedicalRecords
})
</script>
