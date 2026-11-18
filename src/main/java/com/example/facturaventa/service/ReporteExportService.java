package com.example.facturaventa.service;

import com.example.facturaventa.dto.FacturaReporteDTO;
import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Chunk;
import com.lowagie.text.Phrase;
import com.lowagie.text.FontFactory;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import java.io.*;
import java.text.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class ReporteExportService {
    private final FacturaService facturas;
    private final com.example.facturaventa.repository.FacturaVentaRepository repository;
    private static final String[] COLUMNAS={"N.º factura","Fecha","Cliente","Condición IVA","Punto de venta","Total","Ítems"};

    public byte[] pdf(java.time.LocalDate desde, java.time.LocalDate hasta, String estado, Double minimo) {
        List<FacturaReporteDTO> filas=facturas.buscarFacturasFiltradas(desde,hasta,estado,minimo);
        try (ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            Document documento=new Document(PageSize.A4.rotate(),32,32,30,30);
            PdfWriter.getInstance(documento,out);
            documento.open();
            documento.add(new Paragraph("Reporte ejecutivo de facturas de venta",FontFactory.getFont(FontFactory.HELVETICA_BOLD,17)));
            documento.add(new Paragraph("Fecha de generación: "+java.time.LocalDate.now()+"  |  Facturas: "+filas.size(),FontFactory.getFont(FontFactory.HELVETICA,10)));
            documento.add(Chunk.NEWLINE);
            PdfPTable tabla=new PdfPTable(new float[]{1.0f,1.05f,2.25f,1.9f,1.7f,1.15f,.6f});
            tabla.setWidthPercentage(100);
            for(String titulo:COLUMNAS) {
                PdfPCell cell=new PdfPCell(new Phrase(titulo,FontFactory.getFont(FontFactory.HELVETICA_BOLD,9)));
                cell.setBackgroundColor(new java.awt.Color(224,236,248)); cell.setPadding(7); tabla.addCell(cell);
            }
            for(FacturaReporteDTO f:filas) {
                String[] datos={String.valueOf(f.getNumeroFactura()),fecha(f.getFechaEmision()),f.getClienteDenominacion(),
                    f.getCondicionIva(),f.getPuntoVentaDescripcion(),dinero(f.getImporteTotal()),String.valueOf(f.getCantidadItems())};
                for(String dato:datos) {
                    PdfPCell cell=new PdfPCell(new Phrase(dato,FontFactory.getFont(FontFactory.HELVETICA,9)));
                    cell.setPadding(6); tabla.addCell(cell);
                }
            }
            documento.add(tabla);
            documento.add(Chunk.NEWLINE);
            documento.add(new Paragraph("TOTAL GENERAL: "+dinero(filas.stream().mapToDouble(FacturaReporteDTO::getImporteTotal).sum()),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD,11)));
            documento.close();
            return out.toByteArray();
        } catch (DocumentException | IOException ex) {throw new IllegalStateException("No se pudo generar el PDF",ex);}
    }

    @org.springframework.transaction.annotation.Transactional(readOnly=true)
    public byte[] pdfIndividual(Long id) {
        var f=repository.findById(id).orElseThrow(()->new com.example.facturaventa.exception.RecursoNoEncontradoException("Factura "+id+" no encontrada"));
        try(ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            Document doc=new Document(PageSize.A4,40,40,36,50);
            PdfWriter writer=PdfWriter.getInstance(doc,out);
            writer.setPageEvent(new PdfPageEventHelper(){
                @Override public void onEndPage(PdfWriter w,Document d){
                    ColumnText.showTextAligned(w.getDirectContent(),com.lowagie.text.Element.ALIGN_CENTER,
                        new Phrase("Factura #"+f.getNumero()+" - Página "+w.getPageNumber(),FontFactory.getFont(FontFactory.HELVETICA,9)),PageSize.A4.getWidth()/2,25,0);
                }
            });
            doc.addTitle("Factura #"+f.getNumero());doc.open();
            boolean autorizada=f.getCae()!=null&&!f.getCae().isBlank();
            String tipo=autorizada?switch(f.getTipoComprobanteFiscal()==null?0:f.getTipoComprobanteFiscal()){case 1->"Factura A";case 6->"Factura B";case 11->"Factura C";default->"Factura";}:"Factura de venta";
            doc.add(new Paragraph(tipo+" - #"+f.getNumero(),FontFactory.getFont(FontFactory.HELVETICA_BOLD,21)));
            doc.add(new Paragraph(autorizada?"ARCA HOMOLOGACIÓN - SIN VALOR FISCAL":"REGISTRO LOCAL - SIN AUTORIZACIÓN ARCA",FontFactory.getFont(FontFactory.HELVETICA_BOLD,10)));
            doc.add(new Paragraph("Estado: "+f.getEstado()+"    |    Fecha de emisión: "+fecha(f.getFechaEmision()),FontFactory.getFont(FontFactory.HELVETICA,11)));
            if("ANULADA".equals(f.getEstado()))doc.add(new Paragraph("COMPROBANTE ANULADO",FontFactory.getFont(FontFactory.HELVETICA_BOLD,16,java.awt.Color.RED)));
            doc.add(Chunk.NEWLINE);
            var cliente=f.getCliente();
            doc.add(new Paragraph("Cliente: "+(cliente==null?"Sin cliente asignado":cliente.getDenominacion()),FontFactory.getFont(FontFactory.HELVETICA_BOLD,12)));
            doc.add(new Paragraph("Documento: "+(cliente==null||cliente.getDocumento()==null?"Sin informar":cliente.getDocumento()),FontFactory.getFont(FontFactory.HELVETICA,10)));
            if(cliente!=null){
                if(cliente.getTipoCliente()!=null)doc.add(new Paragraph("Tipo de cliente: "+cliente.getTipoCliente(),FontFactory.getFont(FontFactory.HELVETICA,10)));
                if(cliente.getContacto()!=null){var c=cliente.getContacto();String t=java.util.stream.Stream.of(c.getEmail(),c.getTelefono(),c.getCelular()).filter(x->x!=null&&!x.isBlank()).collect(java.util.stream.Collectors.joining(" | "));if(!t.isBlank())doc.add(new Paragraph("Contacto: "+t,FontFactory.getFont(FontFactory.HELVETICA,10)));}
                if(cliente.getDomicilio()!=null){var d=cliente.getDomicilio();String t=java.util.stream.Stream.of(d.getNombreCalle(),d.getNumeroCalle(),d.getLocalidad(),d.getProvincia(),d.getCodigoPostal()).filter(x->x!=null&&!x.isBlank()).collect(java.util.stream.Collectors.joining(" "));if(!t.isBlank())doc.add(new Paragraph("Domicilio: "+t,FontFactory.getFont(FontFactory.HELVETICA,10)));}
            }
            doc.add(new Paragraph("Condición IVA: "+f.getCondicionIva().getDenominacion(),FontFactory.getFont(FontFactory.HELVETICA,10)));
            doc.add(new Paragraph("Punto de venta: "+f.getPuntoVenta().getDescripcion()+"    |    Moneda: "+f.getTipoMoneda().getCodigo(),FontFactory.getFont(FontFactory.HELVETICA,10)));
            doc.add(Chunk.NEWLINE);
            PdfPTable tabla=new PdfPTable(new float[]{3.5f,.85f,1.1f,.8f,.65f,1.15f});tabla.setWidthPercentage(100);tabla.setHeaderRows(1);tabla.setSplitLate(false);
            for(String title:new String[]{"Artículo","Cant.","Precio neto","Bonif. %","IVA %","Total"}) {
                PdfPCell c=new PdfPCell(new Phrase(title,FontFactory.getFont(FontFactory.HELVETICA_BOLD,10)));c.setBackgroundColor(new java.awt.Color(224,236,248));c.setPadding(7);tabla.addCell(c);
            }
            for(var item:f.getDetalles()) {
                String[] vals={(item.getDescripcion()==null?item.getArticulo().getDescripcion():item.getDescripcion())+"\nCódigo: "+(item.getArticulo().getCodigo()==null?"Sin informar":item.getArticulo().getCodigo())+"\nLista: "+item.getListaPrecioArticulo().getListaPrecio().getDenominacion(),item.getCantidad().stripTrailingZeros().toPlainString(),importe(item.getPrecioUnitarioNeto().doubleValue()),item.getPorcentajeBonificacion().stripTrailingZeros().toPlainString(),item.getAlicuotaIva().stripTrailingZeros().toPlainString(),importe(item.getImporteTotal().doubleValue())};
                for(int i=0;i<vals.length;i++){PdfPCell c=new PdfPCell(new Phrase(vals[i],FontFactory.getFont(FontFactory.HELVETICA,10)));c.setPadding(7);if(i>0)c.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_RIGHT);tabla.addCell(c);}
            }
            doc.add(tabla);doc.add(Chunk.NEWLINE);
            Paragraph totales=new Paragraph("Neto: "+importe(f.getImporteNeto())+"\nIVA: "+importe(f.getImporteIva())+"\nTOTAL: "+importe(f.getImporteTotal())+" "+f.getTipoMoneda().getCodigo()+"\nCobrado: "+importe(f.getImporteCobrado())+"\nSaldo: "+importe(f.getImporteSaldo()),FontFactory.getFont(FontFactory.HELVETICA_BOLD,11));totales.setAlignment(com.lowagie.text.Element.ALIGN_RIGHT);totales.setLeading(17);totales.setKeepTogether(true);doc.add(totales);
            if(f.getObservaciones()!=null&&!f.getObservaciones().isBlank()){doc.add(Chunk.NEWLINE);doc.add(new Paragraph("Observaciones: "+f.getObservaciones(),FontFactory.getFont(FontFactory.HELVETICA,10)));}
            if(autorizada){
                doc.add(Chunk.NEWLINE);String fiscal=String.format(Locale.ROOT,"%05d-%08d",f.getPuntoVentaFiscal(),f.getNumeroFiscal());
                Paragraph datos=new Paragraph("Comprobante de homologación: "+fiscal+"\nCAE: "+f.getCae()+"\nVencimiento del CAE: "+(f.getCaeFechaVencimiento()==null?"Sin informar":fecha(f.getCaeFechaVencimiento())),FontFactory.getFont(FontFactory.HELVETICA_BOLD,10));datos.setKeepTogether(true);doc.add(datos);
            }
            doc.close();return out.toByteArray();
        }catch(DocumentException|IOException e){throw new IllegalStateException("No se pudo generar el PDF de la factura",e);}
    }
    private String importe(double valor){NumberFormat n=NumberFormat.getNumberInstance(Locale.forLanguageTag("es-AR"));n.setMinimumFractionDigits(2);n.setMaximumFractionDigits(2);return n.format(valor);}

    public byte[] excel(java.time.LocalDate desde, java.time.LocalDate hasta, String estado, Double minimo) {
        List<FacturaReporteDTO> filas=facturas.buscarFacturasFiltradas(desde,hasta,estado,minimo);
        try (XSSFWorkbook libro=new XSSFWorkbook(); ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            Sheet hoja=libro.createSheet("Ventas");
            CellStyle cabecera=libro.createCellStyle();
            Font negrita=libro.createFont();negrita.setBold(true);cabecera.setFont(negrita);
            cabecera.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
            cabecera.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            CellStyle fecha=libro.createCellStyle();fecha.setDataFormat(libro.getCreationHelper().createDataFormat().getFormat("dd/mm/yyyy"));
            CellStyle moneda=libro.createCellStyle();moneda.setDataFormat(libro.getCreationHelper().createDataFormat().getFormat("#,##0.00"));
            Row titulo=hoja.createRow(0);
            for(int i=0;i<COLUMNAS.length;i++){Cell c=titulo.createCell(i);c.setCellValue(COLUMNAS[i]);c.setCellStyle(cabecera);}
            int n=1;
            for(FacturaReporteDTO f:filas) {
                Row fila=hoja.createRow(n++);
                fila.createCell(0).setCellValue(f.getNumeroFactura());
                Cell cFecha=fila.createCell(1);cFecha.setCellValue(f.getFechaEmision());cFecha.setCellStyle(fecha);
                fila.createCell(2).setCellValue(f.getClienteDenominacion());
                fila.createCell(3).setCellValue(f.getCondicionIva());
                fila.createCell(4).setCellValue(f.getPuntoVentaDescripcion());
                Cell cTotal=fila.createCell(5);cTotal.setCellValue(f.getImporteTotal());cTotal.setCellStyle(moneda);
                fila.createCell(6).setCellValue(f.getCantidadItems());
            }
            Row total=hoja.createRow(n);
            Cell texto=total.createCell(4);texto.setCellValue("TOTAL GENERAL");texto.setCellStyle(cabecera);
            Cell importe=total.createCell(5);
            if(!filas.isEmpty()) importe.setCellFormula("SUM(F2:F"+n+")");
            else importe.setCellValue(0d);
            importe.setCellStyle(moneda);
            int[] anchuras={15,16,34,31,27,18,12};
            for(int i=0;i<anchuras.length;i++)hoja.setColumnWidth(i,anchuras[i]*256);
            hoja.createFreezePane(0,1);
            hoja.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0,Math.max(1,n-1),0,6));
            libro.write(out);
            return out.toByteArray();
        } catch(IOException ex){throw new IllegalStateException("No se pudo generar Excel",ex);}
    }
    private String fecha(java.util.Date d){return new SimpleDateFormat("dd/MM/yyyy").format(d);}
    private String dinero(double v){return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-AR")).format(v);}
}

