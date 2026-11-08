/* =========================================================================
 * data.js — Modelo del diagrama de clases (Sistema de Transporte Público)
 * Fuente: docs/uml/diagrama-clases.puml
 * ========================================================================= */

window.UML_META = {
  title: 'Sistema de Gestión de Transporte Público',
  kicker: 'Proyecto Semestral · Programación I',
  school: 'Universidad Tecnológica de Panamá',
  faculty: 'Facultad de Ingeniería de Sistemas Computacionales',
  professor: 'Prof. Víctor Sánchez',
  authors: 'Eric De León · Jorge Piñ · Carlos Barría · Ángel Díaz',
  diagramName: 'Diagrama de Clases UML',
  repo: 'https://github.com/Jorge-Dev27/semestral-programacion1UML'
};

/* Paleta tipo "línea de metro": un color por paquete */
window.UML_PACKAGES = [
  { id: 'usuario',    label: 'usuario',    color: '#7c93ff' },
  { id: 'ruta',       label: 'ruta',       color: '#2fd08a' },
  { id: 'boleto',     label: 'boleto',     color: '#f5b93d' },
  { id: 'turno',      label: 'turno',      color: '#2bd4e6' },
  { id: 'unidad',     label: 'unidad',     color: '#ff7a94' },
  { id: 'incidencia', label: 'incidencia', color: '#ff6b6b' },
  { id: 'reporte',    label: 'reporte',    color: '#b98cff' }
];

