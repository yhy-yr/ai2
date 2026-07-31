<template>
  <component :is="activeComponent" />
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '../../app/store'
import AdminCommandCenter from './AdminCommandCenter.vue'
import ClinicalCommandCenter from './ClinicalCommandCenter.vue'
import PatientCommandCenter from './PatientCommandCenter.vue'
import DepartmentManage from '../catalog/DepartmentManage.vue'
import MedicineManage from '../medication/MedicineManage.vue'
import AppointmentManage from '../appointments/AppointmentManage.vue'
import DoctorAppointments from '../appointments/DoctorAppointments.vue'
import PatientAppointments from '../appointments/PatientAppointments.vue'
import AppointmentCreate from '../appointments/AppointmentCreate.vue'
import MedicalRecordManage from '../clinical-records/MedicalRecordManage.vue'
import DoctorMedicalRecords from '../clinical-records/DoctorMedicalRecords.vue'
import PatientMedicalRecords from '../clinical-records/PatientMedicalRecords.vue'
import PrescriptionManage from '../medication/PrescriptionManage.vue'
import DoctorPrescriptions from '../medication/DoctorPrescriptions.vue'
import PatientPrescriptions from '../medication/PatientPrescriptions.vue'
import ExaminationManage from '../examinations/ExaminationManage.vue'
import DoctorExaminations from '../examinations/DoctorExaminations.vue'
import PatientExaminations from '../examinations/PatientExaminations.vue'

const route = useRoute()
const auth = useAuthStore()

const roleHomeComponents = {
  ADMIN: AdminCommandCenter,
  DOCTOR: ClinicalCommandCenter,
  PATIENT: PatientCommandCenter
}

const workflowComponents = {
  departments: DepartmentManage,
  medicines: MedicineManage,
  appointments: AppointmentManage,
  'doctor-appointments': DoctorAppointments,
  'patient-appointments': PatientAppointments,
  'create-appointment': AppointmentCreate,
  records: MedicalRecordManage,
  'doctor-records': DoctorMedicalRecords,
  'patient-records': PatientMedicalRecords,
  prescriptions: PrescriptionManage,
  'doctor-prescriptions': DoctorPrescriptions,
  'patient-prescriptions': PatientPrescriptions,
  examinations: ExaminationManage,
  'doctor-examinations': DoctorExaminations,
  'patient-examinations': PatientExaminations
}

const activeComponent = computed(() => {
  const workflow = String(route.query.workflow || '')
  return workflowComponents[workflow] || roleHomeComponents[auth.user?.role] || PatientCommandCenter
})
</script>
