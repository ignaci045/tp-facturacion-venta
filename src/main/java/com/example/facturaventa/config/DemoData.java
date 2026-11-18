package com.example.facturaventa.config;
import com.example.facturaventa.dto.*;
import com.example.facturaventa.model.*;
import com.example.facturaventa.repository.*;
import com.example.facturaventa.service.FacturaService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.math.*;
import java.time.LocalDate;
import java.util.*;
@Component @RequiredArgsConstructor
@ConditionalOnProperty(name="app.demo-data.enabled",havingValue="true")
public class DemoData implements CommandLineRunner {
 private final ClienteRepository clientes;
 private final PuntoVentaRepository puntos;
 private final CondicionIvaRepository condiciones;
 private final TipoMonedaRepository monedas;
 private final ArticuloRepository articulos;
 private final ListaPrecioArticuloRepository precios;
 private final FacturaService facturas;
 private final ListaPrecioRepository listas;
 private final RubroRepository rubros;
 private final MarcaRepository marcas;
 @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager em;
 @org.springframework.transaction.annotation.Transactional
 @Override public void run(String... args) {
  boolean nuevaBase=puntos.count()==0 && clientes.count()==0;
  PuntoVenta punto=puntos.findAll().stream().findFirst().orElseGet(()->puntos.save(new PuntoVenta("Sucursal Central")));
  if(condiciones.count()==0){condiciones.save(new CondicionIva("IVA Responsable Inscripto"));condiciones.save(new CondicionIva("Consumidor Final"));}
  CondicionIva consumidor=condiciones.findAll().stream().filter(c->"Consumidor Final".equals(c.getDenominacion())).findFirst().orElse(condiciones.findAll().get(0));
  TipoMoneda ars=monedas.findAll().stream().findFirst().orElseGet(()->monedas.save(new TipoMoneda("ARS","Pesos argentinos")));
  ListaPrecio general=listas.findByDenominacion("General").orElseGet(()->listas.save(new ListaPrecio("General")));
  ListaPrecio mayorista=listas.findByDenominacion("Mayorista").orElseGet(()->listas.save(new ListaPrecio("Mayorista")));
  if(articulos.count()==0){
   var agua=articulos.save(new Articulo("BidÃ³n de agua",new BigDecimal("21.00")));
   var soda=articulos.save(new Articulo("Soda",new BigDecimal("21.00")));
   var vasos=articulos.save(new Articulo("Vasos descartables",new BigDecimal("10.50")));
   precios.save(new ListaPrecioArticulo(agua,general,new BigDecimal("1000.00")));
   precios.save(new ListaPrecioArticulo(soda,general,new BigDecimal("600.00")));
   precios.save(new ListaPrecioArticulo(vasos,general,new BigDecimal("125.00")));
  }
  if(articulos.findAll().stream().noneMatch(a->a.isActivo()&&a.getAlicuotaIva().signum()==0)){
   var sinIva=articulos.save(new Articulo("ArtÃ­culo sin IVA (prueba Factura C)",BigDecimal.ZERO));
   precios.save(new ListaPrecioArticulo(sinIva,general,new BigDecimal("100.00")));
  }
  Set<String> existentes=new HashSet<>();articulos.findAll().forEach(a->existentes.add(a.getDescripcion()));
  long activos=articulos.findAll().stream().filter(Articulo::isActivo).count();
  for(int n=0;activos<120;n++) {
   String nombre=NOMBRES[n%NOMBRES.length]+(n>=NOMBRES.length?" - variante "+(n/NOMBRES.length+1):"");
   if(existentes.add(nombre)){articulos.save(new Articulo(nombre,new BigDecimal("21.00")));activos++;}
  }
  var rubroBebidas=rubros.findAll().stream().filter(x->x.getDenominacion().equals("Bebidas")).findFirst().orElseGet(()->rubros.save(new Rubro("Bebidas")));
  var rubroAlmacen=rubros.findAll().stream().filter(x->x.getDenominacion().equals("Almacén y hogar")).findFirst().orElseGet(()->rubros.save(new Rubro("Almacén y hogar")));
  var marcaDemo=marcas.findAll().stream().filter(x->x.getDenominacion().equals("Marca demo")).findFirst().orElseGet(()->marcas.save(new Marca("Marca demo")));
  for(Articulo a:articulos.findAll()){
   if(a.getCodigo()==null||a.getCodigo().isBlank())a.setCodigo("ART-"+String.format("%04d",a.getId()));
   if(a.getMarca()==null)a.setMarca(marcaDemo);
   if(a.getRubro()==null)a.setRubro(a.getDescripcion().matches("(?i).*(agua|soda|jugo|gaseosa|leche).*")?rubroBebidas:rubroAlmacen);
  }
  Map<String,ListaPrecioArticulo> porArticuloYLista=new HashMap<>();
  precios.findAllByActivoTrueOrderByIdDesc().forEach(p->porArticuloYLista.putIfAbsent(clave(p.getArticulo().getId(),p.getListaPrecio().getId()),p));
  int indice=0;
  for(Articulo articulo:articulos.findAll()) {
   if(!articulo.isActivo())continue;
   String kg=clave(articulo.getId(),general.getId()),km=clave(articulo.getId(),mayorista.getId());
   ListaPrecioArticulo pg=porArticuloYLista.get(kg);
   if(pg==null){pg=precios.save(new ListaPrecioArticulo(articulo,general,new BigDecimal("150.00").add(new BigDecimal("37.50").multiply(BigDecimal.valueOf(indice)))));porArticuloYLista.put(kg,pg);}
   if(!porArticuloYLista.containsKey(km))porArticuloYLista.put(km,precios.save(new ListaPrecioArticulo(articulo,mayorista,pg.getPrecioNeto().multiply(new BigDecimal("0.90")).setScale(2,RoundingMode.HALF_UP))));
   indice++;
  }
  if(!nuevaBase)return;
  Articulo agua=articulos.findAll().stream().filter(a->"BidÃ³n de agua".equals(a.getDescripcion())).findFirst().orElseThrow();
  Articulo soda=articulos.findAll().stream().filter(a->"Soda".equals(a.getDescripcion())).findFirst().orElseThrow();
  Long precioAgua=porArticuloYLista.get(clave(agua.getId(),general.getId())).getId();
  Long precioSoda=porArticuloYLista.get(clave(soda.getId(),general.getId())).getId();
  String[] nombres={"Ana GarcÃ­a","Bruno PÃ©rez","Carla LÃ³pez","Diego MartÃ­nez","Elena RodrÃ­guez","FerreterÃ­a Norte","AlmacÃ©n Sur","LibrerÃ­a Central","Oficina RÃ­o","Distribuidora Sol"};
  for(int i=0;i<60;i++) {
   Cliente cliente=clientes.save(new Cliente(nombres[i%nombres.length]+" "+String.format("%02d",i+1),String.valueOf(20000000L+i)));
   for(int n=0;n<15;n++)facturas.crear(new FacturaRequestDTO(LocalDate.now().minusDays(n*3+i%3),punto.getId(),cliente.getId(),consumidor.getId(),ars.getId(),BigDecimal.ZERO,"Datos ficticios de prueba",List.of(new FacturaDetalleRequestDTO(precioAgua,BigDecimal.valueOf(1+n%5)),new FacturaDetalleRequestDTO(precioSoda,BigDecimal.valueOf(1+i%4)))));
   if(em!=null){em.flush();em.clear();}
  }
 }
 private static String clave(Long articulo,Long lista){return articulo+"|"+lista;}
 private static final String[] NOMBRES={"Agua mineral 500 ml",
  "Agua mineral 1 L",
  "Agua mineral 1,5 L",
  "Agua mineral 2 L",
  "Soda 500 ml",
  "Soda 1 L",
  "Soda 1,5 L",
  "Soda 2 L",
  "Jugo de naranja 200 ml",
  "Jugo de naranja 500 ml",
  "Jugo de naranja 1 L",
  "Jugo de naranja 1,5 L",
  "Jugo de manzana 200 ml",
  "Jugo de manzana 500 ml",
  "Jugo de manzana 1 L",
  "Jugo de manzana 1,5 L",
  "Gaseosa cola 354 ml",
  "Gaseosa cola 500 ml",
  "Gaseosa cola 1,5 L",
  "Gaseosa cola 2,25 L",
  "Gaseosa lima 354 ml",
  "Gaseosa lima 500 ml",
  "Gaseosa lima 1,5 L",
  "Gaseosa lima 2,25 L",
  "Leche entera 200 ml",
  "Leche entera 500 ml",
  "Leche entera 1 L",
  "Leche entera 2 L",
  "Leche descremada 200 ml",
  "Leche descremada 500 ml",
  "Leche descremada 1 L",
  "Leche descremada 2 L",
  "Arroz 250 g",
  "Arroz 500 g",
  "Arroz 1 kg",
  "Arroz 2 kg",
  "AzÃºcar 250 g",
  "AzÃºcar 500 g",
  "AzÃºcar 1 kg",
  "AzÃºcar 2 kg",
  "Harina 250 g",
  "Harina 500 g",
  "Harina 1 kg",
  "Harina 2 kg",
  "Yerba mate 250 g",
  "Yerba mate 500 g",
  "Yerba mate 1 kg",
  "Yerba mate 2 kg",
  "CafÃ© molido 100 g",
  "CafÃ© molido 250 g",
  "CafÃ© molido 500 g",
  "CafÃ© molido 1 kg",
  "TÃ© negro 10 saquitos",
  "TÃ© negro 20 saquitos",
  "TÃ© negro 25 saquitos",
  "TÃ© negro 50 saquitos",
  "Fideos secos 250 g",
  "Fideos secos 500 g",
  "Fideos secos 1 kg",
  "Fideos secos 2 kg",
  "Lentejas 250 g",
  "Lentejas 500 g",
  "Lentejas 1 kg",
  "Lentejas 2 kg",
  "Garbanzos 250 g",
  "Garbanzos 500 g",
  "Garbanzos 1 kg",
  "Garbanzos 2 kg",
  "Avena 250 g",
  "Avena 500 g",
  "Avena 1 kg",
  "Avena 2 kg",
  "Galletitas de agua 100 g",
  "Galletitas de agua 200 g",
  "Galletitas de agua 300 g",
  "Galletitas de agua 500 g",
  "Galletitas dulces 100 g",
  "Galletitas dulces 200 g",
  "Galletitas dulces 300 g",
  "Galletitas dulces 500 g",
  "Aceite de girasol 500 ml",
  "Aceite de girasol 900 ml",
  "Aceite de girasol 1,5 L",
  "Aceite de girasol 3 L",
  "PurÃ© de tomate 210 g",
  "PurÃ© de tomate 340 g",
  "PurÃ© de tomate 520 g",
  "PurÃ© de tomate 1 kg",
  "Sal fina 100 g",
  "Sal fina 250 g",
  "Sal fina 500 g",
  "Sal fina 1 kg",
  "Cacao en polvo 100 g",
  "Cacao en polvo 200 g",
  "Cacao en polvo 400 g",
  "Cacao en polvo 800 g",
  "Mermelada 200 g",
  "Mermelada 250 g",
  "Mermelada 450 g",
  "Mermelada 1 kg",
  "Dulce de leche 250 g",
  "Dulce de leche 400 g",
  "Dulce de leche 500 g",
  "Dulce de leche 1 kg",
  "Detergente 250 ml",
  "Detergente 500 ml",
  "Detergente 750 ml",
  "Detergente 1 L",
  "Lavandina 500 ml",
  "Lavandina 1 L",
  "Lavandina 2 L",
  "Lavandina 4 L",
  "Servilletas 50 unidades",
  "Servilletas 100 unidades",
  "Servilletas 150 unidades",
  "Servilletas 200 unidades",
  "Bolsas de residuos 10 unidades",
  "Bolsas de residuos 20 unidades",
  "Bolsas de residuos 30 unidades",
  "Bolsas de residuos 50 unidades"};
}
