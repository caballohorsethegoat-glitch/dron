# Dron - Fabric 1.21.1

- Entidad `dronmod:dron` (huevo generador en la pestana de huevos; tambien `/summon dronmod:dron`). No aparece solo.
- Zumbido de dron en bucle cuando esta cerca; las helices giran.
- Detecta jugadores en supervivencia/aventura (24 bloques), se acerca a ~5 bloques y flota.
- A menos de ~8.5 bloques y con linea de vision te agarra: camara bloqueada mirandolo.
- Sale una tecla ESPACIO animada y una barra: pulsa ESPACIO repetidamente para llenarla (baja sola si paras).
- Al liberarte el dron huye ~20 s y luego vuelve.

Compilar: ver `.github/workflows/build.yml` (GitHub Actions) o `gradle build` con JDK 21. Requiere Fabric API 1.21.1.