/* Clases: visibilidad '+' público, '#' protegido, '-' privado */
window.UML_CLASSES = [
  {
    id: 'Usuario', name: 'Usuario', package: 'usuario', isAbstract: true, x: 480, y: 40,
    attributes: [
      { vis: '#', name: 'idUsuario', type: 'int' },
      { vis: '#', name: 'nombre', type: 'String' },
      { vis: '#', name: 'correo', type: 'String' },
      { vis: '-', name: 'contrasena', type: 'String' },
      { vis: '#', name: 'rol', type: 'String' }
    ],
    methods: [
      { vis: '+', name: 'iniciarSesion', params: 'correo : String, contrasena : String', returnType: 'boolean' },
      { vis: '+', name: 'cerrarSesion', params: '', returnType: 'void' },
      { vis: '+', name: 'mostrarMenu', params: '', returnType: 'String', isAbstract: true },
      { vis: '+', name: 'cambiarContrasena', params: 'nueva : String', returnType: 'void' }
    ]
  },
  {
    id: 'Pasajero', name: 'Pasajero', package: 'usuario', isAbstract: false, x: 60, y: 360,
    attributes: [
      { vis: '-', name: 'cedula', type: 'String' },
      { vis: '-', name: 'telefono', type: 'String' },
      { vis: '-', name: 'saldo', type: 'double' }
    ],
    methods: [
      { vis: '+', name: 'comprarBoleto', params: 'horario : Horario, precio : double', returnType: 'Boleto' },
      { vis: '+', name: 'recargarSaldo', params: 'monto : double', returnType: 'void' },
      { vis: '+', name: 'reembolsar', params: 'monto : double', returnType: 'void' },
      { vis: '+', name: 'consultarRutas', params: '', returnType: 'List<Ruta>' },
      { vis: '+', name: 'mostrarMenu', params: '', returnType: 'String' }
    ]
  },
  {
    id: 'Conductor', name: 'Conductor', package: 'usuario', isAbstract: false, x: 480, y: 360,
    attributes: [
      { vis: '-', name: 'numeroLicencia', type: 'String' },
      { vis: '-', name: 'tipoLicencia', type: 'String' },
      { vis: '-', name: 'disponible', type: 'boolean' }
    ],
    methods: [
      { vis: '+', name: 'validarBoleto', params: 'boleto : Boleto', returnType: 'boolean' },
      { vis: '+', name: 'registrarIncidencia', params: 'tipo : String, descripcion : String', returnType: 'Incidencia' },
      { vis: '+', name: 'consultarTurnos', params: '', returnType: 'List<Turno>' },
      { vis: '+', name: 'mostrarMenu', params: '', returnType: 'String' }
    ]
  },
  {
    id: 'Administrador', name: 'Administrador', package: 'usuario', isAbstract: false, x: 900, y: 360,
    attributes: [
      { vis: '-', name: 'cargo', type: 'String' }
    ],
    methods: [
      { vis: '+', name: 'registrarRuta', params: 'ruta : Ruta', returnType: 'void' },
      { vis: '+', name: 'asignarUnidad', params: 'unidad : Unidad, ruta : Ruta', returnType: 'void' },
      { vis: '+', name: 'crearTurno', params: 'c : Conductor, u : Unidad, r : Ruta, fecha : LocalDate', returnType: 'Turno' },
      { vis: '+', name: 'generarReporte', params: 'tipo : String', returnType: 'Reporte' },
      { vis: '+', name: 'mostrarMenu', params: '', returnType: 'String' }
    ]
  },
  {
    id: 'Boleto', name: 'Boleto', package: 'boleto', isAbstract: false, x: 60, y: 720,
    attributes: [
      { vis: '-', name: 'idBoleto', type: 'int' },
      { vis: '-', name: 'codigo', type: 'String' },
      { vis: '-', name: 'fechaCompra', type: 'LocalDateTime' },
      { vis: '-', name: 'precio', type: 'double' },
      { vis: '-', name: 'estado', type: 'String' }
    ],
    methods: [
      { vis: '+', name: 'validar', params: '', returnType: 'boolean' },
      { vis: '+', name: 'anular', params: '', returnType: 'void' },
      { vis: '+', name: 'estaVigente', params: '', returnType: 'boolean' },
      { vis: '+', name: 'generarCodigo', params: '', returnType: 'String' }
    ]
  },
  {
    id: 'Horario', name: 'Horario', package: 'ruta', isAbstract: false, x: 60, y: 1100,
    attributes: [
      { vis: '-', name: 'idHorario', type: 'int' },
      { vis: '-', name: 'horaSalida', type: 'LocalTime' },
      { vis: '-', name: 'horaLlegada', type: 'LocalTime' },
      { vis: '-', name: 'diasOperacion', type: 'String' },
      { vis: '-', name: 'idRuta', type: 'int' }
    ],
    methods: [
      { vis: '+', name: 'estaDisponible', params: 'dia : String', returnType: 'boolean' },
      { vis: '+', name: 'calcularDuracion', params: '', returnType: 'int' }
    ]
  },
  {
    id: 'Ruta', name: 'Ruta', package: 'ruta', isAbstract: false, x: 520, y: 720,
    attributes: [
      { vis: '-', name: 'idRuta', type: 'int' },
      { vis: '-', name: 'nombre', type: 'String' },
      { vis: '-', name: 'origen', type: 'String' },
      { vis: '-', name: 'destino', type: 'String' },
      { vis: '-', name: 'tarifa', type: 'double' },
      { vis: '-', name: 'activa', type: 'boolean' }
    ],
    methods: [
      { vis: '+', name: 'agregarParada', params: 'parada : Parada', returnType: 'void' },
      { vis: '+', name: 'agregarHorario', params: 'horario : Horario', returnType: 'void' },
      { vis: '+', name: 'obtenerParadas', params: '', returnType: 'List<Parada>' },
      { vis: '+', name: 'cambiarEstado', params: 'activa : boolean', returnType: 'void' },
      { vis: '+', name: 'estaCompleta', params: '', returnType: 'boolean' }
    ]
  },
  {
    id: 'Parada', name: 'Parada', package: 'ruta', isAbstract: false, x: 520, y: 1100,
    attributes: [
      { vis: '-', name: 'idParada', type: 'int' },
      { vis: '-', name: 'nombre', type: 'String' },
      { vis: '-', name: 'ubicacion', type: 'String' },
      { vis: '-', name: 'orden', type: 'int' },
      { vis: '-', name: 'idRuta', type: 'int' }
    ],
    methods: [
      { vis: '+', name: 'mostrarInfo', params: '', returnType: 'String' },
      { vis: '+', name: 'esTerminal', params: '', returnType: 'boolean' }
    ]
  },
  {
    id: 'Unidad', name: 'Unidad', package: 'unidad', isAbstract: false, x: 980, y: 720,
    attributes: [
      { vis: '-', name: 'idUnidad', type: 'int' },
      { vis: '-', name: 'placa', type: 'String' },
      { vis: '-', name: 'modelo', type: 'String' },
      { vis: '-', name: 'capacidad', type: 'int' },
      { vis: '-', name: 'estado', type: 'String' },
      { vis: '-', name: 'idRuta', type: 'int' }
    ],
    methods: [
      { vis: '+', name: 'asignarRuta', params: 'ruta : Ruta', returnType: 'void' },
      { vis: '+', name: 'cambiarEstado', params: 'estado : String', returnType: 'void' },
      { vis: '+', name: 'estaDisponible', params: '', returnType: 'boolean' }
    ]
  },
  {
    id: 'Turno', name: 'Turno', package: 'turno', isAbstract: false, x: 1440, y: 480,
    attributes: [
      { vis: '-', name: 'idTurno', type: 'int' },
      { vis: '-', name: 'fecha', type: 'LocalDate' },
      { vis: '-', name: 'horaInicio', type: 'LocalTime' },
      { vis: '-', name: 'horaFin', type: 'LocalTime' }
    ],
    methods: [
      { vis: '+', name: 'calcularHoras', params: '', returnType: 'double' },
      { vis: '+', name: 'finalizar', params: '', returnType: 'void' },
      { vis: '+', name: 'estaFinalizado', params: '', returnType: 'boolean' }
    ]
  },
  {
    id: 'Incidencia', name: 'Incidencia', package: 'incidencia', isAbstract: false, x: 980, y: 1100,
    attributes: [
      { vis: '-', name: 'idIncidencia', type: 'int' },
      { vis: '-', name: 'fecha', type: 'LocalDateTime' },
      { vis: '-', name: 'tipo', type: 'String' },
      { vis: '-', name: 'descripcion', type: 'String' },
      { vis: '-', name: 'estado', type: 'String' }
    ],
    methods: [
      { vis: '+', name: 'cambiarEstado', params: 'estado : String', returnType: 'void' },
      { vis: '+', name: 'cerrar', params: '', returnType: 'void' }
    ]
  },
  {
    id: 'Reporte', name: 'Reporte', package: 'reporte', isAbstract: false, x: 1440, y: 40,
    attributes: [
      { vis: '-', name: 'idReporte', type: 'int' },
      { vis: '-', name: 'tipo', type: 'String' },
      { vis: '-', name: 'fechaGeneracion', type: 'LocalDate' },
      { vis: '-', name: 'contenido', type: 'String' }
    ],
    methods: [
      { vis: '+', name: 'generarPorRuta', params: 'ruta : Ruta', returnType: 'void' },
      { vis: '+', name: 'generarPorUnidad', params: 'unidad : Unidad', returnType: 'void' },
      { vis: '+', name: 'agregarDato', params: 'etiqueta : String, valor : String', returnType: 'void' },
      { vis: '+', name: 'exportar', params: '', returnType: 'String' }
    ]
  }
];

