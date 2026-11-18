# Facturación de ventas — Trabajo práctico

Aplicación académica con Spring Boot, JPA/Hibernate, HTML y TypeScript. Incluye código fuente, pruebas y configuración de compilación. No incluye el iniciador personal, credenciales ni bases de datos del autor.

## Funcionalidades

- Clientes independientes con contacto, domicilio, tipo de cliente e historial de facturas.
- Artículos con código, rubro y marca editables.
- Selección múltiple de precios mediante casillas: una fila por artículo y columnas por lista.
- Descripción y bonificación por renglón; cálculo de neto, IVA, total, cobrado y saldo en el servidor.
- Edición, anulación y eliminación de comprobantes locales; protección de facturas con CAE.
- PDF individual, PDF del listado y exportación a Excel.
- Integración opcional con ARCA en homologación, desactivada por defecto.

## Requisitos

- JDK 17 o superior, con `JAVA_HOME` configurado. También se verificó con JDK 21.
- Internet para descargar dependencias en la primera compilación.
- Maven Wrapper incluido: no hace falta instalar Maven por separado.
- Node.js y npm solo si se modifica o recompila TypeScript. Se incluyen los JavaScript que usa la aplicación.

## Ejecutar en Windows

Abrir PowerShell en la carpeta que contiene `pom.xml`:

```powershell
.\mvnw.cmd spring-boot:run
```

Abrir **http://localhost:8080/**. Mantener abierto el proceso mientras se usa el programa. Detenerlo con `Ctrl+C`.

En Linux/macOS:

```bash
chmod +x mvnw
./mvnw spring-boot:run
```

Si el puerto está ocupado, en Windows:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"
```

Abrir entonces http://localhost:8081/.

## Datos de demostración

Por defecto se usa H2 en archivo; no hace falta instalar PostgreSQL. La primera ejecución sobre una base nueva genera **60 clientes, 900 facturas, 120 artículos y 240 precios**. Los datos son ficticios; los comprobantes iniciales no tienen autorización fiscal. La carga inicial no duplica las facturas al reiniciar.

La base se guarda en `facturas.mv.db` en el directorio desde el que se ejecuta el programa. Los datos persisten al cerrarlo y están excluidos de Git. No se incluye la base personal de presentación.

## Uso

1. En **Clientes**, crear o editar una cuenta. **Ver facturas** muestra su historial.
2. En **Nueva factura**, elegir un cliente existente.
3. Pulsar **Agregar artículo**, marcar los precios y confirmar con **Agregar seleccionados**. La selección se conserva entre páginas y búsquedas.
4. Editar cantidad, descripción y bonificación de cada renglón; guardar.
5. En **Buscar factura**, filtrar comprobantes y usar las acciones de cada fila. **PDF** descarga una sola factura; **PDF del listado** exporta los resultados.
6. En **Artículos**, editar código, descripción, rubro y marca sin cambiar sus precios.

El tipo MINORISTA/MAYORISTA del cliente es informativo: la lista se elige expresamente al facturar. La bonificación admite 0 a 100%, hasta dos decimales. Se aplica al neto antes del IVA; el servidor valida y redondea los importes definitivos.

## Compilar el frontend

Desde la raíz:

```powershell
npm ci --prefix frontend
npm run check --prefix frontend
npm run build --prefix frontend
```

TypeScript genera los JavaScript en `src/main/resources/static`. No se usa `npm start`: Spring Boot sirve la interfaz. Después de recompilar, reiniciar el backend y recargar el navegador.

## Pruebas y compilación

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
java -jar target/facturaventa-1.0.0.jar
```

`package` también ejecuta las pruebas. Las pruebas locales usan H2 en memoria y no necesitan certificados reales. La prueba remota ARCA está omitida por defecto. Las pruebas de caché pueden escribir tickets ficticios en el directorio del usuario. Para ubicarlos dentro de una carpeta ignorada:

```powershell
$env:ARCA_TICKET_CACHE_DIR = Join-Path (Get-Location) "data/test-ticket-cache"
.\mvnw.cmd test
```

## Modelo principal

```text
FacturaVenta 1 ── N FacturaVentaDetalle N ── 1 ListaPrecioArticulo
                                               ├── N:1 Articulo
                                               └── N:1 ListaPrecio
Articulo ── N:1 Rubro / N:1 Marca
Cliente  ── 1:1 Contacto / 1:1 Domicilio
FacturaVenta ── N:1 Cliente / PuntoVenta / CondicionIva / TipoMoneda
```

El detalle recibe `listaPrecioArticuloId`, `cantidad`, `descripcion` y `porcentajeBonificacion`. El servidor recupera el precio exacto elegido y guarda los importes del comprobante. Los cálculos monetarios del detalle utilizan `BigDecimal`.

## ARCA opcional: solo homologación

El código está en `src/main/java/com/example/facturaventa/arca`. El CRUD local funciona sin configurarlo. Esta integración académica no habilita producción.

Se requieren credenciales propias, un certificado PKCS12 válido y autorización del servicio WSFE en homologación. Configurar externamente estas variables antes de iniciar:

| Variable | Contenido |
| --- | --- |
| `ARCA_ENABLED` | `true` para habilitar la integración |
| `ARCA_CUIT` | CUIT representado, de 11 dígitos |
| `ARCA_PKCS12_PATH` | Ruta al PKCS12 privado, fuera del repositorio |
| `ARCA_PKCS12_PASSWORD` | Contraseña del PKCS12 |
| `ARCA_PKCS12_ALIAS` | Alias del PKCS12, si corresponde |
| `ARCA_TICKET_CACHE_DIR` | Directorio privado para la caché cifrada; opcional |

No publicar valores reales, certificados, claves, contraseñas o tickets. Comprobar la conexión desde la aplicación y utilizar los datos fiscales propios de homologación. Crear una factura local no solicita un CAE automáticamente. El repositorio no incluye las credenciales ni las autorizaciones del autor.

## PostgreSQL opcional

Activar `SPRING_PROFILES_ACTIVE=postgres` y configurar `DB_URL`, `DB_USER` y `DB_PASSWORD` externamente, con una base y credenciales propias. La carga demo está desactivada en ese perfil salvo que se indique `DEMO_DATA_ENABLED=true`.

## Estructura

- `src/main/java`: entidades, DTOs, repositorios, servicios, controladores, migraciones y ARCA.
- `src/main/resources`: configuración y frontend servido por Spring Boot.
- `src/test/java`: pruebas automatizadas.
- `frontend/src`: fuentes TypeScript.
- `.mvn`, `mvnw`, `mvnw.cmd`, `pom.xml`: Maven Wrapper y configuración de compilación.

## Subir a GitHub para la entrega

Crear un repositorio vacío y subir el contenido de esta carpeta, con `pom.xml` en la raíz. Alternativamente:

```bash
git init -b main
git add .
git status
git commit -m "Entrega del trabajo práctico de facturación"
git remote add origin URL_DEL_REPOSITORIO
git push -u origin main
```

Reemplazar `URL_DEL_REPOSITORIO` por la URL propia y revisar `git status` antes del commit. No subir la carpeta personal de presentación. El `.gitignore` excluye credenciales, bases, configuraciones locales, dependencias descargadas y compilados; no elimina archivos sensibles que ya hubieran sido versionados.
