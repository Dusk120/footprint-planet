<script setup>
import { ref, computed } from 'vue'

const emit = defineEmits(['confirm', 'cancel'])

const props = defineProps({
  editData: { type: Object, default: null }
})

const name = ref(props.editData?.name || '')
const destination = ref(props.editData?.destination || '')
const location = ref(props.editData?.location || '')
const startDate = ref(props.editData?.startDate || '')
// 是否公开：新建默认公开（后端缺省也是 true）；编辑回显原状态
const isPublic = ref(props.editData?.isPublic ?? true)

const days = ref(
  props.editData?.days?.length
    ? props.editData.days.map(d => ({
        title: d.title,
        items: d.items.map(it => ({ ...it }))
      }))
    : [{ title: '第一天', items: [{ time: '', place: '', note: '' }] }]
)

const waypoints = ref(
  props.editData?.waypoints?.map(w => ({ ...w })) || []
)

const newWaypointName = ref('')
const newWaypointNote = ref('')

function addDay() {
  days.value.push({
    title: `第${days.value.length + 1}天`,
    items: [{ time: '', place: '', note: '' }]
  })
}

function removeDay(i) {
  if (days.value.length <= 1) return
  days.value.splice(i, 1)
  days.value.forEach((d, idx) => { d.title = `第${idx + 1}天` })
}

function addFormItem(dayIdx) {
  days.value[dayIdx].items.push({ time: '', place: '', note: '' })
}

function removeFormItem(dayIdx, itemIdx) {
  if (days.value[dayIdx].items.length <= 1) return
  days.value[dayIdx].items.splice(itemIdx, 1)
}

function addWaypoint() {
  if (!newWaypointName.value.trim()) return
  waypoints.value.push({ name: newWaypointName.value.trim(), note: newWaypointNote.value.trim() })
  newWaypointName.value = ''
  newWaypointNote.value = ''
}

function removeWaypoint(i) {
  waypoints.value.splice(i, 1)
}

const isValid = computed(() => name.value.trim() && destination.value.trim())

function handleConfirm() {
  if (!isValid.value) return
  emit('confirm', {
    name: name.value.trim(),
    destination: destination.value.trim(),
    location: location.value.trim(),
    startDate: startDate.value,
    isPublic: isPublic.value,
    days: days.value.filter(d => d.items.some(it => it.place.trim())),
    waypoints: [...waypoints.value]
  })
}
</script>

