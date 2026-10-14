module.exports = {
  root: true,
  env: {
    node: true,
  },
  extends: ['plugin:vue/essential', 'eslint:recommended'],
  parserOptions: {
    // 项目已从 Vue CLI 迁移到 Vite，无需经 babel 解析；
    // 原先指向 @babel/eslint-parser 会加载仍引用 @vue/cli-plugin-babel/preset 的 babel.config.js，导致全量解析失败
    ecmaVersion: 'latest',
    sourceType: 'module',
  },
  rules: {
    'no-console': process.env.NODE_ENV === 'production' ? 'warn' : 'off',
    'no-debugger': process.env.NODE_ENV === 'production' ? 'warn' : 'off',
    'vue/multi-word-component-names': 'off',
  },
}
