import java.sql.*;
import java.util.*;

public class VideoGameCollectionProyect {
    static Connection conexion = null;
    static int userID = 0;
    static String access_type = "";
    static String ip = "localhost:3306";
    static int usuarioID = 0; // para actualizar usuarios
    static boolean isAdmin = false;

    public static void main(String[] args) {
        try {
            // Establecer conexión a la base de datos
            ConectarABaseD();

            // Realizar login
            if (!login()) {
                System.out.println("Acceso denegado. Saliendo del sistema...");
                return;
            }

            // Agregar checkpoint inicial
            agregaCheckpoint();

            // Mostrar menú principal si el login fue exitoso
            mostrarMenuPrincipal();

        } catch (SQLException e) {
            System.err.println("Error en la conexión o consultas SQL: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Error inesperado: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Cerrar conexión en el bloque finally para asegurarnos que siempre se cierre
            try {
                if (conexion != null && !conexion.isClosed()) {
                    conexion.close();
                    System.out.println("Conexión a la base de datos cerrada.");
                }
            } catch (SQLException e) {
                System.err.println("Error al cerrar la conexión: " + e.getMessage());
            }
        }
    }

    public static Connection ConectarABaseD() {
        try {
            String url = "jdbc:mysql://" + ip + "/videogame_collection?useSSL=false&useTimezone=true&serverTimezone=UTC";
            String usuario = "root";
            String contraseña = ""; // Cambia si tiene contraseña

            conexion = DriverManager.getConnection(url, usuario, contraseña);
            System.out.println("¡Conexión exitosa!");
        } catch (SQLException e) {
            System.err.println("Error al conectar: " + e.getMessage());
        }
        return conexion;
    }

    public static boolean login() {
        Scanner scan = new Scanner(System.in);
        int intentos = 3;

        try {
            while (intentos > 0) {
                System.out.println("\n=== INICIO DE SESIÓN ===");
                System.out.println("Intentos restantes: " + intentos);
                System.out.print("Usuario: ");
                String username = scan.nextLine();

                System.out.print("Contraseña: ");
                String password = scan.nextLine();

                String sql = "SELECT user_id, access_type FROM users " +
                        "WHERE username = ? AND password = SHA2(?, 256) AND active = TRUE";

                try (PreparedStatement pstmt = conexion.prepareStatement(sql)) {
                    pstmt.setString(1, username);
                    pstmt.setString(2, password);

                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            userID = rs.getInt("user_id");
                            isAdmin = rs.getString("access_type").equals("admin");
                            System.out.println("\n¡Acceso concedido! Bienvenido, " + username + ".");
                            return true;
                        } else {
                            System.out.println("\nAcceso denegado: Usuario o contraseña incorrectos.");
                            intentos--;
                        }
                    }
                }
            }

            System.out.println("\nHas excedido el número máximo de intentos. Saliendo del sistema...");
            return false;

        } catch (SQLException e) {
            System.err.println("Error en el sistema de login: " + e.getMessage());
            return false;
        }
    }

    public static void agregaCheckpoint(){
        try {
            String SQL = "INSERT INTO log (user_id, action_type, table_name, record_id, sql_instruction) " +
                    "VALUES (?,'CHECKP', 'None', 0, 'None')";
            PreparedStatement inst = conexion.prepareStatement(SQL);
            inst.setInt(1, userID);
            inst.executeUpdate();
            System.out.println("Checkpoint establecido con exito");
        } catch (SQLException e) {
            System.out.println("agregar checkpoint: " + e.getMessage());
        }
    }

    public static ResultSet transactionSelect(PreparedStatement inst){
        ResultSet result = null;
        try {
            //System.out.println("SQL:"+inst.toString());
            result = inst.executeQuery();
        } catch (SQLException s){
            s.printStackTrace();
        }
        return result;
    }

    public static void despliegaResultados(ResultSet resultados,String tabla) {
        System.out.println("Tabla:"+tabla);
        try {
            //Mostrado columnas
            ResultSetMetaData metaDatos = resultados.getMetaData();
            int columnas = metaDatos.getColumnCount();
            for (int i = 1; i <= columnas; i++) {
                System.out.print("\t," + metaDatos.getColumnName(i));
            }
            System.out.println("");
            //Mostramos los registros
            while(resultados.next()) {
                for(int i = 1; i <= columnas; i++) {
                    System.out.print("\t" + resultados.getObject(i));
                }
                System.out.println("");
            }
            System.out.println("");
        }  catch (SQLException s){
            s.printStackTrace();
        }
    }

    public static void despliegaTabla(String tabla, String columna,String condicion) {
        try {
            if (conexion!=null) {
                conexion.setAutoCommit(false);
                //Preparamos SQL
                if (condicion.length()==0) {
                    condicion = "1";
                }
                String SQL = "SELECT $campo FROM $tableName WHERE $cond";
                String query = SQL.replace("$tableName", tabla);
                query = query.replace("$campo",columna);
                query = query.replace("$cond", condicion);
                PreparedStatement instruccionP = conexion.prepareStatement(query);
                ResultSet resultados = transactionSelect(instruccionP);
                conexion.commit(); // Comprometemos transaccion
                despliegaResultados(resultados,tabla);
            }
        }   catch (SQLException s){
            s.printStackTrace();
            try {
                if (conexion != null) {
                    conexion.rollback();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void addUser() {
        System.out.println("***Agregar nuevo usuario ***");
        Scanner scan = new Scanner(System.in);
        System.out.print("Username:");
        String userName = scan.nextLine();
        System.out.print("Email:");
        String email = scan.nextLine();
        System.out.print("Password:");
        String passWord = scan.nextLine();
        System.out.print("Access type (user/admin):");
        String accessType = scan.nextLine();

        String SQL = "INSERT INTO users (username, email, password, access_type, preferred_platform_id, active) " +
                "VALUES (?, ?, SHA2(?, 256), ?, ?, TRUE)";

        try {
            PreparedStatement statement = conexion.prepareStatement(SQL);
            statement.setString(1, userName);
            statement.setString(2, email);
            statement.setString(3, passWord);
            statement.setString(4, accessType);
            if (actualizaBaseDatos(statement,"INSERT", "users"))
                System.out.println("Usuario agregado exitosamente");;
        } catch (SQLException s) {
            System.out.println(s.getMessage());
        }

    }

    public static void deleteUser() {
        String t,c,w;
        t = "users";
        c = "*";
        w = "1";
        despliegaTabla(t,c,w);

        Scanner scan = new Scanner(System.in);
        System.out.print("ID de ususario a eliminar: ");
        int idUsuario = scan.nextInt();

        if(idUsuario > 0) {
            try {
                String SQL = "UPDATE users SET active = 0 "
                        + "WHERE users.user_id = ?";
                PreparedStatement statement = conexion.prepareStatement(SQL);
                statement.setInt(1, idUsuario);
                if(actualizaBaseDatos(statement, "DELETE" , "users")) {
                    System.out.println("Usuario eliminado exitosamente");
                } else {
                    System.out.println("El usuario no fue eliminado");
                }
            } catch (SQLException s) {
                System.out.println(s.getMessage());
            }
        }
    }

    public static void modifyUser() {
        String t,c,w;
        t = "users";
        c = "id, username, email, access_type";
        w = "1";
        despliegaTabla(t,c,w);

        Scanner scan = new Scanner(System.in);
        System.out.print("ID de usuario a modificar:");
        int idUsuario = scan.nextInt();
        if (idUsuario >0) {
            usuarioID = idUsuario;
            try {
                String SQL = "SELECT username, email, access_type "
                        + "FROM users "
                        + "WHERE users.user_id = ?";
                PreparedStatement statement = conexion.prepareStatement(SQL);
                statement.setInt(1, idUsuario);
                String user ="";
                String email="";
                String access_type="";
                ResultSet rs = statement.executeQuery();
                while(rs.next()) {
                    user = rs.getString("username");
                    email= rs.getString("email");
                    access_type= rs.getString("access_type");
                }
                System.out.println("username:"+user);
                System.out.println("email:"+email);
                System.out.println("admin:"+access_type);
                String newUser, newEmail, newAdmin;
                Scanner sc = new Scanner(System.in);
                System.out.print("Nuevo username: ");
                newUser = sc.nextLine();
                System.out.print("Nuevo email: ");
                newEmail = sc.nextLine();
                System.out.print("Nuevo admin (true/false): ");
                newAdmin = sc.nextLine();
                String campos = "";
                if (newUser.length()>0) {

                    campos = "username = '$user' ";
                    campos = campos.replace("$user", newUser);
                    if (newEmail.length()>0) {
                        campos = campos +", email ='$em' ";
                        campos = campos.replace("$em",newEmail);
                    }
                    if (newAdmin.length()>0) {
                        campos = campos + ", access_type = $ad ";
                        campos = campos.replace("$ad",newAdmin);
                    }
                } else {
                    if (newEmail.length()>0) {
                        campos = "email = '$em' ";
                        campos = campos.replace("$em",newEmail);
                        if (newAdmin.length()>0) {
                            campos = campos + ", access_type = $ad ";
                            campos = campos.replace("$ad",newAdmin);
                        }
                    } else {
                        if (newAdmin.length()>0) {
                            campos = campos + "access_type = $ad ";
                            campos = campos.replace("$ad",newAdmin);
                        }
                    }
                }
                System.out.println("longitud de campos:("+campos.length()+ ")|"+campos);
                if(!campos.isEmpty()) {

                    SQL = "UPDATE users SET "
                            + campos + " WHERE user_id = ?";

                    statement = conexion.prepareStatement(SQL);
                    statement.setInt(1, idUsuario);

                    if (actualizaBaseDatos(statement, "UPDATE", "users")) {
                        System.out.println("Usuario modificado exitosamente");
                    } else {
                        System.out.println("El usuario no fue modificado");
                    }

                }

            } catch (SQLException s) {
                System.out.println(s.getMessage());
            }
        }
    }

    public static void modifyPassword() {

        if (access_type.equals("admin")) {
            String t, c, w;
            t = "users";
            c = "user_id, username, password";
            w = "1";
            despliegaTabla(t, c, w);

            Scanner scan = new Scanner(System.in);
            System.out.print("ID de usuario a modificar:");
            int idUsuario = scan.nextInt();
            if (idUsuario > 0) {
                capturaPassword(idUsuario);
            }
        } else { //ususario no admin
            capturaPassword(userID);
        }
    }

    public static void addPlatform(){
        String t,c,w;
        t = "platform";
        c = "*";
        w = "1";
        despliegaTabla(t,c,w);
        Scanner scan = new Scanner(System.in);
        System.out.print("!Ingrese el nombre de la plataforma a agregar.!");
        String newPlatform = scan.nextLine();

        String SQL = "INSERT INTO platform(platform_name) " +
                "VALUES (?)";
        try {
            PreparedStatement statement = conexion.prepareStatement(SQL);
            statement.setString(1, newPlatform);

            if (actualizaBaseDatos(statement, "INSERT" , "platform")){
                System.out.println("Usuario eliminado exitosamente");
            } else {
                System.out.println("El usuario no fue eliminado");
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void modifyPlatform() {
        // Mostrar todas las plataformas activas
        String t = "platform";
        String c = "platform_id, platform_name, active";
        String w = "1";
        despliegaTabla(t, c, w);

        Scanner scan = new Scanner(System.in);
        System.out.print("ID de Plataforma a modificar: ");
        int idPlataforma = scan.nextInt();

        if (idPlataforma > 0) {
            try {
                // Obtener los datos actuales de la plataforma
                String SQL = "SELECT platform_name, active FROM platform WHERE platform_id = ?";
                PreparedStatement statement = conexion.prepareStatement(SQL);
                statement.setInt(1, idPlataforma);

                ResultSet rs = statement.executeQuery();
                if (rs.next()) {
                    String nombreActual = rs.getString("platform_name");
                    boolean activoActual = rs.getBoolean("active");

                    // Mostrar datos actuales
                    System.out.println("\nDatos actuales de la plataforma:");
                    System.out.println("Nombre: " + nombreActual);
                    System.out.println("Estado: " + (activoActual ? "ACTIVA" : "INACTIVA"));

                    // Solicitar nuevos datos
                    Scanner sc = new Scanner(System.in);
                    System.out.print("\nNuevo nombre (dejar vacío para no cambiar): ");
                    String nuevoNombre = sc.nextLine();

                    System.out.print("¿Activar/Desactivar? (s/n, dejar vacío para no cambiar): ");
                    String opcionActivo = sc.nextLine().toLowerCase();
                    Boolean nuevoActivo = null;

                    if (!opcionActivo.isEmpty()) {
                        nuevoActivo = opcionActivo.equals("s");
                    }

                    // Construir la consulta de actualización
                    StringBuilder campos = new StringBuilder();
                    List<Object> parametros = new ArrayList<>();

                    if (!nuevoNombre.isEmpty()) {
                        campos.append("platform_name = ?");
                        parametros.add(nuevoNombre);
                    }

                    if (nuevoActivo != null) {
                        if (campos.length() > 0) campos.append(", ");
                        campos.append("active = ?");
                        parametros.add(nuevoActivo);
                    }

                    // Solo actualizar si hay cambios
                    if (campos.length() > 0) {
                        SQL = "UPDATE platform SET " + campos.toString() + " WHERE platform_id = ?";
                        parametros.add(idPlataforma);

                        statement = conexion.prepareStatement(SQL);
                        for (int i = 0; i < parametros.size(); i++) {
                            statement.setObject(i + 1, parametros.get(i));
                        }

                        int filasActualizadas = statement.executeUpdate();
                        if (filasActualizadas > 0) {
                            System.out.println("\nPlataforma modificada exitosamente");
                        } else {
                            System.out.println("\nNo se pudo modificar la plataforma");
                        }
                    } else {
                        System.out.println("\nNo se realizaron cambios");
                    }
                } else {
                    System.out.println("\nNo se encontró la plataforma con ID: " + idPlataforma);
                }
            } catch (SQLException s) {
                System.out.println("Error al modificar plataforma: " + s.getMessage());
            }
        } else {
            System.out.println("ID de plataforma no válido");
        }
    }

    public static void deletePlatform() {
        String t,c,w;
        t = "platform";
        c = "*";
        w = "1";
        despliegaTabla(t,c,w);
        Scanner scan = new Scanner(System.in);
        System.out.print("!Ingrese el id de la plataforma a desactivar.!");
        int dPlatform = scan.nextInt();

        if(dPlatform > 0) {
            try {
                String SQL = "UPDATE platform SET active = 0 "
                        + "WHERE platform.platform_id = ?";
                PreparedStatement statement = conexion.prepareStatement(SQL);
                statement.setInt(1, dPlatform);
                if(actualizaBaseDatos(statement, "DELETE" , "platform")) {
                    System.out.println("Plataforma desactivada exitosamente");
                } else {
                    System.out.println("La plataforma no fue desactivada");
                }
            } catch (SQLException s) {
                System.out.println(s.getMessage());
            }
        }
    }

    public static void addGame() {
        Scanner scan = new Scanner(System.in);
        System.out.print("!Ingrese el nombre del videojuego a agregar.!");
        String newGame = scan.nextLine();
        System.out.print("!Ingrese el id de la plataforma del videojuego.!");
        int idPlatfrom = scan.nextInt();
        System.out.print("!Ingrese la fecha que salio el videojuego (example = 1999)");
        int fecha_salida = scan.nextInt();
        System.out.print("!Ingrese url de la imagen del videojuego.!");
        String url = scan.nextLine();

        String SQL = "INSERT INTO `games`(`game_name`, `platform_id`, `year_released`, `image_url`,) " +
                "VALUES (?,?,?,?)";
        try {
            PreparedStatement statement = conexion.prepareStatement(SQL);
            statement.setString(1, newGame);
            statement.setInt(2, idPlatfrom);
            statement.setInt(3,fecha_salida);
            statement.setString(4,url);
            if (actualizaBaseDatos(statement, "INSERT" , "games")) {
                System.out.println("Videojuego agregado con exito!");
            } else {
                System.out.println("No fue posible agregar el videojuego");
            }
        } catch (SQLException e) {
            System.out.printf(e.getMessage());
        }

    }

    public static void modifyGame() throws SQLException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("\n=== MODIFICAR JUEGO ===");

        // Mostrar listado de juegos activos
        String listSql = "SELECT g.game_id, g.game_name, p.platform_name, g.year_released, " +
                "CASE WHEN g.active THEN 'Activo' ELSE 'Inactivo' END as estado " +
                "FROM games g JOIN platform p ON g.platform_id = p.platform_id " +
                "WHERE g.active = TRUE " +
                "ORDER BY g.game_name";

        try (Statement stmt = conexion.createStatement();
             ResultSet rs = stmt.executeQuery(listSql)) {

            System.out.printf("%-5s %-30s %-20s %-10s %-10s%n", "ID", "NOMBRE", "PLATAFORMA", "AÑO", "ESTADO");
            System.out.println("------------------------------------------------------------");

            while(rs.next()) {
                System.out.printf("%-5d %-30s %-20s %-10d %-10s%n",
                        rs.getInt("game_id"),
                        rs.getString("game_name"),
                        rs.getString("platform_name"),
                        rs.getInt("year_released"),
                        rs.getString("estado"));
            }
        }

        // Seleccionar juego a modificar
        System.out.print("\nID del juego a modificar: ");
        int gameId = scanner.nextInt();
        scanner.nextLine(); // Limpiar buffer

        // Obtener datos actuales del juego
        String selectSql = "SELECT g.game_name, g.platform_id, p.platform_name, g.year_released, g.image_url, g.active " +
                "FROM games g JOIN platform p ON g.platform_id = p.platform_id " +
                "WHERE g.game_id = ?";

        try (PreparedStatement pstmt = conexion.prepareStatement(selectSql)) {
            pstmt.setInt(1, gameId);
            ResultSet rs = pstmt.executeQuery();

            if(rs.next()) {
                String currentName = rs.getString("game_name");
                int currentPlatformId = rs.getInt("platform_id");
                String currentPlatformName = rs.getString("platform_name");
                int currentYear = rs.getInt("year_released");
                String currentImageUrl = rs.getString("image_url");
                boolean currentActive = rs.getBoolean("active");

                // Mostrar datos actuales
                System.out.println("\nDatos actuales del juego:");
                System.out.println("Nombre: " + currentName);
                System.out.println("Plataforma: " + currentPlatformName + " (ID: " + currentPlatformId + ")");
                System.out.println("Año de lanzamiento: " + (currentYear > 0 ? currentYear : "No especificado"));
                System.out.println("URL de imagen: " + (currentImageUrl != null ? currentImageUrl : "No especificada"));
                System.out.println("Estado: " + (currentActive ? "Activo" : "Inactivo"));

                // Solicitar nuevos datos
                System.out.print("\nNuevo nombre (dejar vacío para mantener actual): ");
                String newName = scanner.nextLine();

                // Mostrar plataformas disponibles
                System.out.println("\nPlataformas disponibles:");
                String platformsSql = "SELECT platform_id, platform_name FROM platform WHERE active = TRUE ORDER BY platform_name";
                try (Statement stmt = conexion.createStatement();
                     ResultSet platformsRs = stmt.executeQuery(platformsSql)) {
                    while(platformsRs.next()) {
                        System.out.println(platformsRs.getInt("platform_id") + ". " + platformsRs.getString("platform_name"));
                    }
                }

                System.out.print("Nueva plataforma ID (-1 para mantener actual): ");
                int newPlatformId = scanner.nextInt();
                scanner.nextLine();

                System.out.print("Nuevo año de lanzamiento (dejar vacío para mantener actual): ");
                String yearInput = scanner.nextLine();
                Integer newYear = yearInput.isEmpty() ? null : Integer.parseInt(yearInput);

                System.out.print("Nueva URL de imagen (dejar vacío para mantener actual): ");
                String newImageUrl = scanner.nextLine();

                System.out.print("¿Cambiar estado? (s/n): ");
                String changeStatus = scanner.nextLine().toLowerCase();
                Boolean newActive = null;

                if(changeStatus.equals("s")) {
                    System.out.print("¿Activar? (s/n): ");
                    String activate = scanner.nextLine().toLowerCase();
                    newActive = activate.equals("s");
                }

                // Construir la consulta de actualización
                StringBuilder updates = new StringBuilder();
                List<Object> params = new ArrayList<>();

                if(!newName.isEmpty()) {
                    updates.append("game_name = ?, ");
                    params.add(newName);
                }

                if(newPlatformId != -1) {
                    updates.append("platform_id = ?, ");
                    params.add(newPlatformId);
                }

                if(newYear != null) {
                    updates.append("year_released = ?, ");
                    params.add(newYear);
                }

                if(!newImageUrl.isEmpty()) {
                    updates.append("image_url = ?, ");
                    params.add(newImageUrl.isEmpty() ? null : newImageUrl);
                }

                if(newActive != null) {
                    updates.append("active = ?, ");
                    params.add(newActive);
                }

                // Si hay campos para actualizar
                if(updates.length() > 0) {
                    updates.setLength(updates.length() - 2); // Eliminar última coma y espacio
                    String updateSql = "UPDATE games SET " + updates.toString() + " WHERE game_id = ?";
                    params.add(gameId);

                    try (PreparedStatement updateStmt = conexion.prepareStatement(updateSql)) {
                        for(int i = 0; i < params.size(); i++) {
                            updateStmt.setObject(i + 1, params.get(i));
                        }

                        // Ejecutar la actualización con transacción
                        conexion.setAutoCommit(false);
                        int affectedRows = updateStmt.executeUpdate();

                        if(affectedRows > 0) {
                            // Verificar cambios
                            boolean cambiosRealizados = true;

                            // Si solo se cambió el estado activo, verificar ese campo
                            if(updates.toString().equals("active = ?")) {
                                String checkSql = "SELECT active FROM games WHERE game_id = ?";
                                try (PreparedStatement checkStmt = conexion.prepareStatement(checkSql)) {
                                    checkStmt.setInt(1, gameId);
                                    ResultSet checkRs = checkStmt.executeQuery();
                                    if(checkRs.next()) {
                                        cambiosRealizados = (checkRs.getBoolean("active") == newActive);
                                    }
                                }
                            }

                            if(cambiosRealizados) {
                                conexion.commit();
                                System.out.println("\n¡Juego actualizado exitosamente!");

                                // Mostrar datos actualizados
                                String showSql = "SELECT g.game_name, p.platform_name, g.year_released, " +
                                        "CASE WHEN g.active THEN 'Activo' ELSE 'Inactivo' END as estado " +
                                        "FROM games g JOIN platform p ON g.platform_id = p.platform_id " +
                                        "WHERE g.game_id = ?";
                                try (PreparedStatement showStmt = conexion.prepareStatement(showSql)) {
                                    showStmt.setInt(1, gameId);
                                    ResultSet showRs = showStmt.executeQuery();
                                    if(showRs.next()) {
                                        System.out.println("\nDatos actualizados:");
                                        System.out.println("Nombre: " + showRs.getString("game_name"));
                                        System.out.println("Plataforma: " + showRs.getString("platform_name"));
                                        System.out.println("Año: " + showRs.getInt("year_released"));
                                        System.out.println("Estado: " + showRs.getString("estado"));
                                    }
                                }
                            } else {
                                conexion.rollback();
                                System.out.println("\nNo se detectaron cambios. La actualización fue revertida.");
                            }
                        } else {
                            conexion.rollback();
                            System.out.println("\nNo se encontró el juego con ID: " + gameId);
                        }
                    }
                } else {
                    System.out.println("\nNo se realizaron cambios.");
                }
            } else {
                System.out.println("\nNo se encontró el juego con ID: " + gameId);
            }
        } catch (NumberFormatException e) {
            System.out.println("\nError: El año debe ser un número válido.");
        } catch (SQLException e) {
            try {
                if(conexion != null) conexion.rollback();
            } catch (SQLException ex) {
                System.err.println("Error al hacer rollback: " + ex.getMessage());
            }
            System.err.println("Error al modificar juego: " + e.getMessage());
        } finally {
            try {
                conexion.setAutoCommit(true);
            } catch (SQLException e) {
                System.err.println("Error al restaurar auto-commit: " + e.getMessage());
            }
        }
    }

    public static void deleteGame() {
        String t,c,w;
        t = "games";
        c = "*";
        w = "1";
        despliegaTabla(t,c,w);

        Scanner scan = new Scanner(System.in);
        System.out.print("ID de juego a eliminar: ");
        int idGame = scan.nextInt();

        if(idGame > 0) {
            try {
                String SQL = "UPDATE games SET active = 0 "
                        + "WHERE games.game_id = ?";
                PreparedStatement statement = conexion.prepareStatement(SQL);
                statement.setInt(1, idGame);
                if(actualizaBaseDatos(statement, "DELETE" , "games")) {
                    System.out.println("juego eliminado exitosamente");
                } else {
                    System.out.println("El juego no fue eliminado");
                }
            } catch (SQLException s) {
                System.out.println(s.getMessage());
            }
        }
    }

    public static void showUserCollection(int userId) throws SQLException {
        String sql = "SELECT gc.collection_id, g.game_name, p.platform_name, gc.rating " +
                "FROM game_collection gc " +
                "JOIN games g ON gc.game_id = g.game_id " +
                "JOIN platform p ON g.platform_id = p.platform_id " +
                "WHERE gc.user_id = ? AND gc.active = TRUE AND g.active = TRUE AND p.active = TRUE " +
                "ORDER BY g.game_name";

        try (PreparedStatement pstmt = conexion.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                System.out.printf("\n%-5s %-30s %-20s %-10s\n", "ID", "JUEGO", "PLATAFORMA", "RATING");
                System.out.println("----------------------------------------------------");

                while (rs.next()) {
                    System.out.printf("%-5d %-30s %-20s %-10d\n",
                            rs.getInt("collection_id"),
                            rs.getString("game_name"),
                            rs.getString("platform_name"),
                            rs.getInt("rating"));
                }
            }
        }
    }

    private static void showCollectionDetails(int collectionId) throws SQLException {
        String sql = "SELECT gc.collection_id, g.game_name, p.platform_name, gc.rating, " +
                "CASE WHEN gc.active THEN 'Activo' ELSE 'Inactivo' END as estado " +
                "FROM game_collection gc " +
                "JOIN games g ON gc.game_id = g.game_id " +
                "JOIN platform p ON g.platform_id = p.platform_id " +
                "WHERE gc.collection_id = ?";

        try (PreparedStatement pstmt = conexion.prepareStatement(sql)) {
            pstmt.setInt(1, collectionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println("ID: " + rs.getInt("collection_id"));
                    System.out.println("Juego: " + rs.getString("game_name"));
                    System.out.println("Plataforma: " + rs.getString("platform_name"));
                    System.out.println("Rating: " + rs.getInt("rating"));
                    System.out.println("Estado: " + rs.getString("estado"));
                }
            }
        }
    }

    public static void capturaPassword(int idDeUsuario) {
        usuarioID = idDeUsuario;
        Scanner sc = new Scanner(System.in);
        System.out.print("Nuevo password: ");
        String nuevoPassword = sc.nextLine();
        if (nuevoPassword.length() > 4) {
            System.out.print("Confirmar nuevo password: ");
            String confPassword = sc.nextLine();

            if (nuevoPassword.equals(confPassword)) {
                try {
                    String SQL = "UPDATE users SET "
                            + "password =SHA2(?,256) "
                            + "WHERE user_id = ?";
                    PreparedStatement statement = conexion.prepareStatement(SQL);
                    statement.setString(1, nuevoPassword);
                    statement.setInt(2, idDeUsuario);
                    System.out.println(statement.toString());
                    if (actualizaBaseDatos(statement, "UPDATE" , "users")) {
                        System.out.println("Password modificada correctamente");
                    } else {
                        System.out.println("No fue posible modificar el password");
                    }
                } catch (SQLException s) {
                    System.out.println(s.getMessage());
                }
            }
        } else {
            System.out.println("La longitud debe ser mayor a 4 caracteres");
        }
    }

    public static boolean addGame_Collection() {
        Scanner scan = new Scanner(System.in);
        System.out.print("Ingrese el id del juego: ");
        int gameId = scan.nextInt();
        System.out.print("Ingrese el id del usuario: ");
        int userId = scan.nextInt();
        System.out.print("Ingrese el rating del videojuego: ");
        int rating = scan.nextInt();

        String sql = "INSERT INTO game_collection (user_id, game_id, rating, active) VALUES (?, ?, ?, TRUE) " +
                "ON DUPLICATE KEY UPDATE active = TRUE, rating = ?";

        try {
            // Verificar que el juego existe y está activo
            if (!isGameActive(gameId)) {
                System.out.println("Error: El juego no existe o está inactivo");
                return false;
            }

            // Verificar que el usuario existe y está activo
            if (!isUserActive(userId)) {
                System.out.println("Error: El usuario no existe o está inactivo");
                return false;
            }

            // Validar rating (1-10)
            if (rating < 1 || rating > 10) {
                System.out.println("Error: El rating debe estar entre 1 y 10");
                return false;
            }

            conexion.setAutoCommit(false);

            try (PreparedStatement pstmt = conexion.prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                pstmt.setInt(2, gameId);
                pstmt.setInt(3, rating);
                pstmt.setInt(4, rating);

                int affectedRows = pstmt.executeUpdate();

                if (affectedRows > 0) {
                    conexion.commit();
                    System.out.println("Juego agregado a la colección exitosamente");
                    return true;
                } else {
                    conexion.rollback();
                    return false;
                }
            }
        } catch (SQLException e) {
            try {
                conexion.rollback();
            } catch (SQLException ex) {
                System.err.println("Error al hacer rollback: " + ex.getMessage());
            }
            System.err.println("Error al agregar juego a colección: " + e.getMessage());
            return false;
        } finally {
            try {
                conexion.setAutoCommit(true);
            } catch (SQLException e) {
                System.err.println("Error al restaurar auto-commit: " + e.getMessage());
            }
        }
    }

    // Métodos auxiliares de verificación
    private static boolean isGameActive(int gameId) throws SQLException {
        String sql = "SELECT game_id FROM games WHERE game_id = ? AND active = TRUE";
        try (PreparedStatement pstmt = conexion.prepareStatement(sql)) {
            pstmt.setInt(1, gameId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static boolean isUserActive(int userId) throws SQLException {
        String sql = "SELECT user_id FROM users WHERE user_id = ? AND active = TRUE";
        try (PreparedStatement pstmt = conexion.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public static void modifyGame_Collection() {
        Scanner scanner = new Scanner(System.in);

        try {
            // Mostrar la colección actual del usuario
            System.out.println("\n=== MODIFICAR ENTRADA DE COLECCIÓN ===");
            showUserCollection(userID);

            // Solicitar ID de la colección a modificar
            System.out.print("\nIngrese el ID de la entrada de colección a modificar: ");
            while (!scanner.hasNextInt()) {
                System.out.println("Error: Debe ingresar un número válido");
                System.out.print("Ingrese el ID de la entrada de colección a modificar: ");
                scanner.next(); // Limpiar el valor incorrecto
            }
            int collectionId = scanner.nextInt();
            scanner.nextLine(); // Limpiar el buffer

            // Verificar que la entrada de colección pertenece al usuario actual
            if (!validateCollectionOwnership(collectionId, userID)) {
                System.out.println("Error: No puedes modificar una entrada que no te pertenece");
                return;
            }

            // Mostrar datos actuales
            System.out.println("\nDatos actuales de la entrada:");
            showCollectionDetails(collectionId);

            // Solicitar nuevos datos
            System.out.println("\nIngrese los nuevos valores (deje vacío para mantener el actual):");

            // Rating
            Integer newRating = null;
            while (true) {
                System.out.print("Nuevo rating (1-10): ");
                String ratingInput = scanner.nextLine();
                if (ratingInput.isEmpty()) {
                    break;
                }
                try {
                    int rating = Integer.parseInt(ratingInput);
                    if (rating >= 1 && rating <= 10) {
                        newRating = rating;
                        break;
                    } else {
                        System.out.println("Error: El rating debe estar entre 1 y 10");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Error: Ingrese un número válido");
                }
            }

            // Estado (activo/inactivo)
            Boolean newActive = null;
            System.out.print("¿Cambiar estado? (s/n): ");
            String changeStatus = scanner.nextLine().trim().toLowerCase();
            if (changeStatus.equals("s")) {
                System.out.print("¿Activar entrada? (s/n): ");
                String activateInput = scanner.nextLine().trim().toLowerCase();
                newActive = activateInput.equals("s");
            }

            // Validar que al menos un campo va a ser modificado
            if (newRating == null && newActive == null) {
                System.out.println("\nNo se realizaron cambios - no se especificaron valores nuevos");
                return;
            }

            // Construir la consulta SQL
            StringBuilder sql = new StringBuilder("UPDATE game_collection SET ");
            List<Object> params = new ArrayList<>();

            if (newRating != null) {
                sql.append("rating = ?, ");
                params.add(newRating);
            }

            if (newActive != null) {
                sql.append("active = ?, ");
                params.add(newActive);
            }

            // Eliminar la última coma y espacio
            sql.setLength(sql.length() - 2);
            sql.append(" WHERE collection_id = ?");
            params.add(collectionId);

            // Ejecutar la actualización
            conexion.setAutoCommit(false);
            try (PreparedStatement pstmt = conexion.prepareStatement(sql.toString())) {
                // Establecer parámetros
                for (int i = 0; i < params.size(); i++) {
                    pstmt.setObject(i + 1, params.get(i));
                }

                int affectedRows = pstmt.executeUpdate();

                if (affectedRows > 0) {
                    // Verificar cambios
                    boolean changesVerified = verifyChanges(collectionId, newRating, newActive);

                    if (changesVerified) {
                        conexion.commit();
                        System.out.println("\n¡Entrada de colección actualizada exitosamente!");

                        // Mostrar datos actualizados
                        System.out.println("\nDatos actualizados:");
                        showCollectionDetails(collectionId);
                    } else {
                        conexion.rollback();
                        System.out.println("\nError: No se detectaron los cambios esperados. La operación fue revertida.");
                    }
                } else {
                    conexion.rollback();
                    System.out.println("\nError: No se encontró la entrada de colección con ID: " + collectionId);
                }
            }
        } catch (SQLException e) {
            try {
                conexion.rollback();
            } catch (SQLException ex) {
                System.err.println("Error al hacer rollback: " + ex.getMessage());
            }
            System.err.println("Error al modificar colección: " + e.getMessage());
        } finally {
            try {
                conexion.setAutoCommit(true);
            } catch (SQLException e) {
                System.err.println("Error al restaurar auto-commit: " + e.getMessage());
            }
        }
    }

    public static boolean deleteGame_Collection() {
        // Usaremos eliminación lógica (marcar como inactivo)
        Scanner scanner = new Scanner(System.in);
        System.out.print("Ingrese el id de la coleccion que desea desactivar");
        int collectionId = scanner.nextInt();
        String sql = "UPDATE game_collection SET active = FALSE WHERE collection_id = ?";

        try {
            conexion.setAutoCommit(false);

            try (PreparedStatement pstmt = conexion.prepareStatement(sql)) {
                pstmt.setInt(1, collectionId);

                int affectedRows = pstmt.executeUpdate();

                if (affectedRows > 0) {
                    // Verificar que realmente se desactivó
                    String verifySql = "SELECT active FROM game_collection WHERE collection_id = ?";
                    try (PreparedStatement verifyStmt = conexion.prepareStatement(verifySql)) {
                        verifyStmt.setInt(1, collectionId);
                        try (ResultSet rs = verifyStmt.executeQuery()) {
                            if (rs.next() && !rs.getBoolean("active")) {
                                conexion.commit();
                                System.out.println("Juego eliminado de la colección exitosamente");
                                return true;
                            }
                        }
                    }
                }

                conexion.rollback();
                System.out.println("No se encontró la entrada en la colección con ID: " + collectionId);
                return false;
            }
        } catch (SQLException e) {
            try {
                conexion.rollback();
            } catch (SQLException ex) {
                System.err.println("Error al hacer rollback: " + ex.getMessage());
            }
            System.err.println("Error al eliminar juego de colección: " + e.getMessage());
            return false;
        } finally {
            try {
                conexion.setAutoCommit(true);
            } catch (SQLException e) {
                System.err.println("Error al restaurar auto-commit: " + e.getMessage());
            }
        }
    }

    private static boolean verifyChanges(int collectionId, Integer expectedRating, Boolean expectedActive) throws SQLException {
        String sql = "SELECT rating, active FROM game_collection WHERE collection_id = ?";
        try (PreparedStatement pstmt = conexion.prepareStatement(sql)) {
            pstmt.setInt(1, collectionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    boolean ratingOk = (expectedRating == null) || (rs.getInt("rating") == expectedRating);
                    boolean activeOk = (expectedActive == null) || (rs.getBoolean("active") == expectedActive);
                    return ratingOk && activeOk;
                }
            }
        }
        return false;
    }

    private static boolean validateCollectionOwnership(int collectionId, int userId) throws SQLException {
        String sql = "SELECT collection_id FROM game_collection WHERE collection_id = ? AND user_id = ?";
        try (PreparedStatement pstmt = conexion.prepareStatement(sql)) {
            pstmt.setInt(1, collectionId);
            pstmt.setInt(2, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public static boolean actualizaBaseDatos(PreparedStatement instruccion, String accion, String tabla) {
        boolean exito = false;
        try {
            conexion.setAutoCommit(false);

            // Variables comunes para todas las tablas
            int conteo_inicial = 0;
            int conteo_final = 0;
            String campoId = obtenerCampoId(tabla);

            if (accion.equals("INSERT")) {
                // Consulta para obtener el conteo inicial
                String SQL = "SELECT COUNT(*) as cuenta FROM " + tabla;
                try (PreparedStatement inst = conexion.prepareStatement(SQL);
                     ResultSet inicial = inst.executeQuery()) {
                    if (inicial.next()) {
                        conteo_inicial = inicial.getInt("cuenta");
                    }
                }

                // Ejecutar el INSERT
                instruccion.executeUpdate();

                // Consulta para obtener el conteo final
                try (PreparedStatement inst = conexion.prepareStatement(SQL);
                     ResultSet rsFinal = inst.executeQuery()) {
                    if (rsFinal.next()) {
                        conteo_final = rsFinal.getInt("cuenta");
                    }
                }

                // Verificar si se insertó correctamente
                if (conteo_final == (conteo_inicial + 1)) {
                    conexion.commit();
                    exito = true;
                } else {
                    conexion.rollback();
                }
            }
            else if (accion.equals("DELETE")) {
                // Consulta para obtener el conteo inicial de registros inactivos
                String SQL = "SELECT COUNT(*) as cuenta FROM " + tabla + " WHERE active = FALSE";
                try (PreparedStatement inst = conexion.prepareStatement(SQL);
                     ResultSet inicial = inst.executeQuery()) {
                    if (inicial.next()) {
                        conteo_inicial = inicial.getInt("cuenta");
                    }
                }

                // Ejecutar el DELETE (actualmente es una desactivación)
                instruccion.executeUpdate();

                // Consulta para obtener el conteo final de registros inactivos
                try (PreparedStatement inst = conexion.prepareStatement(SQL);
                     ResultSet rsFinal = inst.executeQuery()) {
                    if (rsFinal.next()) {
                        conteo_final = rsFinal.getInt("cuenta");
                    }
                }

                // Verificar si se desactivó correctamente
                if (conteo_final == (conteo_inicial + 1)) {
                    conexion.commit();
                    exito = true;
                } else {
                    conexion.rollback();
                }
            }
            else if (accion.equals("UPDATE")) {
                // Obtener los valores actuales antes de la actualización
                Map<String, Object> valoresIniciales = obtenerValoresIniciales(tabla, campoId);

                // Ejecutar la actualización
                instruccion.executeUpdate();

                // Obtener los valores después de la actualización
                Map<String, Object> valoresFinales = obtenerValoresIniciales(tabla, campoId);

                // Comparar los valores para determinar si hubo cambios
                if (!valoresIniciales.equals(valoresFinales)) {
                    conexion.commit();
                    exito = true;
                } else {
                    conexion.rollback();
                }
            }
        } catch (SQLException s) {
            System.err.println("Error en actualizaBaseDatos: " + s.getMessage());
            try {
                if (conexion != null) conexion.rollback();
            } catch (SQLException ex) {
                System.err.println("Error al hacer rollback: " + ex.getMessage());
            }
        } finally {
            try {
                conexion.setAutoCommit(true);
            } catch (SQLException e) {
                System.err.println("Error al restaurar auto-commit: " + e.getMessage());
            }
        }
        return exito;
    }

    // Método auxiliar para obtener el nombre del campo ID según la tabla
    private static String obtenerCampoId(String tabla) {
        switch (tabla.toLowerCase()) {
            case "users":
                return "user_id";
            case "platform":
                return "platform_id";
            case "games":
                return "game_id";
            case "game_collection":
                return "collection_id";
            default:
                return "id";
        }
    }

    // Método auxiliar para obtener los valores actuales de un registro
    private static Map<String, Object> obtenerValoresIniciales(String tabla, String campoId) throws SQLException {
        Map<String, Object> valores = new HashMap<>();
        String SQL = "SELECT * FROM " + tabla + " WHERE " + campoId + " = ?";

        try (PreparedStatement inst = conexion.prepareStatement(SQL)) {
            inst.setInt(1, usuarioID); // Asume que usuarioID es una variable de clase

            try (ResultSet rs = inst.executeQuery()) {
                if (rs.next()) {
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();

                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i);
                        Object value = rs.getObject(i);
                        valores.put(columnName, value);
                    }
                }
            }
        }
        return valores;
    }

    public static void listarColeccionUsuario(int userId) {
        String sql = "SELECT g.game_id, g.game_name, p.platform_name, gc.rating, " +
                "DATE_FORMAT(gc.date_added, '%Y-%m-%d') as fecha_agregado " +
                "FROM game_collection gc " +
                "JOIN games g ON gc.game_id = g.game_id " +
                "JOIN platform p ON g.platform_id = p.platform_id " +
                "WHERE gc.user_id = ? AND gc.active = TRUE " +
                "ORDER BY g.game_name";

        try (PreparedStatement pstmt = conexion.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            System.out.println("\n=== COLECCIÓN DE VIDEOJUEGOS ===");
            System.out.printf("%-5s %-30s %-20s %-10s %-15s%n",
                    "ID", "JUEGO", "PLATAFORMA", "RATING", "FECHA AGREGADO");
            System.out.println("---------------------------------------------------------------");

            while (rs.next()) {
                System.out.printf("%-5d %-30s %-20s %-10d %-15s%n",
                        rs.getInt("game_id"),
                        rs.getString("game_name"),
                        rs.getString("platform_name"),
                        rs.getInt("rating"),
                        rs.getString("fecha_agregado"));
            }

            // Mostrar estadísticas
            mostrarEstadisticasColeccion(userId);
        } catch (SQLException e) {
            System.err.println("Error al listar colección: " + e.getMessage());
        }
    }

    private static void mostrarEstadisticasColeccion(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) as total, AVG(rating) as promedio " +
                "FROM game_collection " +
                "WHERE user_id = ? AND active = TRUE";

        try (PreparedStatement pstmt = conexion.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                System.out.println("\nEstadísticas:");
                System.out.println("Total de juegos: " + rs.getInt("total"));
                System.out.printf("Rating promedio: %.2f%n", rs.getDouble("promedio"));
            }
        }
    }

    public static void listarJuegosPopulares() {
        String sql = "SELECT g.game_id, g.game_name, COUNT(gc.user_id) as coleccionistas " +
                "FROM games g " +
                "JOIN game_collection gc ON g.game_id = gc.game_id AND gc.active = TRUE " +
                "WHERE g.active = TRUE " +
                "GROUP BY g.game_id " +
                "ORDER BY coleccionistas DESC, g.game_name " +
                "LIMIT 10"; // Top 10 juegos más populares

        try (Statement stmt = conexion.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n=== JUEGOS MÁS POPULARES ===");
            System.out.printf("%-5s %-40s %-15s%n", "ID", "JUEGO", "COLECCIONISTAS");
            System.out.println("------------------------------------------------");

            while (rs.next()) {
                System.out.printf("%-5d %-40s %-15d%n",
                        rs.getInt("game_id"),
                        rs.getString("game_name"),
                        rs.getInt("coleccionistas"));
            }
        } catch (SQLException e) {
            System.err.println("Error al listar juegos populares: " + e.getMessage());
        }
    }

    public static void listarTop5MejorRating() {
        String sql = "SELECT g.game_id, g.game_name, AVG(gc.rating) as rating_promedio, " +
                "COUNT(gc.user_id) as votantes " +
                "FROM games g " +
                "JOIN game_collection gc ON g.game_id = gc.game_id AND gc.active = TRUE " +
                "WHERE g.active = TRUE " +
                "GROUP BY g.game_id " +
                "HAVING COUNT(gc.user_id) >= 3 " + // Mínimo 3 calificaciones
                "ORDER BY rating_promedio DESC " +
                "LIMIT 5";

        try (Statement stmt = conexion.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n=== TOP 5 JUEGOS CON MEJOR RATING ===");
            System.out.printf("%-5s %-40s %-15s %-15s%n",
                    "ID", "JUEGO", "RATING PROMEDIO", "VOTANTES");
            System.out.println("------------------------------------------------------------");

            while (rs.next()) {
                System.out.printf("%-5d %-40s %-15.2f %-15d%n",
                        rs.getInt("game_id"),
                        rs.getString("game_name"),
                        rs.getDouble("rating_promedio"),
                        rs.getInt("votantes"));
            }
        } catch (SQLException e) {
            System.err.println("Error al listar top 5 juegos: " + e.getMessage());
        }
    }

    private static void mostrarMenuPrincipal() throws SQLException {
        Scanner scanner = new Scanner(System.in);
        int opcion;

        do {
            System.out.println("\n=== MENÚ PRINCIPAL ===");
            System.out.println("1. Gestión de Usuarios");
            System.out.println("2. Gestión de Plataformas");
            System.out.println("3. Gestión de Juegos");
            System.out.println("4. Gestión de Colección");
            System.out.println("5. Consultas y Reportes");
            System.out.println("6. Bitácora y Restauración");
            System.out.println("7. Busqueda Avanzada");
            System.out.println("8. Estadisticas Avanzadas");
            System.out.println("0. Salir");
            System.out.print("Seleccione una opción: ");

            opcion = scanner.nextInt();
            scanner.nextLine(); // Limpiar buffer

            switch(opcion) {
                case 1:
                    menuGestionUsuarios();
                    break;
                case 2:
                    menuGestionPlataformas();
                    break;
                case 3:
                    menuGestionJuegos();
                    break;
                case 4:
                    menuGestionColeccion();
                    break;
                case 5:
                    menuConsultasReportes();
                    break;
                case 6:
                    menuBitacoraRestauracion();
                    break;
                case 7:
                    busquedaAvanzada();
                    break;
                case 8:
                    mostrarEstadisticasAvanzadas();
                case 0:
                    System.out.println("Saliendo del sistema...");
                    break;
                default:
                    System.out.println("Opción no válida");
            }
        } while(opcion != 0);

        scanner.close();
    }

    // 1. Menú de Gestión de Usuarios (solo admin)
    private static void menuGestionUsuarios() throws SQLException {
        Scanner scanner = new Scanner(System.in);
        if (!isAdmin) {
            System.out.println("\nAcceso restringido: se requieren privilegios de administrador");
            return;
        }

        int opcion;
        do {
            System.out.println("\n=== GESTIÓN DE USUARIOS ===");
            System.out.println("1. Listar usuarios");
            System.out.println("2. Agregar usuario");
            System.out.println("3. Modificar usuario");
            System.out.println("4. Desactivar usuario");
            System.out.println("5. Modificar password");
            System.out.println("0. Volver al menú principal");
            System.out.print("Seleccione una opción: ");

            opcion = scanner.nextInt();
            scanner.nextLine();

            switch(opcion) {
                case 1:
                    despliegaTabla("users" , "user_id, username, email, preferred_platform_id, access_type, active" , "1");
                    break;
                case 2:
                    addUser();
                    break;
                case 3:
                    modifyUser();
                    break;
                case 4:
                    modifyUser();
                    break;
                case 5:
                    modifyPassword();
                    break;
            }
        } while(opcion != 0);
    }

    // 2. Menú de Gestión de Plataformas (solo admin)
    private static void menuGestionPlataformas() throws SQLException {
        Scanner scanner = new Scanner(System.in);
        if (!isAdmin) {
            System.out.println("\nAcceso restringido: se requieren privilegios de administrador");
            return;
        }

        int opcion;
        do {
            System.out.println("\n=== GESTIÓN DE PLATAFORMAS ===");
            System.out.println("1. Listar plataformas");
            System.out.println("2. Agregar plataforma");
            System.out.println("3. Modificar plataforma");
            System.out.println("4. Desactivar plataforma");
            System.out.println("0. Volver al menú principal");
            System.out.print("Seleccione una opción: ");

            opcion = scanner.nextInt();
            scanner.nextLine();

            switch(opcion) {
                case 1:
                    despliegaTabla("platform" , "platform_id, platform_name, active" , "1");
                    break;
                case 2:
                    addPlatform();
                    break;
                case 3:
                    modifyPlatform();
                    break;
                case 4:
                    deletePlatform();
                    break;
            }
        } while(opcion != 0);
    }

    // 3. Menú de Gestión de Juegos (solo admin)
    private static void menuGestionJuegos() throws SQLException {
        Scanner scanner = new Scanner(System.in);
        if (isAdmin) {
            System.out.println("\nAcceso restringido: se requieren privilegios de administrador");
            return;
        }

        int opcion;
        do {
            System.out.println("\n=== GESTIÓN DE JUEGOS ===");
            System.out.println("1. Listar juegos");
            System.out.println("2. Agregar juego");
            System.out.println("3. Modificar juego");
            System.out.println("4. Desactivar juego");
            System.out.println("0. Volver al menú principal");
            System.out.print("Seleccione una opción: ");

            opcion = scanner.nextInt();
            scanner.nextLine();

            switch(opcion) {
                case 1:
                    despliegaTabla("games", "game_id, game_name, platform_id, year_released, active" , "1");
                    break;
                case 2:
                    addGame();
                    break;
                case 3:
                    modifyGame();
                    break;
                case 4:
                    deleteGame();
                    break;
            }
        } while(opcion != 0);
    }

    // 4. Menú de Gestión de Colección (para todos los usuarios)
    private static void menuGestionColeccion() throws SQLException {
        Scanner scanner = new Scanner(System.in);
        int opcion;
        do {
            System.out.println("\n=== MI COLECCIÓN ===");
            System.out.println("1. Ver mi colección");
            System.out.println("2. Agregar juego a mi colección");
            System.out.println("3. Modificar rating de un juego");
            System.out.println("4. Eliminar juego de mi colección");
            System.out.println("0. Volver al menú principal");
            System.out.print("Seleccione una opción: ");

            opcion = scanner.nextInt();
            scanner.nextLine();

            switch(opcion) {
                case 1:
                    listarColeccionUsuario(userID);
                    break;
                case 2:
                    addGame_Collection();
                    break;
                case 3:
                    modifyGame_Collection();
                    break;
                case 4:
                    deleteGame_Collection();
                    break;
            }
        } while(opcion != 0);
    }

    // 5. Menú de Consultas y Reportes (para todos los usuarios)
    private static void menuConsultasReportes() throws SQLException {
        Scanner scanner = new Scanner(System.in);
        int opcion;
        do {
            System.out.println("\n=== CONSULTAS Y REPORTES ===");
            System.out.println("1. Listar mi colección completa");
            System.out.println("2. Ver juegos más populares");
            System.out.println("3. Top 5 juegos con mejor rating");
            System.out.println("0. Volver al menú principal");
            System.out.print("Seleccione una opción: ");

            opcion = scanner.nextInt();
            scanner.nextLine();

            switch(opcion) {
                case 1:
                    listarColeccionUsuario(usuarioID);
                    break;
                case 2:
                    listarJuegosPopulares();
                    break;
                case 3:
                    listarTop5MejorRating();
                    break;
            }
        } while(opcion != 0);
    }

    // 6. Menú de Bitácora y Restauración (solo admin)
    private static void menuBitacoraRestauracion() throws SQLException {
        Scanner scanner = new Scanner(System.in);
        if (!isAdmin) {
            System.out.println("\nAcceso restringido: se requieren privilegios de administrador");
            return;
        }

        int opcion;
        do {
            System.out.println("\n=== BITÁCORA Y RESTAURACIÓN ===");
            System.out.println("1. Ver registros de bitácora");
            System.out.println("2. Restaurar base de datos");
            System.out.println("0. Volver al menú principal");
            System.out.print("Seleccione una opción: ");

            opcion = scanner.nextInt();
            scanner.nextLine();

            switch(opcion) {
                case 1:
                    //verBitacora();
                    break;
                case 2:
                    //restaurarBaseDatos();
                    break;
            }
        } while(opcion != 0);
    }

    public static void busquedaAvanzada() {
        Scanner scanner = new Scanner(System.in);

        try {
            System.out.println("\n=== BÚSQUEDA AVANZADA ===");
            System.out.println("1. Buscar juegos por nombre");
            System.out.println("2. Filtrar mi colección por plataforma");
            System.out.println("3. Filtrar mi colección por año de lanzamiento");
            System.out.println("4. Búsqueda combinada");
            System.out.print("Seleccione una opción: ");

            int opcion = scanner.nextInt();
            scanner.nextLine(); // Limpiar buffer

            switch(opcion) {
                case 1:
                    buscarJuegosPorNombre();
                    break;
                case 2:
                    filtrarColeccionPorPlataforma();
                    break;
                case 3:
                    filtrarColeccionPorAnio();
                    break;
                case 4:
                    busquedaCombinada();
                    break;
                default:
                    System.out.println("Opción no válida");
            }
        } catch (Exception e) {
            System.err.println("Error en búsqueda avanzada: " + e.getMessage());
        }
    }

    private static void buscarJuegosPorNombre() throws SQLException {
        Scanner scanner = new Scanner(System.in);
        System.out.print("\nIngrese parte del nombre del juego a buscar: ");
        String busqueda = scanner.nextLine();

        String sql = "SELECT g.game_id, g.game_name, p.platform_name, g.year_released " +
                "FROM games g JOIN platform p ON g.platform_id = p.platform_id " +
                "WHERE g.active = TRUE AND p.active = TRUE " +
                "AND g.game_name LIKE ? " +
                "ORDER BY g.game_name";

        try (PreparedStatement pstmt = conexion.prepareStatement(sql)) {
            pstmt.setString(1, "%" + busqueda + "%");
            ResultSet rs = pstmt.executeQuery();

            System.out.println("\nResultados de la búsqueda:");
            System.out.printf("%-5s %-30s %-20s %-10s%n", "ID", "JUEGO", "PLATAFORMA", "AÑO");
            System.out.println("----------------------------------------------------");

            while(rs.next()) {
                System.out.printf("%-5d %-30s %-20s %-10d%n",
                        rs.getInt("game_id"),
                        rs.getString("game_name"),
                        rs.getString("platform_name"),
                        rs.getInt("year_released"));
            }
        }
    }

    private static void filtrarColeccionPorPlataforma() throws SQLException {
        Scanner scanner = new Scanner(System.in);
        // Mostrar plataformas disponibles
        String sqlPlataformas = "SELECT platform_id, platform_name FROM platform WHERE active = TRUE ORDER BY platform_name";
        try (Statement stmt = conexion.createStatement();
             ResultSet rs = stmt.executeQuery(sqlPlataformas)) {

            System.out.println("\nPlataformas disponibles:");
            while(rs.next()) {
                System.out.println(rs.getInt("platform_id") + ". " + rs.getString("platform_name"));
            }
        }

        System.out.print("\nIngrese el ID de la plataforma a filtrar: ");
        int plataformaId = scanner.nextInt();
        scanner.nextLine();

        String sql = "SELECT gc.collection_id, g.game_name, p.platform_name, gc.rating, g.year_released " +
                "FROM game_collection gc " +
                "JOIN games g ON gc.game_id = g.game_id " +
                "JOIN platform p ON g.platform_id = p.platform_id " +
                "WHERE gc.user_id = ? AND gc.active = TRUE " +
                "AND g.platform_id = ? " +
                "ORDER BY g.game_name";

        try (PreparedStatement pstmt = conexion.prepareStatement(sql)) {
            pstmt.setInt(1, userID);
            pstmt.setInt(2, plataformaId);
            ResultSet rs = pstmt.executeQuery();

            System.out.println("\nColección filtrada por plataforma:");
            System.out.printf("%-5s %-30s %-20s %-10s %-10s%n", "ID", "JUEGO", "PLATAFORMA", "RATING", "AÑO");
            System.out.println("------------------------------------------------------------");

            while(rs.next()) {
                System.out.printf("%-5d %-30s %-20s %-10d %-10d%n",
                        rs.getInt("collection_id"),
                        rs.getString("game_name"),
                        rs.getString("platform_name"),
                        rs.getInt("rating"),
                        rs.getInt("year_released"));
            }
        }
    }

    private static void filtrarColeccionPorAnio() throws SQLException {
        Scanner scanner = new Scanner(System.in);
        System.out.print("\nIngrese el año de lanzamiento a filtrar: ");
        int anio = scanner.nextInt();
        scanner.nextLine();

        String sql = "SELECT gc.collection_id, g.game_name, p.platform_name, gc.rating, g.year_released " +
                "FROM game_collection gc " +
                "JOIN games g ON gc.game_id = g.game_id " +
                "JOIN platform p ON g.platform_id = p.platform_id " +
                "WHERE gc.user_id = ? AND gc.active = TRUE " +
                "AND g.year_released = ? " +
                "ORDER BY g.game_name";

        try (PreparedStatement pstmt = conexion.prepareStatement(sql)) {
            pstmt.setInt(1, usuarioID);
            pstmt.setInt(2, anio);
            ResultSet rs = pstmt.executeQuery();

            System.out.println("\nColección filtrada por año:");
            System.out.printf("%-5s %-30s %-20s %-10s %-10s%n", "ID", "JUEGO", "PLATAFORMA", "RATING", "AÑO");
            System.out.println("------------------------------------------------------------");

            while(rs.next()) {
                System.out.printf("%-5d %-30s %-20s %-10d %-10d%n",
                        rs.getInt("collection_id"),
                        rs.getString("game_name"),
                        rs.getString("platform_name"),
                        rs.getInt("rating"),
                        rs.getInt("year_released"));
            }
        }
    }

    private static void busquedaCombinada() throws SQLException {
        Scanner scanner = new Scanner(System.in);
        System.out.print("\nIngrese parte del nombre del juego (dejar vacío para omitir): ");
        String nombre = scanner.nextLine();

        System.out.print("Ingrese el ID de plataforma (0 para omitir): ");
        int plataformaId = scanner.nextInt();
        scanner.nextLine();

        System.out.print("Ingrese el año de lanzamiento (0 para omitir): ");
        int anio = scanner.nextInt();
        scanner.nextLine();

        StringBuilder sql = new StringBuilder(
                "SELECT g.game_id, g.game_name, p.platform_name, g.year_released " +
                        "FROM games g JOIN platform p ON g.platform_id = p.platform_id " +
                        "WHERE g.active = TRUE AND p.active = TRUE ");

        List<Object> params = new ArrayList<>();

        if (!nombre.isEmpty()) {
            sql.append("AND g.game_name LIKE ? ");
            params.add("%" + nombre + "%");
        }

        if (plataformaId > 0) {
            sql.append("AND g.platform_id = ? ");
            params.add(plataformaId);
        }

        if (anio > 0) {
            sql.append("AND g.year_released = ? ");
            params.add(anio);
        }

        sql.append("ORDER BY g.game_name");

        try (PreparedStatement pstmt = conexion.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }

            ResultSet rs = pstmt.executeQuery();

            System.out.println("\nResultados de búsqueda combinada:");
            System.out.printf("%-5s %-30s %-20s %-10s%n", "ID", "JUEGO", "PLATAFORMA", "AÑO");
            System.out.println("----------------------------------------------------");

            while(rs.next()) {
                System.out.printf("%-5d %-30s %-20s %-10d%n",
                        rs.getInt("game_id"),
                        rs.getString("game_name"),
                        rs.getString("platform_name"),
                        rs.getInt("year_released"));
            }
        }
    }

    public static void mostrarEstadisticasAvanzadas() {
        try {
            System.out.println("\n=== ESTADÍSTICAS AVANZADAS ===");

            // 1. Número total de juegos en la base de datos
            String sqlTotalJuegos = "SELECT COUNT(*) as total FROM games WHERE active = TRUE";
            int totalJuegos = ejecutarConsultaCount(sqlTotalJuegos);
            System.out.println("Total de juegos en la base de datos: " + totalJuegos);

            // 2. Número promedio de juegos por usuario
            String sqlPromedio = "SELECT AVG(conteo) as promedio FROM (" +
                    "SELECT COUNT(*) as conteo FROM game_collection " +
                    "WHERE active = TRUE GROUP BY user_id) as subquery";
            double promedioJuegos = ejecutarConsultaPromedio(sqlPromedio);
            System.out.printf("Promedio de juegos por usuario: %.2f%n", promedioJuegos);

            // 3. Plataforma más popular
            String sqlPlataformaPopular = "SELECT p.platform_name, COUNT(*) as total " +
                    "FROM game_collection gc " +
                    "JOIN games g ON gc.game_id = g.game_id " +
                    "JOIN platform p ON g.platform_id = p.platform_id " +
                    "WHERE gc.active = TRUE AND g.active = TRUE AND p.active = TRUE " +
                    "GROUP BY p.platform_id " +
                    "ORDER BY total DESC " +
                    "LIMIT 1";
            try (Statement stmt = conexion.createStatement();
                 ResultSet rs = stmt.executeQuery(sqlPlataformaPopular)) {
                if (rs.next()) {
                    System.out.println("Plataforma más popular: " + rs.getString("platform_name") +
                            " (" + rs.getInt("total") + " juegos en colecciones)");
                }
            }

            // 4. Distribución de juegos por plataforma
            System.out.println("\nDistribución de juegos por plataforma:");
            String sqlDistribucion = "SELECT p.platform_name, COUNT(*) as cantidad " +
                    "FROM games g JOIN platform p ON g.platform_id = p.platform_id " +
                    "WHERE g.active = TRUE AND p.active = TRUE " +
                    "GROUP BY p.platform_id " +
                    "ORDER BY cantidad DESC";
            try (Statement stmt = conexion.createStatement();
                 ResultSet rs = stmt.executeQuery(sqlDistribucion)) {
                System.out.printf("%-20s %-10s%n", "PLATAFORMA", "CANTIDAD");
                System.out.println("----------------------------");
                while(rs.next()) {
                    System.out.printf("%-20s %-10d%n",
                            rs.getString("platform_name"),
                            rs.getInt("cantidad"));
                }
            }

            // 5. Años con más lanzamientos
            System.out.println("\nAños con más lanzamientos:");
            String sqlAniosLanzamientos = "SELECT year_released, COUNT(*) as cantidad " +
                    "FROM games " +
                    "WHERE active = TRUE AND year_released IS NOT NULL " +
                    "GROUP BY year_released " +
                    "ORDER BY cantidad DESC " +
                    "LIMIT 5";
            try (Statement stmt = conexion.createStatement();
                 ResultSet rs = stmt.executeQuery(sqlAniosLanzamientos)) {
                System.out.printf("%-10s %-10s%n", "AÑO", "LANZAMIENTOS");
                System.out.println("-------------------");
                while(rs.next()) {
                    System.out.printf("%-10d %-10d%n",
                            rs.getInt("year_released"),
                            rs.getInt("cantidad"));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al calcular estadísticas: " + e.getMessage());
        }
    }

    // Métodos auxiliares para estadísticas
    private static int ejecutarConsultaCount(String sql) throws SQLException {
        try (Statement stmt = conexion.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private static double ejecutarConsultaPromedio(String sql) throws SQLException {
        try (Statement stmt = conexion.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getDouble(1) : 0.0;
        }
    }

}