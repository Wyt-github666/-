package com.sky.aspect;


import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

// 自定义切面
@Aspect
@Component
@Slf4j
public class AutoFillAspect {

    // 切入点
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    public void autoFillPointcut(){}

    // 前置通知
    @Before("autoFillPointcut()")
    public void autoFill(JoinPoint joinPoint){
        log.info("Auto fill start");
        // 获取到当前被拦截的方法上的数据库操作类型
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class);
        OperationType operationType = autoFill.value();  // 获取操作类型
        // 获取当前被拦截的方法的参数-实体对象
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0){
            return;
        }
        Object entry = args[0];
        // 准备赋值的数据
        LocalDateTime now = LocalDateTime.now();
        Long currentid = BaseContext.getCurrentId();
        // 根据当前不同的操作类型，为对应的属性通过反射来赋值
        if(operationType == OperationType.INSERT){
            // 四个公共字段进行赋值
            try {
                Method setCreaTime = entry.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME,LocalDateTime.class);
                Method setCreaUser = entry.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER,Long.class);
                Method setUpdateTime = entry.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME,LocalDateTime.class);
                Method setUpdateUser = entry.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER,Long.class);

                // 通过反射，来为对象进行赋值
                setCreaTime.invoke(entry,now);
                setCreaUser.invoke(entry,currentid);
                setUpdateTime.invoke(entry,now);
                setUpdateUser.invoke(entry,currentid);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else if(operationType == OperationType.UPDATE){
            // 为两个公共字段进行赋值
            try {
                Method setUpdateTime = entry.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME,LocalDateTime.class);
                Method setUpdateUser = entry.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER,Long.class);
                // 通过反射，来为对象进行赋值
                setUpdateTime.invoke(entry,now);
                setUpdateUser.invoke(entry,currentid);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
