<template>
  <div class="adopt-page">
    <div class="content-wrapper">
      <div class="filter-section">
        <div class="filter-tabs">
          <span
            v-for="category in categoryList"
            :key="category.categoryId"
            :class="['filter-tab', { active: animalFilter === category.categoryId }]"
            @click="animalFilter = category.categoryId"
          >
            {{ category.name }}
          </span>
        </div>
      </div>

      <div class="map-entry-card">
        <div>
          <h3>地图找宠</h3>
          <p>优先展示有可领养宠物的救助站，可按距离快速定位最近站点。</p>
        </div>
        <el-button type="primary" @click="openMapDialog">打开地图找宠物</el-button>
      </div>

      <div class="animals-grid">
        <div
          v-for="animal in animalList"
          :key="animal.animalId"
          class="pet-card"
          @click="goToDetail(animal)"
        >
          <div class="pet-img-wrapper">
            <img :src="baseUrl + animal.image" :alt="animal.name" loading="lazy">
            <div class="pet-status-badge">
              <el-tag type="primary">{{ animal.status }}</el-tag>
            </div>
          </div>
          <div class="pet-info">
            <div class="pet-header">
              <h3>{{ animal.name }}</h3>
              <span class="gender-icon male" v-if="animal.gender === '公'">
                <el-icon><Male /></el-icon>
              </span>
              <span class="gender-icon female" v-else>
                <el-icon><Female /></el-icon>
              </span>
            </div>
            <div class="pet-tags">
              <span class="tag">{{ animal.age }}</span>
              <span class="tag station-tag" @click.stop="goToStation(animal.stationId)">
                <el-icon><House/></el-icon>
                {{ animal.stationName }}
              </span>
            </div>
            <p class="pet-desc">{{ animal.description }}</p>
          </div>
        </div>
      </div>

      <div style="display: flex; justify-content: center;">
        <pagination
          :pageSizes="[8,16,32]"
          v-show="total>0"
          :total="total"
          v-model:page="query.pageNum"
          v-model:limit="query.pageSize"
          @pagination="getList"
        />
      </div>
    </div>

    <el-dialog
      v-model="mapDialogVisible"
      title="附近救助站地图"
      width="86%"
      destroy-on-close
      @opened="initMap"
      @closed="destroyMap"
    >
      <div class="map-toolbar">
        <el-button type="primary" plain @click="locateMe">定位我的位置</el-button>
        <span class="toolbar-text">当前筛选下共 {{ mapStationList.length }} 个救助站可展示</span>
      </div>
      <div class="map-layout">
        <div id="station-map" class="station-map"></div>
        <div class="station-panel">
          <h4>推荐救助站</h4>
          <div v-if="!mapStationList.length" class="empty-tip">暂无可展示救助站，请尝试切换分类。</div>
          <div v-else class="station-list">
            <div
              v-for="station in sortedStationList"
              :key="station.stationId"
              class="station-item"
              @click="focusStation(station)"
            >
              <div class="item-title">{{ station.name }}</div>
              <div class="item-meta">可领养：{{ station.adoptableCount }} 只</div>
              <div class="item-meta" v-if="station.distanceText">距离你约 {{ station.distanceText }}</div>
              <div class="item-address">{{ station.address || '暂无地址' }}</div>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import {computed, nextTick, onMounted, ref, watch} from 'vue'
import {useRouter} from 'vue-router'
import {Female, House, Male} from '@element-plus/icons-vue'
import {ElMessage} from 'element-plus'
import {listAnimal} from '@/api/succour/animal.js'
import {selectAllCategory} from '@/api/succour/category.js'
import {selectStationListByIsAuth} from '@/api/succour/station.js'

const baseUrl = import.meta.env.VITE_APP_BASE_API
const AMAP_WEB_KEY = import.meta.env.VITE_AMAP_WEB_KEY
const AMAP_SECURITY_JS_CODE = import.meta.env.VITE_AMAP_SECURITY_JS_CODE
const AMAP_JS_URL = `https://webapi.amap.com/maps?v=2.0&key=${AMAP_WEB_KEY}&plugin=AMap.Scale,AMap.ToolBar,AMap.Geolocation`

