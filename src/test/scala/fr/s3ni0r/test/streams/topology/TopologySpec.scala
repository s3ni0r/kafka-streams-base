package fr.s3ni0r.test.streams.topology

import java.util.Properties
import java.util.concurrent.TimeUnit

import com.madewithtea.mockedstreams.MockedStreams
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.kstream.{JoinWindows, KStream, Produced}
import org.apache.kafka.streams.{Consumed, KeyValue, StreamsBuilder, StreamsConfig}
import org.apache.kafka.streams.processor.WallclockTimestampExtractor
import org.scalatest.{BeforeAndAfterAll, FreeSpec, Matchers}

class TopologySpec extends FreeSpec with Matchers with BeforeAndAfterAll {
  val streamsConfiguration = new Properties()

  override def beforeAll(): Unit = {
    super.beforeAll()
    streamsConfiguration.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass.getName)
    streamsConfiguration.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass.getName)
    streamsConfiguration.put(StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG, classOf[WallclockTimestampExtractor])
  }


  "Test topology" - {
    "shoud be created successfully" in {
      def int(i: Int) = Integer.valueOf(i)

      val inputTopicA = Seq(
        ("a", "Hello"),
        ("a", "World!"),
        ("b", "Kafka"),
        ("b", "Test")
      )
      val expectedTopicA = Seq(("a", "HELLO"), ("a", "WORLD!"))
      val expectedTopicB = Seq(("b", "KAFKA"), ("b", "TEST"))
      val stringSerdes = Serdes.String()
      val consumedWith = Consumed.`with`(stringSerdes, stringSerdes)
      val producedWith = Produced.`with`(stringSerdes, stringSerdes)

      def upperTopology(builder: StreamsBuilder) = {
        val List(a0, a1) = builder
          .stream("src-topic", consumedWith)
          .branch((k, v) ⇒ k == "a", (k, v) ⇒ k == "b").toList
        a0.map[String, String]((k, v) ⇒ new KeyValue(k.toUpperCase, v.toUpperCase))
          .peek((k, v) ⇒ println(s"$k => $v"))
          .to("out-topic", producedWith)
        a1.map[String, String]((k, v) ⇒ new KeyValue(k.toUpperCase, v.toUpperCase))
          .peek((k, v) ⇒ println(s"$k => $v"))
          .to("out-topic", producedWith)
      }

      val stream = MockedStreams()
        .topology(upperTopology)
        .input("src-topic", stringSerdes, stringSerdes, inputTopicA)
        .output("out-topic", stringSerdes, stringSerdes, expectedTopicA.size * 2)

      stream shouldEqual expectedTopicA

    }


    "should Join two streams correctly" in {
      val inputTopicA = Seq(
        ("aa", "Hello"),
        ("bb", "World!"),
        ("cc", "Kafka"),
        ("dd", "Test")
      )

      val inputTopicB = Seq(
        ("ee", "Hello"),
        ("aa", "World!"),
        ("bb", "Kafka"),
        ("jj", "Test")
      )

      val stringSerdes = Serdes.String()
      val consumedWith = Consumed.`with`(stringSerdes, stringSerdes)
      val producedWith = Produced.`with`(stringSerdes, stringSerdes)

      def joinTopology(builder: StreamsBuilder): Unit = {
        val stream1: KStream[String, String] = builder.stream("stream1")
        val stream2: KStream[String, String] = builder.stream("stream2")

        stream1
          .join(
            stream2,
            (b: String, c: String) ⇒ s"$b => $c",
            JoinWindows.of(TimeUnit.MINUTES.toMillis(5))
          )
          .to("output-topic")
      }

      val streams = MockedStreams()
        .topology(joinTopology)
        .config(streamsConfiguration)
        .input("stream1", stringSerdes, stringSerdes, inputTopicA)
        .input("stream2", stringSerdes, stringSerdes, inputTopicB)
        .output("output-topic", stringSerdes, stringSerdes, inputTopicA.length)

      streams.map(println)
    }
  }
}
