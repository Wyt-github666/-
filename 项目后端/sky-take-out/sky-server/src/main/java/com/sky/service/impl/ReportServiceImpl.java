package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WorkspaceServiceImpl workspaceService;

    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate start, LocalDate end) {
        List<LocalDate> dateList = getDateList(start, end);
        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            Map map = new HashMap();
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            map.put("begin",beginTime);
            map.put("end",endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.getsumBymap(map);
            turnover = turnover == null ? 0.0 : turnover;
            turnoverList.add(turnover);
        }
        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .turnoverList(StringUtils.join(turnoverList,","))
                .build();
    }

    @Override
    public UserReportVO getUserStatistics(LocalDate start, LocalDate end) {
        List<LocalDate> dateList = getDateList(start, end);
        List<Integer> NewuserList = new ArrayList<>();
        List<Integer> TotaluserList = new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map map = new HashMap();
            map.put("end",endTime);
            TotaluserList.add(userMapper.countBymap(map));
            map.put("begin",beginTime);
            NewuserList.add(userMapper.countBymap(map));
        }
        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .totalUserList(StringUtils.join(TotaluserList,","))
                .newUserList(StringUtils.join(NewuserList,","))
                .build();
    }

    public OrderReportVO getOrderStatistics(LocalDate start, LocalDate end) {
        List<LocalDate> dateList = getDateList(start, end);
        List<Integer> AllorderList = new ArrayList<>();
        List<Integer> CompletedorderList = new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map map = new HashMap();
            map.put("end",endTime);
            map.put("begin",beginTime);
            AllorderList.add(orderMapper.getcountorderBymap(map));
            map.put("status", Orders.COMPLETED);
            CompletedorderList.add(orderMapper.getcountorderBymap(map));
        }
        Integer allorderSum =  AllorderList.stream().reduce(Integer::sum).get();
        Integer completeorderSum = CompletedorderList.stream().reduce(Integer::sum).get();
        Double orderCompletionRate = allorderSum != 0 ? (double) completeorderSum / allorderSum : 0.0;
        log.info("订单完成率:{}", orderCompletionRate);
        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .orderCompletionRate(orderCompletionRate)
                .orderCountList(StringUtils.join(AllorderList,","))
                .totalOrderCount(allorderSum)
                .validOrderCount(completeorderSum)
                .validOrderCountList(StringUtils.join(CompletedorderList,","))
                .build();
    }

    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate start, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(start, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        List<GoodsSalesDTO> list = orderMapper.getSalesTop10(beginTime, endTime);
        List<String> nameList = list.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        List<Integer> numberList = list.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        return SalesTop10ReportVO.builder()
                .nameList(StringUtils.join(nameList,","))
                .numberList(StringUtils.join(numberList,","))
                .build();
    }

    public void exportBusinessData(HttpServletResponse response) {
        // 查询最近30天的数据
        LocalDate dataBegin = LocalDate.now().minusDays(30);
        LocalDate dataEnd = LocalDate.now().minusDays(1);
        BusinessDataVO businessDataVO = workspaceService.getBusinessData(LocalDateTime.of(dataBegin, LocalTime.MIN), LocalDateTime.of(dataEnd, LocalTime.MAX));
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/example.xlsx");
        try {
            XSSFWorkbook excel = new XSSFWorkbook(in);
            XSSFSheet sheet = excel.getSheet("sheet1");
            sheet.getRow(1).getCell(1).setCellValue("时间："+ dataBegin + "至" + dataEnd);
            XSSFRow row = sheet.getRow(3);
            row.getCell(2).setCellValue(businessDataVO.getTurnover());
            row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessDataVO.getNewUsers());

            row = sheet.getRow(4);
            row.getCell(2).setCellValue(businessDataVO.getValidOrderCount());
            row.getCell(4).setCellValue(businessDataVO.getUnitPrice());
            for(int i = 0;i < 30; i++){
                LocalDate data = dataBegin.plusDays(i);
                BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(data, LocalTime.MIN), LocalDateTime.of(data, LocalTime.MAX));
                row = sheet.getRow(7+i);
                row.getCell(1).setCellValue(data.toString());
                row.getCell(2).setCellValue(businessData.getTurnover());
                row.getCell(3).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData.getUnitPrice());
                row.getCell(6).setCellValue(businessData.getNewUsers());
            }
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);
            out.close();
            excel.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<LocalDate> getDateList(LocalDate start, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(start);
        while (!start.equals(end)) {
            start = start.plusDays(1);
            dateList.add(start);
        }
        return dateList;
    }
}
