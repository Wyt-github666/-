package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;

@RestController
@RequestMapping("/admin/report")
@Api("统计相关接口")
@Slf4j
public class ReportController {

    @Autowired
    private ReportService reportService;

    @GetMapping("/turnoverStatistics")
    @ApiOperation("营业额统计")
    public Result<TurnoverReportVO> turnoverReport(
             @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin
            ,@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end)
    {
        log.info("营业额相关数据,开始时间：{},结束时间：{}",begin,end);
        return Result.success(reportService.getTurnoverStatistics(begin, end));
    }

    @GetMapping("/userStatistics")
    @ApiOperation("用户统计")
    public Result<UserReportVO> userReport(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin
            ,@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end)
    {
        log.info("用户相关数据,开始时间：{},结束时间：{}",begin,end);
        return Result.success(reportService.getUserStatistics(begin, end));
    }

    @GetMapping("/ordersStatistics")
    @ApiOperation("订单统计")
    public Result<OrderReportVO> orderReport(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin
            ,@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end
    ){
        return Result.success(reportService.getOrderStatistics(begin, end));
    }

    @GetMapping("/top10")
    @ApiOperation("获取菜品销量TOP10")
    public Result<SalesTop10ReportVO> getTop10(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin
            ,@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end
    ){
        return Result.success(reportService.getSalesTop10(begin,end));
    }

    @GetMapping("/export")
    @ApiOperation("导出运营数据报表")
    public void export(HttpServletResponse response) {
        reportService.exportBusinessData(response);
    }
}
