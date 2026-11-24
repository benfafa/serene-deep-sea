const state = {
  coins: 1280,
  minutes: 25,
  timer: null,
};

const views = [...document.querySelectorAll('.view-panel')];
const navItems = [...document.querySelectorAll('.nav-item')];
const pageTitle = document.querySelector('#page-title');
const focusModal = document.querySelector('#focus-modal');
const modalDuration = document.querySelector('#modal-duration');
const toast = document.querySelector('#toast');
const titles = { aquarium: '我的水族箱', scenes: '场景图鉴', store: '鱼苗商店', settings: '禅定设置' };

function showView(viewName) {
  views.forEach((view) => view.classList.toggle('active', view.id === `view-${viewName}`));
  navItems.forEach((item) => item.classList.toggle('active', item.dataset.view === viewName));
  pageTitle.textContent = titles[viewName];
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function showToast(message) {
  toast.textContent = message;
  toast.classList.add('show');
  window.clearTimeout(showToast.timer);
  showToast.timer = window.setTimeout(() => toast.classList.remove('show'), 2600);
}

function updateCoins() {
  document.querySelector('#coin-balance').textContent = state.coins.toLocaleString('en-US');
  document.querySelector('#store-balance').textContent = state.coins.toLocaleString('en-US');
}

navItems.forEach((item) => item.addEventListener('click', () => showView(item.dataset.view)));
document.querySelectorAll('[data-view-target]').forEach((button) => button.addEventListener('click', () => showView(button.dataset.viewTarget)));

document.querySelectorAll('.preset').forEach((preset) => {
  preset.addEventListener('click', () => {
    document.querySelectorAll('.preset').forEach((item) => item.classList.remove('active'));
    preset.classList.add('active');
    state.minutes = Number(preset.dataset.minutes);
    modalDuration.textContent = `${state.minutes} 分钟`;
  });
});

document.querySelector('#start-focus').addEventListener('click', () => {
  modalDuration.textContent = `${state.minutes} 分钟`;
  focusModal.classList.add('open');
  focusModal.setAttribute('aria-hidden', 'false');
});

function closeModal() {
  focusModal.classList.remove('open');
  focusModal.setAttribute('aria-hidden', 'true');
}

document.querySelector('#close-focus').addEventListener('click', closeModal);
focusModal.addEventListener('click', (event) => {
  if (event.target === focusModal) closeModal();
});

document.querySelector('#confirm-focus').addEventListener('click', () => {
  closeModal();
  showToast(`已进入 ${state.minutes} 分钟禅定 · 锁定态预览已开启`);
  window.setTimeout(() => showView('settings'), 450);
});

document.querySelectorAll('.todo-list .check-button').forEach((button) => {
  button.addEventListener('click', () => {
    const item = button.closest('li');
    item.classList.toggle('done');
    button.textContent = item.classList.contains('done') ? '✓' : '';
    const done = document.querySelectorAll('.todo-list li.done').length;
    document.querySelector('.todo-progress > div:first-child span').textContent = `${done} / 4 完成`;
    document.querySelector('.todo-progress > div:first-child b').textContent = `${Math.round(done / 4 * 100)}%`;
    document.querySelector('.progress-track i').style.width = `${done / 4 * 100}%`;
  });
});

document.querySelector('#add-todo').addEventListener('click', () => {
  showToast('原型预览：这里会接入 Android 待办入口');
});

document.querySelectorAll('.buy-button, .price-button').forEach((button) => {
  button.addEventListener('click', () => {
    const cost = Number(button.dataset.cost);
    const item = button.dataset.item;
    if (state.coins < cost) {
      showToast('金币不足，完成一轮禅定后再来看看');
      return;
    }
    state.coins -= cost;
    updateCoins();
    showToast(`${item} 已投放到月光海沟 · -${cost} 金币`);
    button.textContent = '已投放 ✓';
    button.disabled = true;
  });
});

document.querySelectorAll('.duration-control button').forEach((button) => {
  button.addEventListener('click', () => {
    const delta = button.textContent === '+' ? 5 : -5;
    state.minutes = Math.min(120, Math.max(5, state.minutes + delta));
    document.querySelector('.duration-control strong').textContent = `${state.minutes} 分钟`;
    modalDuration.textContent = `${state.minutes} 分钟`;
    showToast(`本轮禅定调整为 ${state.minutes} 分钟`);
  });
});

document.querySelectorAll('.filter-tabs button').forEach((button) => {
  button.addEventListener('click', () => {
    document.querySelectorAll('.filter-tabs button').forEach((item) => item.classList.remove('active'));
    button.classList.add('active');
    showToast(`已切换到「${button.textContent}」场景`);
  });
});

function randomBetween(min, max) {
  return min + Math.random() * (max - min);
}

function setRandomTrajectory(element, options) {
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;
  const symmetric = element.classList.contains('real-jellyfish');
  let previousX = 0;
  const points = [1, 2, 3].map(() => {
    const x = randomBetween(options.xMin, options.xMax);
    const point = {
      x,
      y: randomBetween(options.yMin, options.yMax),
      rotation: randomBetween(-2.2, 2.2),
      scale: randomBetween(.97, 1.02),
      facing: symmetric || x >= previousX ? 1 : -1,
    };
    previousX = x;
    return point;
  });
  points.forEach((point, index) => {
    element.style.setProperty(`--x${index + 1}`, `${point.x.toFixed(1)}px`);
    element.style.setProperty(`--y${index + 1}`, `${point.y.toFixed(1)}px`);
    element.style.setProperty(`--r${index + 1}`, `${point.rotation.toFixed(2)}deg`);
    element.style.setProperty(`--s${index + 1}`, point.scale.toFixed(3));
    element.style.setProperty(`--f${index + 1}`, point.facing);
  });
  element.style.setProperty('--fish-duration', `${randomBetween(options.durationMin, options.durationMax).toFixed(0)}ms`);
  element.style.setProperty('--fish-delay', `${randomBetween(-options.durationMax, 0).toFixed(0)}ms`);
}

document.querySelectorAll('.real-clownfish, .real-tang, .real-clownfish-small').forEach((element) => {
  setRandomTrajectory(element, { xMin: -24, xMax: 95, yMin: -24, yMax: 24, durationMin: 14000, durationMax: 25000 });
});
document.querySelectorAll('.scene-fish').forEach((element) => {
  setRandomTrajectory(element, { xMin: -18, xMax: 48, yMin: -16, yMax: 16, durationMin: 8000, durationMax: 14000 });
});
document.querySelectorAll('.phone-real-fish, .modal-real-fish').forEach((element) => {
  setRandomTrajectory(element, { xMin: -16, xMax: 62, yMin: -14, yMax: 14, durationMin: 9000, durationMax: 16000 });
});
document.querySelectorAll('.real-jellyfish').forEach((element) => {
  setRandomTrajectory(element, { xMin: -18, xMax: 26, yMin: -26, yMax: 24, durationMin: 6000, durationMax: 10000 });
});

const fishInspector = document.querySelector('#fish-inspector');
const fishInspectorTitle = document.querySelector('#fish-inspector-title');
const fishInspectorDescription = document.querySelector('#fish-inspector-description');
const fishInspectorTemperament = document.querySelector('#fish-inspector-temperament');
const fishInspectorRarity = document.querySelector('#fish-inspector-rarity');
const fishInspectorWater = document.querySelector('#fish-inspector-water');
const fishInspectorBond = document.querySelector('#fish-inspector-bond');
const fishProfiles = {
  clownfish: { title: '小丑鱼', description: '会在珊瑚和海葵之间巡游的活泼住客。', temperament: '活泼', rarity: '常见', water: '浅海', bond: 78 },
  blueTang: { title: '蓝吊鱼', description: '喜欢开阔水域，会在水流里保持自己的节奏。', temperament: '好奇', rarity: '稀有', water: '外海', bond: 64 },
  jellyfish: { title: '月光水母', description: '随着水流缓慢漂浮的夜行住客，光线越暗越安静。', temperament: '安静', rarity: '传说', water: '深水', bond: 86 },
  angelfish: { title: '皇家神仙鱼', description: '会在礁石边缘缓慢巡航，喜欢稳定的水流。', temperament: '优雅', rarity: '稀有', water: '珊瑚礁', bond: 71 },
  yellowTang: { title: '黄尾刺尾鱼', description: '明亮的黄色住客，常在珊瑚边缘来回穿梭。', temperament: '明亮', rarity: '常见', water: '浅海', bond: 69 },
  lionfish: { title: '狮子鱼', description: '长鳍会随着水流展开，适合安静观察。', temperament: '缓慢', rarity: '传说', water: '礁石洞', bond: 59 },
  silverJack: { title: '银色鲹鱼', description: '喜欢和同伴保持距离一致的群游住客。', temperament: '群游', rarity: '常见', water: '外海', bond: 73 },
  pinkCoral: { title: '粉珊瑚鱼', description: '慢热的住客，喜欢珊瑚枝之间的阴影。', temperament: '慢热', rarity: '稀有', water: '珊瑚礁', bond: 68 },
  mandarin: { title: '曼达林鱼', description: '细小而华丽，喜欢在珊瑚底部寻找食物。', temperament: '谨慎', rarity: '传说', water: '珊瑚礁', bond: 82 },
  moorish: { title: '花马蹄鱼', description: '黑白黄相间的长鳍住客，喜欢慢慢巡游。', temperament: '优雅', rarity: '稀有', water: '外海', bond: 74 },
  butterflyfish: { title: '长吻蝶鱼', description: '会在珊瑚枝间穿梭，动作轻巧。', temperament: '灵巧', rarity: '稀有', water: '浅海', bond: 70 },
  pufferfish: { title: '绿斑河豚', description: '会用圆滚滚的身体观察周围的水流。', temperament: '好奇', rarity: '传说', water: '礁石洞', bond: 77 },
  royalGramma: { title: '皇家丝绒鱼', description: '紫黄渐变的迷你住客，适合小型珊瑚区。', temperament: '安静', rarity: '稀有', water: '珊瑚礁', bond: 66 },
  firefish: { title: '火焰虾虎鱼', description: '橙红色长鳍住客，喜欢洞穴边缘。', temperament: '警觉', rarity: '常见', water: '礁石洞', bond: 61 },
  discus: { title: '彩纹七彩神仙', description: '圆润的淡水住客，喜欢稳定缓慢的水流。', temperament: '温和', rarity: '稀有', water: '淡水', bond: 72 },
  seahorse: { title: '海马', description: '会用卷曲尾巴抓住水草，缓慢漂浮。', temperament: '安静', rarity: '传说', water: '海草湾', bond: 84 },
  scallop: { title: '扇贝', description: '会在沙地边缘轻轻开合贝壳，反射水面光。', temperament: '安静', rarity: '常见', water: '沙地', bond: 62 },
  mussel: { title: '蓝贻贝', description: '喜欢附着在礁石上，形成小型底栖群落。', temperament: '群居', rarity: '常见', water: '礁石', bond: 58 },
  oyster: { title: '牡蛎', description: '粗糙外壳里藏着柔和的珍珠光泽。', temperament: '安静', rarity: '稀有', water: '礁石', bond: 65 },
  abalone: { title: '鲍鱼', description: '彩虹色贝壳会在低光水域里反射细碎光线。', temperament: '慢热', rarity: '稀有', water: '礁石洞', bond: 76 },
  conch: { title: '海螺', description: '螺旋贝壳会在沙地上留下缓慢移动的轨迹。', temperament: '慢速', rarity: '常见', water: '沙地', bond: 56 },
  hermit: { title: '寄居蟹', description: '会背着贝壳探索水族房的底部角落。', temperament: '好奇', rarity: '常见', water: '沙地', bond: 63 },
};

function getFishProfile(element) {
  const source = element.getAttribute('src') || '';
  if (source.includes('clownfish')) return fishProfiles.clownfish;
  if (source.includes('blue-tang')) return fishProfiles.blueTang;
  if (source.includes('moon-jellyfish')) return fishProfiles.jellyfish;
  const productClass = element.className;
  if (productClass.includes('product-angelfish')) return fishProfiles.angelfish;
  if (productClass.includes('product-yellow')) return fishProfiles.yellowTang;
  if (productClass.includes('product-lionfish')) return fishProfiles.lionfish;
  if (productClass.includes('product-silver')) return fishProfiles.silverJack;
  if (productClass.includes('product-teal')) return fishProfiles.blueTang;
  if (productClass.includes('product-gold')) return fishProfiles.blueTang;
  if (productClass.includes('product-pink')) return fishProfiles.pinkCoral;
  if (productClass.includes('product-mandarin')) return fishProfiles.mandarin;
  if (productClass.includes('product-moorish')) return fishProfiles.moorish;
  if (productClass.includes('product-butterflyfish')) return fishProfiles.butterflyfish;
  if (productClass.includes('product-pufferfish')) return fishProfiles.pufferfish;
  if (productClass.includes('product-royal-gramma')) return fishProfiles.royalGramma;
  if (productClass.includes('product-firefish')) return fishProfiles.firefish;
  if (productClass.includes('product-discus')) return fishProfiles.discus;
  if (productClass.includes('product-seahorse')) return fishProfiles.seahorse;
  if (productClass.includes('product-scallop')) return fishProfiles.scallop;
  if (productClass.includes('product-mussel')) return fishProfiles.mussel;
  if (productClass.includes('product-oyster')) return fishProfiles.oyster;
  if (productClass.includes('product-abalone')) return fishProfiles.abalone;
  if (productClass.includes('product-conch')) return fishProfiles.conch;
  if (productClass.includes('product-hermit')) return fishProfiles.hermit;
  return fishProfiles.clownfish;
}

function openFishInspector(element) {
  const profile = getFishProfile(element);
  document.querySelectorAll('.real-fish, .real-jellyfish, .product-art').forEach((item) => item.classList.remove('fish-selected'));
  element.classList.add('fish-selected');
  fishInspectorTitle.textContent = profile.title;
  fishInspectorDescription.textContent = profile.description;
  fishInspectorTemperament.textContent = profile.temperament;
  fishInspectorRarity.textContent = profile.rarity;
  fishInspectorWater.textContent = profile.water;
  fishInspectorBond.textContent = profile.bond;
  fishInspector.classList.add('open');
  fishInspector.setAttribute('aria-hidden', 'false');
}

function closeFishInspector() {
  fishInspector.classList.remove('open');
  fishInspector.setAttribute('aria-hidden', 'true');
  document.querySelectorAll('.real-fish, .real-jellyfish, .product-art').forEach((item) => item.classList.remove('fish-selected'));
}

const interactiveFish = [...document.querySelectorAll('.real-fish, .real-jellyfish, .fish-product .product-art')];
interactiveFish.forEach((element) => {
  element.setAttribute('tabindex', '0');
  element.setAttribute('role', 'button');
  element.addEventListener('click', () => openFishInspector(element));
  element.addEventListener('keydown', (event) => {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      openFishInspector(element);
    }
  });
});
document.querySelector('#close-fish-inspector').addEventListener('click', closeFishInspector);
document.addEventListener('keydown', (event) => {
  if (event.key === 'Escape') closeFishInspector();
});

updateCoins();
