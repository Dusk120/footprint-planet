// ESLint flat config（ESLint 9+ 的配置格式，替代已废弃的 .eslintrc.js）
// 与旧配置的对应关系：
//   extends: 'plugin:vue/vue3-recommended'  ->  pluginVue.configs['flat/recommended']
//   extends: 'prettier'                     ->  skipFormatting（关闭与 Prettier 冲突的规则）
//   env: { browser, node }                  ->  无需声明：vue/recommended 只含 vue/* 规则，未开启 no-undef
import pluginVue from 'eslint-plugin-vue'
import skipFormatting from '@vue/eslint-config-prettier/skip-formatting'

const isProd = process.env.NODE_ENV === 'production'

export default [
  {
    name: 'footprint/ignores',
    ignores: ['dist/**', 'node_modules/**'],
  },
  ...pluginVue.configs['flat/recommended'],
  skipFormatting,
  {
    name: 'footprint/rules',
    languageOptions: {
      ecmaVersion: 2021,
      sourceType: 'module',
    },
    rules: {
      'vue/multi-word-component-names': 'off',
      'vue/no-unused-components': 'warn',
      'vue/no-unused-vars': 'warn',
      // 换行/缩进交给 Prettier：这条规则的 fixer 只插换行不补缩进，会把标签拆成顶格，
      // 反而破坏可读性（Prettier 也负责同一件事，两边同时管会互相打架）
      'vue/first-attribute-linebreak': 'off',
      'no-console': isProd ? 'warn' : 'off',
      'no-debugger': isProd ? 'warn' : 'off',
      'vue/require-default-prop': 'off',
      'vue/attribute-hyphenation': 'off',
    },
  },
]
