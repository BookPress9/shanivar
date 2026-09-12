const links = [...document.querySelectorAll('.screen-link')];
const screens = [...document.querySelectorAll('.screen')];
const screenIds = screens.map((screen) => screen.dataset.screen);
const dots = document.getElementById('flowDots');
let currentIndex = 0;

screenIds.forEach((_, index) => {
  const dot = document.createElement('i');
  dot.setAttribute('aria-hidden', 'true');
  if (index === 0) dot.classList.add('active');
  dots.appendChild(dot);
});

function selectScreen(id, updateUrl = true) {
  const index = screenIds.indexOf(id);
  if (index < 0) return;
  currentIndex = index;
  screens.forEach((screen) => screen.classList.toggle('active', screen.dataset.screen === id));
  links.forEach((link) => link.classList.toggle('active', link.dataset.target === id));
  [...dots.children].forEach((dot, dotIndex) => dot.classList.toggle('active', dotIndex === index));

  const screen = screens[index];
  document.getElementById('annotationIndex').textContent = `SCREEN ${String(index + 1).padStart(2, '0')} / ${String(screens.length).padStart(2, '0')}`;
  document.getElementById('annotationPhase').textContent = screen.dataset.phase;
  document.getElementById('annotationTitle').textContent = screen.dataset.title;
  document.getElementById('annotationNote').textContent = screen.dataset.note;
  document.getElementById('annotationContract').textContent = screen.dataset.contract;

  if (updateUrl) history.replaceState(null, '', `?screen=${id}`);
}

links.forEach((link) => link.addEventListener('click', () => selectScreen(link.dataset.target)));
document.querySelectorAll('[data-go]').forEach((button) => button.addEventListener('click', () => selectScreen(button.dataset.go)));
document.getElementById('previous').addEventListener('click', () => selectScreen(screenIds[(currentIndex - 1 + screenIds.length) % screenIds.length]));
document.getElementById('next').addEventListener('click', () => selectScreen(screenIds[(currentIndex + 1) % screenIds.length]));

window.addEventListener('keydown', (event) => {
  if (event.key === 'ArrowLeft') selectScreen(screenIds[(currentIndex - 1 + screenIds.length) % screenIds.length]);
  if (event.key === 'ArrowRight') selectScreen(screenIds[(currentIndex + 1) % screenIds.length]);
});

const requested = new URLSearchParams(location.search).get('screen');
selectScreen(screenIds.includes(requested) ? requested : screenIds[0], false);
