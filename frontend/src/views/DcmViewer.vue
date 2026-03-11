<template>
  <div class="dcm-viewer">
    <div class="viewer-header">
      <div class="header-left">
        <router-link to="/" class="back-btn">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M19 12H5M12 19l-7-7 7-7"/>
          </svg>
          返回
        </router-link>
        <h1>医学影像查看器</h1>
      </div>
      <div class="header-info" v-if="imageInfo">
        <span>{{ imageInfo.patientId || '-' }}</span>
        <span>{{ imageInfo.studyDate || '-' }}</span>
        <span>{{ imageInfo.modality || 'CT' }}</span>
      </div>
    </div>

    <div class="viewer-main">
      <div class="canvas-container" ref="containerRef">
        <canvas ref="canvasRef" @wheel="handleWheel" @mousedown="startDrag" @mousemove="handleDrag" @mouseup="endDrag" @mouseleave="endDrag"></canvas>
        <div class="loading-overlay" v-if="loading">
          <div class="spinner"></div>
          <p>加载中... {{ loadedCount }}/{{ totalCount }}</p>
        </div>
      </div>

      <div class="viewer-controls">
        <div class="control-section">
          <h3>窗宽窗位预设</h3>
          <div class="preset-grid">
            <button 
              v-for="preset in windowPresets" 
              :key="preset.name"
              :class="['preset-btn', { active: currentPreset === preset.name }]"
              @click="applyPreset(preset)"
            >
              {{ preset.name }}
            </button>
          </div>
        </div>

        <div class="control-section">
          <h3>手动调节</h3>
          <div class="slider-group">
            <label>窗宽 (Window Width): {{ windowWidth }}</label>
            <input type="range" :min="1" :max="4000" v-model.number="windowWidth" @input="applyManualWindow" />
          </div>
          <div class="slider-group">
            <label>窗位 (Window Center): {{ windowCenter }}</label>
            <input type="range" :min="-1000" :max="3000" v-model.number="windowCenter" @input="applyManualWindow" />
          </div>
        </div>

        <div class="control-section">
          <h3>图像操作</h3>
          <div class="action-btns">
            <button @click="resetView" class="action-btn">重置视图</button>
            <button @click="flipH" class="action-btn">水平翻转</button>
            <button @click="flipV" class="action-btn">垂直翻转</button>
            <button @click="rotate90" class="action-btn">旋转90°</button>
          </div>
        </div>

        <div class="control-section">
          <h3>切片浏览</h3>
          <div class="slider-group">
            <label>当前切片: {{ currentIndex + 1 }} / {{ totalCount }}</label>
            <input type="range" :min="0" :max="totalCount - 1" v-model.number="currentIndex" @input="loadSlice" />
          </div>
          <div class="slice-info" v-if="sliceInfo">
            <span>层厚: {{ sliceInfo.sliceThickness }}mm</span>
            <span>层号: {{ sliceInfo.instanceNumber }}</span>
          </div>
        </div>

        <div class="control-section">
          <h3>缩放</h3>
          <div class="zoom-controls">
            <button @click="zoomIn" class="zoom-btn">+</button>
            <span>{{ (scale * 100).toFixed(0) }}%</span>
            <button @click="zoomOut" class="zoom-btn">-</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import dicomParser from 'dicom-parser'
import cornerstoneCore from 'cornerstone-core'

const containerRef = ref(null)
const canvasRef = ref(null)
const loading = ref(true)
const loadedCount = ref(0)
const totalCount = ref(0)
const currentIndex = ref(0)
const windowWidth = ref(400)
const windowCenter = ref(40)
const currentPreset = ref('软组织')
const scale = ref(1)
const flipH = ref(false)
const flipV = ref(false)
const rotation = ref(0)
const imageInfo = ref(null)
const sliceInfo = ref(null)
const imageData = ref(null)

const windowPresets = [
  { name: '软组织', wc: 40, ww: 400 },
  { name: '肺窗', wc: -600, ww: 1500 },
  { name: '骨窗', wc: 300, ww: 1500 },
  { name: '脑组织', wc: 40, ww: 80 },
  { name: '纵隔', wc: 50, ww: 350 },
  { name: '肝脏', wc: 60, ww: 150 },
]

let dcmFiles = []
let isDragging = false
let lastX = 0
let lastY = 0
let translateX = 0
let translateY = 0
let ctx = null
let originalPixelData = null

const initCornerstone = () => {
  cornerstoneCore.enable(canvasRef.value)
}

const loadDcmList = async () => {
  const response = await fetch('/dcm/files.json')
  const data = await response.json()
  dcmFiles = data.files
  totalCount.value = dcmFiles.length
  loading.value = false
  await loadSlice()
}

