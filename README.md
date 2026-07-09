# MotoClub GPS

Aplicacion Android nativa en Kotlin y Jetpack Compose para crear viajes de motoclub, unirse por codigo y compartir ubicacion en tiempo real.

## Estado actual

- UI en Jetpack Compose.
- Mapa con OpenStreetMap mediante osmdroid.
- Backend con Supabase REST y Supabase Auth.
- Registro e ingreso con email y contrasena.
- Sesion guardada localmente por dispositivo.
- Cada ubicacion se asocia al usuario autenticado.

## Funciones incluidas

- Registro e inicio de sesion.
- Perfil con nombre, email y cierre de sesion.
- Crear viaje con nombre, descripcion y codigo.
- Unirse a viaje por codigo.
- Validaciones para email, contrasena, codigo de viaje y codigo repetido.
- Mapa a pantalla completa.
- Menu desplegable del viaje con descripcion, codigo, companeros y acciones.
- Compartir ubicacion propia.
- Ver ubicaciones de companeros.
- Marcar y cancelar pedido de ayuda.
- Finalizar viaje.

## Supabase

El proyecto usa:

- Project URL: `https://rvanizgirqoxeeoorahv.supabase.co`
- Publishable key configurada en `app/src/main/java/com/motoclubgps/data/SupabaseConfig.kt`

Para preparar la base de datos, ejecutar `supabase-motoclub.sql` en el SQL Editor de Supabase.

No guardar `service_role` ni secret keys dentro de la app.

Los codigos de viaje son unicos entre viajes activos. Si un viaje se finaliza, el mismo codigo se puede reutilizar en otro viaje.

## Compilar APK debug

```powershell
.\gradlew.bat assembleDebug --no-daemon --max-workers=1 --console=plain
```

El APK debug se genera en:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Instalar en Android por ADB

```powershell
& 'C:\Users\Martin\AppData\Local\Android\Sdk\platform-tools\adb.exe' install -r app\build\outputs\apk\debug\app-debug.apk
```
