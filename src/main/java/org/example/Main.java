package org.example;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import picocli.CommandLine;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.Callable;

// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.
@Slf4j
@CommandLine.Command(name = "testProducer", description = "Test producer timeouts")
public class Main implements Callable<Integer> {
    @CommandLine.Option(names = {"-w", "--wait"}, description = "Wait time")
    private int waitTime = 100;

    @CommandLine.Option(names = {"-c", "--config"}, description = "Configuration file")
    private File configFile;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    public Integer call() throws Exception {
        Properties p = new Properties();

        InputStream defaultsFileStream = null;
        InputStream configFileStream = null;

        try {
            // Load the properties file
            defaultsFileStream = Main.class.getClassLoader().getResourceAsStream("config.properties");
            p.load(defaultsFileStream);

            if (configFile != null) {
                configFileStream = new FileInputStream(configFile);
                p.load(configFileStream);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (defaultsFileStream != null) {
                try {
                    defaultsFileStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (configFileStream != null) {
                try {
                    configFileStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        KafkaProducer<String, String> producer = new KafkaProducer<>(p);
        int i = 0;

        while (true) {
            i+=1;

            try {
                producer.send(new ProducerRecord<>("chaz-test", String.valueOf(i), "world"), (RecordMetadata metadata, Exception e) -> {
                    try {
                        Thread.sleep(10_000);
                    } catch (InterruptedException interrupted) {
                        throw new RuntimeException(interrupted);
                    }

                    if (e != null) {
                        throw new RuntimeException(e);
                    }

                    log.info("Processing record with offset {} in partition {}", metadata.offset(), metadata.partition());
                });

                Thread.sleep(waitTime);
            } catch (Exception e) {
                producer.close();
                throw new RuntimeException("Producer blocked at record number " + i, e);
            }
        }
    }
}