const loadSlice = async () => {
  if (!dcmFiles.length || !canvasRef.value) return
  
  const filename = dcmFiles[currentIndex.value]
  try {
    const response = await fetch(`/dcm/${filename}`)
    const arrayBuffer = await response.arrayBuffer()
    
    const byteArray = new Uint8Array(arrayBuffer)
    const dataset = dicomParser.parseDicom(byteArray)
    
    const pixelDataElement = dataset.elements['7FE00010']
    if (!pixelDataElement) {
      console.error('No pixel data found')
      return
    }
    
    const pixelData = dicomParser.readPixelData(dataset)
    const rows = dataset.uint16('x00280010')
    const cols = dataset.uint16('x00280011')
    const bitsAllocated = dataset.uint16('x00280100')
    const bitsStored = dataset.uint16('x00280101')
    const rescaleIntercept = dataset.floatString('x00281052') || 0
    const rescaleSlope = dataset.floatString('x00281053') || 1
    
    originalPixelData = pixelData
    
    imageInfo.value = {
      patientId: dataset.string('x00100020'),
      studyDate: dataset.string('x00080020'),
      modality: dataset.string('x00080060'),
    }
    
    sliceInfo.value = {
      sliceThickness: dataset.floatString('x00180050') || '-',
      instanceNumber: dataset.string('x00200013') || currentIndex.value + 1,
    }
    
    imageData.value = {
      pixelData,
      rows,
      cols,
      bitsAllocated,
      rescaleIntercept,
      rescaleSlope,
    }
    
    renderImage()
  } catch (e) {
    console.error('Error loading DCM:', e)
  }
}

const applyLUT = (pixelData, rows, cols, ww, wc, slope, intercept) => {
  const output = new Uint8ClampedArray(rows * cols * 4)
  const minVal = wc - ww / 2
  const maxVal = wc + ww / 2
  
  for (let i = 0; i < pixelData.length; i++) {
    const val = pixelData[i] * slope + intercept
    let normalized = 0
    
    if (val <= minVal) normalized = 0
    else if (val >= maxVal) normalized = 255
    else normalized = ((val - minVal) / ww) * 255
    
    output[i * 4] = normalized
    output[i * 4 + 1] = normalized
    output[i * 4 + 2] = normalized
    output[i * 4 + 3] = 255
  }
  
  return output
}

const renderImage = () => {
  if (!imageData.value || !ctx) return
  
  const { pixelData, rows, cols, rescaleSlope, rescaleIntercept } = imageData.value
  const rendered = applyLUT(pixelData, rows, cols, windowWidth.value, windowCenter.value, rescaleSlope, rescaleIntercept)
  
  const canvas = canvasRef.value
  canvas.width = cols
  canvas.height = rows
  
  const imageDataObj = new ImageData(rendered, cols, rows)
  ctx.putImageData(imageDataObj, 0, 0)
  
  applyTransform()
}

const applyTransform = () => {
  if (!ctx || !canvasRef.value) return
  
  const canvas = canvasRef.value
  const tempCanvas = document.createElement('canvas')
  tempCanvas.width = canvas.width
  tempCanvas.height = canvas.height
  const tempCtx = tempCanvas.getContext('2d')
  tempCtx.drawImage(canvas, 0, 0)
  
  canvas.width = containerRef.value.clientWidth
  canvas.height = containerRef.value.clientHeight
  
  ctx.fillStyle = '#000'
  ctx.fillRect(0, 0, canvas.width, canvas.height)
  
  ctx.save()
  ctx.translate(canvas.width / 2 + translateX, canvas.height / 2 + translateY)
  ctx.scale(scale.value, scale.value)
  if (flipH.value) ctx.scale(-1, 1)
  if (flipV.value) ctx.scale(1, -1)
  ctx.rotate((rotation.value * Math.PI) / 180)
  ctx.drawImage(tempCanvas, -tempCanvas.width / 2, -tempCanvas.height / 2)
  ctx.restore()
}

const applyPreset = (preset) => {
  currentPreset.value = preset.name
  windowWidth.value = preset.ww
  windowCenter.value = preset.wc
  renderImage()
}

const applyManualWindow = () => {
  currentPreset.value = null
  renderImage()
}

const resetView = () => {
  scale.value = 1
  translateX = 0
  translateY = 0
  flipH.value = false
  flipV.value = false
  rotation.value = 0
  applyPreset({ ww: 400, wc: 40 })
}

const zoomIn = () => {
  scale.value = Math.min(scale.value * 1.2, 10)
  applyTransform()
}

