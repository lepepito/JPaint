@echo off
cd /d "%~dp0"
if exist build rmdir /s /q build
mkdir build\paint\i18n

echo Compilation...
javac -encoding UTF-8 --release 8 -d build src\paint\*.java 2>nul
if errorlevel 1 javac -encoding UTF-8 -d build src\paint\*.java
if errorlevel 1 goto erreur

copy /y src\paint\i18n\*.properties build\paint\i18n\ >nul

echo Creation de JPaint.jar...
jar cfe JPaint.jar paint.Main -C build .
if errorlevel 1 goto erreur

rmdir /s /q build
echo.
echo JPaint.jar a ete cree. Double-cliquez dessus pour lancer l'application.
goto fin

:erreur
echo.
echo ERREUR : la compilation a echoue (verifiez que le JDK est installe).

:fin
pause
