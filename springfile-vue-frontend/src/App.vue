<script setup>
import { ref, onMounted } from 'vue'
import HelloWorld from './components/HelloWorld.vue'
import TheWelcome from './components/TheWelcome.vue'

const message = ref('Loading message from backend...')

onMounted(async () => {
  try {
    // Assuming Spring Boot runs on localhost:8080
    const response = await fetch('http://localhost:8080/api/hello')
    if (response.ok) {
      message.value = await response.text()
    } else {
      message.value = 'Failed to load message from backend.'
    }
  } catch (error) {
    console.error('Error fetching data:', error)
    message.value = 'Error connecting to backend.'
  }
})
</script>

<template>
  <header>
    <img alt="Vue logo" class="logo" src="./assets/logo.svg" width="125" height="125" />

    <div class="wrapper">
      <HelloWorld msg="You did it!" />
      <p>{{ message }}</p>
    </div>
  </header>

  <main>
    <TheWelcome />
  </main>
</template>

<style scoped>
header {
  line-height: 1.5;
}

.logo {
  display: block;
  margin: 0 auto 2rem;
}

@media (min-width: 1024px) {
  header {
    display: flex;
    place-items: center;
    padding-right: calc(var(--section-gap) / 2);
  }

  .logo {
    margin: 0 2rem 0 0;
  }

  header .wrapper {
    display: flex;
    place-items: flex-start;
    flex-wrap: wrap;
  }
}
</style>
