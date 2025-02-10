package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Component
@Slf4j
public class MyTask {

    @Autowired
    private OrderMapper orderMapper;

    @Scheduled(cron = "0 * * * * ?")
    public void processTimeoutOrder(){
        log.info("处理超时订单:{}", LocalDateTime.now());
        Integer STATUS = Orders.PAID;
        LocalDateTime time = LocalDateTime.now().plusMinutes(-15);
        List<Orders> list = orderMapper.getByStatusAndOrderTime(STATUS, time);
        if(list != null && !list.isEmpty()){
            for (Orders order : list) {
                order.setStatus(Orders.CANCELLED);
                order.setCancelReason("订单超时，自动取消");
                order.setCancelTime(LocalDateTime.now());
                orderMapper.update(order);
            }
        }
    }

    @Scheduled(cron = "0 0 1 * * ?")
    public void processDeliveryOrder(){
        log.info("定时处理处于派送中的订单:{}", LocalDateTime.now());
        Integer STATUS = Orders.DELIVERY_IN_PROGRESS;
        LocalDateTime time = LocalDateTime.now().plusMinutes(-60);
        List<Orders> list = orderMapper.getByStatusAndOrderTime(STATUS, time);
        if(list != null && !list.isEmpty()){
            for (Orders order : list) {
                order.setStatus(Orders.COMPLETED);
                orderMapper.update(order);
            }
        }
    }

}
