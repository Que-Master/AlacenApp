package com.example.myapplication.models;

import com.google.firebase.database.PropertyName;
import java.io.Serializable;

public class Producto implements Serializable {

    private String id;

    @PropertyName("nombre")
    private String nombre;

    @PropertyName("marca")
    private String marca;

    @PropertyName("categoria")
    private String categoria;

    @PropertyName("cantidad")
    private String cantidad;

    @PropertyName("stock")
    private int stock;

    @PropertyName("stock_minimo")
    private int stockMinimo;

    @PropertyName("codigo_barras")
    private String codigoBarras;

    @PropertyName("imagen_url")
    private String imagenUrl;

    @PropertyName("ultima_actualizacion")
    private long ultimaActualizacion; // Timestamp en milisegundos

    public Producto() {}

    public Producto(String id, String nombre, String marca, String categoria, String cantidad,
                    int stock, int stockMinimo, String codigoBarras, String imagenUrl) {
        this.id = id;
        this.nombre = nombre;
        this.marca = marca;
        this.categoria = categoria;
        this.cantidad = cantidad;
        this.stock = stock;
        this.stockMinimo = stockMinimo;
        this.codigoBarras = codigoBarras;
        this.imagenUrl = imagenUrl;
        this.ultimaActualizacion = System.currentTimeMillis();
    }

    // ID
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    // Nombre
    @PropertyName("nombre")
    public String getNombre() { return nombre; }
    @PropertyName("nombre")
    public void setNombre(String nombre) { this.nombre = nombre; }

    // Marca
    @PropertyName("marca")
    public String getMarca() { return marca; }
    @PropertyName("marca")
    public void setMarca(String marca) { this.marca = marca; }

    // Categoría
    @PropertyName("categoria")
    public String getCategoria() { return categoria; }
    @PropertyName("categoria")
    public void setCategoria(String categoria) { this.categoria = categoria; }

    // Cantidad
    @PropertyName("cantidad")
    public String getCantidad() { return cantidad; }
    @PropertyName("cantidad")
    public void setCantidad(String cantidad) { this.cantidad = cantidad; }

    // Stock
    @PropertyName("stock")
    public int getStock() { return stock; }
    @PropertyName("stock")
    public void setStock(int stock) { this.stock = stock; }

    // Stock mínimo
    @PropertyName("stock_minimo")
    public int getStockMinimo() { return stockMinimo; }
    @PropertyName("stock_minimo")
    public void setStockMinimo(int stockMinimo) { this.stockMinimo = stockMinimo; }

    // Código de barras
    @PropertyName("codigo_barras")
    public String getCodigoBarras() { return codigoBarras; }
    @PropertyName("codigo_barras")
    public void setCodigoBarras(String codigoBarras) { this.codigoBarras = codigoBarras; }

    // Imagen URL
    @PropertyName("imagen_url")
    public String getImagenUrl() { return imagenUrl; }
    @PropertyName("imagen_url")
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }

    // Última actualización
    @PropertyName("ultima_actualizacion")
    public long getUltimaActualizacion() { return ultimaActualizacion; }
    @PropertyName("ultima_actualizacion")
    public void setUltimaActualizacion(long ultimaActualizacion) { this.ultimaActualizacion = ultimaActualizacion; }
}