const router = useRouter()
const animalFilter = ref('all')
const mapDialogVisible = ref(false)
const stationList = ref([])
const animalList = ref([])
const mapAnimalList = ref([])
const mapRef = ref(null)
const infoWindowRef = ref(null)
const markerMap = ref({})
const myLocation = ref(null)
const total = ref(0)

const query = ref({
  pageNum: 1,
  pageSize: 8,
  categoryId: null,
  status: '可领养'
})

const categoryList = ref([
  {name: '全部伙伴', categoryId: 'all'},
  {name: '汪星人', categoryId: '汪星人'},
  {name: '喵星人', categoryId: '喵星人'},
  {name: '其他宠物', categoryId: '其他宠物'}
])

const stationAnimalCountMap = computed(() => {
  const countMap = {}
  mapAnimalList.value.forEach((animal) => {
    if (!animal.stationId) return
    countMap[animal.stationId] = (countMap[animal.stationId] || 0) + 1
  })
  return countMap
})

const mapStationList = computed(() => {
  return stationList.value
    .filter(station => stationAnimalCountMap.value[station.stationId] > 0)
    .map((station) => {
      const lng = Number(station.longitude)
      const lat = Number(station.latitude)
      const hasValidPoint = !Number.isNaN(lng) && !Number.isNaN(lat)
      const distance = hasValidPoint && myLocation.value
        ? calculateDistance(myLocation.value[1], myLocation.value[0], lat, lng)
        : null
      return {
        ...station,
        lng,
        lat,
        hasValidPoint,
        adoptableCount: stationAnimalCountMap.value[station.stationId],
        distance,
        distanceText: distance === null ? '' : formatDistance(distance)
      }
    })
})

const sortedStationList = computed(() => {
  return [...mapStationList.value].sort((a, b) => {
    if (a.distance !== null && b.distance !== null) return a.distance - b.distance
    if (a.distance !== null) return -1
    if (b.distance !== null) return 1
    return b.adoptableCount - a.adoptableCount
  })
})

const goToStation = (stationId) => router.push(`/user/stationDetail/${stationId}`)
const goToDetail = (animal) => router.push(`/user/animalDetail/${animal.animalId}`)

const getList = () => {
  const queryParams = {...query.value}
  if (queryParams.categoryId === 'all') delete queryParams.categoryId
  listAnimal(queryParams).then(res => {
    total.value = res.total
    animalList.value = res.rows || []
  })
}

const loadMapAnimalList = async () => {
  const queryParams = {
    pageNum: 1,
    pageSize: 2000,
    status: '可领养'
  }
  if (query.value.categoryId) {
    queryParams.categoryId = query.value.categoryId
  }
  const res = await listAnimal(queryParams)
  mapAnimalList.value = res.rows || []
}

const loadStationList = async () => {
  const res = await selectStationListByIsAuth()
  stationList.value = res.data || []
}

const loadAMap = async () => {
  if (!AMAP_WEB_KEY) {
    ElMessage.warning('未配置高德地图 Key，暂时无法使用地图找宠物')
    return false
  }

  if (AMAP_SECURITY_JS_CODE) {
    window._AMapSecurityConfig = {securityJsCode: AMAP_SECURITY_JS_CODE}
  }

  if (window.AMap) return true

  await new Promise((resolve, reject) => {
    const existScript = document.querySelector('script[data-amap="animal-map"]')
    if (existScript) {
      existScript.addEventListener('load', resolve, {once: true})
      existScript.addEventListener('error', reject, {once: true})
      if (window.AMap) resolve()
      return
    }

    const script = document.createElement('script')
    script.dataset.amap = 'animal-map'
    script.src = AMAP_JS_URL
    script.async = true
    script.onload = resolve
    script.onerror = reject
    document.body.appendChild(script)
  })

  return !!window.AMap
}

