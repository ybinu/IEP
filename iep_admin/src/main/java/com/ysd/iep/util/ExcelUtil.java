package com.ysd.iep.util;

import com.ysd.iep.annotation.ExcelField;
import com.ysd.iep.dao.BaseDao;
import lombok.Data;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.formula.functions.T;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 表格工具类
 */
@Data
public class ExcelUtil {
    private BaseDao baseDao;

    public ExcelUtil(BaseDao baseDao) {
        this.baseDao = baseDao;
    }

    /**
     * 导出表格
     *
     * @param response 响应流
     * @param fileName 文件名称
     * @param list     数据集合
     * @throws IOException
     * @throws IllegalAccessException
     */
    public static void exportExcel(HttpServletResponse response, String fileName, List list) throws IOException, IllegalAccessException {
        response.setContentType(".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.addHeader("Content-Disposition", "attachment;filename=" + new String(fileName.getBytes(), "iso-8859-1"));
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet(fileName);
        if (list == null || list.size() == 0) {
            XSSFFont font = workbook.createFont();
            font.setFontHeightInPoints((short) 30);
            font.setFontName("IMPACT");
            font.setItalic(true);
            font.setColor(HSSFColor.RED.index);
            XSSFCellStyle style = workbook.createCellStyle();
            style.setFont(font);
            style.setAlignment(XSSFCellStyle.VERTICAL_CENTER);
            style.setAlignment(XSSFCellStyle.ALIGN_CENTER);
            //合并单元格
            sheet.addMergedRegion(new CellRangeAddress(0, 5, 0, 5));
            XSSFRow row = sheet.createRow(0);
            XSSFCell cell = row.createCell(0);
            cell.setCellValue("暂无数据");
            cell.setCellStyle(style);
        } else {
            XSSFRow rowTop = sheet.createRow(0);
            for (int i = 0; i < list.size(); i++) {
                Object o = list.get(i);
                Class clazz = o.getClass();
                Field[] fields = clazz.getDeclaredFields();
                XSSFRow row = sheet.createRow(i + 1);
                int k = -1;
                for (int j = 0; j < fields.length; j++) {
                    Field f = fields[j];
                    k++;
                    f.setAccessible(true);
                    ExcelField annField = f.getDeclaredAnnotation(ExcelField.class);
                    if (annField != null && annField.showDisplay() == true) {
                        k -= 1;
                        continue;
                    }
                    if (i == 0) {
                        String fieldName = "";
                        if (annField == null)
                            fieldName = f.getName();
                        else
                            fieldName = annField.value();
                        sheet.setColumnWidth(k, fieldName.getBytes().length * 2 * 256);//自适应宽度
                        XSSFCell cell = rowTop.createCell(k);
                        cell.setCellValue(fieldName);
                    }
                    Object fieldValue = f.get(o);
                    String fieldName = fieldValue != null ? fieldValue.toString() : "";
                    XSSFCell cell = row.createCell(k);
                    cell.setCellValue(fieldName);
                }


            }
        }
        ServletOutputStream outputStream = response.getOutputStream();
        workbook.write(outputStream);
        outputStream.flush();
        outputStream.close();
    }

    /**
     * 导入表格返回生成的列名集合和参数集合
     *
     * @param clazz       实体类class
     * @param inputStream 输入流
     * @return map对象 (fieldNames:列名集合,类型:List<String>) (fieldValues 参数集合 类型:List<Object[]>)
     */
    public void importExcel(Class<T> clazz, InputStream inputStream) throws IOException, IllegalAccessException, InstantiationException {
        XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
        XSSFSheet sheet = workbook.getSheetAt(0);
        XSSFRow row;
        String cell;
        List<Field> fields = Arrays.asList(clazz.getDeclaredFields());
        int rows = sheet.getRow(0).getPhysicalNumberOfCells();
        List<String> fieldNames = new ArrayList<>();
        row = sheet.getRow(0);
        //循环第一行 找到所有的列字段
        for (int j = row.getFirstCellNum(); j < row.getPhysicalNumberOfCells(); j++) {
            cell = row.getCell(j).toString();
            for (Field f : fields) {
                f.setAccessible(true);
                if (f.getName().equals(cell)) {
                    fieldNames.add(f.getName());
                    break;
                }
                ExcelField excelField = f.getDeclaredAnnotation(ExcelField.class);
                if (excelField != null && excelField.value().equals(cell)) {
                    fieldNames.add(f.getName());
                    break;
                }
            }
        }
        //没有找到
        if (fieldNames.size() == 0) {
            return;
        }

        List<T> list = new ArrayList();
        for (int i = sheet.getFirstRowNum() + 1; i < sheet.getPhysicalNumberOfRows(); i++) {
            row = sheet.getRow(i);
            T t = clazz.newInstance();
            for (int j = 0; j < rows; j++) {
                XSSFCell xssfCell = row.getCell(j);
                if (xssfCell == null) {
                    continue;
                }
                cell = xssfCell.toString();
                if (cell.equals("")) {
                    continue;
                } else {
                    String result = parseExcel(xssfCell);
                    Field field = fields.get(j);
                    field.set(t, result);
                }
            }
            list.add(t);
        }
        baseDao.saveAll(list);
    }


    /**
     * 日期转换
     *
     * @param cell
     * @return
     */
    private static String parseExcel(Cell cell) {
        String result = new String();
        switch (cell.getCellType()) {
            case HSSFCell.CELL_TYPE_NUMERIC:// 数字类型
                if (HSSFDateUtil.isCellDateFormatted(cell)) {// 处理日期格式、时间格式
                    SimpleDateFormat sdf = null;
                    if (cell.getCellStyle().getDataFormat() == HSSFDataFormat
                            .getBuiltinFormat("h:mm")) {
                        sdf = new SimpleDateFormat("HH:mm");
                    } else {// 日期
                        sdf = new SimpleDateFormat("yyyy-MM-dd");
                    }
                    Date date = cell.getDateCellValue();
                    result = sdf.format(date);
                } else if (cell.getCellStyle().getDataFormat() == 58) {
                    // 处理自定义日期格式：m月d日(通过判断单元格的格式id解决，id的值是58)
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    double value = cell.getNumericCellValue();
                    Date date = org.apache.poi.ss.usermodel.DateUtil
                            .getJavaDate(value);
                    result = sdf.format(date);
                } else {
                    double value = cell.getNumericCellValue();
                    CellStyle style = cell.getCellStyle();
                    DecimalFormat format = new DecimalFormat();
                    String temp = style.getDataFormatString();
                    // 单元格设置成常规
                    if (temp.equals("General")) {
                        format.applyPattern("#");
                    }
                    result = format.format(value);
                }
                break;
            case HSSFCell.CELL_TYPE_STRING:// String类型
                result = cell.getRichStringCellValue().toString();
                break;
            case HSSFCell.CELL_TYPE_BLANK:
                result = "";
            default:
                result = "";
                break;
        }
        return result;
    }

    /**
     * 模板导出
     *
     * @param response 响应流
     * @param fileName 导出文件名称
     * @param fields   需要导出的所有字段名称
     */
    public static void TemplateExprot(HttpServletResponse response, String fileName, List<String> fields) throws IOException {
        response.setContentType(".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.addHeader("Content-Disposition", "attachment;filename=" + new String(fileName.getBytes(), "iso-8859-1"));
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet(fileName);
        XSSFRow row = sheet.createRow(0);
        for (int i = 0; i < fields.size(); i++) {
            XSSFCell cell = row.createCell(i);
            cell.setCellValue(fields.get(i));
        }
        ServletOutputStream outputStream = response.getOutputStream();
        workbook.write(outputStream);
        outputStream.flush();
        outputStream.close();
    }

}
