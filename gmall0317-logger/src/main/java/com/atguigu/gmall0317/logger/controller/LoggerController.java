package com.atguigu.gmall0317.logger.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


/**
 * @author lishiwen
 * @create 2020-08-24 21:20
 */
@RestController
@Slf4j
public class LoggerController {

    @Autowired //
    KafkaTemplate kafkaTemplate;

    @RequestMapping("/hello")
    public String sayHello(@RequestParam("name") String name){
        System.out.println("你好");
        return "你好,"+name;
    }

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
