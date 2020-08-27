package com.atguigu.gmall0317.realtime.util

import java.io.InputStreamReader
import java.util.Properties


/**
 * @author lishiwen
 * @create 2020-08-25 21:51
 */
object PropertiesUtil {

  def main(args: Array[String]): Unit = {
    val properties: Properties = PropertiesUtil.load("config.properties")

    println(properties.getProperty("kafka.broker.list"))
  }

  def load(propertieName:String): Properties ={
    val prop=new Properties();
    prop.load(new InputStreamReader(Thread.currentThread().getContextClassLoader.getResourceAsStream(propertieName) , "UTF-8"))
    prop
  }

}
