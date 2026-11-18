package com.example.facturaventa;
import com.example.facturaventa.config.MigracionListaPrecio;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;
class MigracionListaPrecioTests {
 @Test void migraDatosAnterioresSinCambiarIdsNiImportes() {
  var source=new org.h2.jdbcx.JdbcDataSource();source.setURL("jdbc:h2:mem:migracion-"+System.nanoTime()+";DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
  var sql=new JdbcTemplate(source);
  sql.execute("CREATE TABLE lista_precio_articulo(id BIGINT PRIMARY KEY,articulo_id BIGINT NOT NULL,precio_neto NUMERIC(15,2),activo BOOLEAN)");
  sql.execute("CREATE TABLE factura_venta_detalle(id BIGINT PRIMARY KEY,articulo_id BIGINT NOT NULL,lista_precio_articulo_id BIGINT NOT NULL,importe_total NUMERIC(15,2))");
  sql.update("INSERT INTO lista_precio_articulo VALUES(7,5,100,TRUE)");sql.update("INSERT INTO factura_venta_detalle VALUES(9,5,7,363)");
  MigracionListaPrecio.preparar(source);MigracionListaPrecio.preparar(source);
  assertNotNull(sql.queryForObject("SELECT lista_precio_id FROM lista_precio_articulo WHERE id=7",Long.class));
  assertEquals(1,sql.queryForObject("SELECT COUNT(*) FROM lista_precio",Integer.class));
  assertEquals(363,sql.queryForObject("SELECT importe_total FROM factura_venta_detalle WHERE id=9",java.math.BigDecimal.class).intValue());
  sql.update("INSERT INTO factura_venta_detalle(id,lista_precio_articulo_id,importe_total) VALUES(10,7,121)");
  assertEquals(2,sql.queryForObject("SELECT COUNT(*) FROM factura_venta_detalle",Integer.class));
 }
}
