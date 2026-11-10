/* =========================================================================
 * app.js — Lógica del visor UML interactivo
 * ========================================================================= */
(function () {
  'use strict';

  var NS = 'http://www.w3.org/2000/svg';
  var META = window.UML_META;
  var PACKAGES = window.UML_PACKAGES;
  var CLASSES = window.UML_CLASSES;
  var RELATIONS = window.UML_RELATIONS;
  var PKG = {};
  var byId = {};
  PACKAGES.forEach(function (p) { PKG[p.id] = p; });
  CLASSES.forEach(function (c) { byId[c.id] = c; });

  // Código fuente real: lista plana de archivos, en el orden de la exposición
  var CODE = window.UML_CODE || [];
  var FILES = [];
  var fileOfClass = {};   // clase del UML -> su archivo .java
  var daoOfPkg = {};      // paquete -> su DAO
  CODE.forEach(function (g) {
    var color = g.color || (PKG[g.id] && PKG[g.id].color) || '#94a0b0';
    g.archivos.forEach(function (f) {
      f.grupo = g.label;
      f.color = color;
      f.index = FILES.length;
      FILES.push(f);
      if (f.clase) fileOfClass[f.clase] = f;
      if (f.tipo === 'dao') daoOfPkg[g.id] = f;
    });
  });

  var CARD_W = 340;
  var MIN_Z = 0.35, MAX_Z = 2.4;

  var state = {
    zoom: 0.9, panX: 60, panY: 40,
    selected: null, filter: 'all', query: '',
    dragging: null, dragDX: 0, dragDY: 0,
    panning: false, panStartX: 0, panStartY: 0, gestureRect: null,
    codeTab: 'clase', theme: 'dark', openFile: null
  };

  function $(s, r) { return (r || document).querySelector(s); }
  function $$(s, r) { return Array.prototype.slice.call((r || document).querySelectorAll(s)); }
  function el(tag, cls) { var n = document.createElement(tag); if (cls) n.className = cls; return n; }
  function esc(s) { return String(s).replace(/[&<>"]/g, function (m) { return ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' })[m]; }); }

  var dom = {};

  /* ----------------------- Resaltado de código ----------------------- */
  var JAVA_KW = ('abstract boolean break byte case catch char class continue default do double else enum extends ' +
    'final finally float for if implements import instanceof int interface long new package private protected ' +
    'public return short static super switch this throw throws try void while null true false var').split(' ');
  var SQL_KW = ('SET NAMES CREATE DATABASE IF NOT EXISTS CHARACTER COLLATE USE DROP TABLE PRIMARY KEY FOREIGN ' +
    'REFERENCES ON DELETE CASCADE AUTO_INCREMENT NULL DEFAULT UNIQUE INSERT INTO VALUES TRUE FALSE INT VARCHAR ' +
    'DECIMAL BOOLEAN DATE TIME DATETIME TEXT').split(' ');

  function tokenRegex(lang) {
    if (lang === 'sql') {
      return new RegExp('(--[^\\n]*)|(\'(?:\\\\.|[^\'\\\\])*\')|\\b(\\d+(?:\\.\\d+)?)\\b|\\b(' +
        SQL_KW.join('|') + ')\\b|\\b(SHA2|CURDATE|NOW)\\b', 'g');
    }
    return new RegExp('(\\/\\*[\\s\\S]*?\\*\\/|\\/\\/[^\\n]*)|("(?:\\\\.|[^"\\\\])*"|\'(?:\\\\.|[^\'\\\\])\')|' +
      '\\b(\\d+(?:\\.\\d+)?)\\b|\\b(' + JAVA_KW.join('|') + ')\\b|\\b([A-Z][A-Za-z0-9_]*)\\b|(@[A-Za-z]+)', 'g');
  }

  // Devuelve un arreglo de líneas en HTML con colores
  function highlight(code, lang) {
    var clases = lang === 'sql'
      ? [null, 'tk-com', 'tk-str', 'tk-num', 'tk-kw', 'tk-fn']
      : [null, 'tk-com', 'tk-str', 'tk-num', 'tk-kw', 'tk-ty', 'tk-ann'];
    var re = tokenRegex(lang);
    var lines = [''];
    function push(text, cls) {
      var parts = text.split('\n');
      parts.forEach(function (part, i) {
        if (i > 0) lines.push('');
        if (!part) return;
        lines[lines.length - 1] += cls ? '<span class="' + cls + '">' + esc(part) + '</span>' : esc(part);
      });
    }
    var last = 0, m;
    while ((m = re.exec(code)) !== null) {
      if (m.index > last) push(code.slice(last, m.index), null);
      for (var g = 1; g < m.length; g++) {
        if (m[g] !== undefined) { push(m[0], clases[g]); break; }
      }
      last = re.lastIndex;
    }
    if (last < code.length) push(code.slice(last), null);
    if (lines.length > 1 && lines[lines.length - 1] === '') lines.pop();
    return lines;
  }

  function codeHtml(f) {
    return highlight(f.codigo, f.lenguaje).map(function (l, i) {
      return '<div class="cl"><span class="ln" data-n="' + (i + 1) + '"></span><span class="lc">' + l + '</span></div>';
    }).join('');
  }

  function lineCount(f) { return f.codigo.replace(/\n$/, '').split('\n').length; }

  function copyText(text) {
    var ta = document.createElement('textarea');
    ta.value = text;
    document.body.appendChild(ta);
    ta.select();
    try { document.execCommand('copy'); toast('Código copiado'); }
    catch (e) { toast('No se pudo copiar'); }
    document.body.removeChild(ta);
  }

  /* ----------------------- Sidebar ----------------------- */
  function buildSidebar() {
    var list = $('#pkgList');
    list.innerHTML = '';
    list.appendChild(pkgButton('all', 'Todos', CLASSES.length, true));
    PACKAGES.forEach(function (p) {
      var n = CLASSES.filter(function (c) { return c.package === p.id; }).length;
      list.appendChild(pkgButton(p.id, p.label, n, false));
    });
    buildIndex();
    buildCodeIndex();
  }

  /* ----------------------- Índice de código fuente ----------------------- */
  var TIPO_TAG = { dao: 'DAO', web: 'web', sql: 'SQL', main: 'main', conexion: 'BD' };

  function buildCodeIndex() {
    var idx = $('#codeIndex');
    if (!idx) return;
    idx.innerHTML = '';
    var q = state.query;
    var shown = 0;
    CODE.forEach(function (g) {
      var items = g.archivos.filter(function (f) {
        return !q || f.nombre.toLowerCase().indexOf(q) !== -1 || f.codigo.toLowerCase().indexOf(q) !== -1;
      });
      if (!items.length) return;
      var group = el('div', 'idx-group');
      var head = el('div', 'idx-pkg');
      head.style.setProperty('--pkg', items[0].color);
      head.innerHTML = '<span class="sw"></span>' + esc(g.label);
      group.appendChild(head);
      items.forEach(function (f) {
        var b = el('button', 'idx-item file-item' + (state.openFile === f.index ? ' active' : ''));
        b.dataset.file = f.index;
        b.style.setProperty('--pkg', f.color);
        b.title = f.ruta;
        b.innerHTML = '<span class="file-name">' + esc(f.nombre) + '</span>' +
          (TIPO_TAG[f.tipo] ? '<span class="file-tag">' + TIPO_TAG[f.tipo] + '</span>' : '');
        b.addEventListener('click', function () { openFile(f.index); });
        group.appendChild(b);
        shown++;
      });
      idx.appendChild(group);
    });
    if (!shown) idx.innerHTML = '<div class="idx-empty">Sin coincidencias.</div>';
    var count = $('#codeCount');
    if (count) count.textContent = FILES.length + ' archivos';
  }

  function syncCodeIndexActive() {
    $$('.file-item').forEach(function (b) {
      b.classList.toggle('active', Number(b.dataset.file) === state.openFile);
    });
  }

  /* ----------------------- Visor de código ----------------------- */
  function openFile(i) {
    var f = FILES[i];
    if (!f) return;
    state.openFile = i;
    dom.viewer.hidden = false;
    dom.viewer.style.setProperty('--pkg', f.color);
    $('#cvName').textContent = f.nombre;
    $('#cvPath').textContent = f.ruta;
    $('#cvMeta').innerHTML = '<span class="chip pkg">' + esc(f.grupo) + '</span>' +
      '<span class="chip">' + lineCount(f) + ' líneas</span>';
    $('#cvPos').textContent = (i + 1) + ' / ' + FILES.length;
    $('#cvDiagram').style.display = f.clase ? '' : 'none';
    $('#cvCode').innerHTML = codeHtml(f);
    $('.cv-body', dom.viewer).scrollTop = 0;
    if (window.innerWidth < 900) dom.sidebar.classList.add('collapsed'); // en celular, que no tape el código
    syncCodeIndexActive();
    var active = $('.file-item.active');
    if (active && active.scrollIntoView) active.scrollIntoView({ block: 'nearest' });
  }

  function closeViewer() {
    if (dom.viewer.hidden) return;
    dom.viewer.hidden = true;
    state.openFile = null;
    syncCodeIndexActive();
  }

  function stepFile(dir) {
    if (state.openFile === null) return;
    openFile((state.openFile + dir + FILES.length) % FILES.length);
  }

  function bindViewer() {
    // Que el visor no mueva ni haga zoom al diagrama que está debajo
    ['mousedown', 'click', 'wheel', 'touchstart', 'touchmove'].forEach(function (ev) {
      dom.viewer.addEventListener(ev, function (e) { e.stopPropagation(); }, { passive: true });
    });
    $('#cvClose').addEventListener('click', closeViewer);
    $('#cvPrev').addEventListener('click', function () { stepFile(-1); });
    $('#cvNext').addEventListener('click', function () { stepFile(1); });
    $('#cvCopy').addEventListener('click', function () {
      if (state.openFile !== null) copyText(FILES[state.openFile].codigo);
    });
    $('#cvDiagram').addEventListener('click', function () {
      var f = FILES[state.openFile];
      if (f && f.clase) focusClass(f.clase);
    });
  }

  function pkgButton(id, label, count, isAll) {
    var b = el('button', 'pkg-item' + (isAll ? ' all' : ''));
    b.dataset.pkg = id;
    if (!isAll) b.style.setProperty('--pkg', PKG[id].color);
    b.innerHTML = '<span class="sw"></span><span class="pkg-name">' + esc(label) +
      '</span><span class="pkg-count">' + count + '</span>';
    if (state.filter === id) b.classList.add('active');
    b.addEventListener('click', function () { setFilter(id); });
    return b;
  }

  function buildIndex() {
    var idx = $('#classIndex');
    idx.innerHTML = '';
    var q = state.query;
    PACKAGES.forEach(function (p) {
      var items = CLASSES.filter(function (c) {
        return c.package === p.id && (!q || matches(c, q));
      });
      if (!items.length) return;
      var group = el('div', 'idx-group');
      var head = el('div', 'idx-pkg');
      head.style.setProperty('--pkg', p.color);
      head.innerHTML = '<span class="sw"></span>' + esc(p.label);
      group.appendChild(head);
      items.forEach(function (c) {
        var b = el('button', 'idx-item' + (state.selected === c.id ? ' active' : ''));
        b.dataset.id = c.id;
        b.style.setProperty('--pkg', p.color);
        b.innerHTML = esc(c.name) + (c.isAbstract ? '<span class="abstract-tag">abstract</span>' : '');
        b.addEventListener('click', function () { focusClass(c.id); });
        group.appendChild(b);
      });
      idx.appendChild(group);
    });
    if (!idx.children.length) idx.innerHTML = '<div class="idx-empty">Sin coincidencias.</div>';
  }

  function syncIndexActive() {
    $$('.idx-item').forEach(function (b) {
      b.classList.toggle('active', b.dataset.id === state.selected);
    });
  }

  function matches(c, q) {
    if (c.name.toLowerCase().indexOf(q) !== -1) return true;
    if (c.package.toLowerCase().indexOf(q) !== -1) return true;
    for (var i = 0; i < c.attributes.length; i++) {
      var a = c.attributes[i];
      if (a.name.toLowerCase().indexOf(q) !== -1 || a.type.toLowerCase().indexOf(q) !== -1) return true;
    }
    for (var j = 0; j < c.methods.length; j++) {
      if (c.methods[j].name.toLowerCase().indexOf(q) !== -1) return true;
    }
    return false;
  }

  /* ----------------------- Nodes ----------------------- */
  function buildNodes() {
    dom.nodes.innerHTML = '';
    CLASSES.forEach(function (c, i) {
      var p = PKG[c.package];
      var card = el('div', 'node');
      card.id = 'node-' + c.id;
      card.dataset.id = c.id;
      card.style.left = c.x + 'px';
      card.style.top = c.y + 'px';
      card.style.setProperty('--pkg', p.color);
      card.style.animationDelay = (i * 26) + 'ms';

      function vis(v) {
        if (v === '+') return '<span class="vis vis-plus">+</span>';
        if (v === '#') return '<span class="vis vis-hash">#</span>';
        return '<span class="vis vis-minus">-</span>';
      }

      var attrs = c.attributes.map(function (a) {
        return '<div class="node-row">' + vis(a.vis) + '<span class="mn">' + esc(a.name) +
          '</span><span class="sep">:</span><span class="ty">' + esc(a.type) + '</span></div>';
      }).join('');

      var meths = c.methods.map(function (m) {
        return '<div class="node-row' + (m.isAbstract ? ' abstract' : '') + '">' + vis(m.vis) +
          '<span class="mn">' + esc(m.name) + '</span><span class="sep">(</span><span class="ty">' +
          esc(m.params) + '</span><span class="sep">) :</span><span class="rt">' + esc(m.returnType) + '</span></div>';
      }).join('');

      card.innerHTML =
        '<div class="node-head">' +
          '<span class="sw"></span>' +
          '<div class="node-titles">' +
            (c.isAbstract ? '<span class="node-stereo">&laquo;abstract&raquo;</span>' : '') +
            '<span class="node-name' + (c.isAbstract ? ' abstract' : '') + '">' + esc(c.name) + '</span>' +
          '</div>' +
          '<span class="node-pkg">' + esc(c.package) + '</span>' +
        '</div>' +
        '<div class="node-attrs"><div class="node-section-title">atributos</div>' + attrs + '</div>' +
        '<div class="node-methods"><div class="node-section-title">métodos</div>' + meths + '</div>';

      card.addEventListener('mousedown', function (e) { startDrag(e, c); });
      card.addEventListener('click', function (e) { e.stopPropagation(); selectClass(c.id); });
      dom.nodes.appendChild(card);
    });
  }

  function boxOf(id) {
    var c = byId[id];
    var n = document.getElementById('node-' + id);
    var w = n ? n.offsetWidth : CARD_W;
    var h = n ? n.offsetHeight : 240;
    return { x: c.x, y: c.y, w: w, h: h, cx: c.x + w / 2, cy: c.y + h / 2 };
  }

  function nodeVisible(c) {
    var okPkg = state.filter === 'all' || c.package === state.filter;
    var okQ = !state.query || matches(c, state.query);
    return okPkg && okQ;
  }

  /* ----------------------- Edges ----------------------- */
  function edgePoints(a, b) {
    var dx = b.cx - a.cx, dy = b.cy - a.cy;
    var s, e, pts;
    if (Math.abs(dx) >= Math.abs(dy)) {
      s = { x: dx > 0 ? a.x + a.w : a.x, y: a.cy };
      e = { x: dx > 0 ? b.x : b.x + b.w, y: b.cy };
      var mx = (s.x + e.x) / 2;
      pts = [s, { x: mx, y: s.y }, { x: mx, y: e.y }, e];
    } else {
      s = { x: a.cx, y: dy > 0 ? a.y + a.h : a.y };
      e = { x: b.cx, y: dy > 0 ? b.y : b.y + b.h };
      var my = (s.y + e.y) / 2;
      pts = [s, { x: s.x, y: my }, { x: e.x, y: my }, e];
    }
    return pts;
  }

  function roundedPath(input, r) {
    var p = [];
    input.forEach(function (q) {
      var last = p[p.length - 1];
      if (!last || Math.abs(last.x - q.x) > 0.5 || Math.abs(last.y - q.y) > 0.5) p.push(q);
    });
    if (p.length < 2) return '';
    var d = 'M ' + p[0].x + ' ' + p[0].y;
    for (var i = 1; i < p.length - 1; i++) {
      var a = p[i - 1], b = p[i], c = p[i + 1];
      var d1 = Math.hypot(a.x - b.x, a.y - b.y);
      var d2 = Math.hypot(c.x - b.x, c.y - b.y);
      if (d1 < 0.5 || d2 < 0.5) continue;
      var rr = Math.min(r, d1 / 2, d2 / 2);
      var u1x = (a.x - b.x) / d1, u1y = (a.y - b.y) / d1;
      var u2x = (c.x - b.x) / d2, u2y = (c.y - b.y) / d2;
      var pa = { x: b.x + u1x * rr, y: b.y + u1y * rr };
      var pb = { x: b.x + u2x * rr, y: b.y + u2y * rr };
      d += ' L ' + pa.x + ' ' + pa.y + ' Q ' + b.x + ' ' + b.y + ' ' + pb.x + ' ' + pb.y;
    }
    var lastP = p[p.length - 1];
    d += ' L ' + lastP.x + ' ' + lastP.y;
    return d;
  }

  function polylineMidpoint(pts) {
    var total = 0, i;
    for (i = 1; i < pts.length; i++) total += Math.hypot(pts[i].x - pts[i - 1].x, pts[i].y - pts[i - 1].y);
    var half = total / 2;
    for (i = 1; i < pts.length; i++) {
      var seg = Math.hypot(pts[i].x - pts[i - 1].x, pts[i].y - pts[i - 1].y);
      if (half <= seg || i === pts.length - 1) {
        var t = seg === 0 ? 0 : half / seg;
        return { x: pts[i - 1].x + (pts[i].x - pts[i - 1].x) * t, y: pts[i - 1].y + (pts[i].y - pts[i - 1].y) * t };
      }
      half -= seg;
    }
    return pts[pts.length - 1];
  }

  function alongFrom(p, q, dist) {
    var len = Math.hypot(q.x - p.x, q.y - p.y) || 1;
    return { x: p.x + (q.x - p.x) / len * dist, y: p.y + (q.y - p.y) / len * dist };
  }

  function addLabel(text, pos, cls, hot) {
    var n = el('div', cls + (hot ? ' hot' : ''));
    n.style.left = pos.x + 'px';
    n.style.top = pos.y + 'px';
    n.textContent = text;
    dom.labels.appendChild(n);
  }

  function renderEdges() {
    dom.edges.innerHTML = '';
    dom.labels.innerHTML = '';
    RELATIONS.forEach(function (r) {
      var cs = byId[r.source], ct = byId[r.target];
      if (!nodeVisible(cs) || !nodeVisible(ct)) return;

      var a = boxOf(r.source), b = boxOf(r.target);
      var pts = edgePoints(a, b);
      var d = roundedPath(pts, 16);
      var hot = state.selected && (state.selected === r.source || state.selected === r.target);

      var path = document.createElementNS(NS, 'path');
      path.setAttribute('d', d);
      path.setAttribute('fill', 'none');
      path.setAttribute('class', 'edge ' + r.type + (hot ? ' hot' : ''));
      if (r.type === 'generalization') {
        path.setAttribute('marker-end', 'url(#' + (hot ? 'm-gen-on' : 'm-gen') + ')');
      } else if (r.type === 'composition') {
        path.setAttribute('marker-start', 'url(#' + (hot ? 'm-comp-on' : 'm-comp') + ')');
      } else {
        path.setAttribute('marker-end', 'url(#' + (hot ? 'm-assoc-on' : 'm-assoc') + ')');
      }
      dom.edges.appendChild(path);

      var next = pts[1] || pts[0];
      var prev = pts[pts.length - 2] || pts[pts.length - 1];

      if (r.label) addLabel(r.label, polylineMidpoint(pts), 'edge-label', hot);
      if (r.cardSource) addLabel(r.cardSource, alongFrom(pts[0], next, 24), 'edge-mult', hot);
      if (r.cardTarget) addLabel(r.cardTarget, alongFrom(pts[pts.length - 1], prev, 24), 'edge-mult', hot);
    });
  }

  /* ----------------------- View transform ----------------------- */
  function updateWorldSize() {
    var maxX = 0, maxY = 0;
    CLASSES.forEach(function (c) {
      var b = boxOf(c.id);
      maxX = Math.max(maxX, b.x + b.w);
      maxY = Math.max(maxY, b.y + b.h);
    });
    var W = maxX + 120, H = maxY + 120;
    dom.world.style.width = W + 'px';
    dom.world.style.height = H + 'px';
    dom.edges.setAttribute('width', W);
    dom.edges.setAttribute('height', H);
    dom.edges.setAttribute('viewBox', '0 0 ' + W + ' ' + H);
  }

  function applyTransform() {
    dom.world.style.transform = 'translate(' + state.panX + 'px,' + state.panY + 'px) scale(' + state.zoom + ')';
    dom.zoomValue.textContent = Math.round(state.zoom * 100) + '%';
  }

  function setZoom(z, cx, cy) {
    z = Math.min(MAX_Z, Math.max(MIN_Z, z));
    if (cx != null && cy != null) {
      var f = z / state.zoom;
      state.panX = cx - (cx - state.panX) * f;
      state.panY = cy - (cy - state.panY) * f;
    }
    state.zoom = z;
    applyTransform();
  }

  function worldPoint(clientX, clientY) {
    var r = dom.canvas.getBoundingClientRect();
    return {
      x: (clientX - r.left - state.panX) / state.zoom,
      y: (clientY - r.top - state.panY) / state.zoom
    };
  }

  function fitView() {
    var vis = CLASSES.filter(nodeVisible);
    if (!vis.length) return;
    var minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
    vis.forEach(function (c) {
      var b = boxOf(c.id);
      minX = Math.min(minX, b.x); minY = Math.min(minY, b.y);
      maxX = Math.max(maxX, b.x + b.w); maxY = Math.max(maxY, b.y + b.h);
    });
    var r = dom.canvas.getBoundingClientRect();
    var pad = 56;
    var z = Math.min((r.width - pad * 2) / (maxX - minX), (r.height - pad * 2) / (maxY - minY), 1.35);
    state.zoom = Math.min(MAX_Z, Math.max(MIN_Z, z));
    state.panX = (r.width - (maxX - minX) * state.zoom) / 2 - minX * state.zoom;
    state.panY = (r.height - (maxY - minY) * state.zoom) / 2 - minY * state.zoom;
    applyTransform();
  }

  function focusClass(id) {
    closeViewer();
    selectClass(id);
    var b = boxOf(id);
    var r = dom.canvas.getBoundingClientRect();
    state.panX = r.width / 2 - b.cx * state.zoom;
    state.panY = r.height / 2 - b.cy * state.zoom;
    applyTransform();
  }

  /* ----------------------- Selection ----------------------- */
  function selectClass(id) {
    state.selected = id;
    $$('.node').forEach(function (n) {
      if (n.dataset.id === id) {
        n.classList.add('selected');
        n.classList.remove('dimmed');
      } else {
        n.classList.remove('selected');
        var connected = RELATIONS.some(function (r) {
          return (r.source === id && r.target === n.dataset.id) ||
                 (r.target === id && r.source === n.dataset.id);
        });
        n.classList.toggle('dimmed', !connected);
      }
    });
    renderEdges();
    renderInspector(byId[id]);
    openInspector();
    syncIndexActive();
  }

  function deselect() {
    state.selected = null;
    $$('.node').forEach(function (n) { n.classList.remove('selected', 'dimmed'); });
    renderEdges();
    renderInspector(null);
    syncIndexActive();
  }

  /* ----------------------- Filters ----------------------- */
  function setFilter(pkg) {
    state.filter = pkg;
    $$('.pkg-item').forEach(function (b) { b.classList.toggle('active', b.dataset.pkg === pkg); });
    applyVisibility();
  }

  function applyVisibility() {
    $$('.node').forEach(function (n) {
      n.style.display = nodeVisible(byId[n.dataset.id]) ? '' : 'none';
    });
    renderEdges();
  }

  /* ----------------------- Inspector ----------------------- */
  function openInspector() { dom.inspector.classList.remove('closed'); }
  function closeInspector() { dom.inspector.classList.add('closed'); }

  function renderInspector(c) {
    if (!c) {
      dom.inspBody.style.removeProperty('--pkg');
      dom.inspBody.innerHTML =
        '<div class="insp-empty">' +
          '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path stroke-linecap="round" stroke-linejoin="round" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"/></svg>' +
          '<p>Selecciona una clase del diagrama o del índice para inspeccionar sus atributos, métodos y relaciones.</p>' +
        '</div>';
      return;
    }
    var p = PKG[c.package];
    dom.inspBody.style.setProperty('--pkg', p.color);

    var related = RELATIONS.filter(function (r) { return r.source === c.id || r.target === c.id; });

    var attrs = c.attributes.map(function (a) {
      var vc = a.vis === '+' ? 'vis-plus' : a.vis === '#' ? 'vis-hash' : 'vis-minus';
      return '<div class="list-row"><div class="left"><span class="vis ' + vc + '">' + a.vis +
        '</span><span>' + esc(a.name) + '</span></div><span class="r-ty">' + esc(a.type) + '</span></div>';
    }).join('') || '<div class="list-row"><span class="r-ty">—</span></div>';

    var meths = c.methods.map(function (m) {
      var vc = m.vis === '+' ? 'vis-plus' : m.vis === '#' ? 'vis-hash' : 'vis-minus';
      return '<div class="list-row" style="display:block"><div class="left"><span class="vis ' + vc + '">' + m.vis +
        '</span><span class="' + (m.isAbstract ? 'meth-abstract' : '') + '">' + esc(m.name) + '</span>' +
        '<span class="r-ty" style="margin-left:auto">' + esc(m.returnType) + '</span></div>' +
        '<span class="meth-params">(' + (esc(m.params) || 'sin parámetros') + ')</span></div>';
    }).join('') || '<div class="list-row"><span class="r-ty">—</span></div>';

    var relHtml = related.map(function (r) {
      var isSource = r.source === c.id;
      var other = isSource ? r.target : r.source;
      var card = isSource
        ? (r.cardSource || '') + ' → ' + (r.cardTarget || '')
        : (r.cardTarget || '') + ' ← ' + (r.cardSource || '');
      return '<div class="rel-row" data-goto="' + other + '">' +
        '<div><div class="rel-type">' + r.type + '</div>' +
        '<div class="rel-target">' + esc(other) +
        (r.label ? ' <span style="color:var(--muted);font-size:10px">"' + esc(r.label) + '"</span>' : '') +
        '</div></div><span class="rel-card">' + card + '</span></div>';
    }).join('') || '<div class="list-row"><span class="r-ty">Sin conexiones.</span></div>';

    dom.inspBody.innerHTML =
      '<div class="insp-head">' +
        '<span class="sw"></span>' +
        '<div class="insp-title"><h2>' + esc(c.name) + '</h2>' +
          '<div class="meta"><span class="chip pkg">' + esc(c.package) + '</span>' +
          (c.isAbstract ? '<span class="chip accent">abstract</span>' : '') + '</div>' +
        '</div>' +
        '<button class="insp-close" id="inspClose" title="Cerrar">' +
          '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" d="M6 18L18 6M6 6l12 12"/></svg>' +
        '</button>' +
      '</div>' +
      '<div class="insp-stats">' +
        '<div class="stat"><div class="v">' + c.attributes.length + '</div><div class="k">atributos</div></div>' +
        '<div class="stat"><div class="v">' + c.methods.length + '</div><div class="k">métodos</div></div>' +
        '<div class="stat"><div class="v">' + related.length + '</div><div class="k">relaciones</div></div>' +
      '</div>' +
      '<div class="block"><h3><span class="dot dot-attr"></span>Atributos</h3><div class="list">' + attrs + '</div></div>' +
      '<div class="block"><h3><span class="dot dot-meth"></span>Métodos</h3><div class="list">' + meths + '</div></div>' +
      '<div class="block"><h3><span class="dot dot-rel"></span>Relaciones</h3><div class="list">' + relHtml + '</div></div>' +
      codeBlock(c);

    $('#inspClose').addEventListener('click', closeInspector);
    $$('.rel-row', dom.inspBody).forEach(function (row) {
      row.addEventListener('click', function () { focusClass(row.dataset.goto); });
    });
    bindCodeBlock(c);
  }

  // Bloque "Código" del inspector: el archivo real de la clase y el DAO de su paquete
  function codeFiles(c) {
    var tabs = [];
    if (fileOfClass[c.id]) tabs.push({ key: 'clase', file: fileOfClass[c.id] });
    if (daoOfPkg[c.package]) tabs.push({ key: 'dao', file: daoOfPkg[c.package] });
    return tabs;
  }

  function codeBlock(c) {
    var tabs = codeFiles(c);
    if (!tabs.length) return '';
    return '<div class="block">' +
      '<div class="code-head"><h3 style="margin:0"><span class="dot dot-rel"></span>Código</h3>' +
      '<div class="code-tabs">' + tabs.map(function (t) {
        return '<button class="tab" data-tab="' + t.key + '">' + esc(t.file.nombre) + '</button>';
      }).join('') + '</div></div>' +
      '<div class="code-wrap"><div class="code code-lines" id="codeBox"></div>' +
      '<button class="btn-copy" id="btnCopy">Copiar</button></div>' +
      '<button class="btn-open" id="btnOpenCode">Abrir código completo</button>' +
    '</div>';
  }

  function bindCodeBlock(c) {
    var tabs = codeFiles(c);
    if (!tabs.length) return;
    function current() {
      return (tabs.filter(function (t) { return t.key === state.codeTab; })[0] || tabs[0]).file;
    }
    function paint() {
      var f = current();
      $$('.tab', dom.inspBody).forEach(function (t) {
        t.classList.toggle('active', t.textContent === f.nombre);
      });
      $('#codeBox').innerHTML = codeHtml(f);
    }
    $$('.tab', dom.inspBody).forEach(function (t) {
      t.addEventListener('click', function () { state.codeTab = t.dataset.tab; paint(); });
    });
    $('#btnCopy').addEventListener('click', function () { copyText(current().codigo); });
    $('#btnOpenCode').addEventListener('click', function () { openFile(current().index); });
    paint();
  }

  /* ----------------------- Drag / pan ----------------------- */
  function startDrag(e, c) {
    if (e.button !== 0) return;
    state.dragging = c.id;
    var wp = worldPoint(e.clientX, e.clientY);
    state.dragDX = wp.x - c.x;
    state.dragDY = wp.y - c.y;
    document.getElementById('node-' + c.id).classList.add('dragging');
    e.stopPropagation();
    e.preventDefault();
  }

  function onMouseMove(e) {
    if (state.panning) {
      state.panX = e.clientX - state.panStartX;
      state.panY = e.clientY - state.panStartY;
      applyTransform();
    } else if (state.dragging) {
      var c = byId[state.dragging];
      var wp = worldPoint(e.clientX, e.clientY);
      c.x = wp.x - state.dragDX;
      c.y = wp.y - state.dragDY;
      var n = document.getElementById('node-' + c.id);
      n.style.left = c.x + 'px';
      n.style.top = c.y + 'px';
      updateWorldSize();
      renderEdges();
    }
  }

  function onMouseUp() {
    if (state.panning) { state.panning = false; dom.canvas.classList.remove('panning'); }
    if (state.dragging) {
      var n = document.getElementById('node-' + state.dragging);
      if (n) n.classList.remove('dragging');
      state.dragging = null;
    }
  }

  /* ----------------------- Touch ----------------------- */
  var lastDist = null, midPoint = null;

  function onTouchStart(e) {
    if (e.touches.length === 1) {
      state.panning = true;
      state.panStartX = e.touches[0].clientX - state.panX;
      state.panStartY = e.touches[0].clientY - state.panY;
    } else if (e.touches.length === 2) {
      state.panning = false;
      lastDist = Math.hypot(e.touches[0].clientX - e.touches[1].clientX, e.touches[0].clientY - e.touches[1].clientY);
      midPoint = {
        x: (e.touches[0].clientX + e.touches[1].clientX) / 2,
        y: (e.touches[0].clientY + e.touches[1].clientY) / 2
      };
    }
  }

  function onTouchMove(e) {
    if (e.touches.length === 1 && state.panning) {
      state.panX = e.touches[0].clientX - state.panStartX;
      state.panY = e.touches[0].clientY - state.panStartY;
      applyTransform();
    } else if (e.touches.length === 2 && lastDist && midPoint) {
      var dist = Math.hypot(e.touches[0].clientX - e.touches[1].clientX, e.touches[0].clientY - e.touches[1].clientY);
      var r = dom.canvas.getBoundingClientRect();
      setZoom(state.zoom * (dist / lastDist), midPoint.x - r.left, midPoint.y - r.top);
      lastDist = dist;
    }
  }

  /* ----------------------- Theme / toast / export ----------------------- */
  function setTheme(t) {
    state.theme = t;
    document.documentElement.dataset.theme = t;
    localStorage.setItem('uml-theme', t);
    var moon = $('#iconMoon'), sun = $('#iconSun');
    if (moon) moon.style.display = t === 'light' ? 'none' : '';
    if (sun) sun.style.display = t === 'light' ? '' : 'none';
  }

  var toastTimer = null;
  function toast(msg) {
    dom.toastMsg.textContent = msg;
    dom.toast.classList.add('show');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function () { dom.toast.classList.remove('show'); }, 2400);
  }

  function exportJson() {
    var data = { meta: META, packages: PACKAGES, classes: CLASSES, relations: RELATIONS };
    var blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
    var url = URL.createObjectURL(blob);
    var a = document.createElement('a');
    a.href = url;
    a.download = 'diagrama-transporte-uml.json';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    toast('Diagrama exportado (JSON)');
  }

  /* ----------------------- Init ----------------------- */
  function cacheDom() {
    dom.canvas = $('#canvas');
    dom.world = $('#world');
    dom.edges = $('#edges');
    dom.labels = $('#labels');
    dom.nodes = $('#nodes');
    dom.zoomValue = $('#zoomValue');
    dom.sidebar = $('#sidebar');
    dom.inspector = $('#inspector');
    dom.inspBody = $('#inspBody');
    dom.search = $('#search');
    dom.toast = $('#toast');
    dom.toastMsg = $('#toastMsg');
    dom.viewer = $('#codeViewer');
  }

  function bindControls() {
    $('#btnZoomIn').addEventListener('click', function () { setZoom(state.zoom + 0.15); });
    $('#btnZoomOut').addEventListener('click', function () { setZoom(state.zoom - 0.15); });
    $('#btnFit').addEventListener('click', fitView);
    $('#btnTheme').addEventListener('click', function () {
      setTheme(state.theme === 'dark' ? 'light' : 'dark');
    });
    $('#btnExport').addEventListener('click', exportJson);
    $('#btnSidebar').addEventListener('click', function () { dom.sidebar.classList.toggle('collapsed'); });
    $('#btnInspector').addEventListener('click', function () { dom.inspector.classList.toggle('closed'); });

    dom.search.addEventListener('input', function (e) {
      state.query = e.target.value.toLowerCase().trim();
      buildIndex();
      buildCodeIndex();
      applyVisibility();
    });

    dom.canvas.addEventListener('mousedown', function (e) {
      if (e.button !== 0 || e.target.closest('.node')) return;
      state.panning = true;
      state.panStartX = e.clientX - state.panX;
      state.panStartY = e.clientY - state.panY;
      dom.canvas.classList.add('panning');
    });

    dom.canvas.addEventListener('click', function (e) {
      if (!e.target.closest('.node')) deselect();
    });

    dom.canvas.addEventListener('wheel', function (e) {
      e.preventDefault();
      var r = dom.canvas.getBoundingClientRect();
      setZoom(state.zoom + (-e.deltaY * 0.0014), e.clientX - r.left, e.clientY - r.top);
    }, { passive: false });

    dom.canvas.addEventListener('touchstart', onTouchStart, { passive: true });
    dom.canvas.addEventListener('touchmove', onTouchMove, { passive: true });
    dom.canvas.addEventListener('touchend', function () { state.panning = false; lastDist = null; });

    window.addEventListener('mousemove', onMouseMove);
    window.addEventListener('mouseup', onMouseUp);
    window.addEventListener('resize', renderEdges);

    window.addEventListener('keydown', function (e) {
      if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') return;
      var viewerOpen = dom.viewer && !dom.viewer.hidden;
      if (e.key === 'Escape') { if (viewerOpen) closeViewer(); else deselect(); }
      else if (viewerOpen && e.key === 'ArrowRight') stepFile(1);
      else if (viewerOpen && e.key === 'ArrowLeft') stepFile(-1);
      else if (viewerOpen) return;
      else if (e.key === '+' || e.key === '=') setZoom(state.zoom + 0.12);
      else if (e.key === '-') setZoom(state.zoom - 0.12);
      else if (e.key === '0') fitView();
    });
  }

  function init() {
    cacheDom();
    $('#brandTitle').textContent = META.diagramName;
    $('#brandSub').textContent = META.title;
    $('#statClasses').textContent = CLASSES.length;
    $('#statRelations').textContent = RELATIONS.length;

    if (window.innerWidth < 1180) dom.inspector.classList.add('closed');
    if (window.innerWidth < 900) dom.sidebar.classList.add('collapsed');

    var saved = localStorage.getItem('uml-theme');
    setTheme(saved || 'dark');

    buildSidebar();
    buildNodes();
    updateWorldSize();
    applyTransform();
    bindControls();
    bindViewer();

    requestAnimationFrame(function () {
      updateWorldSize();
      fitView();
      renderEdges();
    });

    if (document.fonts && document.fonts.ready) {
      document.fonts.ready.then(function () { updateWorldSize(); renderEdges(); });
    }
    setTimeout(function () { updateWorldSize(); renderEdges(); }, 400);

    renderInspector(null);
  }

  window.addEventListener('load', init);
})();
