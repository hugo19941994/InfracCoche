# UEM - Sistema de Detección, Alerta y Registro de Infracciones de Tráfico
Aplicación para Android que detecta infracciones de tráfico gracias a la visión por ordenador

## Dependencias
* Terminal Android 4.4+ (SDK mínimo aún por confirmar)
* [Android Studio 1.3+ ](http://developer.android.com/sdk/index.html)[(con Gradle 2.5 experimental)](http://tools.android.com/tech-docs/new-build-system/gradle-experimental)
  * [O en su defecto Eclipse](https://eclipse.org/downloads/)
* [OpenCV4Android 3.0.0](http://opencv.org/downloads.html)

## Configuración
1. Descargar Android Studio (No hace falta descargar OpenCV; viene incluido en nuestra aplicación de manera estática)
2. Instalar Android Studio
  * Incluir __SDK Manager__ si no se ha instalado con aterioridad
  * Incluir __AVD Manager__ si se desea ejecutar la aplicación virtualizada
3. Instalar __Android NDK__ y __Google Play Library__ (_Configure/SDK Manager/Extras/Ndk Bundle_ & _Google Play Service_)
4. Copiar adbkey a __Usuario/.android__
4. Importar Proyecto
5. Si hay problemas al compilar limpiar el entorno de trabajo (_Build/Clean Project_)

## Licencia
Este codigo usa la licencia [Attribution-NonCommercial-NoDerivatives 4.0 International](http://creativecommons.org/licenses/by-nc-nd/4.0/) de Creative Commons