<template>
  <Teleport to="body">
    <div class="dialog-overlay" @click.self="emit('cancel')">
      <div class="dialog glass-panel">
        <div class="dialog-header">
          <span class="dialog-emoji">📋</span>
          <div class="dialog-title-wrap">
            <h2 class="dialog-title">{{ editData ? '编辑规划' : '新建旅游规划' }}</h2>
            <span class="dialog-sub">规划你的旅行行程</span>
          </div>
        </div>

        <div class="dialog-body">
          <!-- 基础信息 -->
          <div class="field">
            <label class="field-label">清单名称 <span class="req">*</span></label>
            <input v-model="name" class="text-input" placeholder="如「敦煌三日游」" maxlength="30" />
          </div>

          <div class="field-row">
            <div class="field flex-1">
              <label class="field-label">目的地 <span class="req">*</span></label>
              <input v-model="destination" class="text-input" placeholder="如「敦煌莫高窟」" />
            </div>
            <div class="field flex-1">
              <label class="field-label">所在省份/地区</label>
              <input v-model="location" class="text-input" placeholder="如「甘肃省」" />
            </div>
          </div>

          <div class="field">
            <label class="field-label">预计出发日期</label>
            <input v-model="startDate" type="date" class="text-input" />
          </div>

          <!-- 是否公开 -->
          <div class="field public-field">
            <div class="public-row">
              <div class="public-label">
                <span class="field-label">公开分享</span>
                <span class="field-hint">开启后，其他用户可在社区看到并复制这份规划</span>
              </div>
              <button
                type="button"
                class="switch"
                :class="{ on: isPublic }"
                :aria-pressed="isPublic"
                @click="isPublic = !isPublic"
              >
                <span class="switch-dot"></span>
              </button>
            </div>
          </div>

          <!-- 每日行程 -->
          <div class="section-divider">
            <span class="divider-label">每日行程</span>
          </div>

          <div v-for="(day, di) in days" :key="di" class="day-block">
            <div class="day-header">
              <span class="day-title">{{ day.title }}</span>
              <button v-if="days.length > 1" class="day-remove" @click="removeDay(di)">删除这一天</button>
            </div>

            <div v-for="(item, ii) in day.items" :key="ii" class="form-item">
              <div class="item-fields">
                <input v-model="item.time" class="text-input time-input" type="time" placeholder="时间" />
                <input v-model="item.place" class="text-input flex-1" placeholder="地点" />
                <input v-model="item.note" class="text-input flex-1" placeholder="备注（选填）" />
              </div>
              <button v-if="day.items.length > 1" class="item-remove" @click="removeFormItem(di, ii)">×</button>
            </div>

            <button class="add-item-btn" @click="addFormItem(di)">＋ 添加行程</button>
          </div>

          <button class="add-day-btn" @click="addDay">＋ 添加新的一天</button>

          <!-- 途径点 -->
          <div class="section-divider">
            <span class="divider-label">途径点 / 中转站</span>
          </div>

          <div v-for="(wp, wi) in waypoints" :key="wi" class="waypoint-tag">
            <span class="wp-name">📍 {{ wp.name }}</span>
            <span v-if="wp.note" class="wp-note">{{ wp.note }}</span>
            <button class="wp-remove" @click="removeWaypoint(wi)">×</button>
          </div>

          <div class="waypoint-add">
            <input v-model="newWaypointName" class="text-input wp-input" placeholder="途径点名称" />
            <input v-model="newWaypointNote" class="text-input wp-input" placeholder="备注（选填）" />
            <button class="wp-add-btn" @click="addWaypoint">添加</button>
          </div>
        </div>

        <div class="dialog-footer">
          <button class="btn-cancel" @click="emit('cancel')">取消</button>
          <button class="btn-confirm" :disabled="!isValid" @click="handleConfirm">
            {{ editData ? '保存修改' : '创建清单' }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.dialog { width: 580px; }

.field-row { display: flex; gap: 12px; }
.flex-1 { flex: 1; }

.day-block {
  background: var(--dialog-day-bg);
  border: 1px solid var(--dialog-day-border);
  border-radius: var(--radius-sm);
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.day-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.day-title { font-size: 14px; font-weight: 700; color: var(--accent-blue); }

.day-remove {
  font-size: 12px;
  color: var(--danger);
  background: none;
  padding: 4px 8px;
  border-radius: var(--radius-xs);
  transition: var(--transition);
}
.day-remove:hover { background: rgba(248, 113, 113, 0.15); }

.form-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.item-fields {
  display: flex;
  gap: 8px;
  flex: 1;
}

.time-input { width: 110px; flex-shrink: 0; }

.item-remove {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--dialog-item-bg);
  color: var(--text-muted);
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: var(--transition);
}
.item-remove:hover { background: rgba(248, 113, 113, 0.2); color: var(--danger); }

.add-item-btn {
  align-self: flex-start;
  font-size: 13px;
  color: var(--accent-blue);
  background: none;
  padding: 4px 0;
  transition: var(--transition);
}
.add-item-btn:hover { opacity: 0.8; }

.add-day-btn {
  align-self: flex-start;
  padding: 8px 16px;
  border-radius: var(--radius-pill);
  background: rgba(56, 189, 248, 0.12);
  border: 1px dashed rgba(56, 189, 248, 0.4);
  color: var(--accent-blue);
  font-size: 13px;
  font-weight: 600;
  transition: var(--transition);
}
.add-day-btn:hover { background: rgba(56, 189, 248, 0.2); }

.waypoint-tag {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: var(--radius-pill);
  background: rgba(56, 189, 248, 0.1);
  border: 1px solid rgba(56, 189, 248, 0.25);
  font-size: 13px;
}
.wp-name { font-weight: 600; color: var(--accent-blue); }
.wp-note { color: var(--text-muted); font-size: 12px; }
.wp-remove {
  margin-left: auto;
  width: 20px; height: 20px;
  border-radius: 50%;
  background: var(--dialog-cancel-bg);
  color: var(--text-muted);
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.wp-remove:hover { background: rgba(248, 113, 113, 0.2); color: var(--danger); }

.waypoint-add {
  display: flex;
  gap: 8px;
  align-items: center;
}
.wp-input { flex: 1; padding: 8px 12px; font-size: 13px; }
.wp-add-btn {
  padding: 8px 16px;
  border-radius: var(--radius-pill);
  background: var(--accent-gradient);
  color: white;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
  transition: var(--transition);
}
.wp-add-btn:hover { opacity: 0.9; }

.public-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  width: 100%;
}
.public-label {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.field-hint {
  font-size: 12px;
  color: var(--text-muted);
}
.switch {
  position: relative;
  width: 46px;
  height: 26px;
  border-radius: var(--radius-pill);
  background: rgba(var(--overlay-rgb), 0.12);
  border: none;
  cursor: pointer;
  flex-shrink: 0;
  transition: var(--transition);
}
.switch.on { background: var(--accent-gradient); }
.switch-dot {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #fff;
  transition: transform 0.2s;
}
.switch.on .switch-dot { transform: translateX(20px); }
</style>
