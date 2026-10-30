<script setup>
defineProps({ title: { type: String, required: true }, description: { type: String, default: '' } })
</script>
<template>
  <header class="workspace-heading">
    <div><h1>{{ title }}</h1><p v-if="description">{{ description }}</p></div>
    <div v-if="$slots.default" class="workspace-heading-actions"><slot /></div>
  </header>
</template>
