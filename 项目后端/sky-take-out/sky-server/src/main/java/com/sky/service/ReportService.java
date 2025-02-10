package com.sky.service;

import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;

public interface ReportService {

    TurnoverReportVO getTurnoverStatistics(LocalDate start, LocalDate end);

    UserReportVO getUserStatistics(LocalDate start, LocalDate end);

    OrderReportVO getOrderStatistics(LocalDate start, LocalDate end);

    SalesTop10ReportVO getSalesTop10(LocalDate start, LocalDate end);

    void exportBusinessData(HttpServletResponse response);
}
