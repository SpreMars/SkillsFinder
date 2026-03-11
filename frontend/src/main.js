import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'
import Home from './views/Home.vue'
import SkillDetail from './views/SkillDetail.vue'
import DcmViewer from './views/DcmViewer.vue'
import './assets/main.css'

const routes = [
  { path: '/', name: 'Home', component: Home },
  { path: '/skill/:id', name: 'SkillDetail', component: SkillDetail },
  { path: '/dcm', name: 'DcmViewer', component: DcmViewer }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

const app = createApp(App)
app.use(router)
app.mount('#app')
