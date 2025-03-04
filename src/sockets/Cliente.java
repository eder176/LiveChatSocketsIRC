package sockets;
import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class Cliente extends JFrame {
    private JTextField campoIntroducir;
    private JTextArea areaPantalla;
    private ObjectOutputStream salida;
    private ObjectInputStream entrada;
    private Socket conexion;
    private String nombre;
    private String servidorHost;
    private int puerto;
    
    public Cliente(String host, int puerto) {
    	super("Cliente");
        this.servidorHost = host;
        this.puerto = puerto;
        
        // Se solicita el nombre del cliente
        nombre = JOptionPane.showInputDialog(this, "Ingrese su nombre:", "Nombre del cliente", JOptionPane.PLAIN_MESSAGE);
        if (nombre == null || nombre.trim().isEmpty()) {
            nombre = "ClienteAnonimo";
        }
        
        Container contenedor = getContentPane();
        
        // Campo para escribir mensajes (deshabilitado hasta conectarse al servidor)
        campoIntroducir = new JTextField();

        campoIntroducir.setEditable(false);
        campoIntroducir.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                enviarDatos(e.getActionCommand());
                campoIntroducir.setText("");
            }
        });
        contenedor.add(campoIntroducir, BorderLayout.NORTH);
        
        // Área para mostrar mensajes
        areaPantalla = new JTextArea();
        areaPantalla.setEditable(false);
        contenedor.add(new JScrollPane(areaPantalla), BorderLayout.CENTER);
        
        setSize(400, 250);
        setVisible(true);
    }
    
    // Conectar con el servidor y comenzar a recibir mensajes
    public void ejecutarCliente() {
        try {
            mostrarMensaje("Intentando conectarse a " + servidorHost + " en el puerto " + puerto + "...");
            conexion = new Socket(servidorHost, puerto);
            salida = new ObjectOutputStream(conexion.getOutputStream());
            salida.flush();
            entrada = new ObjectInputStream(conexion.getInputStream());
            mostrarMensaje("Conectado a " + conexion.getInetAddress().getHostAddress());
            
            // Habilitar el campo para escribir mensajes
            campoIntroducir.setEditable(true);
            
            // Enviar el nombre al servidor (opcional, según como se gestione en el servidor)
            salida.writeObject("NOMBRE>>>" + nombre);
            salida.flush();
            
            // Hilo para escuchar mensajes enviados por el servidor
            new Thread(new Runnable() {
                public void run() {
                    String mensaje;
                    try {
                        while ((mensaje = (String) entrada.readObject()) != null) {
                            mostrarMensaje(mensaje);
                        }
                    } catch (IOException | ClassNotFoundException ex) {
                        mostrarMensaje("Conexión terminada.");
                    }
                }
            }).start();
            
        } catch (IOException ex) {
            ex.printStackTrace();
            mostrarMensaje("Error al conectarse al servidor.");
        }
    }
    
    // Enviar mensaje al servidor sin mostrarlo localmente para evitar duplicados
    private void enviarDatos(String mensaje) {
        try {
            salida.writeObject(nombre + ">>> " + mensaje);
            salida.flush();
        } catch (IOException ex) {
            areaPantalla.append("Error al enviar mensaje.\n");
        }
    }
    
    // Mostrar mensajes en el área de texto de forma segura
    private void mostrarMensaje(final String mensaje) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                areaPantalla.append(mensaje + "\n");
                areaPantalla.setCaretPosition(areaPantalla.getText().length());
            }
        });
    }
    
    public static void main(String[] args) {
        Cliente aplicacion = new Cliente("0.0.0.0", 12345);
        aplicacion.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        aplicacion.ejecutarCliente();
    }
}
