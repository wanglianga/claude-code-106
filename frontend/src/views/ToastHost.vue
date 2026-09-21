<template>
  <div v-if="toast" class="toast" :class="toast.type">{{ toast.msg }}</div>
</template>

<script setup>
import { toast } from '../use.js'
</script>
