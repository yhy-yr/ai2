import { createRouter, createWebHashHistory } from 'vue-router'
import { useAuthStore, roleHome } from '../store'
import SplitWorkspaceLayout from '../layout/SplitWorkspaceLayout.vue'
import LoginPage from '../../pages/LoginPage.vue'
import WorkspacePage from '../../pages/WorkspacePage.vue'
import PatientsPage from '../../pages/PatientsPage.vue'
import EncounterPage from '../../pages/EncounterPage.vue'
import AiPage from '../../pages/AiPage.vue'

const workflowRoutes = ['/workspace', '/patients', '/ai']

const roleWorkflows = {
  ADMIN: ['', 'departments', 'medicines', 'appointments', 'records', 'prescriptions', 'examinations'],
  DOCTOR: ['', 'doctor-appointments', 'doctor-records', 'doctor-prescriptions', 'doctor-examinations'],
  PATIENT: ['', 'create-appointment', 'patient-appointments', 'patient-records', 'patient-prescriptions', 'patient-examinations']
}

const rolePatientViews = {
  ADMIN: ['', 'patients', 'doctors'],
  PATIENT: ['', 'records', 'prescriptions', 'examinations']
}

function isRoleRouteAllowed(to, role) {
  if (to.path.startsWith('/encounter/')) {
    return role === 'DOCTOR'
  }

  if (to.path === '/ai') {
    return role === 'PATIENT'
  }

  if (to.path === '/patients') {
    const view = String(to.query.view || '')
    return Boolean(rolePatientViews[role]?.includes(view))
  }

  if (to.path === '/workspace') {
    const workflow = String(to.query.workflow || '')
    return Boolean(roleWorkflows[role]?.includes(workflow))
  }

  return false
}

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: '/',
      redirect: '/workspace'
    },
    {
      path: '/login',
      component: LoginPage
    },
    {
      path: '/',
      component: SplitWorkspaceLayout,
      children: [
        {
          path: 'workspace',
          component: WorkspacePage
        },
        {
          path: 'patients',
          component: PatientsPage
        },
        {
          path: 'encounter/:id',
          component: EncounterPage
        },
        {
          path: 'ai',
          component: AiPage
        }
      ]
    }
  ]
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()

  if (!auth.bootstrapDone) {
    await auth.bootstrap()
  }

  if (to.path === '/login') {
    return auth.isLoggedIn ? roleHome(auth.user.role) : true
  }

  const isWorkflowRoute = workflowRoutes.includes(to.path) || to.path.startsWith('/encounter/')
  if (!isWorkflowRoute) {
    return auth.isLoggedIn ? roleHome(auth.user.role) : '/login'
  }

  if (!auth.isLoggedIn) {
    return '/login'
  }

  if (!isRoleRouteAllowed(to, auth.user?.role)) {
    return roleHome(auth.user?.role)
  }

  return true
})

export default router
