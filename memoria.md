# Memoria del proyecto MotoClub GPS

## Estado actual

- Proyecto Android nativo en Kotlin + Jetpack Compose.
- Carpeta del proyecto: `D:\proyecto-gps`.
- App instalada y probada en un celular Newsan B30.
- Package name: `com.motoclubgps`.
- La app usa OpenStreetMap mediante `osmdroid`, sin Google Maps API key.
- El APK debug actual se genera en:
  `app/build/outputs/apk/debug/app-debug.apk`.

## Backend Supabase

- Project ref: `rvanizgirqoxeeoorahv`.
- Project URL usada por la app:
  `https://rvanizgirqoxeeoorahv.supabase.co`.
- Key usada en cliente:
  `sb_publishable_SMp8IK0zz-OZZhxHuBPqAA_vRcc6nkV`.
- No usar ni guardar en la app la `service_role` ni secret key.
- SQL ejecutado correctamente desde `supabase-motoclub.sql`.
- Tablas creadas:
  - `trips`
  - `locations`
- Prueba verificada:
  - viaje `RUTA1` creado en Supabase.
  - ubicacion subida a `locations` con `status = ok`.

## Funciones actuales

- Crear viaje con nombre, descripcion y codigo.
- Unirse a viaje por codigo.
- Campo de codigo corregido para escribir, borrar y reescribir normalmente.
- Validaciones de email, contrasena, nombre y codigo de viaje.
- Los codigos son unicos solo entre viajes activos.
- Registro e ingreso con Supabase Auth por email y contrasena.
- Sesion local por dispositivo con el usuario real de Supabase.
- Ver mapa OpenStreetMap.
- Compartir ubicacion desde el menu con `Compartir mi ubicacion`.
- Subir ubicacion a Supabase.
- Guardar puntos historicos en `location_points` para dibujar recorridos.
- Si no hay conexion, guardar puntos pendientes y reenviarlos cuando vuelva.
- Leer ubicaciones cada 3 segundos.
- Boton `Ayuda` para marcar emergencia.
- Mapa a pantalla completa en pantalla de viaje.
- Menu desplegable del viaje con descripcion, codigo, companeros y acciones.
- Lista de companeros y banner de ayuda en pantalla de viaje.
- Boton `Finalizar viaje`.
- Flechas de volver en pantallas internas.
- Flecha superior izquierda del mapa vuelve atras, no finaliza el viaje.

## Archivos importantes

- `app/src/main/java/com/motoclubgps/data/SupabaseConfig.kt`
  contiene URL y publishable key de Supabase.
- `app/src/main/java/com/motoclubgps/data/SupabaseAuthRepository.kt`
  contiene registro, ingreso y sesion local con Supabase Auth.
- `app/src/main/java/com/motoclubgps/data/SupabaseTripRepository.kt`
  contiene llamadas REST a Supabase.
- `app/src/main/java/com/motoclubgps/ui/AppViewModel.kt`
  coordina viajes, ubicacion, ayuda y estado de pantalla.
- `app/src/main/java/com/motoclubgps/ui/screen/MapScreen.kt`
  pantalla principal del viaje con mapa, controles y companeros.
- `supabase-motoclub.sql`
  script para crear tablas y policies en Supabase.

## Pendientes importantes

- Probar con dos celulares:
  1. instalar el mismo `app-debug.apk` en ambos;
  2. crear viaje en uno;
  3. unirse con el mismo codigo en el otro;
  4. tocar `Compartir mi ubicacion` en ambos.
- Preparar APK release firmado para distribucion mas seria.
- Inicializar Git correctamente:
  la carpeta `.git` estaba vacia y `git status` no reconoce repo.
- Crear repo remoto y hacer primer commit/push.

## Reglas de trabajo del usuario

- Antes de modificar archivos, pedir autorizacion.
- Antes de compilar/instalar/reinstalar en el celular, pedir autorizacion.
- No hacer cambios silenciosos.

## Comandos utiles

Ver dispositivos:

```powershell
& 'C:\Users\Martin\AppData\Local\Android\Sdk\platform-tools\adb.exe' devices
```

Instalar APK debug:

```powershell
& 'C:\Users\Martin\AppData\Local\Android\Sdk\platform-tools\adb.exe' install -r app\build\outputs\apk\debug\app-debug.apk
```

Abrir app:

```powershell
& 'C:\Users\Martin\AppData\Local\Android\Sdk\platform-tools\adb.exe' shell am start -n com.motoclubgps/.MainActivity
```

Compilar APK debug:

```powershell
.\gradlew.bat assembleDebug --no-daemon --max-workers=1 --console=plain
```
