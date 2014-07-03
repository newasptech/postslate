java -version 2>&1 | find "64-Bit" >nul:

if errorlevel 1 (
    rem 32-bit
    set mem=1000
) else (
    rem 64-bit
    set mem=2000
)

@echo on
java -Xmx%mem%M -jar postslate.jar %*