/* Relaciones: type = generalization | association | composition */
window.UML_RELATIONS = [
  { id: 'gen-1', source: 'Pasajero', target: 'Usuario', type: 'generalization', cardSource: '', cardTarget: '', label: 'hereda' },
  { id: 'gen-2', source: 'Conductor', target: 'Usuario', type: 'generalization', cardSource: '', cardTarget: '', label: 'hereda' },
  { id: 'gen-3', source: 'Administrador', target: 'Usuario', type: 'generalization', cardSource: '', cardTarget: '', label: 'hereda' },

  { id: 'comp-1', source: 'Ruta', target: 'Parada', type: 'composition', cardSource: '1', cardTarget: '*', label: '' },
  { id: 'comp-2', source: 'Ruta', target: 'Horario', type: 'composition', cardSource: '1', cardTarget: '*', label: '' },

  { id: 'rel-1', source: 'Pasajero', target: 'Boleto', type: 'association', cardSource: '1', cardTarget: '*', label: 'compra' },
  { id: 'rel-2', source: 'Horario', target: 'Boleto', type: 'association', cardSource: '1', cardTarget: '*', label: 'aplica a' },
  { id: 'rel-3', source: 'Conductor', target: 'Incidencia', type: 'association', cardSource: '1', cardTarget: '*', label: 'reporta' },
  { id: 'rel-4', source: 'Unidad', target: 'Incidencia', type: 'association', cardSource: '1', cardTarget: '*', label: 'sufre' },
  { id: 'rel-5', source: 'Administrador', target: 'Turno', type: 'association', cardSource: '1', cardTarget: '*', label: 'crea' },
  { id: 'rel-6', source: 'Conductor', target: 'Turno', type: 'association', cardSource: '1', cardTarget: '*', label: 'ejecuta' },
  { id: 'rel-7', source: 'Unidad', target: 'Turno', type: 'association', cardSource: '1', cardTarget: '*', label: 'asignada a' },
  { id: 'rel-8', source: 'Ruta', target: 'Turno', type: 'association', cardSource: '1', cardTarget: '*', label: 'corresponde' }
];
