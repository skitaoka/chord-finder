@echo off

if exist finder.jar ( del finder.jar )

javac -encoding utf-8 *.java
if errorlevel 1 ( goto end )

jar cvfm chord.jar manifest.mf splash.jpg *.class

:end
del *.class
pause
