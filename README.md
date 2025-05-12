# 🎮 Sistema de Gestión de Videojuegos

### 👥 INTEGRANTES DEL EQUIPO:

- Manuel Enriquez Grijalva
- Sebastian Fernando Armenta Pacheco
- Pablo Francisco Arellano Chavez
- Miguel Ángel Córdova Salcido

### ⚙️ INSTRUCCIONES DE USO:

- Crear una base de datos llamada videojuegos:
- CREATE DATABASE videojuegos;
- Ejecutar el archivo SQL videojuegos.sql para crear todas las tablas, triggers y datos iniciales.
- Interactuar con las tablas usando comandos SQL (en consola o Workbench):
- Agregar videojuegos (INSERT)
- Editar registros (UPDATE)
- Eliminar registros (DELETE)
- Verificar el contenido de la tabla log para observar los cambios registrados automáticamente por los triggers.
- Para restaurar una operación, copiar y ejecutar el contenido del campo sql_instruction desde la tabla log.

### 📌 FUNCIONALIDADES IMPLEMENTADAS:

Funcionalidad	                               Estado	Observaciones
Agregar videojuegos a la colección de usuarios	✔️	Tabla: game_collection
Modificar videojuegos y colecciones	        ✔️	Tabla: games, game_collection
Listar colección de un usuario	                ✔️	JOIN entre users, games, etc.
Listar juegos con más coleccionistas	        ✔️	COUNT + GROUP BY
Mostrar Top 5 juegos por rating	                ✔️	ORDER BY rating DESC LIMIT 5
Registro automático de cambios (bitácora)	✔️	Tabla: log con triggers
Restauración manual desde bitácora	        ✔️	Ejecutar sql_instruction

### 🧱 ESTRUCTURA DE TABLAS:

- users(id_user, username)
- platform(id_platform, name)
- games(id_game, name, rating, id_platform)
- game_collection(id_user, id_game)
- log(id, table_name, operation, sql_instruction, date_time)

### 🤖 DECLARACIÓN DE ASISTENCIA DIGITAL:

- Durante el desarrollo del proyecto se utilizó ChatGPT ,Aria y Deepseek para:
- Redacción de triggers SQL
- Generación de consultas de recuperación
- Explicación de conceptos relacionados con bitácoras
- Generación de partes del código.