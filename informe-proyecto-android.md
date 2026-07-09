# Informe de análisis — MotoClub GPS

Análisis realizado exclusivamente en modo lectura. No se modificaron archivos existentes.

## 1. Resumen arquitectónico

La aplicación utiliza:

- Kotlin.
- Una única `Activity`.
- Jetpack Compose para toda la interfaz.
- Navigation Compose para la navegación.
- Material Design 3.
- Un `AndroidView` para integrar el mapa tradicional de osmdroid.
- Un único `AppViewModel` compartido por todas las pantallas.

La entrada de la aplicación es `MainActivity.kt`. Esta ejecuta `setContent`, aplica el tema y carga `MotoClubGpsApp`.

No existen `Fragment`, archivos XML de layouts ni múltiples Activities.

## 2. Inventario de pantallas

| Pantalla | Ruta | Implementación | Activity/Fragment |
|---|---|---|---|
| Inicio | `home` | `HomeScreen` | `MainActivity` / sin Fragment |
| Acceso y registro | `login` | `LoginScreen` | `MainActivity` / sin Fragment |
| Crear viaje | `create-trip` | `CreateTripScreen` | `MainActivity` / sin Fragment |
| Unirse a viaje | `join-trip` | `JoinTripScreen` | `MainActivity` / sin Fragment |
| Mapa del viaje | `map/{tripId}` | `MapScreen` | `MainActivity` / sin Fragment |
| Perfil | `profile` | `ProfileScreen` | `MainActivity` / sin Fragment |

Las rutas están declaradas en `MotoClubGpsApp.kt`.

### Inicio

Implementación: `HomeScreen.kt`.

Tiene dos estados:

- Usuario no autenticado: muestra únicamente “Login o registro”.
- Usuario autenticado: muestra “Crear viaje”, “Unirse a viaje” y “Perfil”.

No son dos rutas diferentes; es una misma pantalla condicionada por `state.isLoggedIn`.

### Acceso y registro

Implementación: `LoginScreen.kt`.

Incluye:

- Nombre.
- Email.
- Contraseña.
- Acción de ingreso.
- Acción de registro.
- Validación básica mediante habilitación de botones.

Login y registro comparten el mismo formulario y destino.

### Crear viaje

Implementación: `CreateTripScreen.kt`.

Incluye:

- Nombre del viaje.
- Descripción.
- Código para compartir.
- Acción “Crear y abrir mapa”.

Después de crear el viaje navega a `map/{tripId}`.

### Unirse a viaje

Implementación: `JoinTripScreen.kt`.

Solicita un código alfanumérico de hasta ocho caracteres. Cuando encuentra el viaje, navega al mapa.

### Mapa del viaje

Implementación: `MapScreen.kt`.

Es la pantalla funcionalmente más compleja. Contiene:

- Mapa OpenStreetMap a pantalla completa.
- Barra flotante con regreso, código, copiar código y menú.
- Marcadores personalizados de participantes.
- Banner de emergencia.
- Menú lateral del viaje.
- Estado de ubicación compartida.
- Lista de compañeros.
- Solicitud y cancelación de ayuda.
- Salida del viaje.
- Finalización del viaje para el creador.
- Indicación de ubicaciones pendientes por falta de conexión.

El mapa no es un componente Compose nativo. `MapView` de osmdroid se inserta mediante `AndroidView`.

### Perfil

Implementación: `ProfileScreen.kt`.

Muestra:

- Nombre.
- Email.
- Cierre de sesión.

## 3. Superficies secundarias

No son destinos independientes, pero forman parte importante de la experiencia:

- Drawer “Control de ruta”.
- Diálogo para pedir ayuda.
- Confirmación para salir del viaje.
- Confirmación para finalizar el viaje.
- Diálogo genérico de error o aviso.
- Indicador de carga.
- Banner de emergencia sobre el mapa.

El diálogo de error y la estructura común de las pantallas están centralizados en `Common.kt`.

## 4. XML o Jetpack Compose

La interfaz está implementada completamente con Jetpack Compose.

Evidencias:

- `buildFeatures.compose = true`.
- Plugin `org.jetbrains.kotlin.plugin.compose`.
- Dependencias de Compose Material 3 y Navigation Compose.
- `MainActivity` utiliza `setContent`.
- No existe `res/layout`.
- Todos los destinos son funciones `@Composable`.

El único XML encontrado es `res/values/styles.xml`. Define el tema base de ventana Android, pero no contiene una interfaz.

Por tanto:

- Interfaz: Compose.
- Tema inicial del sistema: XML.
- Mapa: vista Android tradicional embebida en Compose.

## 5. Organización de la navegación

La aplicación sigue el patrón single-Activity:

```text
MainActivity
└── MotoClubGpsApp
    └── NavHost
        ├── home
        ├── login
        ├── create-trip
        ├── join-trip
        ├── profile
        └── map/{tripId}
```

El destino inicial siempre es `home`.

Flujos principales:

```text
Home → Login/Registro → Home
Home → Crear viaje → Mapa → Home
Home → Unirse a viaje → Mapa → Home
Home → Perfil → Home
```

Características:

- Las rutas se modelan mediante una clase sellada privada.
- `map/{tripId}` es la única ruta con argumento.
- Login y perfil utilizan `popBackStack()`.
- Al salir del mapa se eliminan los destinos intermedios hasta llegar a `home`.
- No hay navegación inferior, pestañas, deep links ni grafos anidados.
- El estado de autenticación no determina el destino inicial: `HomeScreen` decide qué contenido mostrar.
- El mismo `AppViewModel` se comparte entre todos los destinos.

