package com.atguigu.gmall0317.realtime.app

import java.lang
import java.text.SimpleDateFormat
import java.util.Date

import com.alibaba.fastjson.{JSON, JSONObject}
import com.atguigu.gmall0317.realtime.util.{MyKafkaUtil, OffsetManager, RedisUtil}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import org.apache.spark.SparkConf
import org.apache.spark.streaming.dstream.{DStream, InputDStream}
import org.apache.spark.streaming.kafka010.{HasOffsetRanges, OffsetRange}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import redis.clients.jedis.Jedis

import scala.collection.mutable.ListBuffer

/**
 * @author lishiwen
 * @create 2020-08-25 21:43
 *
 * dau= day alive user
 * uv=user view
 * pv=page view
 */
object DauApp {

  def main(args: Array[String]): Unit = {
    val sparkConf: SparkConf = new SparkConf().setAppName("dau_app").setMaster("local[4]")
    val ssc = new StreamingContext(sparkConf,Seconds(5))
    val topic = "GMALL0317_STARTUP"
    val groupId = "dau_app_group"

    val offsetMap: Map[TopicPartition, Long] = OffsetManager.getOffset(topic,groupId)
    var inputDstream: InputDStream[ConsumerRecord[String, String]] = null
    if (offsetMap!=null){
      inputDstream = MyKafkaUtil.getKafkaStream(topic,ssc,offsetMap,groupId)
    }else{
      inputDstream = MyKafkaUtil.getKafkaStream(topic,ssc,groupId)
    }

    //从流中rdd获得偏移量的结束点 数组
    var offsetRanges: Array[OffsetRange] = null
    val inputWithOffsetDstream: DStream[ConsumerRecord[String, String]] = inputDstream.transform { rdd =>
      offsetRanges = rdd.asInstanceOf[HasOffsetRanges].offsetRanges
      rdd
    }


//    inputDstream.map(_.value()).print(100)


    //前置处理，1，结构化 2，日期  3，小时
    val jsonObjDstream: DStream[JSONObject] = inputWithOffsetDstream.map { record =>
      val jsonString: String = record.value()
      val jsonObj: JSONObject = JSON.parseObject(jsonString)
      val ts: lang.Long = jsonObj.getLong("ts")
      val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH")
      val dateTimeString: String = dateFormat.format(new Date(ts))
      val dateTimeArr: Array[String] = dateTimeString.split(" ")
      val dt: String = dateTimeArr(0)
      val hr: String = dateTimeArr(1)
      jsonObj.put("dt", dt)
      jsonObj.put("hr", hr)
      jsonObj
    }

    val filteredDStream: DStream[JSONObject] = jsonObjDstream.mapPartitions { jsonObjItr =>
      val jedis: Jedis = RedisUtil.getJedisClient //连接池
      val jsonList: List[JSONObject] = jsonObjItr.toList
      println("过滤前："+jsonList.size)
      val fileredList = new ListBuffer[JSONObject]

      for (jsonObj <- jsonList) {
        val dauKey = "dau:" + jsonObj.get("dt")
        val mid = jsonObj.getJSONObject("common").getString("mid")
        val ifNonExists: lang.Long = jedis.sadd(dauKey, mid)

        if (ifNonExists == 1) {
          fileredList+=jsonObj
        }
      }
      jedis.close()
      println("过滤后："+jsonList.size)

      fileredList.toIterator
    }


    //数据中，凡是今天启动过的用户（mid）进行记录，同时进行筛选，如果之前访问过，则过滤掉
    //    val jsonObjFilteredDStream: DStream[Boolean] = jsonObjDstream.map { jsonObj =>
    //      //Redis  type?  string set hash zset list   key?   value
    //      //string  key?   dau:[date]:mid:[mid]   value1/0  api?   setnx 如果有需求：查询今天所有的mid keys dau
    //      //set     key?   dau:[date]     value?  [mid]     api? sadd
    //      //hash    key?   dau:[date]     value? [mid]:1   api? hash  可以但没有必要
    //      //zset    key?/  dau:[date]     value?  [mid] score  api?  zadd   可以但没必要吗？
    //
    ////      val jedis: Jedis = new Jedis("hadoop102", 6379) //连接池
    //      val jedis: Jedis = RedisUtil.getJedisClient //连接池

    //      val dauKey = "dau:" + jsonObj.get("dt")
    //      val mid = jsonObj.getJSONObject("common").getString("mid")
    //      val ifNonExists: lang.Long = jedis.sadd(dauKey, mid)
    //      jedis.close()
    //      if (ifNonExists == 1) {
    //        true
    //      } else {
    //        false
    //      }

    //    }
    //    jsonObjFilteredDStream


    filteredDStream.print(100)


    ssc.start()
    ssc.awaitTermination()
  }
}
