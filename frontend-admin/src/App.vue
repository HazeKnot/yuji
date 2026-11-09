<template>
  <router-view />
</template>

<style>
:root {
  --el-color-primary: #7a9b76;
  --el-color-primary-light-3: #9db89a;
  --el-color-primary-light-5: #b9ceb4;
  --el-color-primary-light-7: #d5e2d2;
  --el-color-primary-light-8: #e3ece0;
  --el-color-primary-light-9: #f1f6ef;
  --el-color-primary-dark-2: #5d7f5a;
}

body {
  margin: 0;
  background: #f5f7f5;
  color: #303133;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Hiragino Sans GB',
    'Microsoft YaHei', sans-serif;
  -webkit-font-smoothing: antialiased;
}

.el-card {
  border-radius: 12px;
  border-color: #eef0ee;
}

.el-table th.el-table__cell {
  background: #fafbf9;
  color: #606266;
  font-weight: 500;
}

.el-button--primary {
  --el-button-bg-color: #7a9b76;
  --el-button-border-color: #7a9b76;
}
</style>
