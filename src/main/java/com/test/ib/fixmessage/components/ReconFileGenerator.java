package com.test.ib.fixmessage.components;

import com.test.ib.fixmessage.dto.Aggregator;
import com.test.ib.fixmessage.dto.Execution;
import com.test.ib.fixmessage.utils.FileManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import quickfix.FieldNotFound;
import quickfix.Message;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Component
@Order(3)
@Slf4j
public class ReconFileGenerator implements CommandLineRunner {

    private final DecimalFormat df2 = new DecimalFormat("#.##");

    @Autowired
    private FileManager fileManager;

    @Override
    public void run(String... args) throws Exception {

        // generate data
        Instant start = Instant.now();
        Map<Aggregator, Double> aggregatedFulFill = generateAggregatedFullFill();
        Map<Aggregator, Double> aggregatedAllMsgs = generateAggregatedAllMsgs();
        Instant finish = Instant.now();
        long timeElapsed = Duration.between(start, finish).toMillis();
        log.info(String.format("ReconFileGenerator : data generation : timeElapsed : %d ms", timeElapsed));

        // create recon file
        start = Instant.now();
        createReconFile(aggregatedAllMsgs, aggregatedFulFill);
        finish = Instant.now();
        timeElapsed = Duration.between(start, finish).toMillis();
        log.info(String.format("ReconFileGenerator : recon file creation : %s : timeElapsed : %d ms",
                System.getProperty("java.io.tmpdir")+"/recon.csv", timeElapsed));
    }

    private void createReconFile(Map<Aggregator, Double> aggregatedAllMsgs, Map<Aggregator, Double> aggregatedFulFill) throws IOException {

        if (aggregatedFulFill.size() != aggregatedAllMsgs.size()) {
            throw new InputMismatchException("Data files are not correct... Please investigate...");
        }

        String tDir = System.getProperty("java.io.tmpdir");
        File file = new File(tDir, "recon.csv");

        List<String> recon = new ArrayList<>();
        recon.add("TxtConta,TxtPapel,TxtLado,TxtPrecoMedio,,CsvConta,CsvPapel,CsvLado,CsvPrecoMedio");
        for (Map.Entry<Aggregator, Double> entry1 : aggregatedAllMsgs.entrySet()) {
            Aggregator key = entry1.getKey();
            Double value1 = entry1.getValue();
            Double value2 = aggregatedFulFill.get(key);
            String sb = key.getAccount() + "," + key.getInstrument() + "," + key.getSide() +
                    "," + df2.format(value1) + ",," +
                    key.getAccount() + "," + key.getInstrument() + "," + key.getSide() +
                    "," + df2.format(value2);
            recon.add(sb);
        }
        FileManager.writeBufferedCSV(recon, "recon.csv");
    }

    private Map<Aggregator, Double> generateAggregatedAllMsgs() throws IOException {

        String tDir = System.getProperty("java.io.tmpdir");
        File file = new File(tDir, "AllMsgs.csv");

        List<Execution> executionsFix = fileManager.readExecutionsFromFile(file);
        Map<Aggregator, Double> result = new HashMap<>();
        Map<Aggregator, Integer> counter = new HashMap<>();
        for (Execution exec : executionsFix) {

            if (exec.getOrderQuantity() > exec.getAccumulatedExecutedQuantity()) {
                continue;
            }

            Aggregator key = Aggregator.builder()
                    .account(exec.getAccount())
                    .instrument(exec.getInstrument())
                    .side(exec.getSide())
                    .build();
            updateAggregationMap(result, counter, key, exec.getExecutionPrice());
        }
        return result;
    }

    private Map<Aggregator, Double> generateAggregatedFullFill() throws IOException, FieldNotFound {
        String tDir = System.getProperty("java.io.tmpdir");
        File file = new File(tDir, "FulFill.txt");

        List<Message> executionsFix = fileManager.readFixExecutionsFromFile(file);
        Map<Aggregator, Double> result = new HashMap<>();
        Map<Aggregator, Integer> counter = new HashMap<>();
        for (Message message : executionsFix) {
            Aggregator key = Aggregator.builder()
                    .account(message.getString(1))
                    .instrument(message.getString(55))
                    .side(message.getChar(54))
                    .build();
            updateAggregationMap(result, counter, key, message.getDouble(6));
        }
        return result;
    }

    private void updateAggregationMap(Map<Aggregator, Double> result, Map<Aggregator, Integer> counter,
                                      Aggregator key, Double avgPx) {
        if (!result.containsKey(key)) {
            result.put(key, avgPx);
            counter.put(key, 1);
        } else {
            //  ('previous mean' * '(count -1)') + 'new value') / 'count'
            // https://math.stackexchange.com/questions/106700/incremental-averageing
            double previous = result.get(key);
            int count = counter.get(key) + 1;
            double actual = avgPx;
            double avg = ((previous * (count - 1)) + actual) / count;
            result.put(key, avg);
            counter.put(key, count);
        }
    }

}