const openMapDialog = () => {
  if (!AMAP_WEB_KEY) {
    ElMessage.warning('未配置高德地图 Key，暂时无法使用地图找宠物')
    return
  }
  mapDialogVisible.value = true
}

const locateMe = () => {
  if (!mapRef.value || !window.AMap) return

  window.AMap.plugin('AMap.Geolocation', () => {
    const geolocation = new window.AMap.Geolocation({enableHighAccuracy: true, timeout: 10000})
    geolocation.getCurrentPosition((status, result) => {
      if (status === 'complete' && result.position) {
        myLocation.value = [result.position.lng, result.position.lat]
        renderMarkers()
        mapRef.value.setCenter(myLocation.value)
        mapRef.value.setZoom(11)
        ElMessage.success('定位成功，已按距离优化排序')
      } else {
        ElMessage.warning('定位失败，请检查浏览器定位权限')
      }
    })
  })
}

const focusStation = (station) => {
  if (!mapRef.value || !station.hasValidPoint) {
    ElMessage.info('该救助站暂无可用坐标')
    return
  }
  const marker = markerMap.value[station.stationId]
  mapRef.value.setCenter([station.lng, station.lat])
  mapRef.value.setZoom(13)
  if (marker && infoWindowRef.value) {
    infoWindowRef.value.setContent(buildInfoContent(station))
    infoWindowRef.value.open(mapRef.value, marker.getPosition())
  }
}

const buildInfoContent = (station) => `
  <div>
    <strong>${station.name}</strong><br/>
    地址：${station.address || '暂无'}<br/>
    可领养宠物：${station.adoptableCount}只
    ${station.distanceText ? `<br/>距离你约：${station.distanceText}` : ''}
  </div>
`

const renderMarkers = () => {
  if (!mapRef.value || !window.AMap) return
  mapRef.value.clearMap()
  markerMap.value = {}

  const points = []
  const validStations = sortedStationList.value.filter(station => station.hasValidPoint)

  validStations.forEach((station) => {
    const marker = new window.AMap.Marker({
      position: [station.lng, station.lat],
      title: station.name,
      map: mapRef.value
    })

    marker.on('click', () => {
      infoWindowRef.value.setContent(buildInfoContent(station))
      infoWindowRef.value.open(mapRef.value, marker.getPosition())
    })

    markerMap.value[station.stationId] = marker
    points.push([station.lng, station.lat])
  })

  if (myLocation.value) {
    const myMarker = new window.AMap.Marker({
      position: myLocation.value,
      map: mapRef.value,
      icon: 'https://webapi.amap.com/theme/v1.3/markers/n/mark_b.png',
      title: '我的位置'
    })
    points.push(myLocation.value)
    markerMap.value.myLocation = myMarker
  }

  if (points.length) {
    mapRef.value.setFitView(Object.values(markerMap.value), false, [80, 80, 80, 80])
  } else {
    ElMessage.info('当前筛选条件下暂无可展示在地图上的救助站')
  }
}

const initMap = async () => {
  await nextTick()
  const loaded = await loadAMap()
  if (!loaded) return

  await Promise.all([loadStationList(), loadMapAnimalList()])

  if (!mapRef.value) {
    mapRef.value = new window.AMap.Map('station-map', {
      zoom: 5,
      center: [116.4074, 39.9042],
      viewMode: '2D'
    })
    mapRef.value.addControl(new window.AMap.Scale())
    mapRef.value.addControl(new window.AMap.ToolBar())
    infoWindowRef.value = new window.AMap.InfoWindow({
      offset: new window.AMap.Pixel(0, -30)
    })
  }

  renderMarkers()
}

const destroyMap = () => {
  markerMap.value = {}
  infoWindowRef.value = null
  if (mapRef.value) {
    mapRef.value.destroy()
    mapRef.value = null
  }
}

