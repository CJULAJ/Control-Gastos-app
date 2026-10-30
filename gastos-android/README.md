# Control de Gastos — Aplicación Android

Aplicación móvil nativa para Android desarrollada en **Kotlin** orientada a la gestión y control de finanzas personales. Permite a los usuarios registrarse, autenticarse de manera segura, administrar sus gastos diarios, aplicar filtros por categoría y fecha, y analizar sus hábitos financieros mediante estadísticas visuales y un desglose por categorías.

---

## 📌 Tabla de Contenidos
- [Descripción General](#-descripción-general)
- [Características Principales](#-características-principales)
- [Tecnologías y Librerías Utilizadas](#-tecnologías-y-librerías-utilizadas)
- [Arquitectura del Proyecto](#-arquitectura-del-proyecto)
- [Estructura de Carpetas](#-estructura-de-carpetas)
- [Persistencia de Datos y Seguridad](#-persistencia-de-datos-y-seguridad)
- [Instalación y Ejecución](#-instalación-y-ejecución)
- [Pruebas Automatizadas](#-pruebas-automatizadas)
- [Capturas de Pantalla](#-capturas-de-pantalla)
- [Repositorio](#-repositorio)
- [Autor e Información Académica](#-autor-e-información-académica)
- [Estado del Proyecto](#-estado-del-proyecto)

---

## 📖 Descripción General

**Control de Gastos** es un proyecto universitario diseñado con el propósito de ofrecer una herramienta accesible, segura e intuitiva para la administración financiera individual. La aplicación implementa aislamiento estricto de datos por usuario en SQLite, almacenamiento local mediante **Room Database**, y protección de credenciales mediante técnicas criptográficas estándar de la industria.

La interfaz de usuario utiliza componentes y estilos de interfaz Android, ofreciendo soporte para **Tema Claro** y **Tema Oscuro**, navegación por secciones fija mediante View Binding, y una gráfica estadística de los últimos 7 días.

---

## ✨ Características Principales

- 🔐 **Autenticación y Gestión de Sesión:**
    - Registro de usuarios con validaciones en tiempo real (mínimo 3 caracteres para usuario y 8 para contraseña).
    - Inicio de sesión seguro y persistencia de sesión local mediante `SharedPreferences`.
    - Botón interactivo para alternar la visibilidad de la contraseña (`👁️` / `🙈`).
    - Cierre de sesión con cuadro de diálogo de confirmación.

- 🛡️ **Aislamiento y Seguridad de Datos:**
    - Separación estricta de registros por `usuarioId` en la base de datos local SQLite.
    - Protección de contraseñas con algoritmo **PBKDF2WithHmacSHA256** (210,000 iteraciones) y sal aleatoria (`SecureRandom`).
    - Verificación de hash en tiempo constante para mitigar ataques de temporización (*timing attacks*).

- 💵 **Gestión de Gastos (CRUD Completo):**
    - **Creación:** Registro de gastos con descripción, monto en Quetzales (`Q`), categoría y fecha.
    - **Lectura:** Visualización en `RecyclerView` ordenado cronológicamente.
    - **Edición:** Diálogo emergente para modificar descripción, monto, categoría y fecha (incluye integrador interactivo `DatePickerDialog`).
    - **Eliminación:** Borrado de registros con cuadro de confirmación.

- 📊 **Análisis y Estadísticas Visuales:**
    - **Resumen rápido:** Tarjetas dinámicas de *"Gastado hoy"* y *"Últimos 7 días"*.
    - **Gráfica semanal:** Gráfica de barras redondeadas tipo píldora para los últimos 7 días, indicando el monto exacto en cada barra y resaltando el día de mayor consumo.
    - **Métricas:** Cálculo del promedio diario de gasto y detección automática del día pico de consumo.
    - **Desglose por Categoría:** Visualización con emojis representativos, porcentajes y barras de progreso horizontales por cada categoría de gasto.

- 🎨 **Interfaz de Usuario y UX:**
    - Saludo personalizado según el momento del día (*Buenos días*, *Buenas tardes*, *Buenas noches*).
    - Navegación por pestañas fija (*Inicio*, *Gastos*, *Agregar*, *Datos*, *Perfil*) con resaltado dinámico de iconos y textos.
    - Botón flotante de acción rápida para agregar transacciones.
    - Compatibilidad completa con **Tema Oscuro** y **Tema Claro**.

---

## 🛠️ Tecnologías y Librerías Utilizadas

| Categoría | Tecnología / Librería | Versión Real |
| :--- | :--- | :--- |
| **Lenguaje** | Kotlin | `2.2.10` |
| **Construcción** | Android Gradle Plugin (AGP) | `9.1.1` |
| **Procesador de Anotaciones** | Kotlin Symbol Processing (KSP) | `2.3.10` |
| **Android SDK** | Compile SDK / Target SDK / Min SDK | `37 / 36 / 24` |
| **Base de Datos** | Room Database (SQLite) | `2.8.4` |
| **UI & Layouts** | Material Components, ConstraintLayout, RecyclerView, ViewBinding | `1.14.0 / 2.2.2 / 1.4.0` |
| **Asincronía** | Kotlin Coroutines & Lifecycle Runtime KTX | `2.11.0` |
| **Criptografía** | `javax.crypto.SecretKeyFactory` (PBKDF2) | Java Standard Library |
| **Módulo Core** | `gastos-core` (biblioteca Kotlin/JVM publicada en Maven local) | `1.0.1` |

---

## 🏗️ Arquitectura del Proyecto

El proyecto utiliza una **separación por capas**: la interfaz Android depende del contrato de repositorio definido en el core, mientras que la implementación concreta de persistencia utiliza Room. El core también dispone de un repositorio en memoria para funcionar sin Android:

```
┌────────────────────────────────────────────────────────┐
│              Capa de Presentación (UI)                  │
│   Activities, ViewBinding, Adapters, XML Layouts        │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│             Capa del Core / Dominio                    │
│   ControlGastos (Servicio), Gasto (Modelo), Categoria  │
│   GastoRepository (Interfaz de Datos)                   │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│              Capa de Datos Local (Room)                │
│   GastoRepositoryRoom, AutenticacionRepository,         │
│   SeguridadPassword, AppDatabase, DAOs y Entities      │
└────────────────────────────────────────────────────────┘
```

1. **Módulo Core (`gastos-core` - v1.0.1):**
    - Compilado como biblioteca Kotlin/JVM e instalado en el repositorio Maven local (`.m2`). El código fuente del core debe permanecer accesible en el repositorio de entrega.
    - Contiene los modelos del dominio (`Gasto`, `Categoria`), la interfaz `GastoRepository` y la clase de servicio `ControlGastos`.
    - Permite ejecutar la lógica de gastos sin Android mediante una implementación en memoria del repositorio y un programa de consola.

2. **Capa de Datos Local (`data.local`):**
    - Implementa la interfaz `GastoRepository` a través de `GastoRepositoryRoom`.
    - Se encarga del mapeo entre los modelos del dominio (`Gasto`) y las entidades persistence de Room (`GastoEntity`).
    - Inyecta el `usuarioId` actual en cada consulta para asegurar que ningún usuario pueda consultar ni modificar gastos ajenos.

3. **Capa de Presentación (UI):**
    - Utiliza `ViewBinding` para la manipulación de vistas de forma segura.
    - Coordina la UI mediante corrutinas (`lifecycleScope.launch`) asegurando que las operaciones de base de datos se ejecuten en hilos de fondo (`Dispatchers.IO`).

---

## 📂 Estructura de Carpetas

El árbol siguiente corresponde al **proyecto Android**, no necesariamente a la raíz completa del repositorio. La biblioteca `gastos-core` es un proyecto Maven independiente con su propio `pom.xml` y `src/main/kotlin/`. Para la entrega, deben estar accesibles ambos códigos fuente.


```text
ControlGastos/
├── app/
│   ├── build.gradle.kts                # Configuración Gradle del módulo de la aplicación
│   └── src/
│       ├── androidTest/                # Pruebas de instrumentación en Android
│       │   └── java/umg/edu/gt/controlgastos/ExampleInstrumentedTest.kt
│       ├── test/                       # Pruebas unitarias locales de JUnit
│       │   └── java/umg/edu/gt/controlgastos/ExampleUnitTest.kt
│       └── main/
│           ├── AndroidManifest.xml     # Declaración de actividades y configuración global
│           ├── java/umg/edu/gt/controlgastos/
│           │   ├── GastoAdapter.kt     # Adaptador para el RecyclerView de gastos
│           │   ├── LoginActivity.kt    # Pantalla de inicio de sesión
│           │   ├── MainActivity.kt     # Pantalla principal (Navegación y Secciones)
│           │   ├── RegistroActivity.kt # Pantalla de registro de usuario
│           │   └── data/local/
│           │       ├── AppDatabase.kt             # Configuración de Room Database y Migraciones
│           │       ├── AutenticacionRepository.kt # Lógica de inicio de sesión y registro
│           │       ├── GastoDao.kt                # Consultas SQLite para Gastos
│           │       ├── GastoEntity.kt             # Entidad Room para la tabla 'gastos'
│           │       ├── GastoRepositoryRoom.kt     # Conector entre gastos-core y Room
│           │       ├── SeguridadPassword.kt       # Generación de Sal y Hash PBKDF2
│           │       ├── UsuarioDao.kt              # Consultas SQLite para Usuarios
│           │       └── UsuarioEntity.kt           # Entidad Room para la tabla 'usuarios'
│           └── res/
│               ├── drawable/           # Recursos vectoriales y fondos redondeados
│               ├── layout/             # Diseños XML (activity_main, login, registro, etc.)
│               ├── values/             # Colores, cadenas y temas para Modo Claro
│               └── values-night/       # Colores optimizados para Modo Oscuro
├── gradle/
│   └── libs.versions.toml              # Catálogo de versiones de dependencias (Version Catalog)
├── build.gradle.kts                    # Configuración raíz de Gradle
├── settings.gradle.kts                 # Configuración de repositorios Maven y módulos
└── README.md                           # Documentación del proyecto
```

---

## 🗄️ Persistencia de Datos y Seguridad

### Base de Datos (`AppDatabase.kt`)
La aplicación utiliza una base de datos local SQLite administrada por **Room Database** llamada `"control_gastos.db"`.

- **Versión Actual:** `3`
- **Migraciones Registradas:**
    - `MIGRATION_1_2`: Creación de la tabla `usuarios` e índice único para `nombreUsuario`.
    - `MIGRATION_2_3`: Adición de la columna `usuarioId` en la tabla `gastos` con valor por defecto `0`.

### Esquema de Tablas
- **Tabla `usuarios` (`UsuarioEntity.kt`):**
    - `id`: `INTEGER` (PRIMARY KEY AUTOINCREMENT)
    - `nombreUsuario`: `TEXT` (Índice Único)
    - `passwordHash`: `TEXT`
    - `passwordSalt`: `TEXT`

- **Tabla `gastos` (`GastoEntity.kt`):**
    - `id`: `INTEGER` (PRIMARY KEY AUTOINCREMENT)
    - `descripcion`: `TEXT`
    - `monto`: `REAL`
    - `categoria`: `TEXT`
    - `fecha`: `TEXT` (`AAAA-MM-DD`)
    - `usuarioId`: `INTEGER` (Clave de relación con la tabla usuarios)

### Seguridad de Contraseñas (`SeguridadPassword.kt`)
Las contraseñas de los usuarios jamás se almacenan en texto plano:
1. Al registrarse, se genera una sal aleatoria criptográfica de 16 bytes con `SecureRandom`.
2. Se calcula un hash utilizando **PBKDF2WithHmacSHA256** con 210,000 iteraciones y una clave de 256 bits.
3. Se almacena la sal y el hash codificados en Base64.
4. Al iniciar sesión, se rehace el procedimiento y se comparan los bytes mediante `MessageDigest.isEqual` en tiempo constante para evitar vulnerabilidades de análisis de tiempo.

---

## 🚀 Instalación y Ejecución

El proyecto utiliza **dos componentes independientes**: la aplicación Android (Gradle) y la biblioteca `gastos-core` (Maven). La aplicación consume `umg.edu.gt.progra2:gastos-core:1.0.1` desde el repositorio Maven local.

### Requisitos
- Android Studio compatible con la configuración Gradle del proyecto.
- JDK configurado para Android Studio/Gradle y Maven; verificar la versión exigida por el wrapper y AGP instalados.
- Maven para compilar e instalar `gastos-core`.
- Emulador o dispositivo Android compatible con el `minSdk` configurado (API 24 según la documentación del proyecto).

### Preparación

1. Clona el repositorio:

   ```bash
   git clone https://github.com/CJULAJ/Control-Gastos-app.git
   cd Control-Gastos-app
   ```

2. Localiza la carpeta del **código fuente de `gastos-core`** (la que contiene su `pom.xml`). Desde esa carpeta ejecuta:

   ```bash
   mvn clean test
   mvn clean install
   ```

   `mvn clean install` instala la biblioteca en `~/.m2/repository` (Windows: `%USERPROFILE%\.m2\repository`). Confirma que se genere `gastos-core-1.0.1.jar`. **Si el código del core no aparece en el repositorio clonado, es necesario incorporarlo antes de considerar reproducible la instalación.**

3. Localiza la carpeta del **proyecto Android** (la que contiene `settings.gradle.kts` y `gradlew`), ábrela en Android Studio y sincroniza Gradle. La configuración debe incluir `mavenLocal()` para resolver la biblioteca.

4. Compila desde esa carpeta:

   ```bash
   # macOS / Linux
   ./gradlew assembleDebug
   ```

   ```powershell
   # Windows PowerShell
   .\gradlew.bat assembleDebug
   ```

5. Ejecuta el módulo `app` en el emulador o dispositivo, o instala el APK generado en `app/build/outputs/apk/debug/` (ruta relativa a la raíz Android).

> **Nota sobre rutas:** el árbol de carpetas de este README muestra la estructura interna del proyecto Android. Si el repositorio contiene una carpeta contenedora para Android y otra para el core, hay que entrar en cada una antes de ejecutar sus respectivos comandos.

---

## 🧪 Pruebas y verificación

### Pruebas independientes de `gastos-core`

El core contiene la lógica de dominio en **Kotlin puro**, sin dependencias de Android. En el código fuente compartido se identifican `GastoRepository`, `GastoRepositoryEnMemoria`, `ControlGastos` y una entrada de consola (`Main.kt`), lo que permite ejercitar las operaciones de gastos sin abrir el emulador.

Durante el desarrollo se reportó la ejecución de **8 pruebas automatizadas con resultado `BUILD SUCCESS`**. Sin embargo, el archivo comprimido del core disponible para esta revisión es una versión anterior y **no incluye la clase de pruebas ni la configuración JUnit 5**; por tanto, ese resultado debe respaldarse con el código actualizado y una captura de la ejecución, antes de presentarlo como evidencia verificable.

Para ejecutar las pruebas desde la carpeta que contiene el `pom.xml` del core:

```bash
mvn clean test
```

Para ejecutar el menú de consola independiente de Android, si se conserva la configuración del plugin de ejecución:

```bash
mvn compile exec:java
```

Las pruebas del core deben cubrir, según su implementación actual, validaciones, registro y consulta de gastos, totales y agrupaciones por categoría. **No deben confundirse con las pruebas de plantilla del módulo Android.**

### Pruebas del módulo Android

- `app/src/test/.../ExampleUnitTest.kt`: prueba unitaria básica generada por la plantilla.
- `app/src/androidTest/.../ExampleInstrumentedTest.kt`: prueba básica de instrumentación en dispositivo o emulador.

Desde la raíz del proyecto Android:

```bash
./gradlew test
./gradlew connectedAndroidTest
```

En Windows, sustituye `./gradlew` por `.\gradlew.bat`. El segundo comando requiere un dispositivo o emulador conectado. La presencia de estos archivos no equivale a una cobertura automatizada completa del CRUD, autenticación o persistencia.

### Pruebas funcionales manuales

Durante el desarrollo se verificaron en la aplicación: registro, inicio/cierre de sesión, visibilidad de contraseña, CRUD de gastos, edición de fechas, filtros y estadísticas. Para la entrega se recomienda adjuntar capturas de los resultados de pruebas del core y una demostración de estas funciones en Android.

---

## 🔗 Repositorio

- **GitHub Repository:** [https://github.com/CJULAJ/Control-Gastos-app](https://github.com/CJULAJ/Control-Gastos-app)

---

## 👨‍🎓 Autor e Información Académica

- **Nombre Completo:** `[Completar con nombre completo]`
- **Carnet Universitario:** `[Tu Carnet Aquí]`
- **Universidad:** Universidad Mariano Gálvez de Guatemala
- **Curso:** Programación II
- **Carrera:** Ingeniería en Sistemas de Información y Ciencias de la Computación

---

## 📊 Estado del Proyecto

- **Estado:** `Versión funcional preparada para entrega`; pendiente de incorporar evidencias y comprobar la reproducción de la compilación desde un clon limpio.
- **Funcionalidades verificadas:**
    - Registro e Inicio de sesión con autenticación salteada PBKDF2.
    - CRUD completo de gastos (Agregar, Listar, Editar con DatePicker, Eliminar).
    - Filtrado por categoría y fecha.
    - Aislamiento estricto de registros por usuario.
    - Gráficas y desglose porcentual por categorías.
    - Soporte completo para Tema Claro y Tema Oscuro.
````
