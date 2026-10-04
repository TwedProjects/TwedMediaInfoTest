# TwedMediaInfoTest

Aplicación Android mínima de prueba e integración para [TwedMediaInfo](https://github.com/TwedMusic/TwedMediaInfo).

Este proyecto funciona como un esqueleto de prueba durante el desarrollo de TwedMediaInfo. Su objetivo principal es comprobar que el AAR puede integrarse y utilizarse correctamente desde una aplicación Android real.

La aplicación también puede servir posteriormente como una referencia sencilla para mostrar cómo un desarrollador puede integrar TwedMediaInfo en una aplicación.

## Objetivo

`TwedMediaInfoTest` no es la biblioteca TwedMediaInfo.

Es una aplicación Android deliberadamente simple que permite probar la biblioteca mediante un flujo básico:

```text
Aplicación
    ↓
Selector de archivos del sistema
    ↓
Archivo multimedia seleccionado
    ↓
TwedMediaInfo
    ↓
Análisis del archivo
    ↓
Resultados mostrados en pantalla
```

La aplicación tendrá una única pantalla y evitará añadir funcionalidades que no sean necesarias para probar TwedMediaInfo.

## Funcionamiento

El flujo principal será:

1. El usuario abre la aplicación.
2. La aplicación muestra una pantalla sencilla.
3. El usuario pulsa un botón para seleccionar un archivo.
4. Android abre el selector de archivos del sistema.
5. El usuario selecciona un archivo multimedia.
6. La aplicación recibe el `Uri` proporcionado por Android.
7. El archivo se procesa mediante TwedMediaInfo.
8. Los resultados obtenidos se muestran en la misma pantalla.

Conceptualmente:

```text
┌─────────────────────────────┐
│      TwedMediaInfoTest      │
│                             │
│   [ Seleccionar archivo ]   │
│                             │
│   Archivo: example.mp3      │
│                             │
│   Resultados:               │
│   Format: MPEG Audio        │
│   Codec: ...                │
│   Duration: ...              │
│   ...                        │
└─────────────────────────────┘
```

La interfaz definitiva puede evolucionar durante el desarrollo, pero el proyecto debe mantenerse intencionalmente pequeño.

## Propósito durante el desarrollo

La aplicación se utilizará para validar TwedMediaInfo después de cambios en:

- JNI.
- Código C++.
- MediaInfoLib.
- API pública.
- Manejo de archivos.
- Manejo de `Uri`.
- Bibliotecas nativas.
- Empaquetado del AAR.
- Compatibilidad entre versiones de Android.
- Arquitecturas nativas.

La aplicación permite comprobar el comportamiento de TwedMediaInfo desde el punto de vista de un consumidor real del AAR.

## Arquitectura

La relación entre ambos proyectos es:

```text
TwedMediaInfo
    │
    └── Biblioteca Android (AAR)
            │
            ├── API pública
            ├── JNI
            ├── C++ adapter
            └── libtwedmediainfo.so
                    │
                    ├── MediaInfoLib
                    ├── ZenLib
                    └── zlib


TwedMediaInfoTest
    │
    └── Aplicación Android
            │
            └── Consume el AAR de TwedMediaInfo
```

`TwedMediaInfoTest` no debe duplicar la implementación interna de TwedMediaInfo.

La aplicación debe consumir la biblioteca mediante su API pública, de forma similar a como lo haría cualquier aplicación externa.

## Prueba mediante archivos

La aplicación utilizará el selector de archivos del sistema de Android en lugar de asumir que un archivo seleccionado corresponde a una ruta física accesible directamente.

El flujo esperado es:

```text
Android Storage Access Framework
            ↓
          Uri
            ↓
      TwedMediaInfo
            ↓
       MediaInfoLib
            ↓
        Información
            ↓
       Interfaz de prueba
```

Esto permite probar también la capacidad de TwedMediaInfo para trabajar correctamente con archivos proporcionados mediante `Uri` y `ContentResolver`.

## Resultados

Los resultados del análisis se mostrarán directamente en la pantalla.

La aplicación puede utilizarse para comprobar información como:

- formato;
- streams;
- codecs;
- duración;
- información técnica;
- metadata;
- parámetros proporcionados por MediaInfo;
- cualquier otra información expuesta por la API pública de TwedMediaInfo.

La cantidad y presentación de los datos dependerá de la API disponible en cada etapa del desarrollo.

## Biblioteca probada

El proyecto está destinado a probar:

**TwedMediaInfo**

La configuración objetivo de TwedMediaInfo incluye:

- MediaInfoLib 26.05
- ZenLib 0.4.41
- zlib 1.3.2
- Android API 27+
- `arm64-v8a`
- `armeabi-v7a`
- Java 17
- Kotlin JVM target 17
- `libtwedmediainfo.so`
- Compatibilidad con page-size de 16 KB

Las versiones y características definitivas deben comprobarse siempre contra la versión actual de TwedMediaInfo.

## Page-size de 16 KB

La compatibilidad con page-size de 16 KB es un requisito de TwedMediaInfo.

`TwedMediaInfoTest` permite complementar las validaciones de compilación comprobando que el AAR resultante funcione correctamente en una aplicación Android real.

La compilación exitosa del AAR no se considera por sí sola una validación completa de integración.

## Arquitecturas

La biblioteca está orientada actualmente a:

```text
arm64-v8a
armeabi-v7a
```

`TwedMediaInfoTest` sirve para probar el AAR correspondiente a estas arquitecturas en dispositivos reales.

## Flujo de desarrollo

El flujo principal es:

```text
Modificar TwedMediaInfo
        ↓
Compilar AAR
        ↓
Integrar AAR en TwedMediaInfoTest
        ↓
Compilar aplicación
        ↓
Instalar en dispositivo
        ↓
Seleccionar archivo multimedia
        ↓
Procesar mediante TwedMediaInfo
        ↓
Mostrar resultados
        ↓
Validar
```

Cuando se introducen cambios importantes en la biblioteca, esta aplicación permite detectar problemas de integración antes de considerar una versión como estable.

## Filosofía del proyecto

La aplicación debe mantenerse deliberadamente pequeña.

No pretende convertirse en:

- un reproductor multimedia;
- un gestor de archivos;
- una biblioteca de música;
- un editor de metadata;
- una aplicación multimedia completa.

Su función es únicamente proporcionar una interfaz mínima para probar y demostrar TwedMediaInfo.

## Estado

Proyecto en desarrollo.

La aplicación se utilizará principalmente como herramienta de integración durante el desarrollo de TwedMediaInfo y como ejemplo mínimo de utilización de la biblioteca.

## Relación con TwedMediaInfo

Repositorio principal:

[TwedMediaInfo](https://github.com/TwedMusic/TwedMediaInfo)

`TwedMediaInfo` contiene la biblioteca y su implementación.

`TwedMediaInfoTest` contiene únicamente la aplicación Android utilizada para probar y demostrar esa biblioteca.

## Licencia

Este proyecto no incluye una licencia específica.

`TwedMediaInfoTest` es un esqueleto de aplicación creado para probar y demostrar la integración de TwedMediaInfo durante su desarrollo.

No debe confundirse la ausencia de una licencia en este repositorio con las licencias de TwedMediaInfo o de las dependencias utilizadas por la biblioteca..

Para consultar las licencias correspondientes a TwedMediaInfo y sus dependencias, consulte el repositorio de la biblioteca:

[TwedMediaInfo](https://github.com/TwedMusic/TwedMediaInfo)
