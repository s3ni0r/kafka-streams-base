package fr.s3ni0r.test.streams

import java.util.Properties
import java.util.concurrent.TimeUnit

import fr.s3ni0r.test.utils.{KafkaLocalServer, MessageListener, MessageSender, RecordProcessorTrait}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization._
import org.apache.kafka.streams.kstream._
import org.apache.kafka.streams.processor.WallclockTimestampExtractor
import org.apache.kafka.streams.{KafkaStreams, StreamsBuilder, StreamsConfig}
import org.scalatest.{BeforeAndAfterAll, FreeSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StreamSpec extends FreeSpec with Matchers with BeforeAndAfterAll {
  var kafkaServer = null.asInstanceOf[KafkaLocalServer]
  val inputTopic1 = "stream-1"
  val inputTopic2 = "stream-2"
  val outputTopic = "output"
  val broker = "localhost:9092"
  val clientId = "streamJoin"

  override def beforeAll(): Unit = {
    super.beforeAll()
    kafkaServer = KafkaLocalServer(true, Some("local_state_data"))
    kafkaServer.start()
  }

  "Testing Kafka should be fun" - {
    "join two streams should work" in {
      //Create needed topics
      kafkaServer.createTopic(inputTopic1)
      kafkaServer.createTopic(inputTopic2)
      kafkaServer.createTopic(outputTopic)
      //Configure Serdes
      val intSerdes = Serdes.Integer()
      val stringSerdes = Serdes.String()
      //Configure properties
      val streamsConfiguration = new Properties()
      streamsConfiguration.put(StreamsConfig.APPLICATION_ID_CONFIG, "stream-joiner")
      streamsConfiguration.put(StreamsConfig.CLIENT_ID_CONFIG, clientId)
      streamsConfiguration.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, broker)
      streamsConfiguration.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, stringSerdes.getClass.getName)
      streamsConfiguration.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, stringSerdes.getClass.getName)
      streamsConfiguration.put(StreamsConfig.STATE_DIR_CONFIG, "local_state_data")
      streamsConfiguration.put(StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG, classOf[WallclockTimestampExtractor])
      streamsConfiguration.put(StreamsConfig.REPLICATION_FACTOR_CONFIG, "1")
      //Build
      val builder = new StreamsBuilder()
      val textLines1: KStream[String, String] = builder.stream(inputTopic1)
      val textLines2: KStream[String, String] = builder.stream(inputTopic2)

      val joined = textLines1.join(
        textLines2,
        (lv: String, rv: String) ⇒ s"$lv => $rv",
        JoinWindows.of(TimeUnit.MINUTES.toMillis(5)),
        Serdes.String(),
        Serdes.String(),
        Serdes.String()
      )
      joined.to(outputTopic)
      val streams = new KafkaStreams(builder.build, streamsConfiguration)
      streams.start()
      // produce data to input topics
      val sender = MessageSender[String, String](broker, classOf[StringSerializer].getName, classOf[StringSerializer].getName)

      Future {
        1 to 1000 map {
          e ⇒
            Thread.sleep(50)
            sender.writeKeyValue(inputTopic1, e.toString, s"element 1")
        }
      }

      Future {
        1 to 1000 map {
          e ⇒
            Thread.sleep(50)
            sender.writeKeyValue(inputTopic2, e.toString, s"element 2")
        }
      }

      // listen to output topic
      val listener = MessageListener(
        broker,
        outputTopic,
        clientId,
        classOf[StringDeserializer].getName,
        classOf[StringDeserializer].getName,
        new RecordProcessor
      )


      //      listener.readKeyValues(1000).map(println)
      val t = listener.waitUntilMinKeyValueRecordsReceived(1000, 50000)

      streams.close()
    }

    class RecordProcessor extends RecordProcessorTrait[String, Long] {
      override def processRecord(record: ConsumerRecord[String, Long]): Unit = {
        println(s"Get Message $record")
      }
    }
  }

  override def afterAll(): Unit = {
    super.afterAll()
    kafkaServer.stop()
  }
}
