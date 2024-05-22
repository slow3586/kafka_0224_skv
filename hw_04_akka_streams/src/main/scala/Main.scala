import akka.actor.ActorSystem
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.kafka.{ConsumerSettings, ProducerSettings, Subscriptions}
import akka.stream.ClosedShape
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, RunnableGraph, Sink, Source, ZipN}
import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.admin.{Admin, AdminClientConfig, NewTopic}
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}

import java.util
import scala.concurrent.ExecutionContextExecutor
import scala.language.postfixOps

object Main extends App {
  private val HOST = "kafka"
  private val PORT0 = HOST + ":9091"
  private val PORT1 = HOST + ":9092"
  
  implicit val SYSTEM: ActorSystem = ActorSystem()
  implicit val EXECUTOR: ExecutionContextExecutor = SYSTEM.dispatcher
  
  private val TOPIC = "TOPIC_NAME"
  private val SUBSCRIPTION = Subscriptions.topics(TOPIC)
  private val PRODUCER_SETTINGS: ProducerSettings[String, String] = ProducerSettings(
    SYSTEM,
    new StringSerializer,
    new StringSerializer
  ).withBootstrapServers(PORT0)
  private val CONSUMER_SETTINGS: ConsumerSettings[String, String] = ConsumerSettings(
    SYSTEM,
    new StringDeserializer,
    new StringDeserializer
  ).withBootstrapServers(PORT1)
    .withGroupId("GROUP_ID")
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
  
  println("Creating topic!")
  val admin: Admin = Admin.create(util.Map.of(
    AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, PORT0.asInstanceOf[Object]))
  admin.createTopics(util.List.of(new NewTopic(
    TOPIC,
    1,
    1.toShort)))
  admin.close()
  println("Topic created!")
  
  Thread.sleep(1000)
  
  println("Starting consumer!")
  RunnableGraph.fromGraph(GraphDSL.create() {
    implicit builder =>
      val input = builder.add(Consumer.plainSource(CONSUMER_SETTINGS, SUBSCRIPTION))
      val recordMap = builder.add(Flow[ConsumerRecord[String, String]].map(r => r.value().toInt))
      
      val broadcast = builder.add(Broadcast[Int](3))
      val zip = builder.add(ZipN[Int](3))
      
      val map0 = builder.add(Flow[Int].map(x => x * 10))
      val map1 = builder.add(Flow[Int].map(x => x * 2))
      val map2 = builder.add(Flow[Int].map(x => x * 3))
      
      val sum = builder.add(Flow[Seq[Int]].map(v => v.sum))
      
      val sink = builder.add(Sink.foreach(println))
      
      input ~> recordMap ~> broadcast
      
      broadcast.out(0) ~> map0 ~> zip.in(0)
      broadcast.out(1) ~> map1 ~> zip.in(1)
      broadcast.out(2) ~> map2 ~> zip.in(2)
      
      zip.out ~> sum ~> sink
      
      ClosedShape
  }).run
  
  Thread.sleep(1000)
  
  println("Running producer!")
  RunnableGraph.fromGraph(GraphDSL.create() {
    implicit builder =>
      builder.add(Source(1 to 15)) ~>
        builder.add(Flow[Int].map(i => new ProducerRecord[String, String](TOPIC, i.toString))) ~>
        Producer.plainSink(PRODUCER_SETTINGS)
      
      ClosedShape
  }).run
  
  Thread.sleep(120_000)
}