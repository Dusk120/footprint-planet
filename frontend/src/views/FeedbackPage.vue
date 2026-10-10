<script setup>
import FeedbackForm from '../components/FeedbackForm.vue'
</script>

<template>
  <div class="feedback-page">
    <div class="feedback-content">
      <FeedbackForm />
    </div>
  </div>
</template>

<style scoped>
.feedback-page {
  padding: 40px;
  height: 100%;
  display: flex;
  align-items: flex-start;
}

.feedback-content {
  width: 100%;
  max-width: 700px;
  margin: 0 auto;
}
</style>
