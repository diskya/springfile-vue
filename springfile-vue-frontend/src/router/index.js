import { createRouter, createWebHistory } from 'vue-router';
import HomeView from '../views/HomeView.vue';
import FileManagerView from '../views/FileManagerView.vue';

const routes = [
  {
    path: '/',
    name: 'Home',
    component: HomeView,
  },
  {
    path: '/files',
    name: 'FileManager',
    component: FileManagerView,
    // Example of lazy loading (optional, but good practice for larger apps)
    // component: () => import('../views/FileManagerView.vue')
  },
];

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL), // Use history mode
  routes,
});

export default router;
