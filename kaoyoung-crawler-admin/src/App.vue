<template>
  <router-view />
</template>

<script setup>
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html {
  scroll-behavior: smooth;
}

body {
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

::-webkit-scrollbar {
  width: 10px;
  height: 10px;
}

::-webkit-scrollbar-track {
  background: rgba(10, 10, 15, 0.5);
  border-radius: 5px;
}

::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, var(--primary-color), var(--secondary-color));
  border-radius: 5px;
  box-shadow: 0 0 10px var(--glow-color);
}

::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(180deg, var(--secondary-color), var(--primary-color));
  box-shadow: 0 0 15px var(--glow-color);
}

::-webkit-scrollbar-corner {
  background: rgba(10, 10, 15, 0.5);
}
</style>