const calculateDistance = (lat1, lng1, lat2, lng2) => {
  const toRad = (deg) => (deg * Math.PI) / 180
  const R = 6371000
  const dLat = toRad(lat2 - lat1)
  const dLng = toRad(lng2 - lng1)
  const a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
    + Math.cos(toRad(lat1)) * Math.cos(toRad(lat2))
    * Math.sin(dLng / 2) * Math.sin(dLng / 2)
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
  return R * c
}

const formatDistance = (distance) => {
  if (distance < 1000) return `${Math.round(distance)}米`
  return `${(distance / 1000).toFixed(1)}公里`
}

watch(animalFilter, (newVal) => {
  query.value.categoryId = newVal === 'all' ? null : newVal
  query.value.pageNum = 1
  getList()
  if (mapDialogVisible.value) {
    loadMapAnimalList().then(() => renderMarkers())
  }
})

onMounted(() => {
  getList()
  loadStationList()
  loadMapAnimalList()

  selectAllCategory().then(res => {
    categoryList.value = [{name: '全部伙伴', categoryId: 'all'}, ...(res.data || [])]
  })
})
</script>

<style scoped>
.adopt-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  min-height: calc(100vh - 60px);
}

.content-wrapper {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.filter-section {
  display: flex;
  justify-content: center;
}

.filter-tabs {
  display: inline-flex;
  background: #f0f0f0;
  padding: 5px;
  border-radius: 40px;
  gap: 5px;
}

.filter-tab {
  padding: 10px 25px;
  border-radius: 30px;
  cursor: pointer;
  font-weight: 600;
  color: #64748b;
  transition: all 0.3s;
}

.filter-tab.active {
  background: #ffffff;
  color: #2c3e50;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.map-entry-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border: 1px solid #dbeafe;
  background: linear-gradient(90deg, #eff6ff 0%, #f8fafc 100%);
  border-radius: 14px;
  padding: 16px 18px;
}

.map-entry-card h3 {
  margin: 0;
  font-size: 18px;
}

.map-entry-card p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 13px;
}

.animals-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 30px;
}

.pet-card {
  background: #ffffff;
  border-radius: 24px;
  overflow: hidden;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
  cursor: pointer;
}

.pet-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
}

.pet-img-wrapper {
  height: 240px;
  position: relative;
  overflow: hidden;
}

.pet-img-wrapper img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s;
}

.pet-card:hover .pet-img-wrapper img {
  transform: scale(1.05);
}

.pet-status-badge {
  position: absolute;
  top: 15px;
  left: 15px;
  padding: 4px 12px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: bold;
  color: #ffffff;
}

.pet-info {
  padding: 20px;
}

.pet-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.pet-header h3 {
  font-size: 20px;
  margin: 0;
}

.gender-icon {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #ffffff;
}

.gender-icon.male { background: #89CFF0; }
.gender-icon.female { background: #FFB7B2; }

.pet-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.tag {
  background: #f8f9fa;
  padding: 4px 10px;
  border-radius: 8px;
  font-size: 12px;
  color: #64748b;
}

.station-tag {
  background: #e6f7ff;
  color: #1890ff;
  cursor: pointer;
}

.station-tag:hover { background: #bae7ff; }

.pet-desc {
  font-size: 13px;
  color: #64748b;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.map-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.toolbar-text {
  color: #64748b;
  font-size: 13px;
}

.map-layout {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 16px;
}

.station-map {
  width: 100%;
  height: 62vh;
  min-height: 460px;
}

.station-panel {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 12px;
  overflow: auto;
  height: 62vh;
}

.station-panel h4 {
  margin: 0 0 10px;
}

.station-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.station-item {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 10px;
  cursor: pointer;
}

.station-item:hover {
  border-color: #60a5fa;
  background: #f8fbff;
}

.item-title {
  font-weight: 600;
  margin-bottom: 4px;
}

.item-meta,
.item-address,
.empty-tip {
  font-size: 12px;
  color: #64748b;
}

@media (max-width: 960px) {
  .map-layout {
    grid-template-columns: 1fr;
  }

  .station-panel,
  .station-map {
    height: 45vh;
    min-height: 340px;
  }

  .map-entry-card {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