Punto a revisar: después de crear o unirse, el formulario permanece inicialmente debajo del mapa en el back stack, aunque la salida explícita del mapa hace `popBackStack("home", false)` y lo elimina. El gesto físico de retroceso debería comprobarse para asegurar que respeta el mismo comportamiento.

## 6. Componentes Material Design

La aplicación usa Material 3 mediante `androidx.compose.material3`.

Componentes identificados:

- `MaterialTheme`
- `Scaffold`
- `Text`
- `Button`
- `OutlinedButton`
- `ElevatedButton`
- `FilledTonalButton`
- `TextButton`
- `OutlinedTextField`
- `Icon`
- `IconButton`
- `Surface`
- `AlertDialog`
- `CircularProgressIndicator`
- `ModalNavigationDrawer`
- `ModalDrawerSheet`
- `HorizontalDivider`
- Iconos de Material Extended

También utiliza formas Material como `RoundedCornerShape` y `CircleShape`.

No se utilizan:

- `TopAppBar`
- `NavigationBar`
- `Card`
- `Snackbar`
- `FloatingActionButton`
- `DatePicker`
- `SearchBar`
- `ListItem`
- Material adaptive layouts

El tema de `Theme.kt` configura una paleta clara propia, pero conserva tipografía y formas predeterminadas. No existe tema oscuro.

## 7. Evaluación visual y candidatas a rediseño

### Prioridad alta: Inicio

Actualmente es esencialmente un título seguido por uno o tres botones. Carece de:

- Identidad visual del motoclub.
- Resumen de usuario.
- Estado de viajes.
- Jerarquía entre la acción principal y las secundarias.
- Contenido útil cuando no hay sesión.

Sería conveniente convertirla en un dashboard con cabecera, identidad gráfica y tarjetas de acciones.

### Prioridad alta: Mapa

Es la pantalla crítica para uso en ruta. Aunque tiene más trabajo visual que el resto, necesita especial atención por:

- Exceso de acciones dentro del drawer.
- Dos controles de cierre simultáneos en el drawer.
- Acciones de emergencia mezcladas con acciones administrativas.
- Botones “Salir” y “Finalizar” visualmente demasiado similares.
- Barra superior flotante con cuatro acciones en poco espacio.
- Banner de emergencia que puede ocultar contenido del mapa.
- Dependencia del color para diferenciar estados.
- Falta de una leyenda clara para marcadores y estados.
- Riesgo de interacción difícil con guantes o durante una parada breve.

El rediseño debería priorizar legibilidad exterior, controles grandes, emergencia muy distinguible y acceso rápido a centrar ubicación y compañeros.

### Prioridad alta: Acceso y registro

Login y registro están mezclados en un único formulario. El campo “Nombre” aparece incluso al ingresar, aunque no se usa para esa operación.

Conviene separar los modos con pestañas, selector segmentado o pantallas distintas, incorporando:

- Explicación de errores junto al campo.
- Visibilidad de contraseña.
- Tipos de teclado y acciones IME apropiadas.
- Estado de carga dentro del botón.
- Jerarquía clara entre entrar y crear cuenta.

### Prioridad media-alta: Crear viaje

Es un formulario lineal sin ayuda contextual. El código puede dejarse vacío y generarse automáticamente desde la lógica, pero la interfaz no lo explica.

Posibles mejoras:

- Generador de código visible.
- Explicación sobre cómo compartirlo.
- Contador o límite visible.
- Vista previa o resumen antes de crear.
- Tratamiento más claro de campos obligatorios y opcionales.

### Prioridad media: Unirse a viaje

La pantalla tiene mucho espacio vacío para una sola entrada.

Sería candidata a:

- Campo de código de gran tamaño.
- Pegado automático desde portapapeles.
- Escaneo QR.
- Explicación breve sobre dónde obtener el código.
- Estado de búsqueda claramente visible.

### Prioridad media: Perfil

Es una presentación textual mínima. No tiene avatar, edición, preferencias, privacidad ni información de estado.

Podría evolucionar a una pantalla de cuenta con secciones visuales. Sin embargo, su rediseño tiene menor impacto funcional que Inicio, Mapa o Acceso.

### Componente transversal: estructura común

`ScreenScaffold` construye manualmente la cabecera con `Row`, `Text` e `IconButton`. Sería más consistente utilizar `TopAppBar` y gestionar correctamente:

- Insets de barras del sistema.
- Scroll.
- Acciones de pantalla.
- Comportamiento responsive.
- Semántica de navegación.

## 8. Observaciones adicionales

- La captura incluida parece corresponder a una versión anterior: muestra el título “Mapa” sobre una pantalla vacía, mientras que el código actual implementa mapa completo y barra “Viaje en curso”.
- El error de red de la captura expone directamente un mensaje técnico en inglés. Aunque la lógica actual mejora algunos errores offline, conviene traducir y normalizar todas las excepciones antes de mostrarlas.
- No existen previews `@Preview`, lo que dificulta validar estados visuales de manera aislada.
- No se observa adaptación específica para orientación horizontal, tablet, tamaños compactos o accesibilidad.
- Algunos textos omiten tildes (`Contrasena`, `Descripcion`, `Codigo`, `Companeros`).
- Los formularios no usan `KeyboardOptions`, autofill ni mensajes de validación por campo.
- El tema solo define cinco colores y no personaliza tipografía, formas ni esquema oscuro.

## Conclusión

El proyecto tiene una arquitectura UI compacta y clara: una Activity, seis pantallas Compose y navegación centralizada. La base Material 3 está correctamente incorporada, aunque el diseño general sigue siendo funcional y básico.

El orden recomendado de rediseño es:

1. Mapa y controles de ruta.
2. Inicio.
3. Acceso y registro.
4. Crear y unirse a viaje.
5. Perfil.
6. Sistema visual transversal: tema, tipografía, barras, errores, carga y accesibilidad.
