// 生成后端城市字典 src/main/resources/city-dict.json
// 数据源：前端 public/textures 下的 china-cities.json / china-provinces-geo.json / china-geo.json
// 逻辑与 src/data/cities.js 的 loadCityPoints() 完全一致，保证字典与前端可点亮城市一一对应。
// 用法：node scripts/generate-city-dict.js
const fs = require('fs');
const path = require('path');

const TEXTURES = path.join(__dirname, '..', 'frontend', 'public', 'textures');
const OUT = path.join(__dirname, '..', 'src', 'main', 'resources', 'city-dict.json');

// 与前端 src/data/cities.js 的 MUNI_POINTS 保持一致
const MUNI_POINTS = [
  { name: '北京', lng: 116.41, lat: 39.90, adcode: '110000' },
  { name: '上海', lng: 121.47, lat: 31.23, adcode: '310000' },
  { name: '天津', lng: 117.19, lat: 39.13, adcode: '120000' },
  { name: '重庆', lng: 106.55, lat: 29.56, adcode: '500000' },
  { name: '香港', lng: 114.17, lat: 22.32, adcode: '810000' },
  { name: '澳门', lng: 113.55, lat: 22.19, adcode: '820000' },
  { name: '台湾', lng: 121.51, lat: 25.04, adcode: '710000' }
];

const muniSet = new Set(MUNI_POINTS.map(p => p.adcode));

const provGeo = JSON.parse(fs.readFileSync(path.join(TEXTURES, 'china-provinces-geo.json'), 'utf8'));
const china = JSON.parse(fs.readFileSync(path.join(TEXTURES, 'china-geo.json'), 'utf8'));
const cities = JSON.parse(fs.readFileSync(path.join(TEXTURES, 'china-cities.json'), 'utf8'));

// 地级市名 -> adcode（排除直辖市/特区，它们被 MUNI_POINTS 单独注入）
const regionByName = new Map();
Object.entries(provGeo).forEach(([provCode, fc]) => {
  if (muniSet.has(provCode)) return;
  (fc.features || []).forEach(f => {
    const name = f.properties && f.properties.name;
    const ad = String((f.properties && f.properties.adcode) || '');
    if (name && ad && !regionByName.has(name)) regionByName.set(name, ad);
  });
});

// 省级 adcode -> 省名
const provinceByCode = new Map();
china.features.forEach(f => provinceByCode.set(String(f.properties.adcode), f.properties.name));

const dict = {};
cities.forEach(c => {
  const ad = regionByName.get(c.name) || '';
  if (!ad) return;
  dict[ad] = {
    name: c.name,
    province: provinceByCode.get(ad.slice(0, 2) + '0000') || ''
  };
});
MUNI_POINTS.forEach(p => {
  dict[p.adcode] = {
    name: p.name,
    province: provinceByCode.get(p.adcode) || ''
  };
});

fs.writeFileSync(OUT, JSON.stringify(dict));
console.log('生成完成:', OUT);
console.log('城市总数:', Object.keys(dict).length);
console.log('样例:', JSON.stringify(Object.entries(dict).slice(0, 6)));
