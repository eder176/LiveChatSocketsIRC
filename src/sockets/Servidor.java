package sockets;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class Servidor extends JFrame {
    private JTextField campoIntroducir;
    private JTextArea areaPantalla;
    private ServerSocket servidor;
    private final Map<Integer, ObjectOutputStream> clientes = new ConcurrentHashMap<>();
    private static final int PUERTO = 12345;
    private static AtomicInteger clientIdCounter = new AtomicInteger(0);

    public Servidor() {
        super("Servidor");
        Container contenedor = getContentPane();

        // Campo para enviar mensajes a todos los clientes
        campoIntroducir = new JTextField();
        campoIntroducir.setEditable(true);
        campoIntroducir.addActionListener(evento -> {
            broadcast("SERVIDOR>>> " + evento.getActionCommand());
            campoIntroducir.setText("");
        });
        contenedor.add(campoIntroducir, BorderLayout.NORTH);

        // Área para mostrar mensajes
        areaPantalla = new JTextArea();
        areaPantalla.setEditable(false);
        contenedor.add(new JScrollPane(areaPantalla), BorderLayout.CENTER);

        setSize(400, 250);
        setVisible(true);
    }

    // Método para iniciar el servidor y aceptar clientes
    public void ejecutarServidor() {
        try {
            servidor = new ServerSocket(PUERTO, 100);
            mostrarMensaje("Servidor iniciado en el puerto " + PUERTO + "\n");
            while (true) {
                Socket conexionCliente = servidor.accept();
                new Thread(new ManejadorCliente(conexionCliente)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Clase interna para manejar a cada cliente en un hilo separado
    private class ManejadorCliente implements Runnable {
        private Socket conexion;
        private ObjectOutputStream salida;
        private ObjectInputStream entrada;
        private int idCliente;

        public ManejadorCliente(Socket socket) {
            this.conexion = socket;
            // Asigna un ID único a cada cliente
            this.idCliente = clientIdCounter.incrementAndGet();
        }

        @Override
        public void run() {
            try {
                obtenerFlujos();
                broadcast("Nuevo cliente (" + idCliente + ") conectado desde " 
                        + conexion.getInetAddress().getHostAddress());
                procesarConexion();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                cerrarConexion();
            }
        }

        // Inicializa los flujos de entrada y salida
        private void obtenerFlujos() throws IOException {
            salida = new ObjectOutputStream(conexion.getOutputStream());
            salida.flush();
            entrada = new ObjectInputStream(conexion.getInputStream());
            clientes.put(idCliente, salida);
            mostrarMensaje("Cliente " + idCliente + " conectado desde: " 
                    + conexion.getInetAddress().getHostAddress() + "\n");
            actualizarContadorClientes();
        }

        // Lee mensajes enviados por el cliente y los retransmite a todos
        private void procesarConexion() throws IOException, ClassNotFoundException {
            String mensaje;
            while ((mensaje = (String) entrada.readObject()) != null) {
                if (mensaje.equals("CLIENTE>>> TERMINAR")) {
                    break;
                }
                broadcast("Cliente " + idCliente + " >>> " + mensaje);
            }
        }

        // Cierra la conexión y actualiza la lista de clientes
        private void cerrarConexion() {
            try {
                clientes.remove(idCliente);
                if (entrada != null) entrada.close();
                if (salida != null) salida.close();
                if (conexion != null) conexion.close();
                mostrarMensaje("Cliente " + idCliente + " desconectado\n");
                broadcast("Cliente " + idCliente + " se ha desconectado.");
                actualizarContadorClientes();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Envía el mensaje a todos los clientes y lo muestra en el área de mensajes
    private void broadcast(String mensaje) {
        mostrarMensaje(mensaje);
        for (ObjectOutputStream out : clientes.values()) {
            try {
                out.writeObject(mensaje);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Muestra un mensaje en el área de texto de forma segura (en el hilo de Swing)
    private void mostrarMensaje(final String mensaje) {
        SwingUtilities.invokeLater(() -> {
            areaPantalla.append(mensaje + "\n");
            areaPantalla.setCaretPosition(areaPantalla.getText().length());
        });
    }
    
    // Actualiza el título de la ventana con el número de clientes conectados
    private void actualizarContadorClientes() {
        SwingUtilities.invokeLater(() -> {
            setTitle("Servidor - Conectados: " + clientes.size());
        });
    }

    public static void main(String[] args) {
        Servidor aplicacion = new Servidor();
        aplicacion.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        aplicacion.ejecutarServidor();
    }
}
