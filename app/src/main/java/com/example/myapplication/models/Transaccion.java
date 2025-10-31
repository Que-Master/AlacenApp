package com.example.myapplication.models;

import java.io.Serializable;

public class Transaccion implements Serializable {
    private String codigo;
    private String producto;
    private String tipo;
    private int cambio;
    private int stock_final;
    private long timestamp;
    private String mensaje; // Nuevo campo para mostrar texto legible en historial

    public Transaccion() {}

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getProducto() { return producto; }
    public void setProducto(String producto) { this.producto = producto; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public int getCambio() { return cambio; }
    public void setCambio(int cambio) { this.cambio = cambio; }

    public int getStock_final() { return stock_final; }
    public void setStock_final(int stock_final) { this.stock_final = stock_final; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getMensaje() { return mensaje; }           // Getter nuevo
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }  // Setter nuevo
}
