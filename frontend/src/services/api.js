const BASE_URL = '/api'

function withQuery(path, params = {}) {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      search.set(key, value)
    }
  })
  const query = search.toString()
  return query ? `${path}?${query}` : path
}

async function request(path, options = {}) {
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {})
  }

  const sessionToken = localStorage.getItem('sessionToken')
  if (sessionToken) {
    headers['X-Session-Token'] = sessionToken
  }

  const config = {
    ...options,
    headers
  }

  if (config.body && typeof config.body !== 'string') {
    config.body = JSON.stringify(config.body)
  }

  let response
  try {
    response = await fetch(`${BASE_URL}${path}`, config)
  } catch (error) {
    throw new Error('无法连接后端服务，请确认后端已启动')
  }

  const result = await response.json().catch(() => ({
    code: 1,
    message: '数据同步失败',
    data: null
  }))

  if (!response.ok) {
    throw new Error(resolveErrorMessage(result, response.status))
  }

  if (result.code !== 0) {
    throw new Error(resolveErrorMessage(result, response.status))
  }

  return result.data
}

function resolveErrorMessage(result, status) {
  if (result?.message) {
    return result.message
  }
  if (result?.error && result?.path) {
    return `${result.error}：${result.path}`
  }
  if (result?.error) {
    return result.error
  }
  return `数据同步失败，状态码 ${status}`
}

export default {
  login(payload) {
    return request('/auth/login', {
      method: 'POST',
      body: payload
    })
  },
  register(payload) {
    return request('/auth/register', {
      method: 'POST',
      body: payload
    })
  },
  me() {
    return request('/auth/me')
  },
  logout() {
    return request('/auth/logout', {
      method: 'POST'
    })
  },
  adminDashboard() {
    return request('/dashboard/admin')
  },
  doctorDashboard() {
    return request('/dashboard/doctor')
  },
  patientDashboard() {
    return request('/dashboard/patient')
  },
  aiStatus() {
    return request('/ai/status')
  },
  aiRisk(symptoms) {
    return request('/ai/risk', {
      method: 'POST',
      body: { symptoms }
    })
  },
  visitProgress(appointmentId) {
    return request(withQuery('/patient/visitProgress', { appointmentId }))
  },
  visitCompleteness(appointmentId) {
    return request(withQuery('/doctor/visit-completeness', { appointmentId }))
  },
  aiConsultationApi: {
    analyze(symptoms) {
      return request('/ai-consultations/analyze', {
        method: 'POST',
        body: { symptoms }
      })
    },
    my() {
      return request('/ai-consultations/my')
    }
  },
  appointmentApi: {
    create(payload) {
      return request('/appointments', {
        method: 'POST',
        body: payload
      })
    },
    my() {
      return request('/appointments/my')
    },
    cancel(id) {
      return request(`/appointments/${id}`, {
        method: 'DELETE'
      })
    },
    list() {
      return request('/appointments')
    },
    doctorList() {
      return request('/appointments/doctor')
    },
    updateStatus(id, status) {
      return request(`/appointments/${id}/status`, {
        method: 'PUT',
        body: { status }
      })
    }
  },
  medicalRecordApi: {
    aiDraft(appointmentId) {
      return request(`/medical-records/ai-draft/${appointmentId}`, {
        method: 'POST'
      })
    },
    create(payload) {
      return request('/medical-records', {
        method: 'POST',
        body: payload
      })
    },
    update(id, payload) {
      return request(`/medical-records/${id}`, {
        method: 'PUT',
        body: payload
      })
    },
    doctorList() {
      return request('/medical-records/doctor')
    },
    my() {
      return request('/medical-records/my')
    },
    list() {
      return request('/medical-records')
    },
    detail(id) {
      return request(`/medical-records/${id}`)
    }
  },
  departmentApi: {
    list(keyword = '') {
      return request(withQuery('/departments', { keyword }))
    },
    create(payload) {
      return request('/departments', {
        method: 'POST',
        body: payload
      })
    },
    update(id, payload) {
      return request(`/departments/${id}`, {
        method: 'PUT',
        body: payload
      })
    },
    remove(id) {
      return request(`/departments/${id}`, {
        method: 'DELETE'
      })
    }
  },
  medicineApi: {
    list(keyword = '') {
      return request(withQuery('/medicines', { keyword }))
    },
    create(payload) {
      return request('/medicines', {
        method: 'POST',
        body: payload
      })
    },
    update(id, payload) {
      return request(`/medicines/${id}`, {
        method: 'PUT',
        body: payload
      })
    },
    remove(id) {
      return request(`/medicines/${id}`, {
        method: 'DELETE'
      })
    }
  },
  doctorApi: {
    list(keyword = '') {
      return request(withQuery('/doctors', { keyword }))
    },
    detail(id) {
      return request(`/doctors/${id}`)
    },
    byDepartment(departmentId) {
      return request(`/doctors/by-department/${departmentId}`)
    },
    create(payload) {
      return request('/doctors', {
        method: 'POST',
        body: payload
      })
    },
    update(id, payload) {
      return request(`/doctors/${id}`, {
        method: 'PUT',
        body: payload
      })
    },
    remove(id) {
      return request(`/doctors/${id}`, {
        method: 'DELETE'
      })
    }
  },
  patientApi: {
    list(keyword = '') {
      return request(withQuery('/patients', { keyword }))
    },
    detail(id) {
      return request(`/patients/${id}`)
    },
    create(payload) {
      return request('/patients', {
        method: 'POST',
        body: payload
      })
    },
    update(id, payload) {
      return request(`/patients/${id}`, {
        method: 'PUT',
        body: payload
      })
    },
    remove(id) {
      return request(`/patients/${id}`, {
        method: 'DELETE'
      })
    }
  },
  prescriptionApi: {
    create(payload) {
      return request('/prescriptions', {
        method: 'POST',
        body: payload
      })
    },
    doctorList() {
      return request('/prescriptions/doctor')
    },
    my() {
      return request('/prescriptions/my')
    },
    list() {
      return request('/prescriptions')
    },
    detail(id) {
      return request(`/prescriptions/${id}`)
    }
  },
  examinationApi: {
    create(payload) {
      return request('/examinations', {
        method: 'POST',
        body: payload
      })
    },
    update(id, payload) {
      return request(`/examinations/${id}`, {
        method: 'PUT',
        body: payload
      })
    },
    doctorList() {
      return request('/examinations/doctor')
    },
    my() {
      return request('/examinations/my')
    },
    list() {
      return request('/examinations')
    }
  }
}
