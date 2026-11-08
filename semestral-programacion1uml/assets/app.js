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

  var CARD_W = 340;
  var MIN_Z = 0.35, MAX_Z = 2.4;

  var state = {
    zoom: 0.9, panX: 60, panY: 40,
    selected: null, filter: 'all', query: '',
    dragging: null, dragDX: 0, dragDY: 0,
    panning: false, panStartX: 0, panStartY: 0, gestureRect: null,
    codeLang: 'java', theme: 'dark'
  };

  function $(s, r) { return (r || document).querySelector(s); }
  function $$(s, r) { return Array.prototype.slice.call((r || document).querySelectorAll(s)); }
  function el(tag, cls) { var n = document.createElement(tag); if (cls) n.className = cls; return n; }
  function esc(s) { return String(s).replace(/[&<>"]/g, function (m) { return ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' })[m]; }); }

  var dom = {};

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
      '<div class="block">' +
        '<div class="code-head"><h3 style="margin:0"><span class="dot dot-rel"></span>Código</h3>' +
        '<div class="code-tabs"><button class="tab" data-lang="java">Java</button>' +
        '<button class="tab" data-lang="ts">TypeScript</button></div></div>' +
        '<div class="code-wrap"><pre class="code" id="codeBox"></pre>' +
        '<button class="btn-copy" id="btnCopy">Copiar</button></div>' +
      '</div>';

    $('#inspClose').addEventListener('click', closeInspector);
    $$('.tab', dom.inspBody).forEach(function (t) {
      t.addEventListener('click', function () {
        state.codeLang = t.dataset.lang;
        paintTabs();
        $('#codeBox').textContent = codeFor(c, state.codeLang);
      });
    });
    $('#btnCopy').addEventListener('click', function () {
      var code = $('#codeBox').textContent;
      var ta = document.createElement('textarea');
      ta.value = code;
      document.body.appendChild(ta);
      ta.select();
      try { document.execCommand('copy'); toast('Código copiado'); }
      catch (e) { toast('No se pudo copiar'); }
      document.body.removeChild(ta);
    });
    $$('.rel-row', dom.inspBody).forEach(function (row) {
      row.addEventListener('click', function () { focusClass(row.dataset.goto); });
    });
    paintTabs();
    $('#codeBox').textContent = codeFor(c, state.codeLang);
  }

  function paintTabs() {
    $$('.tab', dom.inspBody).forEach(function (t) {
      t.classList.toggle('active', t.dataset.lang === state.codeLang);
    });
  }

  /* ----------------------- Code generation ----------------------- */
  function mod(v) { return v === '+' ? 'public' : v === '#' ? 'protected' : 'private'; }
  function tsType(t) {
    if (t === 'int' || t === 'double') return 'number';
    if (t === 'String') return 'string';
    return t;
  }

  function codeFor(c, lang) {
    var gen = RELATIONS.filter(function (r) { return r.source === c.id && r.type === 'generalization'; })[0];
    var ext = gen ? ' extends ' + gen.target : '';

    if (lang === 'java') {
      var out = 'package ' + c.package + ';\n\n';
      out += 'public ' + (c.isAbstract ? 'abstract ' : '') + 'class ' + c.name + ext + ' {\n';
      c.attributes.forEach(function (a) { out += '    ' + mod(a.vis) + ' ' + a.type + ' ' + a.name + ';\n'; });
      out += '\n';
      c.methods.forEach(function (m) {
        if (m.isAbstract) out += '    ' + mod(m.vis) + ' abstract ' + m.returnType + ' ' + m.name + '(' + m.params + ');\n';
        else out += '    ' + mod(m.vis) + ' ' + m.returnType + ' ' + m.name + '(' + m.params + ') {\n        // TODO\n    }\n\n';
      });
      return out + '}';
    }

    var ts = '// paquete: ' + c.package + '\n\n';
    ts += 'export ' + (c.isAbstract ? 'abstract ' : '') + 'class ' + c.name + ext + ' {\n';
    c.attributes.forEach(function (a) { ts += '    ' + mod(a.vis) + ' ' + a.name + ': ' + tsType(a.type) + ';\n'; });
    ts += '\n';
    c.methods.forEach(function (m) {
      if (m.isAbstract) ts += '    ' + mod(m.vis) + ' abstract ' + m.name + '(' + m.params + '): ' + tsType(m.returnType) + ';\n';
      else ts += '    ' + mod(m.vis) + ' ' + m.name + '(' + m.params + '): ' + tsType(m.returnType) + ' {\n        // TODO\n    }\n\n';
    });
    return ts + '}';
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
      if (e.key === 'Escape') deselect();
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