const zoomOut = () => {
  scale.value = Math.max(scale.value / 1.2, 0.1)
  applyTransform()
}

const flipHAxis = () => {
  flipH.value = !flipH.value
  applyTransform()
}

const flipVAxis = () => {
  flipV.value = !flipV.value
  applyTransform()
}

const rotate90deg = () => {
  rotation.value = (rotation.value + 90) % 360
  applyTransform()
}

const handleWheel = (e) => {
  e.preventDefault()
  if (e.deltaY < 0) zoomIn()
  else zoomOut()
}

const startDrag = (e) => {
  isDragging = true
  lastX = e.clientX
  lastY = e.clientY
}

const handleDrag = (e) => {
  if (!isDragging) return
  translateX += e.clientX - lastX
  translateY += e.clientY - lastY
  lastX = e.clientX
  lastY = e.clientY
  applyTransform()
}

const endDrag = () => {
  isDragging = false
}

onMounted(() => {
  if (canvasRef.value) {
    ctx = canvasRef.value.getContext('2d')
    loadDcmList()
  }
})

onUnmounted(() => {
  if (canvasRef.value) {
    cornerstoneCore.disable(canvasRef.value)
  }
})
</script>

<style scoped>
.dcm-viewer {
  min-height: 100vh;
  background: #0a0a0a;
  color: #fff;
}

.viewer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 2rem;
  background: rgba(255, 255, 255, 0.05);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 1.5rem;
}

.header-left h1 {
  font-size: 1.25rem;
  font-weight: 600;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: #60a5fa;
  text-decoration: none;
  padding: 0.5rem 1rem;
  border-radius: 8px;
  background: rgba(96, 165, 250, 0.1);
  transition: all 0.2s;
}

.back-btn:hover {
  background: rgba(96, 165, 250, 0.2);
}

.back-btn svg {
  width: 18px;
  height: 18px;
}

.header-info {
  display: flex;
  gap: 1.5rem;
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.7);
}

.viewer-main {
  display: flex;
  height: calc(100vh - 70px);
}

.canvas-container {
  flex: 1;
  position: relative;
  background: #000;
  overflow: hidden;
  cursor: grab;
}

.canvas-container:active {
  cursor: grabbing;
}

.canvas-container canvas {
  display: block;
  max-width: 100%;
  max-height: 100%;
}

.loading-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.8);
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid rgba(255, 255, 255, 0.1);
  border-top-color: #60a5fa;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.viewer-controls {
  width: 320px;
  padding: 1.5rem;
  background: rgba(255, 255, 255, 0.03);
  border-left: 1px solid rgba(255, 255, 255, 0.1);
  overflow-y: auto;
}

.control-section {
  margin-bottom: 1.5rem;
}

.control-section h3 {
  font-size: 0.875rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 0.75rem;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.preset-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.5rem;
}

.preset-btn {
  padding: 0.5rem;
  font-size: 0.8rem;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  color: rgba(255, 255, 255, 0.8);
  cursor: pointer;
  transition: all 0.2s;
}

.preset-btn:hover {
  background: rgba(96, 165, 250, 0.1);
  border-color: rgba(96, 165, 250, 0.3);
}

.preset-btn.active {
  background: rgba(96, 165, 250, 0.2);
  border-color: #60a5fa;
  color: #60a5fa;
}

.slider-group {
  margin-bottom: 1rem;
}

.slider-group label {
  display: block;
  font-size: 0.75rem;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 0.5rem;
}

.slider-group input[type="range"] {
  width: 100%;
  height: 6px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 3px;
  outline: none;
  -webkit-appearance: none;
}

.slider-group input[type="range"]::-webkit-slider-thumb {
  -webkit-appearance: none;
  width: 14px;
  height: 14px;
  background: #60a5fa;
  border-radius: 50%;
  cursor: pointer;
}

.action-btns {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.5rem;
}

.action-btn {
  padding: 0.5rem;
  font-size: 0.8rem;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  color: rgba(255, 255, 255, 0.8);
  cursor: pointer;
  transition: all 0.2s;
}

.action-btn:hover {
  background: rgba(167, 139, 250, 0.1);
  border-color: rgba(167, 139, 250, 0.3);
}

.zoom-controls {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.zoom-btn {
  width: 36px;
  height: 36px;
  font-size: 1.25rem;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  color: #fff;
  cursor: pointer;
  transition: all 0.2s;
}

.zoom-btn:hover {
  background: rgba(96, 165, 250, 0.2);
  border-color: #60a5fa;
}

.slice-info {
  display: flex;
  gap: 1rem;
  font-size: 0.75rem;
  color: rgba(255, 255, 255, 0.5);
  margin-top: 0.5rem;
}
</style>
