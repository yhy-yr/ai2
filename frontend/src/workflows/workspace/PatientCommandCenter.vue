<template>
  <section class="patient-home">
    <header class="patient-hero">
      <div>
        <span>患者服务</span>
        <h1>我的健康时间线</h1>
        <p>查看预约、病历、处方、检查结果和 AI 预问诊建议。</p>
      </div>
      <router-link class="primary-action" to="/ai">开始 AI 预问诊</router-link>
    </header>

    <div class="care-grid">
      <router-link v-for="item in cards" :key="item.path" class="care-card" :to="item.path">
        <span :class="item.tone"><component :is="item.icon" :size="22" /></span>
        <strong>{{ item.title }}</strong>
        <p>{{ item.desc }}</p>
      </router-link>
    </div>

    <div class="patient-grid">
      <article class="timeline-panel">
        <div class="panel-head">
          <h2>近期诊疗记录</h2>
          <router-link to="/patients?view=records">查看全部</router-link>
        </div>
        <ol>
          <li>
            <time>今天 08:30</time>
            <strong>内科复诊</strong>
            <p>咳嗽、胸闷 3 天，医生建议完善检查。</p>
          </li>
          <li>
            <time>昨天 19:10</time>
            <strong>AI 预问诊</strong>
            <p>系统提示中风险，建议线下就诊。</p>
          </li>
          <li>
            <time>上周三</time>
            <strong>处方记录</strong>
            <p>请按医嘱用药，如症状未缓解及时复诊。</p>
          </li>
        </ol>
      </article>

      <article class="reminder-panel">
        <div class="panel-head">
          <h2>健康提醒</h2>
        </div>
        <div class="reminder high">
          <strong>检查结果待查看</strong>
          <p>血常规和 CRP 检查已录入，请关注医生结论。</p>
        </div>
        <div class="reminder">
          <strong>复诊建议</strong>
          <p>如 3 天后咳嗽、胸闷未缓解，建议复诊。</p>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup>
import { Bot, CalendarPlus, ClipboardList, FlaskConical, Pill, TimerReset } from '@lucide/vue'

const cards = [
  { title: 'AI 预问诊', desc: '输入症状，获取辅助风险提示', path: '/ai', icon: Bot, tone: 'blue' },
  { title: '预约挂号', desc: '选择科室、医生和就诊时间', path: '/workspace?workflow=create-appointment', icon: CalendarPlus, tone: 'teal' },
  { title: '就诊进度', desc: '查看预约和接诊状态', path: '/workspace?workflow=patient-appointments', icon: TimerReset, tone: 'green' },
  { title: '病历记录', desc: '按时间查看医生保存的病历', path: '/patients?view=records', icon: ClipboardList, tone: 'blue' },
  { title: '处方记录', desc: '查看处方和用药建议', path: '/patients?view=prescriptions', icon: Pill, tone: 'amber' },
  { title: '检查结果', desc: '查看检查结果和医生结论', path: '/patients?view=examinations', icon: FlaskConical, tone: 'rose' },
]
</script>

<style scoped>
.patient-home {
  display: grid;
  gap: 18px;
}

.patient-hero,
.care-card,
.timeline-panel,
.reminder-panel {
  border: 1px solid rgba(205, 218, 234, 0.82);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 18px 48px rgba(22, 45, 74, 0.07);
}

.patient-hero {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: center;
  padding: 30px;
}

.patient-hero span {
  color: #0891b2;
  font-size: 13px;
  font-weight: 900;
}

.patient-hero h1 {
  margin: 8px 0 0;
  color: #0f172a;
  font-size: 31px;
  font-weight: 900;
  letter-spacing: 0;
}

.patient-hero p {
  margin: 10px 0 0;
  color: #64748b;
  line-height: 1.7;
}

.primary-action {
  display: inline-flex;
  min-height: 48px;
  align-items: center;
  justify-content: center;
  padding: 0 18px;
  border-radius: 14px;
  color: #ffffff;
  background: linear-gradient(135deg, #0891b2, #0f766e);
  box-shadow: 0 18px 34px rgba(8, 145, 178, 0.22);
  font-weight: 900;
}

.care-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.care-card {
  display: grid;
  gap: 10px;
  min-height: 150px;
  padding: 20px;
}

.care-card > span {
  display: grid;
  width: 46px;
  height: 46px;
  place-items: center;
  border-radius: 16px;
}

.blue { color: #2563eb; background: #dbeafe; }
.teal { color: #0f766e; background: #ccfbf1; }
.green { color: #15803d; background: #dcfce7; }
.amber { color: #d97706; background: #fef3c7; }
.rose { color: #e11d48; background: #ffe4e6; }

.care-card strong,
.panel-head h2,
.reminder strong {
  color: #0f172a;
  font-weight: 900;
}

.care-card p,
.reminder p {
  margin: 0;
  color: #64748b;
  line-height: 1.7;
}

.patient-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 0.9fr);
  gap: 18px;
}

.timeline-panel,
.reminder-panel {
  padding: 22px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: baseline;
}

.panel-head h2 {
  margin: 0;
  font-size: 19px;
}

.panel-head a {
  color: #0e7490;
  font-weight: 900;
}

ol {
  display: grid;
  gap: 18px;
  padding: 0;
  margin: 18px 0 0;
  list-style: none;
}

li {
  padding-left: 18px;
  border-left: 3px solid #8bd5e5;
}

li time {
  color: #0891b2;
  font-size: 12px;
  font-weight: 900;
}

li strong {
  display: block;
  margin-top: 5px;
  color: #0f172a;
}

li p {
  margin: 5px 0 0;
  color: #64748b;
  line-height: 1.7;
}

.reminder {
  margin-top: 16px;
  padding: 16px;
  border-radius: 16px;
  background: #f8fcff;
}

.reminder.high {
  background: #fff7ed;
}

@media (max-width: 980px) {
  .patient-hero,
  .patient-grid {
    grid-template-columns: 1fr;
  }

  .patient-hero {
    align-items: flex-start;
    flex-direction: column;
  }

  .care-grid {
    grid-template-columns: 1fr;
  }
}
</style>
