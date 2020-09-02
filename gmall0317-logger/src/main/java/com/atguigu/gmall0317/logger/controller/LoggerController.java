package com.atguigu.gmall0317.logger.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


/**
 * @author lishiwen
 * @create 2020-08-24 21:20
 */
/*
    @RestController 类上  标识类是一个controller服务  返回字符串
    @Controller     类上  标识类是一个controller服务  返回网页
    @RequestMapping("/applog")  方法  定义请求路径  让指定请求路径进入指定方法
    @RequestBody                通过请求体，隐式的传递参数，参数值比较长


    logback使用
        Java    利用lombok    用@slf4j 创建log.info  debug   输出日志

        logback.xml
            appender  定义appender的输出方式   控制台  文件  消息队列，输出格式
            logger    如何调用appender，用什么级别日志[debug、 info、 warn、 error]

    kafka
        springboot  KafkaTemplate工具 非常简单的构造producer 可以直接从application.properties加载配置
        kafka的分区，   可以在kafka/config/server.properties中， num.perprotions默认值，重启生效。
 */
@RestController
@Slf4j
public class LoggerController {

    @Autowired //对某个接口或者类进行自动装配 单例模式
    KafkaTemplate kafkaTemplate;

    //测试方法
    @RequestMapping("/hello")
    public String sayHello(@RequestParam("name") String name){
        System.out.println("你好");
        return "你好,"+name;
    }

    //todo 日志落盘
    @RequestMapping("/applog")
    public String applog(@RequestBody String logString){
//        System.out.println(logString);
        //1 logfile log4j logback log4j2 logging...
        log.info(logString);//日志级别 [trace] debug info warn error [fatal]
        //2 发送kafka
        JSONObject jsonObject = JSON.parseObject(logString);
        if (jsonObject.getString("start")!=null && jsonObject.getString("start").length() > 0){
            kafkaTemplate.send("GMALL0317_STARTUP",logString);//kafka可以自动建立topic 参数使用默认值
        } else {
            kafkaTemplate.send("GMALL0317_EVEN",logString);
        }

        return "success 0317";
    }
}
